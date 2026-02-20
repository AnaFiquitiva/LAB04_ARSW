package edu.eci.arsw.blueprints.persistence.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import static jakarta.persistence.FetchType.EAGER;

/**
 * Entidad JPA que representa un Blueprint en la base de datos PostgreSQL.
 * Se mapea a la tabla "blueprints"; los puntos se almacenan en "blueprint_points".
 */
@Entity
@Table(
    name = "blueprints",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_blueprint_author_name",
        columnNames = {"author", "name"}
    )
)
public class BlueprintEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String author;

    @Column(nullable = false)
    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "blueprint_points",
        joinColumns = @JoinColumn(name = "blueprint_id")
    )
    @OrderColumn(name = "point_order")
    private List<PointEmbeddable> points = new ArrayList<>();

    /** Constructor sin argumentos requerido por JPA. */
    public BlueprintEntity() { }

    public BlueprintEntity(String author, String name, List<PointEmbeddable> points) {
        this.author = author;
        this.name   = name;
        if (points != null) this.points.addAll(points);
    }

    public Long getId()                      { return id; }
    public String getAuthor()                { return author; }
    public String getName()                  { return name; }
    public List<PointEmbeddable> getPoints() { return points; }

    public void setId(Long id)               { this.id = id; }
    public void setAuthor(String author)     { this.author = author; }
    public void setName(String name)         { this.name = name; }
    public void setPoints(List<PointEmbeddable> points) { this.points = points; }
}
