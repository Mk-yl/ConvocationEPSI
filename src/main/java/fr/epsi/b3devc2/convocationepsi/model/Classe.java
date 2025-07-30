package fr.epsi.b3devc2.convocationepsi.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Entity
@Table(name = "classe")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Classe {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;

    // Relation avec TypeExamen
    @ManyToMany
    private List<TypeExamen> typesExamen;

    // Relation avec Certification
    @ManyToMany
    private List<Certification> certifications;
}
