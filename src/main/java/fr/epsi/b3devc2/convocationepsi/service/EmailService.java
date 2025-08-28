package fr.epsi.b3devc2.convocationepsi.service;

import fr.epsi.b3devc2.convocationepsi.dto.CandidatDto;
import fr.epsi.b3devc2.convocationepsi.dto.SendEmailRequestDto;
import fr.epsi.b3devc2.convocationepsi.storage.InMemorySessionStorage;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final InMemorySessionStorage sessionStorage;
    private final ZipService zipService;

    @Value("${mail.sender.default}")
    private String defaultSender;


    /**
     * Envoie les convocations par email aux candidats d'une session
     * @param request Contient l'ID de la session, le label de l'examen et les emails en copie
     */
    public void sendConvocationsByEmail(SendEmailRequestDto request) {
        String sessionId = request.getSessionId();
        List<CandidatDto> candidats = sessionStorage.getCandidats(sessionId);

        if (candidats == null || candidats.isEmpty()) {
            throw new IllegalArgumentException("Aucun candidat trouvé pour la session");
        }

        byte[] zip = sessionStorage.getFile(sessionId);
        if (zip == null) {
            throw new IllegalStateException("Aucune convocation générée pour cette session.");
        }

        Map<CandidatDto, byte[]> convocations = null;
        try {
            convocations = zipService.extractFilesFromZip(zip, candidats);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (Map.Entry<CandidatDto, byte[]> entry : convocations.entrySet()) {
            sendConvocationEmail(entry.getKey(), entry.getValue(), request.getExamenLabel(), request.getCcEmails());
        }
    }

    /**
     * Envoie un email de convocation à un candidat avec le PDF en pièce jointe
     */
    public void sendConvocationEmail(CandidatDto candidat, byte[] pdfFile, String examenLabel, List<String> ccEmails) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(defaultSender);
            helper.setTo(candidat.getEmail());

            if (ccEmails != null && !ccEmails.isEmpty()) {
                helper.setCc(ccEmails.toArray(new String[0]));
            }

            helper.setSubject("Convocation – " + examenLabel);
            helper.setText(createEmailContent(), true);

            String fileName = String.format("Convocation_%s_%s.pdf",
                    candidat.getNom().replaceAll("[^a-zA-Z0-9]", "_"),
                    candidat.getPrenom().replaceAll("[^a-zA-Z0-9]", "_"));

            helper.addAttachment(fileName, new ByteArrayResource(pdfFile));

            mailSender.send(message);
            log.info("Email envoyé à {}", candidat.getEmail());

        } catch (Exception e) {
            log.error("Erreur lors de l’envoi à {}: {}", candidat.getEmail(), e.getMessage());
        }
    }

    /**
     * Contenu de l'email en HTML
     */
    private String createEmailContent() {
        return """
        <html>
        <body>
            <p>Bonjour,</p>
            <p>Tu trouveras, ci-joint, ta convocation.</p>
            <p>Bonne journée.</p>
            <br>
            <br>
            <br>
            <br>
            <div>
            <div>
               <div style='display: flex; align-items: center;'>
                <p><strong>Cordialement</strong></p>
                <p><strong>Ecoles EPSI-WIS</strong></p>
                <p><strong>L'équipe pédagogique</strong></p>
                <p>02 40 76 60 87</p>
                <p>pedagogie@campus-cd.com</p>
                <p>16, boulevard Général de Gaulle, 44200 Nantes</p>
               </div>
                <div>
                    <img src='data:image/png;base64,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' alt='EPSI-WIS' style='height: 50px; margin-right: 10px;'/>
                </div>
            </div>
        </body>
        </html>
        """;
    }
}
