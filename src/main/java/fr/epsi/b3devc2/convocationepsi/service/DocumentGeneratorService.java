package fr.epsi.b3devc2.convocationepsi.service;

import fr.epsi.b3devc2.convocationepsi.dto.CandidatDto;
import fr.epsi.b3devc2.convocationepsi.dto.GenerateConvocationRequestDto;
import fr.epsi.b3devc2.convocationepsi.model.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ClassPathResource;
import org.apache.poi.util.Units;
import org.apache.poi.openxml4j.opc.PackagePartName;
import org.apache.poi.openxml4j.opc.PackagingURIHelper;
import org.apache.poi.openxml4j.opc.TargetMode;

import java.io.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
@Slf4j
public class DocumentGeneratorService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // Pattern pour détecter les variables complètes même réparties sur plusieurs runs
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{[A-Z_]+\\}\\}");

    /**
     * Génère un document Word pour un candidat
     */
    public byte[] generateConvocationForCandidat(
            CandidatDto candidat,
            GenerateConvocationRequestDto request,
            InputStream templateInputStream,
            Ville ville,
            TypeExamen typeExamen,
            Certification certification,
            Adresse adresse,
            DureeEpreuve dureeEpreuve) throws IOException {

        log.info("Génération de convocation pour {} {}", candidat.getPrenom(), candidat.getNom());

        // Créer le mapping des variables avec validation
        Map<String, String> variables = createVariableMapping(
                candidat, request, ville, typeExamen, certification, adresse, dureeEpreuve
        );

        log.debug("Variables créées: {}", variables);

        // Charger le template et remplacer les variables
        try (XWPFDocument document = new XWPFDocument(templateInputStream);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Remplacer les variables avec la méthode améliorée
            replaceAllVariables(document, variables);

            // Insérer la signature si présente
            if (request.getSignatureImage() != null && !request.getSignatureImage().isEmpty()) {
                try (InputStream sigStream = request.getSignatureImage().getInputStream()) {
                    insertSignatureImage(document, "{{SIGN}}", sigStream);
                    log.info("Signature insérée dans le document pour {} {}", candidat.getPrenom(), candidat.getNom());
                } catch (Exception e) {
                    log.error("Erreur lors de l'insertion de la signature pour {} {}: {}",
                            candidat.getPrenom(), candidat.getNom(), e.getMessage());
                }
            }

            document.write(out);
            byte[] result = out.toByteArray();
            log.info("Document généré avec succès pour {} {} (taille: {} bytes)",
                    candidat.getPrenom(), candidat.getNom(), result.length);

            return result;
        } catch (Exception e) {
            log.error("Erreur lors de la génération du document pour {} {}: {}",
                    candidat.getPrenom(), candidat.getNom(), e.getMessage(), e);
            throw new IOException("Erreur lors de la génération: " + e.getMessage(), e);
        }
    }

    /**
     * Crée le mapping des variables pour le remplacement avec validation
     */
    /**
     * Crée le mapping des variables pour le remplacement avec validation et debug détaillé
     */
    private Map<String, String> createVariableMapping(
            CandidatDto candidat,
            GenerateConvocationRequestDto request,
            Ville ville,
            TypeExamen typeExamen,
            Certification certification,
            Adresse adresse,
            DureeEpreuve dureeEpreuve) {

        Map<String, String> variables = new HashMap<>();



        // Nettoyer et préparer les noms
        String nomClean = candidat.getNom();
        String prenomClean = candidat.getPrenom();



        // Informations candidat avec espaces correctement gérés
        variables.put("{{NOM}}", nomClean.toUpperCase());
        variables.put("{{PRENOM}}", prenomClean);

        // Construction explicite avec debug
        String nomPrenom = nomClean.toUpperCase() + "  " + prenomClean;
        String prenomNom = prenomClean + "  " + nomClean.toUpperCase();


        variables.put("{{NOM_PRENOM}}", nomPrenom);
        variables.put("{{PRENOM_NOM}}", prenomNom);

        variables.put("{{CIVILITE}}", safeValue(candidat.getCivilite()));
        variables.put("{{EMAIL}}", safeValue(candidat.getEmail()));
        variables.put("{{GROUPE}}", safeValue(candidat.getGroupe()));
        variables.put("{{NUMERO_JURY}}", safeValue(candidat.getNumeroJury()));
        variables.put("{{SALLE}}", safeValue(candidat.getSalle()));

        // Dates et heures avec validation
        if (candidat.getDatePassage() != null) {
            variables.put("{{DATE}}", candidat.getDatePassage().format(DATE_FORMATTER));
            variables.put("{{DATE_PASSAGE}}", candidat.getDatePassage().format(DATE_FORMATTER));
        } else {
            variables.put("{{DATE}}", "Non définie");
            variables.put("{{DATE_PASSAGE}}", "Non définie");
        }

        if (candidat.getHeurePassage() != null) {
            variables.put("{{HORAIRE}}", candidat.getHeurePassage().format(TIME_FORMATTER));
            variables.put("{{HEURE}}", candidat.getHeurePassage().format(TIME_FORMATTER));
            variables.put("{{HEURE_PASSAGE}}", candidat.getHeurePassage().format(TIME_FORMATTER));
        } else {
            variables.put("{{HORAIRE}}", "Non définie");
            variables.put("{{HEURE}}", "Non définie");
            variables.put("{{HEURE_PASSAGE}}", "Non définie");
        }

        if (request.getDateRendu() != null) {
            variables.put("{{DATE_RENDU}}", request.getDateRendu().format(DATE_FORMATTER));
        } else {
            variables.put("{{DATE_RENDU}}", "Non définie");
        }

        if (request.getHeureRendu() != null) {
            variables.put("{{HEURE_RENDU}}", request.getHeureRendu().format(TIME_FORMATTER));
        } else {
            variables.put("{{HEURE_RENDU}}", "Non définie");
        }

        // Informations examen
        variables.put("{{TYPE_EXAMEN}}", typeExamen != null ? safeValue(typeExamen.getNom()) : "Non défini");
        variables.put("{{CERTIFICATION}}", certification != null ? safeValue(" " + certification.getNom()) : "Non définie");
        variables.put("{{DUREE_EPREUVE}}", dureeEpreuve != null ? safeValue(dureeEpreuve.getNom()) : "Non définie");

        // Informations lieu
        variables.put("{{VILLE}}", ville != null ? safeValue(ville.getNom()) : "Non définie");
        variables.put("{{ADRESSE}}", adresse != null ? safeValue(adresse.getRue()) : "Non définie");

        // Lien Drive avec validation spéciale
        String lienDrive = request.getLienDrive();
        if (lienDrive != null && !lienDrive.trim().isEmpty()) {
            variables.put("{{LIEN_DRIVE}}", lienDrive.trim());
        } else {
            variables.put("{{LIEN_DRIVE}}", "Lien non disponible");
        }



        return variables;
    }

    /**
     * Sécurise un nom en gérant les espaces et caractères spéciaux
     */


    /**
     * Sécurise une valeur en gérant les valeurs nulles
     */


    /**
     * Remplace toutes les variables dans le document de manière robuste
     */
    private void replaceAllVariables(XWPFDocument document, Map<String, String> variables) {
        // Remplacer dans les paragraphes principaux
        replaceVariablesInParagraphs(document.getParagraphs(), variables);

        // Remplacer dans les tableaux
        for (XWPFTable table : document.getTables()) {
            replaceVariablesInTable(table, variables);
        }

        // Remplacer dans les en-têtes
        for (XWPFHeader header : document.getHeaderList()) {
            replaceVariablesInParagraphs(header.getParagraphs(), variables);
            for (XWPFTable table : header.getTables()) {
                replaceVariablesInTable(table, variables);
            }
        }

        // Remplacer dans les pieds de page
        for (XWPFFooter footer : document.getFooterList()) {
            replaceVariablesInParagraphs(footer.getParagraphs(), variables);
            for (XWPFTable table : footer.getTables()) {
                replaceVariablesInTable(table, variables);
            }
        }
    }

    /**
     * Remplace les variables dans une liste de paragraphes
     */
    private void replaceVariablesInParagraphs(List<XWPFParagraph> paragraphs, Map<String, String> variables) {
        for (XWPFParagraph paragraph : paragraphs) {
            replaceVariablesInParagraph(paragraph, variables);
        }
    }


    // Ajoutez cette liste des variables qui doivent être en gras
    private static final Set<String> BOLD_VARIABLES = Set.of(
            "{{HORAIRE}}",
            "{{SALLE}}",
            "{{ADRESSE}}"
    );

    /**
     * Version corrigée du remplacement de variables avec gestion appropriée des espaces
     */
    private void replaceVariablesInParagraph(XWPFParagraph paragraph, Map<String, String> variables) {
        List<XWPFRun> runs = paragraph.getRuns();
        if (runs.isEmpty()) return;

        // Reconstituer le texte complet du paragraphe
        StringBuilder fullText = new StringBuilder();
        for (XWPFRun run : runs) {
            String runText = run.getText(0);
            if (runText != null) {
                fullText.append(runText);
            }
        }

        String originalText = fullText.toString();
        String modifiedText = originalText;
        boolean hasChanges = false;
        Map<String, String> appliedVariables = new HashMap<>();

        log.debug("Texte paragraphe AVANT: '{}'", originalText);

        // Remplacer toutes les variables et garder une trace de celles remplacées
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            if (modifiedText.contains(entry.getKey())) {
                log.debug("REMPLACEMENT: '{}' -> '{}'", entry.getKey(), entry.getValue());
                modifiedText = modifiedText.replace(entry.getKey(), entry.getValue());
                appliedVariables.put(entry.getKey(), entry.getValue());
                hasChanges = true;
            }
        }

        // Si des changements ont été effectués, reconstruire le paragraphe
        if (hasChanges) {
            log.debug("Texte paragraphe APRÈS: '{}'", modifiedText);

            // Sauvegarder le formatage du premier run
            XWPFRun firstRun = runs.get(0);
            boolean isBold = firstRun.isBold();
            boolean isItalic = firstRun.isItalic();
            UnderlinePatterns underline = firstRun.getUnderline();
            String fontFamily = firstRun.getFontFamily();
            int fontSize = firstRun.getFontSize();

            // Supprimer tous les runs existants
            while (runs.size() > 0) {
                paragraph.removeRun(0);
            }

            // Créer le nouveau contenu avec formatage approprié
            createFormattedRuns(paragraph, modifiedText, appliedVariables, isBold, isItalic, underline, fontFamily, fontSize);
        }
    }

    /**
     * Crée les runs avec le formatage approprié pour les variables en gras
     */
    private void createFormattedRuns(XWPFParagraph paragraph, String text, Map<String, String> appliedVariables,
                                     boolean defaultBold, boolean defaultItalic, UnderlinePatterns defaultUnderline,
                                     String defaultFont, int defaultSize) {

        // Trouver toutes les positions des valeurs qui doivent être en gras
        Map<Integer, String> boldPositions = new HashMap<>();

        for (Map.Entry<String, String> entry : appliedVariables.entrySet()) {
            // Une variable doit être en gras si :
            // 1. Elle était déjà en gras dans le template (defaultBold = true)
            // 2. OU elle est dans la liste des variables qui doivent être en gras
            if (defaultBold || BOLD_VARIABLES.contains(entry.getKey())) {
                String value = entry.getValue();
                int index = text.indexOf(value);
                while (index != -1) {
                    boldPositions.put(index, value);
                    index = text.indexOf(value, index + value.length());
                }
            }
        }

        if (boldPositions.isEmpty()) {
            // Aucune variable en gras, créer un seul run avec le formatage par défaut
            XWPFRun run = paragraph.createRun();
            run.setText(text);
            applyFormatting(run, defaultBold, defaultItalic, defaultUnderline, defaultFont, defaultSize);
        } else {
            // Créer des runs séparés pour les parties en gras et normales
            List<TextSegment> segments = createTextSegments(text, boldPositions);

            for (TextSegment segment : segments) {
                XWPFRun run = paragraph.createRun();
                run.setText(segment.text);
                // Pour les segments en gras, forcer le gras même si defaultBold est false
                applyFormatting(run, segment.isBold || defaultBold, defaultItalic, defaultUnderline, defaultFont, defaultSize);
            }
        }
    }

    /**
     * Classe pour représenter un segment de texte avec son formatage
     */
    private static class TextSegment {
        String text;
        boolean isBold;

        TextSegment(String text, boolean isBold) {
            this.text = text;
            this.isBold = isBold;
        }
    }

    /**
     * Crée les segments de texte avec leur formatage
     */
    private List<TextSegment> createTextSegments(String text, Map<Integer, String> boldPositions) {
        List<TextSegment> segments = new ArrayList<>();

        if (boldPositions.isEmpty()) {
            segments.add(new TextSegment(text, false));
            return segments;
        }

        // Trier les positions
        List<Integer> sortedPositions = new ArrayList<>(boldPositions.keySet());
        sortedPositions.sort(Integer::compareTo);

        int currentPos = 0;

        for (int position : sortedPositions) {
            String boldValue = boldPositions.get(position);

            // Ajouter le texte avant la variable (si il y en a)
            if (position > currentPos) {
                String beforeText = text.substring(currentPos, position);
                if (!beforeText.isEmpty()) {
                    segments.add(new TextSegment(beforeText, false));
                }
            }

            // Ajouter la variable en gras
            segments.add(new TextSegment(boldValue, true));
            currentPos = position + boldValue.length();
        }

        // Ajouter le texte restant (si il y en a)
        if (currentPos < text.length()) {
            String remainingText = text.substring(currentPos);
            if (!remainingText.isEmpty()) {
                segments.add(new TextSegment(remainingText, false));
            }
        }

        return segments;
    }

    /**
     * Applique le formatage à un run
     */
    private void applyFormatting(XWPFRun run, boolean bold, boolean italic, UnderlinePatterns underline,
                                 String fontFamily, int fontSize) {
        if (bold) {
            run.setBold(true);
        }
        if (italic) {
            run.setItalic(true);
        }
        if (underline != null && underline != UnderlinePatterns.NONE) {
            run.setUnderline(underline);
        }
        if (fontFamily != null && !fontFamily.isEmpty()) {
            run.setFontFamily(fontFamily);
        }
        if (fontSize > 0) {
            run.setFontSize(fontSize);
        }
    }

    /**
     * Version simplifiée sans la gestion des variables réparties sur plusieurs runs
     * (car elle est maintenant gérée dans replaceVariablesInParagraph)
     */
    private void handleVariablesAcrossRunsWithBold(XWPFParagraph paragraph, Map<String, String> variables) {
        // Cette méthode n'est plus nécessaire car la logique est intégrée dans replaceVariablesInParagraph
        // Garder pour compatibilité ou supprimer si pas utilisée ailleurs
    }

    /**
     * Méthode utilitaire pour nettoyer les espaces multiples
     */
    private String cleanSpaces(String text) {
        if (text == null) return "";
        return text.trim().replaceAll("\\s+", " ");
    }

    /**
     * Version améliorée de safeValue avec nettoyage des espaces
     */
    private String safeValue(String value) {
        if (value == null) return "";
        return cleanSpaces(value);
    }

    /**
     * Vérifie si une variable est contenue entièrement dans un seul run
     */
    private boolean isVariableInSingleRun(List<XWPFRun> runs, String variable) {
        for (XWPFRun run : runs) {
            String runText = run.getText(0);
            if (runText != null && runText.contains(variable)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Remplace les variables dans un tableau
     */
    private void replaceVariablesInTable(XWPFTable table, Map<String, String> variables) {
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                replaceVariablesInParagraphs(cell.getParagraphs(), variables);
                // Gérer les tableaux imbriqués
                for (XWPFTable nestedTable : cell.getTables()) {
                    replaceVariablesInTable(nestedTable, variables);
                }
            }
        }
    }

    /**
     * Insère une image de signature de manière robuste
     */
    public void insertSignatureImage(XWPFDocument doc, String placeholder, InputStream imageStream) throws Exception {
        boolean signatureInserted = false;

        // Chercher dans tous les paragraphes
        for (XWPFParagraph paragraph : doc.getParagraphs()) {
            if (insertSignatureInParagraph(paragraph, placeholder, imageStream)) {
                signatureInserted = true;
                break;
            }
        }

        // Chercher dans les tableaux si pas encore trouvé
        if (!signatureInserted) {
            for (XWPFTable table : doc.getTables()) {
                if (insertSignatureInTable(table, placeholder, imageStream)) {
                    signatureInserted = true;
                    break;
                }
            }
        }

        // Chercher dans les en-têtes et pieds de page si pas encore trouvé
        if (!signatureInserted) {
            for (XWPFHeader header : doc.getHeaderList()) {
                for (XWPFParagraph paragraph : header.getParagraphs()) {
                    if (insertSignatureInParagraph(paragraph, placeholder, imageStream)) {
                        signatureInserted = true;
                        break;
                    }
                }
                if (signatureInserted) break;
            }
        }

        if (!signatureInserted) {
            for (XWPFFooter footer : doc.getFooterList()) {
                for (XWPFParagraph paragraph : footer.getParagraphs()) {
                    if (insertSignatureInParagraph(paragraph, placeholder, imageStream)) {
                        signatureInserted = true;
                        break;
                    }
                }
                if (signatureInserted) break;
            }
        }

        if (!signatureInserted) {
            log.warn("Placeholder de signature '{}' non trouvé dans le document", placeholder);
        }
    }

    /**
     * Insère la signature dans un paragraphe
     */
    private boolean insertSignatureInParagraph(XWPFParagraph paragraph, String placeholder, InputStream imageStream) throws Exception {
        // Reconstituer le texte du paragraphe
        StringBuilder fullText = new StringBuilder();
        List<XWPFRun> runs = paragraph.getRuns();

        for (XWPFRun run : runs) {
            String runText = run.getText(0);
            if (runText != null) {
                fullText.append(runText);
            }
        }

        if (fullText.toString().contains(placeholder)) {
            // Supprimer tous les runs
            while (!runs.isEmpty()) {
                paragraph.removeRun(0);
            }

            // Créer un nouveau run avec l'image
            XWPFRun newRun = paragraph.createRun();

            // Réinitialiser le stream si nécessaire
            if (imageStream.markSupported()) {
                imageStream.reset();
            }

            newRun.addPicture(
                    imageStream,
                    XWPFDocument.PICTURE_TYPE_PNG,
                    "signature.png",
                    Units.toEMU(260), // Largeur réduite pour éviter les décalages
                    Units.toEMU(70)   // Hauteur réduite
            );

            return true;
        }

        return false;
    }

    /**
     * Insère la signature dans un tableau
     */
    private boolean insertSignatureInTable(XWPFTable table, String placeholder, InputStream imageStream) throws Exception {
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                for (XWPFParagraph paragraph : cell.getParagraphs()) {
                    if (insertSignatureInParagraph(paragraph, placeholder, imageStream)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Génère le nom du fichier pour un candidat
     */
    public String generateFileName(CandidatDto candidat, String extension) {
        String nom = safeName(candidat.getNom()).trim().replaceAll("\\s+", "_");
        String prenom = safeName(candidat.getPrenom()).trim().replaceAll("\\s+", "_");

        String ext = (extension == null || extension.isBlank()) ? "pdf" : extension.toLowerCase();
        return String.format("Convocation_%s_%s.%s", nom, prenom, ext);
    }

    private String safeName(String input) {
        if (input == null) return "";
        // ⚠️ On garde les lettres accentuées
        // On ne supprime que ce qui est vraiment problématique pour les systèmes de fichiers
        return input.replaceAll("[\\\\/:*?\"<>|]", "");
    }

}