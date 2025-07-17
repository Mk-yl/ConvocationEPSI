package fr.epsi.b3devc2.convocationepsi.service;

import fr.epsi.b3devc2.convocationepsi.dto.RecipientDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.io.UnsupportedEncodingException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    @Value("${app.convocation.from-email}")
    private String fromEmail;

    @Value("${app.convocation.from-name}")
    private String fromName;

    @Value("${app.convocation.subject}")
    private String defaultSubject;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public void sendConvocation(RecipientDTO recipient, byte[] convocationDocument) throws MessagingException, UnsupportedEncodingException {
        log.info("Envoi de convocation à: {}", recipient.getEmail());

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail, fromName);
        helper.setTo(recipient.getEmail());
        helper.setSubject(generateSubject(recipient));
        helper.setText(generateEmailContent(recipient), true);

        // Ajouter la convocation en pièce jointe
        String fileName = String.format("Convocation_%s_%s.docx",
                recipient.getNom(), recipient.getPrenom());
        helper.addAttachment(fileName, new ByteArrayResource(convocationDocument));

        mailSender.send(message);
        log.info("Convocation envoyée avec succès à: {}", recipient.getEmail());
    }

    public CompletableFuture<Void> sendConvocationAsync(RecipientDTO recipient, byte[] convocationDocument) {
        return CompletableFuture.runAsync(() -> {
            try {
                sendConvocation(recipient, convocationDocument);
            } catch (MessagingException | UnsupportedEncodingException e) {
                log.error("Erreur lors de l'envoi de la convocation à {}: {}", recipient.getEmail(), e.getMessage());
                throw new RuntimeException("Erreur envoi email", e);
            }
        }, executorService);
    }

    public CompletableFuture<Void> sendBulkConvocations(List<RecipientDTO> recipients,
                                                        java.util.Map<Long, byte[]> documents) {
        log.info("Envoi en masse de {} convocations", recipients.size());

        List<CompletableFuture<?>> futures = recipients.stream()
                .map(recipient -> {
                    byte[] document = documents.get(recipient.getId());
                    if (document != null) {
                        return sendConvocationAsync(recipient, document);
                    } else {
                        log.warn("Document non trouvé pour le destinataire: {}", recipient.getEmail());
                        return CompletableFuture.completedFuture(null);
                    }
                })
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> log.info("Tous les emails ont été traités"));
    }

    private String generateSubject(RecipientDTO recipient) {
        return String.format("%s - %s",
                defaultSubject,
                recipient.getDatePassage().format(DATE_FORMATTER));
    }

    private String generateEmailContent(RecipientDTO recipient) {
        StringBuilder content = new StringBuilder();

        content.append("<html><body>");
        content.append("<h2>Convocation à l'examen</h2>");

        content.append("<p>Bonjour ").append(recipient.getFullName()).append(",</p>");

        content.append("<p>Vous êtes convoqué(e) en tant que <strong>")
                .append(recipient.getType().getDisplayName())
                .append("</strong> pour l'examen aux détails suivants :</p>");

        content.append("<div style='border: 1px solid #ccc; padding: 15px; margin: 10px 0; background-color: #f9f9f9;'>");
        content.append("<h3>Détails de la convocation</h3>");
        content.append("<ul>");
        content.append("<li><strong>Date :</strong> ").append(recipient.getDatePassage().format(DATE_FORMATTER)).append("</li>");
        content.append("<li><strong>Heure :</strong> ").append(recipient.getHeurePassage().format(TIME_FORMATTER)).append("</li>");
        content.append("<li><strong>Salle :</strong> ").append(recipient.getSalle()).append("</li>");

        if (recipient.getClasse() != null && !recipient.getClasse().isEmpty()) {
            content.append("<li><strong>Classe :</strong> ").append(recipient.getClasse()).append("</li>");
        }

        if (recipient.getGroupe() != null && !recipient.getGroupe().isEmpty()) {
            content.append("<li><strong>Groupe :</strong> ").append(recipient.getGroupe()).append("</li>");
        }

        content.append("</ul>");
        content.append("</div>");

        if (recipient.getType() ==  fr.epsi.b3devc2.convocationepsi.model.enums.RecipientType.CANDIDAT) {
            content.append("<div style='border-left: 4px solid #007bff; padding-left: 15px; margin: 15px 0;'>");
            content.append("<h4>Instructions pour le candidat</h4>");
            content.append("<ul>");
            content.append("<li>Vous devez vous présenter <strong>15 minutes avant</strong> l'heure indiquée</li>");
            content.append("<li>Munissez-vous d'une <strong>pièce d'identité valide</strong></li>");
            content.append("<li>Apportez votre matériel autorisé</li>");
            content.append("<li>Respectez les consignes de l'établissement</li>");
            content.append("</ul>");
            content.append("</div>");
        } else {
            content.append("<div style='border-left: 4px solid #28a745; padding-left: 15px; margin: 15px 0;'>");
            content.append("<h4>Instructions pour le jury</h4>");
            content.append("<ul>");
            content.append("<li>Vous devez vous présenter <strong>30 minutes avant</strong> l'heure indiquée</li>");
            content.append("<li>Consultez les dossiers des candidats en amont</li>");
            content.append("<li>Respectez les critères d'évaluation</li>");
            content.append("</ul>");
            content.append("</div>");
        }

        content.append("<p>Vous trouverez votre convocation officielle en pièce jointe.</p>");

        content.append("<p style='color: #dc3545;'><strong>Important :</strong> En cas d'empêchement, veuillez contacter l'administration dans les plus brefs délais.</p>");

        content.append("<p>Cordialement,<br/>");
        content.append("<strong>L'Administration</strong></p>");

        content.append("</body></html>");

        return content.toString();
    }

    public void shutdown() {
        executorService.shutdown();
    }


}
