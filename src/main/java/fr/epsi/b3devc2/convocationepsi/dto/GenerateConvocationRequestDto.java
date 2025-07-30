package fr.epsi.b3devc2.convocationepsi.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerateConvocationRequestDto {

    @NotBlank(message = "Session ID est obligatoire")
    private String sessionId;

    @NotNull (message = "Fichier modèle est obligatoire")
    private MultipartFile templateFile;

    @NotNull(message = "Ville ID est obligatoire")
    private Long villeId;

    @NotNull(message = "Type examen ID est obligatoire")
    private Long typeExamenId;

    @NotNull(message = "Certification ID est obligatoire")
    private Long certificationId;

    @NotNull(message = "Adresse ID est obligatoire")
    private Long adresseId;

    @NotNull(message = "Durée épreuve ID est obligatoire")
    private Long dureeEpreuveId;

    @NotNull(message = "Date rendu est obligatoire")
    private LocalDate dateRendu;

    @NotNull(message = "Heure rendu est obligatoire")
    private LocalTime heureRendu;

    @NotBlank(message = "Lien Drive est obligatoire")
    private String lienDrive;


    // champ pour signature image
    @NotBlank(message = "Signature image est obligatoire")
    private MultipartFile signatureImage;

}