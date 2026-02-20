# Punto 1 – Familiarización con el código base

## 📦 Capa `model`

### `Point`
```java
public record Point(int x, int y) { }
```
- Es un **Java record** (inmutable por diseño). Solo almacena coordenadas `x` e `y`.

### `Blueprint`
- Representa un plano con `author`, `name` y una lista de `Point`.
- La lista es internamente un `ArrayList` pero se expone como **inmutable** (`Collections.unmodifiableList`), por eso existe `addPoint()` para modificarla de forma controlada.
- `equals` y `hashCode` están basados únicamente en `author + name` → dos blueprints son iguales si tienen el mismo autor y nombre.

---

## 💾 Capa `persistence`

### Interfaz `BlueprintPersistence`
Contrato que cualquier implementación debe cumplir:

| Método | Descripción |
|---|---|
| `saveBlueprint` | Guarda un blueprint (lanza excepción si ya existe) |
| `getBlueprint(author, name)` | Busca uno específico |
| `getBlueprintsByAuthor(author)` | Busca todos de un autor |
| `getAllBlueprints()` | Retorna todos |
| `addPoint(author, name, x, y)` | Agrega un punto a uno existente |

### `InMemoryBlueprintPersistence`
- Implementa la interfaz usando un `ConcurrentHashMap<String, Blueprint>` (thread-safe).
- La clave del mapa es `"author:name"`.
- Precarga 3 blueprints de prueba: `john/house`, `john/garage`, `jane/garden`.
- Lanza `BlueprintNotFoundException` cuando no encuentra, y `BlueprintPersistenceException` si ya existe al guardar.

---

## ⚙️ Capa `services`

### `BlueprintsServices`
- Actúa como **orquestador** entre el controlador y la persistencia.
- Recibe por inyección de dependencias: `BlueprintPersistence` (repositorio) y `BlueprintsFilter` (filtro activo).
- **Punto clave**: solo `getBlueprint()` aplica el filtro. `getAllBlueprints()` y `getBlueprintsByAuthor()` **no filtran** → esto es algo que se puede mejorar en el laboratorio.

---

## 🎮 Capa `controllers`

### `BlueprintsAPIController`
Path base actual: `/blueprints` (en el lab se cambiará a `/api/v1/blueprints`)

| Método | Endpoint | Descripción | HTTP actual |
|---|---|---|---|
| GET | `/blueprints` | Todos los blueprints | 200 |
| GET | `/blueprints/{author}` | Por autor | 200 / 404 |
| GET | `/blueprints/{author}/{bpname}` | Uno específico | 200 / 404 |
| POST | `/blueprints` | Crear nuevo | 201 / 403 ⚠️ |
| PUT | `/blueprints/{author}/{bpname}/points` | Agregar punto | 202 / 404 |

> ⚠️ El POST devuelve `403 FORBIDDEN` cuando el blueprint ya existe — debería ser `409 CONFLICT` o `400 BAD REQUEST`. Esto se corregirá en el punto 3.

---

## 🔍 Capa `filters`

- `BlueprintsFilter` es una interfaz con un solo método `apply(Blueprint)`.
- `IdentityFilter` es la implementación por defecto: **devuelve el blueprint sin modificar**.
- En el punto 5 se implementarán `RedundancyFilter` y `UndersamplingFilter`.

---

## 🔄 Flujo de datos

```
HTTP Request
    → BlueprintsAPIController
        → BlueprintsServices (aplica filtro si aplica)
            → BlueprintPersistence (InMemoryBlueprintPersistence)
                → ConcurrentHashMap<"author:name", Blueprint>
```

---

## 📁 Estructura de paquetes analizada

```
edu.eci.arsw.blueprints
  ├── model/
  │     ├── Blueprint.java      → Entidad principal (author, name, List<Point>)
  │     └── Point.java          → Record inmutable (x, y)
  ├── persistence/
  │     ├── BlueprintPersistence.java          → Interfaz del repositorio
  │     ├── BlueprintNotFoundException.java    → Excepción recurso no encontrado
  │     ├── BlueprintPersistenceException.java → Excepción de persistencia
  │     └── impl/
  │           └── InMemoryBlueprintPersistence.java → Implementación en memoria
  ├── services/
  │     └── BlueprintsServices.java → Lógica de negocio y orquestación
  ├── filters/
  │     ├── BlueprintsFilter.java  → Interfaz del filtro
  │     └── IdentityFilter.java    → Filtro por defecto (sin transformación)
  └── controllers/
        └── BlueprintsAPIController.java → API REST
```
