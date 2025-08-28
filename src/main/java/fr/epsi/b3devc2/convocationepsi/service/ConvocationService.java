package fr.epsi.b3devc2.convocationepsi.service;


import fr.epsi.b3devc2.convocationepsi.dto.*;
import fr.epsi.b3devc2.convocationepsi.model.*;
import fr.epsi.b3devc2.convocationepsi.repository.*;
import fr.epsi.b3devc2.convocationepsi.storage.InMemorySessionStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ConvocationService {

    private final ExcelParserService excelService;
    private final DocumentGeneratorService wordService;
    private final ZipService zipService;
    private final EmailService emailService;


    private final InMemorySessionStorage sessionStorage;

    // Repositories
    private final VilleRepository villeRepository;
    private final TypeExamenRepository typeExamenRepository;
    private final CertificationRepository certificationRepository;
    private final AdresseRepository adresseRepository;
    private final DureeEpreuveRepository dureeEpreuveRepository;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");

    /**
     * Importe les candidats depuis un fichier Excel
     */
    public ImportResponseDto importCandidats(MultipartFile file) {
        log.info("Début de l'importation des candidats depuis le fichier: {}", file.getOriginalFilename());

        List<String> errors = new ArrayList<>();
        String sessionId = sessionStorage.generateSessionId();

        try {
            // Validation du fichier
            if (file.isEmpty()) {
                throw new IllegalArgumentException("Le fichier est vide");
            }

            if (!isExcelFile(file)) {
                throw new IllegalArgumentException("Le fichier doit être au format Excel (.xlsx)");
            }

            // Lecture du fichier Excel
            List<CandidatDto> candidats = excelService.readCandidatsFromExcel(file);

            if (candidats.isEmpty()) {
                throw new IllegalArgumentException("Aucun candidat trouvé dans le fichier");
            }

            // Validation des données
            validateCandidats(candidats, errors);

            // Stockage en mémoire
            sessionStorage.storeCandidats(sessionId, candidats);

            log.info("Importation réussie: {} candidats importés avec la session {}", candidats.size(), sessionId);

            return new ImportResponseDto(
                    candidats,
                    sessionId,
                    candidats.size(),
                    errors,
                    "Importation réussie"
            );

        } catch (Exception e) {
            log.error("Erreur lors de l'importation: {}", e.getMessage());
            errors.add("Erreur d'importation: " + e.getMessage());

            return new ImportResponseDto(
                    null,
                    null,
                    0,
                    errors,
                    "Échec de l'importation"
            );
        }
    }

    /**
     * Génère les convocations pour tous les candidats d'une session
     */
    public GenerateResponseDto generateConvocations(GenerateConvocationRequestDto request) {
        log.info("Génération des convocations pour la session: {}", request.getSessionId());

        try {
            List<CandidatDto> candidats = sessionStorage.getCandidats(request.getSessionId());
            if (candidats == null || candidats.isEmpty()) {
                throw new IllegalArgumentException("Aucun candidat trouvé pour cette session");
            }

            Ville ville = villeRepository.findById(request.getVilleId())
                    .orElseThrow(() -> new IllegalArgumentException("Ville non trouvée"));
            TypeExamen typeExamen = typeExamenRepository.findById(request.getTypeExamenId())
                    .orElseThrow(() -> new IllegalArgumentException("Type d'examen non trouvé"));
            Certification certification = certificationRepository.findById(request.getCertificationId())
                    .orElseThrow(() -> new IllegalArgumentException("Certification non trouvée"));
            Adresse adresse = adresseRepository.findById(request.getAdresseId())
                    .orElseThrow(() -> new IllegalArgumentException("Adresse non trouvée"));
            DureeEpreuve dureeEpreuve = dureeEpreuveRepository.findById(request.getDureeEpreuveId())
                    .orElseThrow(() -> new IllegalArgumentException("Durée d'épreuve non trouvée"));

            Map<String, byte[]> generatedFiles = new HashMap<>();

            for (CandidatDto candidat : candidats) {
                try (InputStream freshStream = request.getTemplateFile().getInputStream()) {

                    // 1. Génère le Word
                    byte[] wordFile = wordService.generateConvocationForCandidat(
                            candidat, request, freshStream,
                            ville, typeExamen, certification, adresse, dureeEpreuve
                    );

                    // 2. Convertit en PDF
                    byte[] pdfFile = DocxToPdfConverter.convertDocxToPdf(wordFile);

                    // 3. Nom du fichier PDF
                    // 3. Nom du fichier PDF
                    String fileName = wordService.generateFileName(candidat, "pdf");

                    generatedFiles.put(fileName, pdfFile);


                } catch (Exception e) {
                    log.error("Erreur lors de la génération pour {} {}: {}", candidat.getPrenom(), candidat.getNom(), e.getMessage());
                    throw new RuntimeException("Erreur lors de la génération pour " +
                            candidat.getPrenom() + " " + candidat.getNom(), e);
                }
            }

            // Création du ZIP
            byte[] zipFile = zipService.createZipArchive(generatedFiles);
            sessionStorage.storeFile(request.getSessionId(), zipFile);

            String downloadUrl = "/api/download/" + request.getSessionId();

            log.info("Génération terminée: {} fichiers générés", generatedFiles.size());

            return new GenerateResponseDto(
                    request.getSessionId(),
                    generatedFiles.size(),
                    downloadUrl,
                    "Génération réussie"
            );

        } catch (Exception e) {
            log.error("Erreur lors de la génération des convocations: {}", e.getMessage());
            throw new RuntimeException("Erreur lors de la génération: " + e.getMessage(), e);
        }
    }





    /**
     * Envoie les convocations par email
     */
    public void sendConvocationsByEmail(SendEmailRequestDto request) throws IOException {
        log.info("Envoi des convocations par email pour la session {}", request.getSessionId());

        List<CandidatDto> candidats = sessionStorage.getCandidats(request.getSessionId());
        if (candidats == null || candidats.isEmpty()) {
            throw new IllegalArgumentException("Aucun candidat trouvé pour cette session");
        }

        // Récupération du fichier ZIP
        byte[] zip = sessionStorage.getFile(request.getSessionId());
        if (zip == null) {
            throw new IllegalStateException("Le fichier ZIP n'existe pas pour cette session. Génère les convocations d'abord.");
        }

        // Extraire les fichiers PDF
        Map<CandidatDto, byte[]> convocations = zipService.extractFilesFromZip(zip, candidats);

        // Envoi des emails (on reste dans la classe, donc on peut appeler méthode privée)
        for (Map.Entry<CandidatDto, byte[]> entry : convocations.entrySet()) {
            emailService.sendConvocationEmail(entry.getKey(), entry.getValue(), request.getExamenLabel(), request.getCcEmails());

        }
    }





    /**
     * Récupère le fichier ZIP généré
     */
    public byte[] getGeneratedFile(String sessionId) {
        byte[] fileData = sessionStorage.getFile(sessionId);
        if (fileData == null) {
            throw new IllegalArgumentException("Aucun fichier trouvé pour cette session");
        }
        return fileData;
    }

    /**
     * Valide le format du fichier Excel
     */
    private boolean isExcelFile(MultipartFile file) {
        String filename = file.getOriginalFilename();
        return filename != null && filename.toLowerCase().endsWith(".xlsx");
    }

    /**
     * Valide les données des candidats
     */
    private void validateCandidats(List<CandidatDto> candidats, List<String> errors) {
        for (int i = 0; i < candidats.size(); i++) {
            CandidatDto candidat = candidats.get(i);

            // Validation de l'email
            if (!isValidEmail(candidat.getEmail())) {
                errors.add("Ligne " + (i + 2) + ": Email invalide - " + candidat.getEmail());
            }

            // Validation du nom
            if (candidat.getNom() == null || candidat.getNom().trim().isEmpty()) {
                errors.add("Ligne " + (i + 2) + ": Nom manquant");
            }

            // Validation du prénom
            if (candidat.getPrenom() == null || candidat.getPrenom().trim().isEmpty()) {
                errors.add("Ligne " + (i + 2) + ": Prénom manquant");
            }

            // Validation de la date
            if (candidat.getDatePassage() == null) {
                errors.add("Ligne " + (i + 2) + ": Date de passage manquante");
            }

            // Validation de l'heure
            if (candidat.getHeurePassage() == null) {
                errors.add("Ligne " + (i + 2) + ": Heure de passage manquante");
            }
        }
    }

    /**
     * Valide le format de l'email
     */
    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Nettoie les sessions anciennes
     */
    public void cleanupOldSessions() {
        sessionStorage.cleanup();
    }
}