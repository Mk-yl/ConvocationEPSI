package fr.epsi.b3devc2.convocationepsi.storage;


import fr.epsi.b3devc2.convocationepsi.dto.RecipientDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
@Service
public class InMemorySessionStorage {

    private final Map<String, List<RecipientDTO>> sessionData = new ConcurrentHashMap<>();
    private final Map<String, Map<Long, byte[]>> generatedDocuments = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public void storeRecipients(String sessionId, List<RecipientDTO> recipients) {
        // Assigner des IDs uniques aux destinataires
        recipients.forEach(recipient -> {
            if (recipient.getId() == null) {
                recipient.setId(idGenerator.getAndIncrement());
            }
        });

        sessionData.put(sessionId, recipients);
        log.info("Stockage de {} destinataires pour la session {}", recipients.size(), sessionId);
    }

    public List<RecipientDTO> getRecipients(String sessionId) {
        return sessionData.get(sessionId);
    }

    public void storeGeneratedDocument(String sessionId, Long recipientId, byte[] document) {
        generatedDocuments.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>())
                .put(recipientId, document);
        log.debug("Document généré stocké pour le destinataire {} de la session {}", recipientId, sessionId);
    }

    public byte[] getGeneratedDocument(String sessionId, Long recipientId) {
        Map<Long, byte[]> sessionDocs = generatedDocuments.get(sessionId);
        return sessionDocs != null ? sessionDocs.get(recipientId) : null;
    }

    public Map<Long, byte[]> getAllGeneratedDocuments(String sessionId) {
        return generatedDocuments.getOrDefault(sessionId, new ConcurrentHashMap<>());
    }

    public void clearSession(String sessionId) {
        sessionData.remove(sessionId);
        generatedDocuments.remove(sessionId);
        log.info("Session {} nettoyée", sessionId);
    }

    public boolean hasSession(String sessionId) {
        return sessionData.containsKey(sessionId);
    }

    public int getRecipientCount(String sessionId) {
        List<RecipientDTO> recipients = sessionData.get(sessionId);
        return recipients != null ? recipients.size() : 0;
    }

    public List<RecipientDTO> getRecipientsByType(String sessionId,  fr.epsi.b3devc2.convocationepsi.model.enums.RecipientType type) {
        List<RecipientDTO> recipients = sessionData.get(sessionId);
        if (recipients == null) return null;

        return recipients.stream()
                .filter(recipient -> recipient.getType() == type)
                .collect(Collectors.toList());
    }
}