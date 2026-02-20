package edu.eci.arsw.blueprints.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para la clase ApiResponse.
 * Verifican los factory methods, los códigos HTTP, los mensajes y el campo _links.
 */
class ApiResponseTest {

    @Test
    @DisplayName("ok() retorna código 200 y mensaje correcto")
    void okShouldReturn200() {
        ApiResponse<String> r = ApiResponse.ok("payload");
        assertEquals(200, r.getCode());
        assertEquals("execute ok", r.getMessage());
        assertEquals("payload", r.getData());
        assertTrue(r.getLinks().isEmpty());
    }

    @Test
    @DisplayName("created() retorna código 201 y mensaje correcto")
    void createdShouldReturn201() {
        ApiResponse<String> r = ApiResponse.created("nuevo");
        assertEquals(201, r.getCode());
        assertEquals("resource created", r.getMessage());
        assertEquals("nuevo", r.getData());
    }

    @Test
    @DisplayName("accepted() retorna código 202")
    void acceptedShouldReturn202() {
        ApiResponse<Void> r = ApiResponse.accepted(null);
        assertEquals(202, r.getCode());
        assertEquals("update accepted", r.getMessage());
        assertNull(r.getData());
    }

    @Test
    @DisplayName("badRequest() retorna código 400 con mensaje personalizado y data null")
    void badRequestShouldReturn400() {
        ApiResponse<Object> r = ApiResponse.badRequest("campo requerido");
        assertEquals(400, r.getCode());
        assertEquals("campo requerido", r.getMessage());
        assertNull(r.getData());
    }

    @Test
    @DisplayName("notFound() retorna código 404 con mensaje personalizado")
    void notFoundShouldReturn404() {
        ApiResponse<Object> r = ApiResponse.notFound("recurso no existe");
        assertEquals(404, r.getCode());
        assertEquals("recurso no existe", r.getMessage());
        assertNull(r.getData());
    }

    @Test
    @DisplayName("conflict() retorna código 409")
    void conflictShouldReturn409() {
        ApiResponse<Object> r = ApiResponse.conflict("ya existe");
        assertEquals(409, r.getCode());
        assertEquals("ya existe", r.getMessage());
        assertNull(r.getData());
    }

    @Test
    @DisplayName("withLinks() agrega el mapa _links a una respuesta existente")
    void withLinksShouldAttachHypermediaLinks() {
        Map<String, String> links = Map.of(
                "self",           "http://localhost:8080/api/v1/blueprints/john/house",
                "all-blueprints", "http://localhost:8080/api/v1/blueprints"
        );

        ApiResponse<String> r = ApiResponse.ok("data").withLinks(links);

        assertEquals(200, r.getCode());
        assertEquals(2, r.getLinks().size());
        assertEquals("http://localhost:8080/api/v1/blueprints/john/house",
                r.getLinks().get("self"));
        assertEquals("http://localhost:8080/api/v1/blueprints",
                r.getLinks().get("all-blueprints"));
    }

    @Test
    @DisplayName("withLinks() no muta la instancia original")
    void withLinksShouldBeImmutable() {
        ApiResponse<String> original = ApiResponse.ok("data");
        ApiResponse<String> withLinks = original.withLinks(Map.of("self", "http://x"));

        assertTrue(original.getLinks().isEmpty(),
                "La instancia original no debe tener links");
        assertEquals(1, withLinks.getLinks().size());
    }
}
