package fr.epsi.b3devc2.convocationepsi.service;


import fr.epsi.b3devc2.convocationepsi.dto.RecipientDTO;
import fr.epsi.b3devc2.convocationepsi.model.enums.RecipientType;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ExcelParserService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public List<RecipientDTO> parseExcelFile(MultipartFile file) throws IOException {
        log.info("Début du parsing du fichier Excel: {}", file.getOriginalFilename());

        List<RecipientDTO> recipients = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // Ignorer la première ligne (headers)
            int startRow = 1;
            int lastRow = sheet.getLastRowNum();

            log.info("Traitement de {} lignes de données", lastRow - startRow + 1);

            for (int i = startRow; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null || isEmptyRow(row)) {
                    continue;
                }

                try {
                    RecipientDTO recipient = parseRow(row);
                    if (recipient != null) {
                        recipients.add(recipient);
                    }
                } catch (Exception e) {
                    log.error("Erreur lors du parsing de la ligne {}: {}", i + 1, e.getMessage());
                }
            }
        }

        log.info("Parsing terminé. {} destinataires extraits", recipients.size());
        return recipients;
    }

    private RecipientDTO parseRow(Row row) {
        try {
            return RecipientDTO.builder()
                    .civilite(getCellStringValue(row.getCell(0)))
                    .nom(getCellStringValue(row.getCell(1)))
                    .prenom(getCellStringValue(row.getCell(2)))
                    .email(getCellStringValue(row.getCell(3)))
                    .classe(getCellStringValue(row.getCell(4)))
                    .groupe(getCellStringValue(row.getCell(5)))
                    .datePassage(parseDate(getCellStringValue(row.getCell(6))))
                    .heurePassage(parseTime(getCellStringValue(row.getCell(7))))
                    .salle(getCellStringValue(row.getCell(8)))
                    .type(parseRecipientType(getCellStringValue(row.getCell(9))))
                    .build();
        } catch (Exception e) {
            log.error("Erreur lors du parsing de la ligne: {}", e.getMessage());
            return null;
        }
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue().toString();
                } else {
                    yield String.valueOf((long) cell.getNumericCellValue());
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return LocalDate.now();
        }

        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (Exception e) {
            log.warn("Format de date invalide: {}. Utilisation de la date actuelle", dateStr);
            return LocalDate.now();
        }
    }

    private LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            return LocalTime.of(9, 0);
        }

        try {
            return LocalTime.parse(timeStr, TIME_FORMATTER);
        } catch (Exception e) {
            log.warn("Format d'heure invalide: {}. Utilisation de 09:00", timeStr);
            return LocalTime.of(9, 0);
        }
    }

    private RecipientType parseRecipientType(String typeStr) {
        if (typeStr == null || typeStr.isEmpty()) {
            return RecipientType.CANDIDAT;
        }

        try {
            return RecipientType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Type de destinataire invalide: {}. Utilisation de CANDIDAT", typeStr);
            return RecipientType.CANDIDAT;
        }
    }

    private boolean isEmptyRow(Row row) {
        for (Cell cell : row) {
            if (cell != null && !getCellStringValue(cell).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}