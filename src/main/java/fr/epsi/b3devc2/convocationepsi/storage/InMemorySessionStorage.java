package fr.epsi.b3devc2.convocationepsi.storage;

import fr.epsi.b3devc2.convocationepsi.dto.CandidatDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class InMemorySessionStorage {

    private final Map<String, List<CandidatDto>> candidatsStorage = new ConcurrentHashMap<>();
    private final Map<String, byte[]> filesStorage = new ConcurrentHashMap<>();
    private final Map<String, Map<CandidatDto, byte[]>> individualFilesStorage = new ConcurrentHashMap<>();

    /**
     * Génère un nouvel ID de session
     */
    public String generateSessionId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Stocke la liste des candidats pour une session
     */
    public void storeCandidats(String sessionId, List<CandidatDto> candidats) {
        log.info("Stockage de {} candidats pour la session {}", candidats.size(), sessionId);
        candidatsStorage.put(sessionId, candidats);
    }

    /**
     * Récupère la liste des candidats pour une session
     */
    public List<CandidatDto> getCandidats(String sessionId) {
        List<CandidatDto> candidats = candidatsStorage.get(sessionId);
        if (candidats == null) {
            log.warn("Aucun candidat trouvé pour la session {}", sessionId);
        }
        return candidats;
    }

    /**
     * Stocke un fichier zip généré
     */
    public void storeFile(String sessionId, byte[] fileData) {
        log.info("Stockage du fichier zip pour la session {}", sessionId);
        filesStorage.put(sessionId, fileData);
    }

    /**
     * Récupère un fichier zip
     */
    public byte[] getFile(String sessionId) {
        byte[] fileData = filesStorage.get(sessionId);
        if (fileData == null) {
            log.warn("Aucun fichier trouvé pour la session {}", sessionId);
        }
        return fileData;
    }
    // Nouveau : stocke les fichiers individuels
    public void storeIndividualFiles(String sessionId, Map<CandidatDto, byte[]> files) {
        individualFilesStorage.put(sessionId, files);
    }

    // Nouveau : récupère les fichiers individuels
    public Map<CandidatDto, byte[]> getIndividualFiles(String sessionId) {
        return individualFilesStorage.get(sessionId);
    }

    /**
     * Supprime les données d'une session
     */
    public void clearSession(String sessionId) {
        log.info("Suppression des données de la session {}", sessionId);
        candidatsStorage.remove(sessionId);
        filesStorage.remove(sessionId);
    }

    /**
     * Vérifie si une session existe
     */
    public boolean sessionExists(String sessionId) {
        return candidatsStorage.containsKey(sessionId);
    }

    /**
     * Récupère le nombre de candidats pour une session
     */
    public int getCandidatsCount(String sessionId) {
        List<CandidatDto> candidats = candidatsStorage.get(sessionId);
        return candidats != null ? candidats.size() : 0;
    }

    /**
     * Nettoyage périodique des sessions anciennes (optionnel)
     */
    public void cleanup() {
        log.info("Nettoyage du stockage en mémoire");
        candidatsStorage.clear();
        filesStorage.clear();
    }
}