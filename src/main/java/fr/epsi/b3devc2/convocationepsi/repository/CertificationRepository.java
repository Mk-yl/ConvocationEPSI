package fr.epsi.b3devc2.convocationepsi.repository;


import fr.epsi.b3devc2.convocationepsi.model.Certification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.web.bind.annotation.CrossOrigin;

@RepositoryRestResource(collectionResourceRel = "certifications", path = "certifications")
@CrossOrigin
public interface CertificationRepository extends JpaRepository<Certification, Long> {
}
