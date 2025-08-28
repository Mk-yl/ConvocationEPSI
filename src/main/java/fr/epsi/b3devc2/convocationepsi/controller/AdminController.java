package fr.epsi.b3devc2.convocationepsi.controller;

import fr.epsi.b3devc2.convocationepsi.model.*;
import fr.epsi.b3devc2.convocationepsi.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminController {

    private final VilleRepository villeRepo;
    private final AdresseRepository adresseRepo;
    private final CertificationRepository certificationRepo;
    private final ClasseRepository classeRepo;
    private final DureeEpreuveRepository dureeRepo;
    private final TypeExamenRepository typeExamenRepo;

    // === Ville ===
    @GetMapping("/villes")
    public List<Ville> getAllVilles() { return villeRepo.findAll(); }

    @PostMapping("/villes")
    public Ville createVille(@RequestBody Ville v) { return villeRepo.save(v); }

    @PutMapping("/villes/{id}")
    public Ville updateVille(@PathVariable Long id, @RequestBody Ville v) {
        Ville existing = villeRepo.findById(id).orElseThrow();
        existing.setNom(v.getNom());
        return villeRepo.save(existing);
    }

    @DeleteMapping("/villes/{id}")
    public void deleteVille(@PathVariable Long id) { villeRepo.deleteById(id); }

    // === Adresse ===
    @GetMapping("/adresses")
    public List<Adresse> getAllAdresses() { return adresseRepo.findAll(); }

    @PostMapping("/adresses")
    public Adresse createAdresse(@RequestBody Adresse a) { return adresseRepo.save(a); }

    @PutMapping("/adresses/{id}")
    public Adresse updateAdresse(@PathVariable Long id, @RequestBody Adresse a) {
        Adresse existing = adresseRepo.findById(id).orElseThrow();
        existing.setRue(a.getRue());
        return adresseRepo.save(existing);
    }

    @DeleteMapping("/adresses/{id}")
    public void deleteAdresse(@PathVariable Long id) { adresseRepo.deleteById(id); }

    // === Certification ===
    @GetMapping("/certifications")
    public List<Certification> getAllCertifications() { return certificationRepo.findAll(); }

    @PostMapping("/certifications")
    public Certification createCertification(@RequestBody Certification c) {
        return certificationRepo.save(c);
    }

    @PutMapping("/certifications/{id}")
    public Certification updateCertification(@PathVariable Long id, @RequestBody Certification c) {
        Certification existing = certificationRepo.findById(id).orElseThrow();
        existing.setNom(c.getNom());
        existing.setDescription(c.getDescription());
        return certificationRepo.save(existing);
    }

    @DeleteMapping("/certifications/{id}")
    public void deleteCertification(@PathVariable Long id) { certificationRepo.deleteById(id); }

    // === TypeExamen ===
    @GetMapping("/types-examen")
    public List<TypeExamen> getAllTypeExamen() { return typeExamenRepo.findAll(); }

    @PostMapping("/types-examen")
    public TypeExamen createTypeExamen(@RequestBody TypeExamen t) { return typeExamenRepo.save(t); }

    @PutMapping("/types-examen/{id}")
    public TypeExamen updateTypeExamen(@PathVariable Long id, @RequestBody TypeExamen t) {
        TypeExamen existing = typeExamenRepo.findById(id).orElseThrow();
        existing.setNom(t.getNom());
        existing.setDescription(t.getDescription());
        return typeExamenRepo.save(existing);
    }

    @DeleteMapping("/types-examen/{id}")
    public void deleteTypeExamen(@PathVariable Long id) { typeExamenRepo.deleteById(id); }

    // === DureeEpreuve ===
    @GetMapping("/durees")
    public List<DureeEpreuve> getAllDurees() { return dureeRepo.findAll(); }

    @PostMapping("/durees")
    public DureeEpreuve createDuree(@RequestBody DureeEpreuve d) { return dureeRepo.save(d); }

    @PutMapping("/durees/{id}")
    public DureeEpreuve updateDuree(@PathVariable Long id, @RequestBody DureeEpreuve d) {
        DureeEpreuve existing = dureeRepo.findById(id).orElseThrow();
        existing.setNom(d.getNom());
        return dureeRepo.save(existing);
    }

    @DeleteMapping("/durees/{id}")
    public void deleteDuree(@PathVariable Long id) { dureeRepo.deleteById(id); }

    // === Classe ===
    @GetMapping("/classes")
    public List<Classe> getAllClasses() { return classeRepo.findAll(); }

    @PostMapping("/classes")
    public Classe createClasse(@RequestBody Classe c) { return classeRepo.save(c); }

    @PutMapping("/classes/{id}")
    public Classe updateClasse(@PathVariable Long id, @RequestBody Classe c) {
        Classe existing = classeRepo.findById(id).orElseThrow();
        existing.setNom(c.getNom());
        existing.setCertifications(c.getCertifications());
        existing.setTypesExamen(c.getTypesExamen());
        return classeRepo.save(existing);
    }

    @DeleteMapping("/classes/{id}")
    public void deleteClasse(@PathVariable Long id) { classeRepo.deleteById(id); }
}
