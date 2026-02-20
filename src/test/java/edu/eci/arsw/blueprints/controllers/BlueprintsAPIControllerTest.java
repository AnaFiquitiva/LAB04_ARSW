package edu.eci.arsw.blueprints.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración del controlador REST.
 * Usa el perfil "inmemory" para no requerir base de datos real.
 * Datos precargados: john/house, john/garage, jane/garden.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("inmemory")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class BlueprintsAPIControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String BASE = "/api/v1/blueprints";

    // ── GET /api/v1/blueprints ───────────────────────────────────────────────

    @Test
    @DisplayName("GET todos los blueprints → 200 con 3 elementos precargados")
    void getAllShouldReturn200WithPreloadedData() throws Exception {
        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("execute ok"))
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$._links.self").exists());
    }

    // ── GET /api/v1/blueprints/{author} ─────────────────────────────────────

    @Test
    @DisplayName("GET por autor existente → 200 con sus blueprints")
    void getByAuthorShouldReturn200() throws Exception {
        mockMvc.perform(get(BASE + "/john"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$._links.self").value(containsString("/john")))
                .andExpect(jsonPath("$._links.all-blueprints").exists());
    }

    @Test
    @DisplayName("GET por autor inexistente → 404")
    void getByUnknownAuthorShouldReturn404() throws Exception {
        mockMvc.perform(get(BASE + "/noexiste"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    // ── GET /api/v1/blueprints/{author}/{name} ───────────────────────────────

    @Test
    @DisplayName("GET blueprint existente → 200 con HATEOAS links")
    void getBlueprintShouldReturn200WithLinks() throws Exception {
        mockMvc.perform(get(BASE + "/john/house"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.author").value("john"))
                .andExpect(jsonPath("$.data.name").value("house"))
                .andExpect(jsonPath("$.data.points", hasSize(4)))
                .andExpect(jsonPath("$._links.self").value(containsString("/john/house")))
                .andExpect(jsonPath("$._links.add-point").value(containsString("/points")))
                .andExpect(jsonPath("$._links.author-blueprints").value(containsString("/john")))
                .andExpect(jsonPath("$._links.all-blueprints").exists());
    }

    @Test
    @DisplayName("GET blueprint inexistente → 404")
    void getUnknownBlueprintShouldReturn404() throws Exception {
        mockMvc.perform(get(BASE + "/john/noexiste"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    // ── POST /api/v1/blueprints ──────────────────────────────────────────────

    @Test
    @DisplayName("POST blueprint nuevo → 201 Created con HATEOAS links")
    void postNewBlueprintShouldReturn201() throws Exception {
        String body = """
                {"author":"test","name":"new_bp","points":[{"x":1,"y":2},{"x":3,"y":4}]}
                """;

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message").value("resource created"))
                .andExpect(jsonPath("$.data.author").value("test"))
                .andExpect(jsonPath("$.data.name").value("new_bp"))
                .andExpect(jsonPath("$._links.self").value(containsString("/test/new_bp")))
                .andExpect(jsonPath("$._links.add-point").value(containsString("/points")));
    }

    @Test
    @DisplayName("POST blueprint duplicado → 409 Conflict")
    void postDuplicateBlueprintShouldReturn409() throws Exception {
        String body = """
                {"author":"john","name":"house","points":[{"x":0,"y":0}]}
                """;

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("POST con autor vacío → 400 Bad Request")
    void postWithBlankAuthorShouldReturn400() throws Exception {
        String body = """
                {"author":"","name":"test","points":[]}
                """;

        // Spring Validation lanza MethodArgumentNotValidException antes de que llegue
        // al controlador → 400 con cuerpo vacío (comportamiento nativo de Spring).
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ── PUT /api/v1/blueprints/{author}/{name}/points ────────────────────────

    @Test
    @DisplayName("PUT agregar punto a blueprint existente → 202 Accepted con links")
    void addPointToExistingBlueprintShouldReturn202() throws Exception {
        mockMvc.perform(put(BASE + "/john/house/points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"x\":99,\"y\":88}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.code").value(202))
                .andExpect(jsonPath("$.message").value("update accepted"))
                .andExpect(jsonPath("$._links.blueprint").value(containsString("/john/house")))
                .andExpect(jsonPath("$._links.all-blueprints").exists());
    }

    @Test
    @DisplayName("PUT agregar punto a blueprint inexistente → 404 Not Found")
    void addPointToUnknownBlueprintShouldReturn404() throws Exception {
        mockMvc.perform(put(BASE + "/john/noexiste/points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"x\":1,\"y\":1}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }
}
