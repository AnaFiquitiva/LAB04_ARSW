# Laboratorio #4 — REST API Blueprints

**Escuela Colombiana de Ingeniería Julio Garavito**  
**Curso:** Arquitecturas de Software (ARSW)  
**Fecha:** 19 de febrero de 2026

---

## Descripción

API REST para gestión de planos (blueprints) desarrollada con **Java 21** y **Spring Boot 3.3.9**. Implementa los cinco niveles de madurez progresiva de una API REST: desde persistencia en memoria hasta HATEOAS, pasando por PostgreSQL, buenas prácticas HTTP, documentación OpenAPI y filtros intercambiables mediante Spring Profiles.

---

## Requisitos previos

| Herramienta | Versión mínima |
|---|---|
| Java (JDK) | 21 |
| Maven | 3.9+ |
| Docker Desktop | 4.x (solo para perfil PostgreSQL) |

---

## Ejecución rápida

### Modo en memoria (sin base de datos)

```powershell
mvn "spring-boot:run"
```

### Modo PostgreSQL

```powershell
# 1. Levantar contenedor
docker compose up -d

# 2. Iniciar la aplicación
mvn "spring-boot:run" "-Dspring-boot.run.profiles=postgres"
```

### Con filtros de puntos

```powershell
# Elimina puntos duplicados consecutivos
mvn "spring-boot:run" "-Dspring-boot.run.profiles=redundancy"

# Conserva 1 de cada 2 puntos
mvn "spring-boot:run" "-Dspring-boot.run.profiles=undersampling"

# Combinado: PostgreSQL + filtro
mvn "spring-boot:run" "-Dspring-boot.run.profiles=postgres,redundancy"
```

---

## URLs de la API

| Recurso | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Base de la API | http://localhost:8080/api/v1/blueprints |

---

## Endpoints

| Método | Ruta | Descripción | Código exitoso |
|---|---|---|---|
| `GET` | `/api/v1/blueprints` | Todos los blueprints | 200 |
| `GET` | `/api/v1/blueprints/{author}` | Blueprints de un autor | 200 |
| `GET` | `/api/v1/blueprints/{author}/{name}` | Blueprint específico | 200 |
| `POST` | `/api/v1/blueprints` | Crear blueprint | 201 |
| `PUT` | `/api/v1/blueprints/{author}/{name}/points` | Agregar punto | 202 |

### Ejemplo POST

```powershell
$body = '{"author":"john","name":"house","points":[{"x":10,"y":20},{"x":30,"y":40}]}'
Invoke-WebRequest "http://localhost:8080/api/v1/blueprints" -Method POST -Body $body -ContentType "application/json" -UseBasicParsing
```

### Formato de respuesta (todas las rutas)

```json
{
  "code": 201,
  "message": "resource created",
  "data": { "author": "john", "name": "house", "points": [...] },
  "_links": {
    "self":              "http://localhost:8080/api/v1/blueprints/john/house",
    "add-point":         "http://localhost:8080/api/v1/blueprints/john/house/points",
    "author-blueprints": "http://localhost:8080/api/v1/blueprints/john",
    "all-blueprints":    "http://localhost:8080/api/v1/blueprints"
  }
}
```

---

## Estructura del proyecto

```
src/main/java/edu/eci/arsw/blueprints/
├── config/
│   └── OpenApiConfig.java            # Metadata OpenAPI: título, contacto, licencia, servidor
├── controllers/
│   ├── ApiResponse.java              # Envelope genérico con _links (HATEOAS)
│   └── BlueprintsAPIController.java  # 5 endpoints REST con anotaciones OpenAPI
├── filters/
│   ├── BlueprintsFilter.java         # Interfaz del filtro
│   ├── IdentityFilter.java           # @Profile("!redundancy & !undersampling")
│   ├── RedundancyFilter.java         # @Profile("redundancy")
│   └── UndersamplingFilter.java      # @Profile("undersampling")
├── model/
│   ├── Blueprint.java
│   └── Point.java
├── persistence/
│   ├── BlueprintPersistence.java     # Interfaz de persistencia
│   ├── entity/
│   │   ├── BlueprintEntity.java      # @Entity JPA
│   │   └── PointEmbeddable.java      # @Embeddable
│   └── impl/
│       ├── BlueprintJpaRepository.java        # Spring Data JPA
│       ├── InMemoryBlueprintPersistence.java  # @Profile("!postgres")
│       └── PostgresBlueprintPersistence.java  # @Profile("postgres")
└── services/
    └── BlueprintsServices.java       # Aplica el filtro activo en todos los GETs

docs/
├── 01_familiarizacion_codigo_base.md
├── 02_migracion_postgresql.md
├── 03_buenas_practicas_rest.md
├── 03b_nivel3_hateoas.md
├── 04_openapi_swagger.md
└── 05_filtros_blueprints.md
```

