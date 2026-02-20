package edu.eci.arsw.blueprints.persistence.entity;

import jakarta.persistence.Embeddable;

/**
 * Embeddable JPA para almacenar un punto (x, y) como parte de BlueprintEntity.
 * Se guarda en la tabla blueprint_points mediante @ElementCollection.
 */
@Embeddable
public class PointEmbeddable {

    private int x;
    private int y;

    /** Constructor sin argumentos requerido por JPA. */
    public PointEmbeddable() { }

    public PointEmbeddable(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() { return x; }
    public int getY() { return y; }

    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }
}
