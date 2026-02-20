package edu.eci.arsw.blueprints.persistence.impl;

import edu.eci.arsw.blueprints.persistence.entity.BlueprintEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para BlueprintEntity.
 * Spring genera la implementación en tiempo de ejecución.
 */
public interface BlueprintJpaRepository extends JpaRepository<BlueprintEntity, Long> {

    Optional<BlueprintEntity> findByAuthorAndName(String author, String name);

    List<BlueprintEntity> findByAuthor(String author);

    boolean existsByAuthorAndName(String author, String name);
}