---

## Resumen de implementación por punto

### Punto 1 — Familiarización
Análisis del código base: modelo de dominio, persistencia en memoria, servicios y controlador. Documentado en `docs/01_familiarizacion_codigo_base.md`.

### Punto 2 — Migración a PostgreSQL
- Docker Compose con `postgres:16-alpine`.
- Entidades JPA: `BlueprintEntity` + `PointEmbeddable` (`@ElementCollection(fetch=EAGER)`).
- `PostgresBlueprintPersistence` activa con `@Profile("postgres")`.
- Tablas generadas automáticamente por Hibernate (`ddl-auto=update`).

### Punto 3 — Buenas prácticas REST + HATEOAS (Level 3)
- Ruta versionada `/api/v1/blueprints`.
- `ApiResponse<T>`: campos `code`, `message`, `data`, `_links`.
- Códigos HTTP semánticos: 200, 201, 202, 400, 404, 409.
- Links hipermedia en cada respuesta (Richardson Level 3).

### Punto 4 — OpenAPI / Swagger
- `springdoc-openapi-starter-webmvc-ui:2.6.0`.
- `OpenApiConfig` bean con título, versión, contacto, licencia y URL de servidor.
- Anotaciones `@Tag`, `@Operation`, `@ApiResponses`, `@Parameter`, `@Schema` en el controlador.

### Punto 5 — Filtros con Spring Profiles
- `RedundancyFilter`: elimina puntos `(x,y)` duplicados consecutivos.
- `UndersamplingFilter`: conserva índices pares (1 de cada 2 puntos).
- `IdentityFilter`: sin transformación (activo por defecto).
- Los perfiles de persistencia y filtro son combinables: `postgres,redundancy`.

---

## Buenas prácticas aplicadas

| Práctica | Implementación |
|---|---|
| Versionamiento de API | Prefijo `/api/v1/` en todas las rutas |
| Recursos en plural | `/blueprints`, no `/blueprint` |
| Sub-recursos relacionales | `/blueprints/{author}/{name}/points` |
| Verbos HTTP correctos | GET (lectura), POST (creación), PUT (actualización) |
| Códigos de estado semánticos | 200/201/202/400/404/409 según el escenario |
| Respuesta uniforme | `ApiResponse<T>` en todos los endpoints |
| HATEOAS Level 3 | Campo `_links` con URLs navegables en cada respuesta |
| Separación de capas | Modelo → Persistencia → Servicio → Controlador |
| Inversión de dependencias | El servicio usa `BlueprintsFilter` (interfaz), no la clase concreta |
| Configuración por entorno | Spring Profiles para persistencia y filtros |
| Contenerización de BD | Docker Compose para PostgreSQL |

---

## Configuración PostgreSQL

**`docker-compose.yml`**
```yaml
services:
  postgres:
    image: postgres:16-alpine
    container_name: blueprints-db
    environment:
      POSTGRES_DB: blueprintsdb
      POSTGRES_USER: blueprints
      POSTGRES_PASSWORD: blueprints123
    ports:
      - "5432:5432"
    volumes:
      - blueprints_data:/var/lib/postgresql/data
volumes:
  blueprints_data:
```

**`application-postgres.properties`**
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/blueprintsdb
spring.datasource.username=blueprints
spring.datasource.password=blueprints123
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

---

## Ejecutar tests

```powershell
mvn test
```

---

## Criterios de evaluación

| Criterio | Peso | Estado |
|---|---|---|
| Diseño de API (versionamiento, DTOs, ApiResponse) | 25% | Implementado |
| Migración a PostgreSQL (repositorio y persistencia correcta) | 25% | Implementado |
| Uso correcto de códigos HTTP y control de errores | 20% | Implementado |
| Documentación con OpenAPI/Swagger + README | 15% | Implementado |
| Pruebas básicas (unitarias o de integración) | 15% | Implementado |  