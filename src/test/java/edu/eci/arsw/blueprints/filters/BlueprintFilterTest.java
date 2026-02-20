package edu.eci.arsw.blueprints.filters;

import edu.eci.arsw.blueprints.model.Blueprint;
import edu.eci.arsw.blueprints.model.Point;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para los tres filtros de blueprints.
 * No requieren contexto de Spring — se instancian directamente.
 */
class BlueprintFilterTest {

    // ── IdentityFilter ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("IdentityFilter")
    class IdentityFilterTests {

        private IdentityFilter filter;

        @BeforeEach
        void setUp() { filter = new IdentityFilter(); }

        @Test
        @DisplayName("Retorna el mismo blueprint sin modificar los puntos")
        void shouldReturnBlueprintUnchanged() {
            Blueprint bp = new Blueprint("john", "house",
                    List.of(new Point(1, 2), new Point(3, 4)));

            Blueprint result = filter.apply(bp);

            assertEquals("john", result.getAuthor());
            assertEquals("house", result.getName());
            assertEquals(2, result.getPoints().size());
            assertEquals(new Point(1, 2), result.getPoints().get(0));
            assertEquals(new Point(3, 4), result.getPoints().get(1));
        }

        @Test
        @DisplayName("Acepta blueprint sin puntos y lo retorna sin cambios")
        void shouldHandleEmptyPoints() {
            Blueprint bp = new Blueprint("john", "empty", List.of());
            Blueprint result = filter.apply(bp);
            assertTrue(result.getPoints().isEmpty());
        }
    }

    // ── RedundancyFilter ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("RedundancyFilter")
    class RedundancyFilterTests {

        private RedundancyFilter filter;

        @BeforeEach
        void setUp() { filter = new RedundancyFilter(); }

        @Test
        @DisplayName("Elimina duplicados consecutivos simples")
        void shouldRemoveConsecutiveDuplicates() {
            Blueprint bp = new Blueprint("test", "bp",
                    List.of(new Point(1, 1), new Point(1, 1), new Point(2, 2)));

            Blueprint result = filter.apply(bp);

            assertEquals(2, result.getPoints().size());
            assertEquals(new Point(1, 1), result.getPoints().get(0));
            assertEquals(new Point(2, 2), result.getPoints().get(1));
        }

        @Test
        @DisplayName("Elimina múltiples bloques de duplicados consecutivos")
        void shouldRemoveMultipleConsecutiveBlocks() {
            // (1,1),(1,1),(2,2),(2,2),(2,2),(3,3) → (1,1),(2,2),(3,3)
            Blueprint bp = new Blueprint("test", "bp",
                    List.of(
                            new Point(1, 1), new Point(1, 1),
                            new Point(2, 2), new Point(2, 2), new Point(2, 2),
                            new Point(3, 3)
                    ));

            Blueprint result = filter.apply(bp);

            assertEquals(3, result.getPoints().size());
            assertEquals(new Point(1, 1), result.getPoints().get(0));
            assertEquals(new Point(2, 2), result.getPoints().get(1));
            assertEquals(new Point(3, 3), result.getPoints().get(2));
        }

        @Test
        @DisplayName("No elimina puntos iguales no consecutivos")
        void shouldNotRemoveNonConsecutiveDuplicates() {
            // (1,1),(2,2),(1,1) → los tres se conservan (no son consecutivos entre sí)
            Blueprint bp = new Blueprint("test", "bp",
                    List.of(new Point(1, 1), new Point(2, 2), new Point(1, 1)));

            Blueprint result = filter.apply(bp);

            assertEquals(3, result.getPoints().size());
        }

        @Test
        @DisplayName("Acepta lista vacía sin lanzar excepción")
        void shouldHandleEmptyList() {
            Blueprint bp = new Blueprint("test", "bp", List.of());
            Blueprint result = filter.apply(bp);
            assertTrue(result.getPoints().isEmpty());
        }

        @Test
        @DisplayName("Conserva el nombre y autor del blueprint")
        void shouldPreserveMetadata() {
            Blueprint bp = new Blueprint("author", "name",
                    List.of(new Point(0, 0), new Point(0, 0)));

            Blueprint result = filter.apply(bp);

            assertEquals("author", result.getAuthor());
            assertEquals("name", result.getName());
        }
    }

    // ── UndersamplingFilter ──────────────────────────────────────────────────

    @Nested
    @DisplayName("UndersamplingFilter")
    class UndersamplingFilterTests {

        private UndersamplingFilter filter;

        @BeforeEach
        void setUp() { filter = new UndersamplingFilter(); }

        @Test
        @DisplayName("Conserva solo los puntos en índices pares (1 de cada 2)")
        void shouldKeepEvenIndexedPoints() {
            // índices: 0→(1,1) 1→(2,2) 2→(3,3) 3→(4,4) 4→(5,5) 5→(6,6)
            // resultado: índices 0,2,4 → (1,1),(3,3),(5,5)
            Blueprint bp = new Blueprint("test", "bp",
                    List.of(
                            new Point(1, 1), new Point(2, 2),
                            new Point(3, 3), new Point(4, 4),
                            new Point(5, 5), new Point(6, 6)
                    ));

            Blueprint result = filter.apply(bp);

            assertEquals(3, result.getPoints().size());
            assertEquals(new Point(1, 1), result.getPoints().get(0));
            assertEquals(new Point(3, 3), result.getPoints().get(1));
            assertEquals(new Point(5, 5), result.getPoints().get(2));
        }

        @Test
        @DisplayName("No transforma blueprints con 2 o menos puntos")
        void shouldNotTransformWhenTwoOrFewerPoints() {
            Blueprint bp2 = new Blueprint("test", "bp",
                    List.of(new Point(1, 1), new Point(2, 2)));
            assertEquals(2, filter.apply(bp2).getPoints().size());

            Blueprint bp1 = new Blueprint("test", "bp", List.of(new Point(1, 1)));
            assertEquals(1, filter.apply(bp1).getPoints().size());
        }

        @Test
        @DisplayName("Maneja número impar de puntos correctamente")
        void shouldHandleOddNumberOfPoints() {
            // 5 puntos → índices 0,2,4 → 3 puntos
            Blueprint bp = new Blueprint("test", "bp",
                    List.of(
                            new Point(1, 1), new Point(2, 2), new Point(3, 3),
                            new Point(4, 4), new Point(5, 5)
                    ));

            Blueprint result = filter.apply(bp);

            assertEquals(3, result.getPoints().size());
            assertEquals(new Point(1, 1), result.getPoints().get(0));
            assertEquals(new Point(3, 3), result.getPoints().get(1));
            assertEquals(new Point(5, 5), result.getPoints().get(2));
        }

        @Test
        @DisplayName("Conserva el nombre y autor del blueprint")
        void shouldPreserveMetadata() {
            Blueprint bp = new Blueprint("autor", "plano",
                    List.of(new Point(1, 1), new Point(2, 2), new Point(3, 3)));

            Blueprint result = filter.apply(bp);

            assertEquals("autor", result.getAuthor());
            assertEquals("plano", result.getName());
        }
    }
}
