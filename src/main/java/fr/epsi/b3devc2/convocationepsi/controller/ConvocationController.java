package fr.epsi.b3devc2.convocationepsi.controller;

import fr.epsi.b3devc2.convocationepsi.dto.GenerateConvocationRequestDto;
import fr.epsi.b3devc2.convocationepsi.dto.GenerateResponseDto;
import fr.epsi.b3devc2.convocationepsi.dto.ImportResponseDto;
import fr.epsi.b3devc2.convocationepsi.dto.SendEmailRequestDto;
import fr.epsi.b3devc2.convocationepsi.service.ConvocationService;
import fr.epsi.b3devc2.convocationepsi.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ConvocationController {

    private final ConvocationService convocationService;
    private final EmailService emailService;

    @PostMapping("/import")
    public ResponseEntity<ImportResponseDto> importCandidats(
            @RequestParam("file") MultipartFile file) {
        try {
            ImportResponseDto response = convocationService.importCandidats(file);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur lors de l'importation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ImportResponseDto(null, null,0,
                            java.util.List.of("Erreur: " + e.getMessage()),
                            "Échec de l'importation"));
        }
    }

    @PostMapping(value = "/generate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GenerateResponseDto> generateConvocations(
            @RequestPart("data") GenerateConvocationRequestDto request,
            @RequestPart("templateFile") MultipartFile templateFile,
            @RequestPart(value = "signatureImage", required = false) MultipartFile signatureImage) {
        try {
            request.setTemplateFile(templateFile);
            request.setSignatureImage(signatureImage);

            GenerateResponseDto response = convocationService.generateConvocations(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur lors de la génération : {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenerateResponseDto(null, 0, null, "Erreur : " + e.getMessage()));
        }
    }



    @GetMapping("/download/{sessionId}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String sessionId) {
        try {
            byte[] fileData = convocationService.getGeneratedFile(sessionId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "convocations_" + sessionId + ".zip");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileData);

        } catch (Exception e) {
            log.error("Erreur lors du téléchargement: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @PostMapping("/send-emails")
    public ResponseEntity<String> sendEmails(@RequestBody SendEmailRequestDto request) {
        try {
            emailService.sendConvocationsByEmail(request);
            return ResponseEntity.ok("Emails envoyés avec succès");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de l'envoi : " + e.getMessage());
        }
    }


}