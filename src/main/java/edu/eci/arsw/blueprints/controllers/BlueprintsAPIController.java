package edu.eci.arsw.blueprints.controllers;

import edu.eci.arsw.blueprints.model.Blueprint;
import edu.eci.arsw.blueprints.model.Point;
import edu.eci.arsw.blueprints.persistence.BlueprintNotFoundException;
import edu.eci.arsw.blueprints.persistence.BlueprintPersistenceException;
import edu.eci.arsw.blueprints.services.BlueprintsServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/blueprints")
@Tag(name = "Blueprints", description = "Operaciones CRUD sobre planos (Blueprints)")
public class BlueprintsAPIController {

    private final BlueprintsServices services;

    public BlueprintsAPIController(BlueprintsServices services) { this.services = services; }

    // ── Helper: URL base del contexto actual ─────────────────────────────────

    /** Construye la URL base {@code scheme://host:port/api/v1/blueprints} dinámicamente. */
    private String base() {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/blueprints")
                .toUriString();
    }

    // ── GET /api/v1/blueprints ───────────────────────────────────────────────

    @Operation(
        summary     = "Obtener todos los blueprints",
        description = "Retorna el listado completo de blueprints registrados en el sistema."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listado retornado exitosamente",
            content = @Content(schema = @Schema(implementation = edu.eci.arsw.blueprints.controllers.ApiResponse.class)))
    })
    @GetMapping
    public ResponseEntity<edu.eci.arsw.blueprints.controllers.ApiResponse<Set<Blueprint>>> getAll() {
        Set<Blueprint> all = services.getAllBlueprints();
        String b = base();
        Map<String, String> links = new LinkedHashMap<>();
        links.put("self", b);
        return ResponseEntity.ok(
                edu.eci.arsw.blueprints.controllers.ApiResponse.ok(all).withLinks(links));
    }

    // ── GET /api/v1/blueprints/{author} ─────────────────────────────────────

    @Operation(
        summary     = "Obtener blueprints por autor",
        description = "Retorna todos los blueprints creados por el autor especificado."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Blueprints del autor encontrados",
            content = @Content(schema = @Schema(implementation = edu.eci.arsw.blueprints.controllers.ApiResponse.class))),
        @ApiResponse(responseCode = "404", description = "No existen blueprints para ese autor",
            content = @Content(schema = @Schema(implementation = edu.eci.arsw.blueprints.controllers.ApiResponse.class)))
    })
    @GetMapping("/{author}")
    public ResponseEntity<edu.eci.arsw.blueprints.controllers.ApiResponse<?>> byAuthor(
            @Parameter(description = "Nombre del autor del blueprint", example = "john")
            @PathVariable String author) {
        try {
            String b = base();
            Map<String, String> links = new LinkedHashMap<>();
            links.put("self",           b + "/" + author);
            links.put("all-blueprints", b);
            return ResponseEntity.ok(
                    edu.eci.arsw.blueprints.controllers.ApiResponse
                            .ok(services.getBlueprintsByAuthor(author)).withLinks(links));
        } catch (BlueprintNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(edu.eci.arsw.blueprints.controllers.ApiResponse.notFound(e.getMessage()));
        }
    }

    // ── GET /api/v1/blueprints/{author}/{bpname} ─────────────────────────────

    @Operation(
        summary     = "Obtener un blueprint específico",
        description = "Retorna el blueprint identificado por autor y nombre."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Blueprint encontrado",
            content = @Content(schema = @Schema(implementation = edu.eci.arsw.blueprints.controllers.ApiResponse.class))),
        @ApiResponse(responseCode = "404", description = "Blueprint no encontrado",
            content = @Content(schema = @Schema(implementation = edu.eci.arsw.blueprints.controllers.ApiResponse.class)))
    })
    @GetMapping("/{author}/{bpname}")
    public ResponseEntity<edu.eci.arsw.blueprints.controllers.ApiResponse<?>> byAuthorAndName(
            @Parameter(description = "Nombre del autor", example = "john") @PathVariable String author,
            @Parameter(description = "Nombre del blueprint", example = "house") @PathVariable String bpname) {
        try {
            String b = base();
            Map<String, String> links = new LinkedHashMap<>();
            links.put("self",              b + "/" + author + "/" + bpname);
            links.put("add-point",         b + "/" + author + "/" + bpname + "/points");
            links.put("author-blueprints", b + "/" + author);
            links.put("all-blueprints",    b);
            return ResponseEntity.ok(
                    edu.eci.arsw.blueprints.controllers.ApiResponse
                            .ok(services.getBlueprint(author, bpname)).withLinks(links));
        } catch (BlueprintNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(edu.eci.arsw.blueprints.controllers.ApiResponse.notFound(e.getMessage()));
        }
    }

    // ── POST /api/v1/blueprints ──────────────────────────────────────────────

    @Operation(
        summary     = "Crear un nuevo blueprint",
        description = "Registra un nuevo blueprint con sus puntos. Retorna 409 si ya existe uno con el mismo autor y nombre."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Blueprint creado exitosamente",
            content = @Content(schema = @Schema(implementation = edu.eci.arsw.blueprints.controllers.ApiResponse.class))),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos (autor o nombre vacíos)",
            content = @Content(schema = @Schema(implementation = edu.eci.arsw.blueprints.controllers.ApiResponse.class))),
        @ApiResponse(responseCode = "409", description = "Ya existe un blueprint con ese autor y nombre",
            content = @Content(schema = @Schema(implementation = edu.eci.arsw.blueprints.controllers.ApiResponse.class)))
    })
    @PostMapping
    public ResponseEntity<edu.eci.arsw.blueprints.controllers.ApiResponse<?>> add(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Datos del blueprint a crear",
                required = true,
                content = @Content(schema = @Schema(implementation = NewBlueprintRequest.class)))
            @Valid @RequestBody NewBlueprintRequest req) {
        if (req.author() == null || req.author().isBlank()
                || req.name() == null || req.name().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(edu.eci.arsw.blueprints.controllers.ApiResponse.badRequest(
                            "author and name are required"));
        }
        try {
            Blueprint bp = new Blueprint(req.author(), req.name(), req.points());
            services.addNewBlueprint(bp);
            String b = base();
            Map<String, String> links = new LinkedHashMap<>();
            links.put("self",              b + "/" + bp.getAuthor() + "/" + bp.getName());
            links.put("add-point",         b + "/" + bp.getAuthor() + "/" + bp.getName() + "/points");
            links.put("author-blueprints", b + "/" + bp.getAuthor());
            links.put("all-blueprints",    b);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(edu.eci.arsw.blueprints.controllers.ApiResponse.created(bp).withLinks(links));
        } catch (BlueprintPersistenceException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(edu.eci.arsw.blueprints.controllers.ApiResponse.conflict(e.getMessage()));
        }
    }

    // ── PUT /api/v1/blueprints/{author}/{bpname}/points ─────────────────────

    @Operation(
        summary     = "Agregar un punto a un blueprint",
        description = "Añade un nuevo punto (x, y) al blueprint identificado por autor y nombre."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Punto agregado correctamente",
            content = @Content(schema = @Schema(implementation = edu.eci.arsw.blueprints.controllers.ApiResponse.class))),
        @ApiResponse(responseCode = "404", description = "Blueprint no encontrado",
            content = @Content(schema = @Schema(implementation = edu.eci.arsw.blueprints.controllers.ApiResponse.class)))
    })
    @PutMapping("/{author}/{bpname}/points")
    public ResponseEntity<edu.eci.arsw.blueprints.controllers.ApiResponse<?>> addPoint(
            @Parameter(description = "Nombre del autor", example = "john") @PathVariable String author,
            @Parameter(description = "Nombre del blueprint", example = "house") @PathVariable String bpname,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Punto que se desea agregar",
                required = true,
                content = @Content(schema = @Schema(implementation = Point.class)))
            @RequestBody Point p) {
        try {
            services.addPoint(author, bpname, p.x(), p.y());
            String b = base();
            Map<String, String> links = new LinkedHashMap<>();
            links.put("blueprint",      b + "/" + author + "/" + bpname);
            links.put("all-blueprints", b);
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(edu.eci.arsw.blueprints.controllers.ApiResponse.accepted(null).withLinks(links));
        } catch (BlueprintNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(edu.eci.arsw.blueprints.controllers.ApiResponse.notFound(e.getMessage()));
        }
    }

    // ── DTO de entrada ───────────────────────────────────────────────────────

    @Schema(description = "Datos requeridos para crear un nuevo Blueprint")
    public record NewBlueprintRequest(
            @Schema(description = "Autor del blueprint", example = "john") @NotBlank String author,
            @Schema(description = "Nombre del blueprint", example = "house")  @NotBlank String name,
            @Schema(description = "Lista de puntos del blueprint")            @Valid List<Point> points
    ) { }
}
