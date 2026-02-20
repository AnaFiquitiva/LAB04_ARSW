# Punto 4 — Documentación con OpenAPI / Swagger

**Laboratorio 4 · Arquitecturas de Software (ARSW)**  
Escuela Colombiana de Ingeniería Julio Garavito  
Fecha: 19 de febrero de 2026

---

## 1. Objetivo

Integrar **springdoc-openapi** para generar automáticamente la especificación OpenAPI 3.0 de la API REST, y habilitar la interfaz **Swagger UI** para explorar y probar los endpoints directamente desde el navegador.

---

## 2. Dependencia Maven

La librería fue incluida en `pom.xml` desde el inicio del proyecto:

```xml
<dependency>
  <groupId>org.springdoc</groupId>
  <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
  <version>2.6.0</version>
</dependency>
```

Esta dependencia provee:
- Auto-configuración del bean `OpenAPI`.
- Servlet que genera `/v3/api-docs` (JSON OpenAPI 3.0).
- Interfaz Swagger UI en `/swagger-ui.html`.

---

## 3. Configuración global — `OpenApiConfig.java`

**Ruta:** `config/OpenApiConfig.java`

```java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI api() {
        return new OpenAPI()
                .info(new Info()
                        .title("ARSW Blueprints API")
                        .version("v1.0.0")
                        .description("""
                                REST API para gestión de Blueprints.
                                Laboratorio #4 – Arquitecturas de Software (ARSW).
                                Escuela Colombiana de Ingeniería Julio Garavito.
                                """)
                        .contact(new Contact()
                                .name("Escuela Colombiana de Ingeniería")
                                .url("https://www.escuelaing.edu.co"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080")
                                    .description("Servidor local")
                ));
    }
}
```

**Campos configurados:**

| Campo | Valor |
|---|---|
| Título | `ARSW Blueprints API` |
| Versión | `v1.0.0` |
| Contacto | Escuela Colombiana de Ingeniería |
| Licencia | MIT License |
| Servidor | `http://localhost:8080` |

---

## 4. Anotaciones en el controlador

### 4.1 `@Tag` — Agrupación de endpoints

```java
@RestController
@RequestMapping("/api/v1/blueprints")
@Tag(name = "Blueprints", description = "Operaciones CRUD sobre planos (Blueprints)")
public class BlueprintsAPIController { ... }
```

Todos los endpoints del controlador aparecen agrupados bajo la etiqueta **Blueprints** en Swagger UI.

---

### 4.2 `@Operation` y `@ApiResponses` — Por método

Cada endpoint recibe:
- `@Operation(summary, description)` — título breve y descripción larga.
- `@ApiResponses` — lista de posibles respuestas con código HTTP, descripción y esquema.

**Ejemplo — GET todos:**
```java
@Operation(
    summary     = "Obtener todos los blueprints",
    description = "Retorna el listado completo de blueprints registrados en el sistema."
)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Listado retornado exitosamente",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
})
@GetMapping
public ResponseEntity<...> getAll() { ... }
```

**Ejemplo — POST crear:**
```java
@Operation(
    summary     = "Crear un nuevo blueprint",
    description = "Crea y persiste un nuevo blueprint. Retorna 409 si ya existe uno con el mismo autor y nombre."
)
@ApiResponses({
    @ApiResponse(responseCode = "201", description = "Blueprint creado exitosamente",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @ApiResponse(responseCode = "409", description = "Blueprint ya existe",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
})
@PostMapping
public ResponseEntity<...> add(@Valid @RequestBody NewBlueprintRequest req) { ... }
```

---

### 4.3 `@Parameter` — Variables de ruta

```java
@GetMapping("/{author}/{bpname}")
public ResponseEntity<...> byAuthorAndName(
    @Parameter(description = "Nombre del autor",    example = "john") @PathVariable String author,
    @Parameter(description = "Nombre del blueprint", example = "house") @PathVariable String bpname
) { ... }
```

Swagger UI muestra cada campo del path con descripción y valor de ejemplo pre-cargado.

---

### 4.4 `@Schema` — DTO de entrada

