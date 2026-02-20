package edu.eci.arsw.blueprints.persistence.impl;

import edu.eci.arsw.blueprints.model.Blueprint;
import edu.eci.arsw.blueprints.model.Point;
import edu.eci.arsw.blueprints.persistence.BlueprintNotFoundException;
import edu.eci.arsw.blueprints.persistence.BlueprintPersistence;
import edu.eci.arsw.blueprints.persistence.BlueprintPersistenceException;
import edu.eci.arsw.blueprints.persistence.entity.BlueprintEntity;
import edu.eci.arsw.blueprints.persistence.entity.PointEmbeddable;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementación de BlueprintPersistence usando PostgreSQL a través de Spring Data JPA.
 * Activa únicamente con el perfil "postgres":
 *   mvn spring-boot:run -Dspring-boot.run.profiles=postgres
 */
@Repository
@Profile("postgres")
@Transactional
public class PostgresBlueprintPersistence implements BlueprintPersistence {

    private final BlueprintJpaRepository jpaRepository;

    public PostgresBlueprintPersistence(BlueprintJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    // ── Conversión dominio → entidad ─────────────────────────────────────────

    private BlueprintEntity toEntity(Blueprint bp) {
        List<PointEmbeddable> pts = bp.getPoints().stream()
                .map(p -> new PointEmbeddable(p.x(), p.y()))
                .collect(Collectors.toList());
        return new BlueprintEntity(bp.getAuthor(), bp.getName(), pts);
    }

    // ── Conversión entidad → dominio ─────────────────────────────────────────

    private Blueprint toDomain(BlueprintEntity entity) {
        List<Point> pts = entity.getPoints().stream()
                .map(p -> new Point(p.getX(), p.getY()))
                .collect(Collectors.toList());
        return new Blueprint(entity.getAuthor(), entity.getName(), pts);
    }

    // ── Implementación del contrato BlueprintPersistence ─────────────────────

    @Override
    public void saveBlueprint(Blueprint bp) throws BlueprintPersistenceException {
        if (jpaRepository.existsByAuthorAndName(bp.getAuthor(), bp.getName())) {
            throw new BlueprintPersistenceException(
                    "Blueprint already exists: " + bp.getAuthor() + "/" + bp.getName());
        }
        jpaRepository.save(toEntity(bp));
    }

    @Override
    public Blueprint getBlueprint(String author, String name) throws BlueprintNotFoundException {
        return jpaRepository.findByAuthorAndName(author, name)
                .map(this::toDomain)
                .orElseThrow(() -> new BlueprintNotFoundException(
                        "Blueprint not found: %s/%s".formatted(author, name)));
    }

    @Override
    public Set<Blueprint> getBlueprintsByAuthor(String author) throws BlueprintNotFoundException {
        List<BlueprintEntity> entities = jpaRepository.findByAuthor(author);
        if (entities.isEmpty()) {
            throw new BlueprintNotFoundException("No blueprints for author: " + author);
        }
        return entities.stream().map(this::toDomain).collect(Collectors.toSet());
    }

    @Override
    public Set<Blueprint> getAllBlueprints() {
        return jpaRepository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toSet());
    }

    @Override
    public void addPoint(String author, String name, int x, int y) throws BlueprintNotFoundException {
        BlueprintEntity entity = jpaRepository.findByAuthorAndName(author, name)
                .orElseThrow(() -> new BlueprintNotFoundException(
                        "Blueprint not found: %s/%s".formatted(author, name)));
        entity.getPoints().add(new PointEmbeddable(x, y));
        jpaRepository.save(entity);
    }
}
