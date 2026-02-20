# Punto 3 — Buenas Prácticas de API REST

**Laboratorio 4 · Arquitecturas de Software (ARSW)**  
Escuela Colombiana de Ingeniería Julio Garavito  
Fecha: 19 de febrero de 2026

---

## 1. Objetivo

Aplicar convenciones estándar de diseño REST sobre el controlador existente:

- Prefijo versionado `/api/v1/` en todas las rutas.
- Respuesta uniforme envolvente (`envelope pattern`) con `ApiResponse<T>`.
- Códigos de estado HTTP correctos para cada operación y cada escenario de error.
- DTO de entrada con validación declarativa (`@Valid`, `@NotBlank`).

---

## 2. Cambio de ruta base

### Antes
```java
@RequestMapping("/blueprints")
```

### Después
```java
@RequestMapping("/api/v1/blueprints")
```

El segmento `/api/v1/` sigue la convención de APIs REST públicas:
- `/api` — distingue recursos REST de páginas HTML u otros recursos.
- `/v1` — permite evolucionar la API en versiones futuras sin romper clientes existentes.

---

## 3. Respuesta uniforme — `ApiResponse<T>`

### Problema previo

El controlador retornaba diferentes tipos según el escenario:
- `Blueprint` directamente en éxito.
- `String` con mensaje de error en excepción.
- Sin código de estado explícito en varios casos.

### Solución — Record genérico

**Ruta:** `controllers/ApiResponse.java`

```java
public record ApiResponse<T>(int code, String message, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "execute ok", data);
    }
    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(201, "resource created", data);
    }
    public static <T> ApiResponse<T> accepted(T data) {
        return new ApiResponse<>(202, "update accepted", data);
    }
    public static <T> ApiResponse<T> badRequest(String message) {
        return new ApiResponse<>(400, message, null);
    }
    public static <T> ApiResponse<T> notFound(String message) {
        return new ApiResponse<>(404, message, null);
    }
    public static <T> ApiResponse<T> conflict(String message) {
        return new ApiResponse<>(409, message, null);
    }
}
```

**Estructura JSON de todas las respuestas:**

```json
{
  "code": 200,
  "message": "execute ok",
  "data": { ... }
}
```

Ventajas del patrón:
- El cliente siempre parsea el mismo esquema, independientemente del endpoint.
- El campo `code` permite al frontend distinguir resultado sin inspeccionar el HTTP status.
- El campo `data` es `null` en errores, evitando valores parciales.

---

## 4. Tabla de códigos HTTP implementados

| Endpoint | Escenario | Código HTTP | `ApiResponse` factory |
|---|---|---|---|
| `GET /api/v1/blueprints` | Siempre OK | **200** | `ok(data)` |
| `GET /api/v1/blueprints/{author}` | Autor encontrado | **200** | `ok(data)` |
| `GET /api/v1/blueprints/{author}` | Autor no existe | **404** | `notFound(msg)` |
| `GET /api/v1/blueprints/{author}/{name}` | Blueprint encontrado | **200** | `ok(data)` |
| `GET /api/v1/blueprints/{author}/{name}` | Blueprint no existe | **404** | `notFound(msg)` |
| `POST /api/v1/blueprints` | Creado exitosamente | **201** | `created(bp)` |
| `POST /api/v1/blueprints` | Campos vacíos/nulos | **400** | `badRequest(msg)` |
| `POST /api/v1/blueprints` | Ya existe ese blueprint | **409** | `conflict(msg)` |
| `PUT /api/v1/blueprints/{author}/{name}/points` | Punto agregado | **202** | `accepted(null)` |
| `PUT /api/v1/blueprints/{author}/{name}/points` | Blueprint no existe | **404** | `notFound(msg)` |

---

## 5. Implementación del controlador

```java
@RestController
@RequestMapping("/api/v1/blueprints")
public class BlueprintsAPIController {

    // POST → 201 Created
    @PostMapping
    public ResponseEntity<ApiResponse<?>> add(@Valid @RequestBody NewBlueprintRequest req) {
        try {
            Blueprint bp = new Blueprint(req.author(), req.name(), req.points());
            services.addNewBlueprint(bp);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(bp));          // ← 201
        } catch (BlueprintPersistenceException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.conflict(e.getMessage())); // ← 409
        }
    }

    // PUT → 202 Accepted
    @PutMapping("/{author}/{bpname}/points")
    public ResponseEntity<ApiResponse<?>> addPoint(...) {
        try {
            services.addPoint(author, bpname, p.x(), p.y());
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.accepted(null));        // ← 202
        } catch (BlueprintNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.notFound(e.getMessage())); // ← 404
        }
    }
}
```

---

## 6. DTO de entrada con validación

```java
public record NewBlueprintRequest(
        @NotBlank String author,
        @NotBlank String name,
        @Valid List<Point> points
) { }
```

`@Valid` en el parámetro del método activa Bean Validation automáticamente. Campos vacíos o nulos retornan `400 Bad Request` sin código adicional en el servicio.

---

## 7. Evidencia de pruebas

### POST exitoso — `201 Created`

```http
POST http://localhost:8080/api/v1/blueprints
Content-Type: application/json

{
  "author": "john",
  "name": "house",
  "points": [{"x":10,"y":20}, {"x":30,"y":40}]
}
```

Respuesta:
```json
{
  "code": 201,
  "message": "resource created",
  "data": {
    "author": "john",
    "name": "house",
    "points": [{"x":10,"y":20},{"x":30,"y":40}]
  }
}
```

---

### GET blueprint existente — `200 OK`

```http
GET http://localhost:8080/api/v1/blueprints/john/house
```

Respuesta:
```json
{
  "code": 200,
  "message": "execute ok",
  "data": {
    "author": "john",
    "name": "house",
    "points": [{"x":10,"y":20},{"x":30,"y":40}]
  }
}
```

---

### GET blueprint inexistente — `404 Not Found`

```http
GET http://localhost:8080/api/v1/blueprints/john/nonexistent
```

Respuesta:
```json
{
  "code": 404,
  "message": "Blueprint not found: john/nonexistent",
  "data": null
}
```

---

### POST duplicado — `409 Conflict`

```http
POST http://localhost:8080/api/v1/blueprints
{ "author": "john", "name": "house", "points": [] }
```

Respuesta:
```json
{
  "code": 409,
  "message": "Blueprint already exists: john/house",
  "data": null
}
```

---

### PUT agregar punto — `202 Accepted`

```http
PUT http://localhost:8080/api/v1/blueprints/john/house/points
Content-Type: application/json

{"x": 50, "y": 60}
```

Respuesta:
```json
{
  "code": 202,
  "message": "update accepted",
  "data": null
}
```

---

## 8. Por qué cada código

| Código | Semántica REST | Uso en el proyecto |
|---|---|---|
| **200 OK** | Operación GET completada | Lectura exitosa |
| **201 Created** | Recurso nuevo creado | `POST` exitoso |
| **202 Accepted** | Petición aceptada, procesada | `PUT` de punto (operación menor) |
| **400 Bad Request** | Datos de entrada inválidos | Campos vacíos en POST |
| **404 Not Found** | Recurso no encontrado | Author o blueprint inexistente |
| **409 Conflict** | Violación de unicidad | Blueprint ya existe |
