package fr.epsi.b3devc2.convocationepsi.repository;

import fr.epsi.b3devc2.convocationepsi.model.Classe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.web.bind.annotation.CrossOrigin;

@RepositoryRestResource(collectionResourceRel = "classes", path = "classes")
@CrossOrigin
public interface ClasseRepository extends JpaRepository<Classe, Long> {

}
