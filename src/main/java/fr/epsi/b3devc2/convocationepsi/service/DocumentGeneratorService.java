package fr.epsi.b3devc2.convocationepsi.service;


import fr.epsi.b3devc2.convocationepsi.dto.RecipientDTO;
import lombok.extern.slf4j.Slf4j;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class DocumentGeneratorService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public byte[] generateConvocationDocx(RecipientDTO recipient) throws IOException, Docx4JException {
        log.debug("Génération de convocation pour: {}", recipient.getFullName());

        // Créer un nouveau document Word
        WordprocessingMLPackage wordPackage = WordprocessingMLPackage.createPackage();
        MainDocumentPart mainDocumentPart = wordPackage.getMainDocumentPart();

        // Créer le contenu de la convocation
        String convocationContent = generateConvocationContent(recipient);

        // Ajouter le contenu au document
        mainDocumentPart.addParagraphOfText(convocationContent);

        // Convertir en byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        wordPackage.save(outputStream);

        log.debug("Document généré avec succès pour: {}", recipient.getFullName());
        return outputStream.toByteArray();
    }

    public byte[] generateConvocationDocxWithTemplate(RecipientDTO recipient, String templatePath) throws IOException, Docx4JException {
        log.debug("Génération de convocation avec template pour: {}", recipient.getFullName());

        try {
            // Charger le template (si disponible)
            // Pour cette implémentation, on utilisera un template par défaut
            WordprocessingMLPackage wordPackage = WordprocessingMLPackage.createPackage();
            MainDocumentPart mainDocumentPart = wordPackage.getMainDocumentPart();

            // Créer le contenu personnalisé
            String content = generateConvocationContent(recipient);

            // Remplacer les variables dans le template
            Map<String, String> variables = createVariablesMap(recipient);
            String processedContent = replaceVariables(content, variables);

            // Ajouter le contenu au document
            mainDocumentPart.addParagraphOfText(processedContent);

            // Convertir en byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            wordPackage.save(outputStream);

            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Erreur lors de la génération du document avec template: {}", e.getMessage());
            // Fallback vers la génération simple
            return generateConvocationDocx(recipient);
        }
    }

    private String generateConvocationContent(RecipientDTO recipient) {
        StringBuilder content = new StringBuilder();

        content.append("CONVOCATION À L'EXAMEN\n\n");
        content.append("-------------------------------------------\n\n");

        content.append("Destinataire: ").append(recipient.getFullName()).append("\n");
        content.append("Email: ").append(recipient.getEmail()).append("\n");

        if (recipient.getClasse() != null && !recipient.getClasse().isEmpty()) {
            content.append("Classe: ").append(recipient.getClasse()).append("\n");
        }

        if (recipient.getGroupe() != null && !recipient.getGroupe().isEmpty()) {
            content.append("Groupe: ").append(recipient.getGroupe()).append("\n");
        }

        content.append("Type: ").append(recipient.getType().getDisplayName()).append("\n\n");

        content.append("DÉTAILS DE LA CONVOCATION\n");
        content.append("-------------------------------------------\n");
        content.append("Date: ").append(recipient.getDatePassage().format(DATE_FORMATTER)).append("\n");
        content.append("Heure: ").append(recipient.getHeurePassage().format(TIME_FORMATTER)).append("\n");
        content.append("Salle: ").append(recipient.getSalle()).append("\n\n");

        if (recipient.getType() == fr.epsi.b3devc2.convocationepsi.model.enums.RecipientType.CANDIDAT) {
            content.append("INSTRUCTIONS POUR LE CANDIDAT\n");
            content.append("-------------------------------------------\n");
            content.append("- Vous devez vous présenter 15 minutes avant l'heure indiquée\n");
            content.append("- Munissez-vous d'une pièce d'identité valide\n");
            content.append("- Apportez votre matériel autorisé\n");
            content.append("- Respectez les consignes de l'établissement\n\n");
        } else {
            content.append("INSTRUCTIONS POUR LE JURY\n");
            content.append("-------------------------------------------\n");
            content.append("- Vous devez vous présenter 30 minutes avant l'heure indiquée\n");
            content.append("- Consultez les dossiers des candidats en amont\n");
            content.append("- Respectez les critères d'évaluation\n\n");
        }

        content.append("En cas d'empêchement, veuillez contacter l'administration dans les plus brefs délais.\n\n");
        content.append("Cordialement,\n");
        content.append("L'Administration");

        return content.toString();
    }

    private Map<String, String> createVariablesMap(RecipientDTO recipient) {
        Map<String, String> variables = new HashMap<>();
        variables.put("{{CIVILITE}}", recipient.getCivilite());
        variables.put("{{NOM}}", recipient.getNom());
        variables.put("{{PRENOM}}", recipient.getPrenom());
        variables.put("{{EMAIL}}", recipient.getEmail());
        variables.put("{{CLASSE}}", recipient.getClasse() != null ? recipient.getClasse() : "");
        variables.put("{{GROUPE}}", recipient.getGroupe() != null ? recipient.getGroupe() : "");
        variables.put("{{DATE}}", recipient.getDatePassage().format(DATE_FORMATTER));
        variables.put("{{HEURE}}", recipient.getHeurePassage().format(TIME_FORMATTER));
        variables.put("{{SALLE}}", recipient.getSalle());
        variables.put("{{TYPE}}", recipient.getType().getDisplayName());
        variables.put("{{FULL_NAME}}", recipient.getFullName());

        return variables;
    }

    private String replaceVariables(String content, Map<String, String> variables) {
        String result = content;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }
}