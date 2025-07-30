package fr.epsi.b3devc2.convocationepsi.repository;


import fr.epsi.b3devc2.convocationepsi.model.Ville;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.web.bind.annotation.CrossOrigin;

@RepositoryRestResource(collectionResourceRel = "villes", path = "villes")
@CrossOrigin
public interface VilleRepository extends JpaRepository<Ville, Long> {
}