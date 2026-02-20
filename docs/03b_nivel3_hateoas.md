# Punto 3b — Level 3 REST: HATEOAS (Hypermedia as the Engine of Application State)

**Laboratorio 4 · Arquitecturas de Software (ARSW)**  
Escuela Colombiana de Ingeniería Julio Garavito  
Fecha: 19 de febrero de 2026

---

## 1. Contexto teórico — Modelo de Madurez de Richardson

El **Modelo de Madurez de Richardson** define cuatro niveles de adopción de REST:

| Nivel | Nombre | Descripción |
|---|---|---|
| **Level 0** | HTTP como transporte | Un solo endpoint, RPC sobre HTTP (estilo SOAP) |
| **Level 1** | Recursos / URLs | Cada entidad tiene su propia URL (`/blueprints`, `/blueprints/john`) |
| **Level 2** | Verbos y códigos HTTP | Se usan `GET`, `POST`, `PUT`, `DELETE` con sus códigos correctos (200, 201, 404…) |
| **Level 3** | Controles hipermedia | Las respuestas incluyen `_links` que guían al cliente al siguiente paso posible |

El proyecto ya tenía implementados los niveles 0, 1 y 2. Este punto implementa el **Level 3 — HATEOAS**.

---

## 2. ¿Qué es HATEOAS?

**HATEOAS** significa *Hypermedia as the Engine of Application State*.  
El principio establece que **cada respuesta de la API debe incluir los enlaces a las acciones disponibles** a partir de ese estado, de manera que el cliente no necesita conocer las URLs de antemano — las descubre navegando la API.

**Analogía:** igual que en HTML un navegador sigue `<a href="...">` sin conocer de antemano todas las páginas, un cliente REST sigue los `_links` sin hard-codear URLs.

### Beneficios

- El cliente es independiente de la estructura interna de URLs (bajo acoplamiento).
- La API puede evolucionar cambiando URLs sin romper clientes.
- La respuesta es autodescriptiva: el cliente sabe qué puede hacer a continuación.

---

## 3. Cambios implementados

### 3.1 `ApiResponse<T>` — de `record` a `class` con `_links`

**Ruta:** `controllers/ApiResponse.java`

**Antes (Level 2):**
```java
public record ApiResponse<T>(int code, String message, T data) { ... }
```

**Después (Level 3):**
```java
public class ApiResponse<T> {

    private final int code;
    private final String message;
    private final T data;

    @JsonProperty("_links")
    private final Map<String, String> links;   // ← NUEVO

    // Método fluent para agregar links a cualquier respuesta
    public ApiResponse<T> withLinks(Map<String, String> links) {
        return new ApiResponse<>(this.code, this.message, this.data, links);
    }

    // Factory methods sin cambios: ok(), created(), accepted(), ...
}
```

**Estructura JSON resultante:**
```json
{
  "code": 200,
  "message": "execute ok",
  "data": { ... },
  "_links": {
    "self":              "http://localhost:8080/api/v1/blueprints/john/house",
    "add-point":         "http://localhost:8080/api/v1/blueprints/john/house/points",
    "author-blueprints": "http://localhost:8080/api/v1/blueprints/john",
    "all-blueprints":    "http://localhost:8080/api/v1/blueprints"
  }
}
```

---

### 3.2 Helper `base()` en el controlador — URLs dinámicas

```java
private String base() {
    return ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/api/v1/blueprints")
            .toUriString();
}
```

`ServletUriComponentsBuilder` resuelve automáticamente `scheme://host:port` desde el request HTTP entrante, evitando hardcodear `http://localhost:8080`.

---

### 3.3 Links por endpoint

Cada endpoint construye su propio mapa de links relevantes al estado retornado:

#### `GET /api/v1/blueprints`
```java
links.put("self", base());
```
Solo se ofrece el link propio — es la raíz de la colección.

---

#### `GET /api/v1/blueprints/{author}`
```java
links.put("self",           base() + "/" + author);
links.put("all-blueprints", base());
```
El cliente puede volver a la colección completa.

---

#### `GET /api/v1/blueprints/{author}/{bpname}`
```java
links.put("self",              base() + "/" + author + "/" + bpname);
links.put("add-point",         base() + "/" + author + "/" + bpname + "/points");
links.put("author-blueprints", base() + "/" + author);
links.put("all-blueprints",    base());
```
Desde un blueprint específico el cliente puede: ver el blueprint, agregar un punto, ver todos del autor, o ver todos.

---

