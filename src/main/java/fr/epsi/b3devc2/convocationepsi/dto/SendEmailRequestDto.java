package fr.epsi.b3devc2.convocationepsi.dto;


import lombok.Data;

import java.util.List;

@Data
public class SendEmailRequestDto {
    private String sessionId;             // Pour récupérer les convocations
    private String examenLabel;           // Pour l'objet du mail
    private List<String> ccEmails;        // Pour mettre en copie
}
