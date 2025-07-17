package fr.epsi.b3devc2.convocationepsi.dto;


import fr.epsi.b3devc2.convocationepsi.model.enums.RecipientType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class RecipientDTO {
    private Long id;

    @NotBlank(message = "La civilité est obligatoire")
    private String civilite;

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    private String prenom;

    @Email(message = "L'email doit être valide")
    @NotBlank(message = "L'email est obligatoire")
    private String email;

    private String classe;

    private String groupe;

    @NotNull(message = "La date de passage est obligatoire")
    private LocalDate datePassage;

    @NotNull(message = "L'heure de passage est obligatoire")
    private LocalTime heurePassage;

    @NotBlank(message = "La salle est obligatoire")
    private String salle;

    @NotNull(message = "Le type de destinataire est obligatoire")
    private RecipientType type;

    // Méthodes utilitaires
    public String getFullName() {
        return String.format("%s %s %s", civilite, prenom, nom);
    }

    public String getFormattedDateTime() {
        return String.format("%s à %s", datePassage, heurePassage);
    }
}