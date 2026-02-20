package edu.eci.arsw.blueprints.filters;

import edu.eci.arsw.blueprints.model.Blueprint;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Filtro identidad (sin transformación): retorna el blueprint sin cambios.
 * Activo únicamente cuando NO está activo ningún otro filtro.
 * Perfiles que lo desactivan: "redundancy", "undersampling".
 */
@Component
@Profile("!redundancy & !undersampling")
public class IdentityFilter implements BlueprintsFilter {
    @Override
    public Blueprint apply(Blueprint bp) { return bp; }
}