#### `POST /api/v1/blueprints` → 201 Created
```java
links.put("self",              base() + "/" + bp.getAuthor() + "/" + bp.getName());
links.put("add-point",         base() + "/" + bp.getAuthor() + "/" + bp.getName() + "/points");
links.put("author-blueprints", base() + "/" + bp.getAuthor());
links.put("all-blueprints",    base());
```
Al crear un recurso el cliente recibe inmediatamente su URL y las acciones posibles.

---

#### `PUT /api/v1/blueprints/{author}/{bpname}/points` → 202 Accepted
```java
links.put("blueprint",      base() + "/" + author + "/" + bpname);
links.put("all-blueprints", base());
```
Después de agregar un punto el cliente puede consultar el blueprint actualizado.

---

## 4. Tabla resumen de links por operación

| Método | Ruta | Link `self` | Links adicionales |
|---|---|---|---|
| GET | `/api/v1/blueprints` | ✅ colección | — |
| GET | `/api/v1/blueprints/{author}` | ✅ autor | `all-blueprints` |
| GET | `/api/v1/blueprints/{author}/{name}` | ✅ blueprint | `add-point`, `author-blueprints`, `all-blueprints` |
| POST | `/api/v1/blueprints` (201) | ✅ blueprint creado | `add-point`, `author-blueprints`, `all-blueprints` |
| PUT | `/{author}/{name}/points` (202) | — | `blueprint`, `all-blueprints` |

---

## 5. Evidencia de pruebas

### GET blueprint específico — `200 OK` con `_links`

**Request:**
```http
GET http://localhost:8080/api/v1/blueprints/john/house
```

**Response:**
```json
{
  "code": 200,
  "message": "execute ok",
  "data": {
    "author": "john",
    "name": "house",
    "points": [
      {"x": 0,  "y": 0},
      {"x": 10, "y": 0},
      {"x": 10, "y": 10},
      {"x": 0,  "y": 10}
    ]
  },
  "_links": {
    "self":              "http://localhost:8080/api/v1/blueprints/john/house",
    "add-point":         "http://localhost:8080/api/v1/blueprints/john/house/points",
    "author-blueprints": "http://localhost:8080/api/v1/blueprints/john",
    "all-blueprints":    "http://localhost:8080/api/v1/blueprints"
  }
}
```

---

### POST crear blueprint — `201 Created` con `_links`

**Request:**
```http
POST http://localhost:8080/api/v1/blueprints
Content-Type: application/json

{"author":"john","name":"building","points":[{"x":5,"y":5}]}
```

**Response:**
```json
{
  "code": 201,
  "message": "resource created",
  "data": {
    "author": "john",
    "name": "building",
    "points": [{"x": 5, "y": 5}]
  },
  "_links": {
    "self":              "http://localhost:8080/api/v1/blueprints/john/building",
    "add-point":         "http://localhost:8080/api/v1/blueprints/john/building/points",
    "author-blueprints": "http://localhost:8080/api/v1/blueprints/john",
    "all-blueprints":    "http://localhost:8080/api/v1/blueprints"
  }
}
```

---

### PUT agregar punto — `202 Accepted` con `_links`

**Request:**
```http
PUT http://localhost:8080/api/v1/blueprints/john/building/points
Content-Type: application/json

{"x": 99, "y": 88}
```

**Response:**
```json
{
  "code": 202,
  "message": "update accepted",
  "data": null,
  "_links": {
    "blueprint":      "http://localhost:8080/api/v1/blueprints/john/building",
    "all-blueprints": "http://localhost:8080/api/v1/blueprints"
  }
}
```

---

## 6. Comparación Level 2 vs Level 3

| Aspecto | Level 2 | Level 3 (HATEOAS) |
|---|---|---|
| Respuesta | `{ code, message, data }` | `{ code, message, data, _links }` |
| URLs | El cliente las conoce de antemano | El cliente las descubre en las respuestas |
| Acoplamiento | Alto (cliente depende de URLs) | Bajo (cliente sigue links) |
| Autodescripción | No | Sí |
| Navegación | El cliente construye URLs manualmente | El cliente navega siguiendo `_links` |

---

## 7. Archivos modificados

| Archivo | Cambio |
|---|---|
| `controllers/ApiResponse.java` | De `record` a `class`; campo `_links` con `@JsonProperty`; método `withLinks()` |
| `controllers/BlueprintsAPIController.java` | Import `ServletUriComponentsBuilder`; helper `base()`; construcción de `Map<String,String> links` en cada endpoint |