```java
@Schema(description = "Datos requeridos para crear un nuevo Blueprint")
public record NewBlueprintRequest(
    @Schema(description = "Autor del blueprint",       example = "john")  @NotBlank String author,
    @Schema(description = "Nombre del blueprint",      example = "house") @NotBlank String name,
    @Schema(description = "Lista de puntos del blueprint")                @Valid List<Point> points
) { }
```

Permite que Swagger UI genere el formulario de prueba con campos prellenados y descripciones inline.

---

## 5. Tabla completa de endpoints documentados

| Método | Ruta | Summary | Códigos documentados |
|---|---|---|---|
| `GET` | `/api/v1/blueprints` | Obtener todos los blueprints | 200 |
| `GET` | `/api/v1/blueprints/{author}` | Obtener blueprints por autor | 200, 404 |
| `GET` | `/api/v1/blueprints/{author}/{bpname}` | Obtener un blueprint específico | 200, 404 |
| `POST` | `/api/v1/blueprints` | Crear un nuevo blueprint | 201, 400, 409 |
| `PUT` | `/api/v1/blueprints/{author}/{bpname}/points` | Agregar un punto a un blueprint | 202, 404 |

---

## 6. URLs de acceso

| Recurso | URL |
|---|---|
| Swagger UI (interfaz gráfica) | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON (spec completo) | http://localhost:8080/v3/api-docs |

---

## 7. Activación y prueba

```powershell
# Iniciar la aplicación (perfil inmemory)
mvn spring-boot:run

# Verificar que el JSON OpenAPI responde
Invoke-WebRequest http://localhost:8080/v3/api-docs -UseBasicParsing | Select-Object StatusCode

# Abrir Swagger UI en el navegador
Start-Process "http://localhost:8080/swagger-ui.html"
```

---

## 8. Evidencia — Respuesta del spec OpenAPI

Consulta al endpoint `/v3/api-docs` verificando metadata y endpoints registrados:

```
info.title       : ARSW Blueprints API
info.version     : v1.0.0
info.contact     : Escuela Colombiana de Ingeniería (https://www.escuelaing.edu.co)
info.license     : MIT License (https://opensource.org/licenses/MIT)

Endpoints detectados:
PUT  /api/v1/blueprints/{author}/{bpname}/points  → Agregar un punto a un blueprint
GET  /api/v1/blueprints                           → Obtener todos los blueprints
POST /api/v1/blueprints                           → Crear un nuevo blueprint
GET  /api/v1/blueprints/{author}                  → Obtener blueprints por autor
GET  /api/v1/blueprints/{author}/{bpname}         → Obtener un blueprint específico
```

---

## 9. Anotaciones utilizadas — Referencia rápida

| Anotación | Paquete | Propósito |
|---|---|---|
| `@Tag` | `io.swagger.v3.oas.annotations.tags` | Agrupa endpoints en Swagger UI |
| `@Operation` | `io.swagger.v3.oas.annotations` | Documenta un solo método HTTP |
| `@ApiResponses` | `io.swagger.v3.oas.annotations.responses` | Contenedor de múltiples `@ApiResponse` |
| `@ApiResponse` | `io.swagger.v3.oas.annotations.responses` | Documenta un código HTTP de respuesta |
| `@Content` | `io.swagger.v3.oas.annotations.media` | Especifica el media type de la respuesta |
| `@Schema` | `io.swagger.v3.oas.annotations.media` | Documenta un campo, clase o DTO |
| `@Parameter` | `io.swagger.v3.oas.annotations` | Documenta un parámetro de ruta/query |

---

## 10. Archivos modificados / creados

| Archivo | Cambio |
|---|---|
| `pom.xml` | `springdoc-openapi-starter-webmvc-ui:2.6.0` (ya existía) |
| `config/OpenApiConfig.java` | Bean `OpenAPI` con `Info`, `Contact`, `License`, `Server` |
| `controllers/BlueprintsAPIController.java` | `@Tag`, `@Operation`, `@ApiResponses`, `@Parameter`, `@Schema` |
