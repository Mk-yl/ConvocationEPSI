package fr.epsi.b3devc2.convocationepsi.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "duree_epreuve")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DureeEpreuve {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;
}