package fr.epsi.b3devc2.convocationepsi.dto;


import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidatDto {
    private String groupe;
    private String civilite;
    private String nom;
    private String prenom;
    private String email;
    private LocalDate datePassage;
    private LocalTime heurePassage;
    private String salle;
    private String numeroJury;
}