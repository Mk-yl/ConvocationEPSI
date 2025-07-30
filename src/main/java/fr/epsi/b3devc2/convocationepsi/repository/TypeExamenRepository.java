package fr.epsi.b3devc2.convocationepsi.repository;

import fr.epsi.b3devc2.convocationepsi.model.TypeExamen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.web.bind.annotation.CrossOrigin;

@RepositoryRestResource(collectionResourceRel = "typeExamens", path = "type-examens")
@CrossOrigin
public interface TypeExamenRepository extends JpaRepository<TypeExamen, Long> {
}