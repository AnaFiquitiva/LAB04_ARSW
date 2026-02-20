package edu.eci.arsw.blueprints.controllers;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Map;

/**
 * Respuesta uniforme para todos los endpoints de la API (Level 3 REST — HATEOAS).
 *
 * <pre>
 * {
 *   "code": 200,
 *   "message": "execute ok",
 *   "data": { ... },
 *   "_links": {
 *     "self":              "http://localhost:8080/api/v1/blueprints/john/house",
 *     "author-blueprints": "http://localhost:8080/api/v1/blueprints/john",
 *     "all-blueprints":    "http://localhost:8080/api/v1/blueprints"
 *   }
 * }
 * </pre>
 *
 * @param <T> Tipo del cuerpo de datos retornado.
 */
public class ApiResponse<T> {

    private final int code;
    private final String message;
    private final T data;

    /** Controles hipermedia (Level 3 REST — HATEOAS). */
    @JsonProperty("_links")
    private final Map<String, String> links;

    // ── Constructor completo ─────────────────────────────────────────────────

    public ApiResponse(int code, String message, T data, Map<String, String> links) {
        this.code    = code;
        this.message = message;
        this.data    = data;
        this.links   = links != null ? links : Collections.emptyMap();
    }

    private ApiResponse(int code, String message, T data) {
        this(code, message, data, Collections.emptyMap());
    }

    // ── Getters (necesarios para serialización Jackson) ──────────────────────

    public int getCode()                  { return code; }
    public String getMessage()            { return message; }
    public T getData()                    { return data; }
    public Map<String, String> getLinks() { return links; }

    // ── Fluent: añade links a una respuesta ya creada ────────────────────────

    /**
     * Retorna una nueva instancia con los controles hipermedia indicados.
     *
     * @param links Mapa {@code nombre → URL} de las acciones disponibles.
     */
    public ApiResponse<T> withLinks(Map<String, String> links) {
        return new ApiResponse<>(this.code, this.message, this.data, links);
    }

    // ── Factory methods ──────────────────────────────────────────────────────

    /** 200 OK */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "execute ok", data);
    }

    /** 201 Created */
    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(201, "resource created", data);
    }

    /** 202 Accepted */
    public static <T> ApiResponse<T> accepted(T data) {
        return new ApiResponse<>(202, "update accepted", data);
    }

    /** 400 Bad Request */
    public static <T> ApiResponse<T> badRequest(String message) {
        return new ApiResponse<>(400, message, null);
    }

    /** 404 Not Found */
    public static <T> ApiResponse<T> notFound(String message) {
        return new ApiResponse<>(404, message, null);
    }

    /** 409 Conflict */
    public static <T> ApiResponse<T> conflict(String message) {
        return new ApiResponse<>(409, message, null);
    }
}
