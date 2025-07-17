package fr.epsi.b3devc2.convocationepsi.controller;


import fr.epsi.b3devc2.convocationepsi.dto.RecipientDTO;
import fr.epsi.b3devc2.convocationepsi.service.DocumentGeneratorService;
import fr.epsi.b3devc2.convocationepsi.service.EmailService;
import fr.epsi.b3devc2.convocationepsi.service.ExcelParserService;
import fr.epsi.b3devc2.convocationepsi.storage.InMemorySessionStorage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ConvocationController {

    private final ExcelParserService excelParserService;
    private final DocumentGeneratorService documentGeneratorService;
    private final EmailService emailService;
    private final InMemorySessionStorage sessionStorage;

    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importExcelFile(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {

        log.info("Début de l'importation du fichier: {}", file.getOriginalFilename());

        try {
            // Validation du fichier
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Le fichier est vide"));
            }

            if (!isValidExcelFile(file)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Le fichier doit être au format .xlsx"));
            }

            // Parser le fichier Excel
            List<RecipientDTO> recipients = excelParserService.parseExcelFile(file);

            if (recipients.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Aucun destinataire trouvé dans le fichier"));
            }

            // Générer un ID de session
            String sessionId = generateSessionId(request);

            // Stocker les destinataires en mémoire
            sessionStorage.storeRecipients(sessionId, recipients);

            // Préparer la réponse
            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", sessionId);
            response.put("totalRecipients", recipients.size());
            response.put("recipients", recipients);
            response.put("message", "Fichier importé avec succès");

            log.info("Importation terminée: {} destinataires pour la session {}",
                    recipients.size(), sessionId);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("Erreur lors de l'importation du fichier: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la lecture du fichier: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Erreur inattendue lors de l'importation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur inattendue: " + e.getMessage()));
        }
    }

    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateConvocations(
            @RequestParam("sessionId") String sessionId,
            @RequestParam(value = "format", defaultValue = "docx") String format) {

        log.info("Génération des convocations pour la session: {}", sessionId);

        try {
            // Vérifier la session
            if (!sessionStorage.hasSession(sessionId)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Session non trouvée"));
            }

            List<RecipientDTO> recipients = sessionStorage.getRecipients(sessionId);
            if (recipients == null || recipients.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Aucun destinataire trouvé pour cette session"));
            }

            // Générer les documents
            int successCount = 0;
            int errorCount = 0;

            for (RecipientDTO recipient : recipients) {
                try {
                    byte[] document;
                    if ("docx".equalsIgnoreCase(format)) {
                        document = documentGeneratorService.generateConvocationDocx(recipient);
                    } else {
                        // Pour l'instant, on ne supporte que DOCX
                        document = documentGeneratorService.generateConvocationDocx(recipient);
                    }

                    sessionStorage.storeGeneratedDocument(sessionId, recipient.getId(), document);
                    successCount++;

                } catch (Exception e) {
                    log.error("Erreur lors de la génération pour {}: {}",
                            recipient.getEmail(), e.getMessage());
                    errorCount++;
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", sessionId);
            response.put("totalRecipients", recipients.size());
            response.put("successCount", successCount);
            response.put("errorCount", errorCount);
            response.put("message", String.format("Génération terminée: %d succès, %d erreurs",
                    successCount, errorCount));

            log.info("Génération terminée pour la session {}: {} succès, {} erreurs",
                    sessionId, successCount, errorCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la génération des convocations: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la génération: " + e.getMessage()));
        }
    }

    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendConvocations(
            @RequestParam("sessionId") String sessionId) {

        log.info("Envoi des convocations pour la session: {}", sessionId);

        try {
            // Vérifier la session
            if (!sessionStorage.hasSession(sessionId)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Session non trouvée"));
            }

            List<RecipientDTO> recipients = sessionStorage.getRecipients(sessionId);
            Map<Long, byte[]> documents = sessionStorage.getAllGeneratedDocuments(sessionId);

            if (recipients == null || recipients.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Aucun destinataire trouvé"));
            }

            if (documents.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Aucun document généré. Veuillez d'abord générer les convocations"));
            }

            // Envoyer les emails de manière asynchrone
            CompletableFuture<Void> sendingFuture = emailService.sendBulkConvocations(recipients, documents);

            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", sessionId);
            response.put("totalRecipients", recipients.size());
            response.put("message", "Envoi des convocations en cours...");

            log.info("Envoi initié pour {} destinataires de la session {}",
                    recipients.size(), sessionId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de l'envoi des convocations: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de l'envoi: " + e.getMessage()));
        }
    }

    @GetMapping("/download/{sessionId}")
    public ResponseEntity<ByteArrayResource> downloadConvocations(
            @PathVariable String sessionId) {

        log.info("Téléchargement des convocations pour la session: {}", sessionId);

        try {
            // Vérifier la session
            if (!sessionStorage.hasSession(sessionId)) {
                return ResponseEntity.badRequest().build();
            }

            List<RecipientDTO> recipients = sessionStorage.getRecipients(sessionId);
            Map<Long, byte[]> documents = sessionStorage.getAllGeneratedDocuments(sessionId);

            if (recipients == null || recipients.isEmpty() || documents.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            // Créer un ZIP avec tous les documents
            ByteArrayOutputStream zipOut = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(zipOut)) {

                for (RecipientDTO recipient : recipients) {
                    byte[] document = documents.get(recipient.getId());
                    if (document != null) {
                        String fileName = String.format("Convocation_%s_%s.docx",
                                recipient.getNom(), recipient.getPrenom());

                        ZipEntry entry = new ZipEntry(fileName);
                        zip.putNextEntry(entry);
                        zip.write(document);
                        zip.closeEntry();
                    }
                }
            }

            ByteArrayResource resource = new ByteArrayResource(zipOut.toByteArray());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"convocations_" + sessionId + ".zip\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

        } catch (Exception e) {
            log.error("Erreur lors du téléchargement: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSessionInfo(
            @PathVariable String sessionId) {

        if (!sessionStorage.hasSession(sessionId)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Session non trouvée"));
        }

        List<RecipientDTO> recipients = sessionStorage.getRecipients(sessionId);
        Map<Long, byte[]> documents = sessionStorage.getAllGeneratedDocuments(sessionId);

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", sessionId);
        response.put("totalRecipients", recipients != null ? recipients.size() : 0);
        response.put("generatedDocuments", documents.size());
        response.put("recipients", recipients);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> clearSession(
            @PathVariable String sessionId) {

        sessionStorage.clearSession(sessionId);

        return ResponseEntity.ok(Map.of(
                "message", "Session nettoyée avec succès",
                "sessionId", sessionId
        ));
    }

    private boolean isValidExcelFile(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        return fileName != null && fileName.toLowerCase().endsWith(".xlsx");
    }

    private String generateSessionId(HttpServletRequest request) {
        return "session_" + System.currentTimeMillis() + "_" +
                request.getRemoteAddr().hashCode();
    }
}