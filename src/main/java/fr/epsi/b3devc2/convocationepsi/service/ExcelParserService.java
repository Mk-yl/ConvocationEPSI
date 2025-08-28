package fr.epsi.b3devc2.convocationepsi.service;

import fr.epsi.b3devc2.convocationepsi.dto.CandidatDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ExcelParserService {
    
    /**
     * Lit le fichier Excel et extrait les candidats
     */
    public List<CandidatDto> readCandidatsFromExcel(MultipartFile file) throws IOException {
        log.info("Lecture du fichier Excel: {}", file.getOriginalFilename());

        List<CandidatDto> candidats = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // Vérifier si la feuille contient des données
            if (sheet.getLastRowNum() < 1) {
                throw new IllegalArgumentException("Le fichier Excel ne contient pas de données");
            }

            // Lire l'en-tête pour valider le format
            Row headerRow = sheet.getRow(0);
            validateHeader(headerRow);

            // Lire les lignes de données
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null && !isEmptyRow(row)) {
                    try {
                        CandidatDto candidat = readCandidatFromRow(row, i + 1);
                        candidats.add(candidat);
                    } catch (Exception e) {
                        log.error("Erreur lors de la lecture de la ligne {}: {}", i + 1, e.getMessage());
                        throw new IllegalArgumentException("Erreur ligne " + (i + 1) + ": " + e.getMessage());
                    }
                }
            }
        }

        log.info("Lecture terminée: {} candidats extraits", candidats.size());
        return candidats;
    }

    /**
     * Valide l'en-tête du fichier Excel
     */
    private void validateHeader(Row headerRow) {
        if (headerRow == null) {
            throw new IllegalArgumentException("L'en-tête du fichier Excel est manquant");
        }

        String[] expectedHeaders = {
                "Groupe", "Civilité", "Nom", "Prénom", "Email",
                "Date", "Heure", "Salle", "Numéro Jury"
        };

        for (int i = 0; i < expectedHeaders.length; i++) {
            Cell cell = headerRow.getCell(i);
            if (cell == null || !expectedHeaders[i].equalsIgnoreCase(getCellValueAsString(cell).trim())) {
                log.warn("En-tête colonne {}: attendu '{}', trouvé '{}'",
                        i + 1, expectedHeaders[i],
                        cell != null ? getCellValueAsString(cell).trim() : "null");
                // On continue sans erreur pour plus de flexibilité
            }
        }
    }

    /**
     * Lit un candidat depuis une ligne Excel
     */
    private CandidatDto readCandidatFromRow(Row row, int rowNumber) {
        CandidatDto candidat = new CandidatDto();

        try {
            // Groupe
            candidat.setGroupe(getCellValueAsString(row.getCell(0)));

            // Civilité
            candidat.setCivilite(getCellValueAsString(row.getCell(1)));

            // Nom
            candidat.setNom(getCellValueAsString(row.getCell(2)));
            if (candidat.getNom().trim().isEmpty()) {
                throw new IllegalArgumentException("Le nom est obligatoire");
            }

            // Prénom
            candidat.setPrenom(getCellValueAsString(row.getCell(3)));
            if (candidat.getPrenom().trim().isEmpty()) {
                throw new IllegalArgumentException("Le prénom est obligatoire");
            }

            // Email
            candidat.setEmail(getCellValueAsString(row.getCell(4)));
            if (candidat.getEmail().trim().isEmpty()) {
                throw new IllegalArgumentException("L'email est obligatoire");
            }

            // Date - Gestion flexible des formats de date
            candidat.setDatePassage(parseDateFromCell(row.getCell(5)));

            // Heure - Gestion flexible des formats d'heure
            candidat.setHeurePassage(parseTimeFromCell(row.getCell(6)));

            // Salle
            candidat.setSalle(getCellValueAsString(row.getCell(7)));

            // Numéro Jury
            candidat.setNumeroJury(getCellValueAsString(row.getCell(8)));

            return candidat;

        } catch (Exception e) {
            log.error("Erreur ligne {}: {}", rowNumber, e.getMessage());
            throw new RuntimeException("Erreur ligne " + rowNumber + ": " + e.getMessage());
        }
    }

    /**
     * Parse une date depuis une cellule Excel (gère les formats numériques et texte)
     */
    private LocalDate parseDateFromCell(Cell cell) {
        if (cell == null) {
            return LocalDate.now(); // Date par défaut
        }

        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        // Cellule formatée comme date
                        return cell.getLocalDateTimeCellValue().toLocalDate();
                    } else {
                        // Nombre qui pourrait être une date Excel
                        double numericValue = cell.getNumericCellValue();
                        return DateUtil.getJavaDate(numericValue).toInstant()
                                .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                    }

                case STRING:
                    String dateStr = cell.getStringCellValue().trim();
                    if (dateStr.isEmpty()) {
                        return LocalDate.now();
                    }

                    // Essayer différents formats de date
                    return tryParseDateString(dateStr);

                default:
                    log.warn("Type de cellule non supporté pour la date: {}", cell.getCellType());
                    return LocalDate.now();
            }
        } catch (Exception e) {
            log.warn("Impossible de parser la date depuis la cellule, utilisation de la date actuelle: {}", e.getMessage());
            return LocalDate.now();
        }
    }

    /**
     * Essaie de parser une chaîne de date avec différents formats
     */
    private LocalDate tryParseDateString(String dateStr) {
        DateTimeFormatter[] formatters = {
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("d/M/yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("dd/MM/yy"),
                DateTimeFormatter.ofPattern("d/M/yy")
        };

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (DateTimeParseException ignored) {
                // Continuer avec le format suivant
            }
        }

        log.warn("Impossible de parser la date '{}', utilisation de la date actuelle", dateStr);
        return LocalDate.now();
    }

    /**
     * Parse une heure depuis une cellule Excel
     */
    private LocalTime parseTimeFromCell(Cell cell) {
        if (cell == null) {
            return LocalTime.of(9, 0); // Heure par défaut
        }

        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        // Cellule formatée comme heure
                        return cell.getLocalDateTimeCellValue().toLocalTime();
                    } else {
                        // Nombre décimal représentant une fraction de jour
                        double timeValue = cell.getNumericCellValue();
                        // Convertir la fraction en heures et minutes
                        int totalMinutes = (int) (timeValue * 24 * 60);
                        int hours = totalMinutes / 60;
                        int minutes = totalMinutes % 60;
                        return LocalTime.of(hours % 24, minutes);
                    }

                case STRING:
                    String timeStr = cell.getStringCellValue().trim();
                    if (timeStr.isEmpty()) {
                        return LocalTime.of(9, 0);
                    }

                    return tryParseTimeString(timeStr);

                default:
                    log.warn("Type de cellule non supporté pour l'heure: {}", cell.getCellType());
                    return LocalTime.of(9, 0);
            }
        } catch (Exception e) {
            log.warn("Impossible de parser l'heure depuis la cellule, utilisation de 09:00: {}", e.getMessage());
            return LocalTime.of(9, 0);
        }
    }

    /**
     * Essaie de parser une chaîne d'heure avec différents formats
     */
    private LocalTime tryParseTimeString(String timeStr) {
        DateTimeFormatter[] formatters = {
                DateTimeFormatter.ofPattern("HH:mm"),
                DateTimeFormatter.ofPattern("H:mm"),
                DateTimeFormatter.ofPattern("HH:mm:ss"),
                DateTimeFormatter.ofPattern("H:mm:ss")
        };

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalTime.parse(timeStr, formatter);
            } catch (DateTimeParseException ignored) {
                // Continuer avec le format suivant
            }
        }

        log.warn("Impossible de parser l'heure '{}', utilisation de 09:00", timeStr);
        return LocalTime.of(9, 0);
    }

    /**
     * Extrait la valeur d'une cellule comme chaîne de caractères
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        try {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue().trim();

                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return cell.getLocalDateTimeCellValue().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                    } else {
                        // Vérifier si c'est un entier
                        double numValue = cell.getNumericCellValue();
                        if (numValue == Math.floor(numValue)) {
                            return String.valueOf((long) numValue);
                        } else {
                            return String.valueOf(numValue);
                        }
                    }

                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());

                case FORMULA:
                    // Évaluer la formule
                    try {
                        FormulaEvaluator evaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
                        CellValue cellValue = evaluator.evaluate(cell);
                        switch (cellValue.getCellType()) {
                            case STRING:
                                return cellValue.getStringValue().trim();
                            case NUMERIC:
                                return String.valueOf((long) cellValue.getNumberValue());
                            case BOOLEAN:
                                return String.valueOf(cellValue.getBooleanValue());
                            default:
                                return "";
                        }
                    } catch (Exception e) {
                        log.warn("Erreur lors de l'évaluation de la formule: {}", e.getMessage());
                        return "";
                    }

                case BLANK:
                    return "";

                default:
                    return "";
            }
        } catch (Exception e) {
            log.warn("Erreur lors de la lecture de la cellule: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Vérifie si une ligne est vide
     */
    private boolean isEmptyRow(Row row) {
        if (row == null) {
            return true;
        }

        for (int i = 0; i < Math.min(row.getLastCellNum(), 9); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && !getCellValueAsString(cell).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}