package fr.epsi.b3devc2.convocationepsi.repository;


import fr.epsi.b3devc2.convocationepsi.model.DureeEpreuve;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.web.bind.annotation.CrossOrigin;

@RepositoryRestResource(collectionResourceRel = "dureeEpreuves", path = "duree-epreuves")
@CrossOrigin
public interface DureeEpreuveRepository extends JpaRepository<DureeEpreuve, Long> {
}