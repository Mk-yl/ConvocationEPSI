package fr.epsi.b3devc2.convocationepsi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import fr.epsi.b3devc2.convocationepsi.dto.CandidatDto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
@Slf4j
public class ZipService {

    /**
     * Crée une archive ZIP contenant tous les fichiers de convocation
     */
    public byte[] createZipArchive(Map<String, byte[]> files) throws IOException {
        log.info("Création d'une archive ZIP avec {} fichiers", files.size());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            for (Map.Entry<String, byte[]> entry : files.entrySet()) {
                String fileName = entry.getKey();
                byte[] fileData = entry.getValue();

                // Créer une entrée dans le ZIP
                ZipEntry zipEntry = new ZipEntry(fileName);
                zos.putNextEntry(zipEntry);

                // Écrire le contenu du fichier
                zos.write(fileData);
                zos.closeEntry();

                log.debug("Fichier ajouté au ZIP: {}", fileName);
            }

            zos.finish();
            byte[] zipData = baos.toByteArray();

            log.info("Archive ZIP créée avec succès, taille: {} bytes", zipData.length);
            return zipData;

        } catch (IOException e) {
            log.error("Erreur lors de la création de l'archive ZIP", e);
            throw e;
        }
    }

    public Map<CandidatDto, byte[]> extractFilesFromZip(byte[] zipBytes, List<CandidatDto> candidats) throws IOException {
        Map<CandidatDto, byte[]> result = new HashMap<>();

        try (ByteArrayInputStream bais = new ByteArrayInputStream(zipBytes);
             ZipInputStream zis = new ZipInputStream(bais)) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();

                CandidatDto matching = candidats.stream()
                        .filter(c -> name.contains(c.getNom().replaceAll("[^a-zA-Z0-9]", "_")) &&
                                name.contains(c.getPrenom().replaceAll("[^a-zA-Z0-9]", "_")))
                        .findFirst()
                        .orElse(null);

                if (matching != null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    zis.transferTo(baos);
                    result.put(matching, baos.toByteArray());
                }

                zis.closeEntry();
            }
        }

        return result;
    }


    /**
     * Génère un nom de fichier ZIP basé sur la date/heure
     */
    public String generateZipFileName() {
        return "convocations_" + System.currentTimeMillis() + ".zip";
    }
}