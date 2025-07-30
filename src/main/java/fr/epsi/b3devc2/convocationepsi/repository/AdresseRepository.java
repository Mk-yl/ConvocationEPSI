package fr.epsi.b3devc2.convocationepsi.repository;


import fr.epsi.b3devc2.convocationepsi.model.Adresse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.web.bind.annotation.CrossOrigin;

@RepositoryRestResource(collectionResourceRel = "adresses", path = "adresses")
@CrossOrigin
public interface AdresseRepository extends JpaRepository<Adresse, Long> {
}