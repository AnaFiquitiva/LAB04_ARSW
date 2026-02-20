# Punto 5 — Filtros de Blueprints con Spring Profiles

**Laboratorio 4 · Arquitecturas de Software (ARSW)**  
Escuela Colombiana de Ingeniería Julio Garavito  
Fecha: 19 de febrero de 2026

---

## 1. Objetivo

Implementar un sistema de filtros intercambiables sobre los blueprints, de manera que la lógica de transformación de puntos sea seleccionable en tiempo de arranque mediante **Spring Profiles**, sin cambiar una sola línea de código de negocio.

---

## 2. Arquitectura del sistema de filtros

### Interfaz común

Todos los filtros implementan el mismo contrato:

```java
public interface BlueprintsFilter {
    Blueprint apply(Blueprint bp);
}
```

El servicio depende únicamente de la interfaz — no conoce qué implementación está activa:

```java
@Service
public class BlueprintsServices {
    private final BlueprintsFilter filter;   // inyectado por Spring según el perfil activo

    public Blueprint getBlueprint(String author, String name) throws BlueprintNotFoundException {
        return filter.apply(persistence.getBlueprint(author, name));
    }
}
```

Este diseño aplica el principio **Open/Closed**: agregar un nuevo filtro no requiere modificar el servicio ni el controlador.

---

## 3. Filtros implementados

### 3.1 `IdentityFilter` — Sin transformación (perfil: ninguno)

**Ruta:** `filters/IdentityFilter.java`

```java
@Component
@Profile("!redundancy & !undersampling")
public class IdentityFilter implements BlueprintsFilter {
    @Override
    public Blueprint apply(Blueprint bp) { return bp; }
}
```

Activo cuando **no** está activo ni `redundancy` ni `undersampling`.  
Comportamiento: retorna el blueprint exactamente como está en persistencia.

> **Nota:** La anotación `@Profile("!redundancy & !undersampling")` es necesaria para que Spring no encuentre múltiples beans del tipo `BlueprintsFilter` cuando algún otro perfil está activo, lo que causaría `NoUniqueBeanDefinitionException`.

---

### 3.2 `RedundancyFilter` — Elimina puntos duplicados consecutivos (perfil: `redundancy`)

**Ruta:** `filters/RedundancyFilter.java`

```java
@Component
@Profile("redundancy")
public class RedundancyFilter implements BlueprintsFilter {
    @Override
    public Blueprint apply(Blueprint bp) {
        List<Point> in = bp.getPoints();
        if (in.isEmpty()) return bp;
        List<Point> out = new ArrayList<>();
        Point prev = null;
        for (Point p : in) {
            if (prev == null || !(prev.x() == p.x() && prev.y() == p.y())) {
                out.add(p);
                prev = p;
            }
        }
        return new Blueprint(bp.getAuthor(), bp.getName(), out);
    }
}
```

**Algoritmo:** recorre la lista de puntos linealmente y agrega cada punto solo si es diferente al anterior. Complejidad: $O(n)$.

**Ejemplo:**

| Entrada | Salida |
|---|---|
| `(1,1),(1,1),(2,2),(2,2),(2,2),(3,3)` | `(1,1),(2,2),(3,3)` |
| `(0,0),(1,1),(1,1),(2,0)` | `(0,0),(1,1),(2,0)` |

---

### 3.3 `UndersamplingFilter` — Conserva 1 de cada 2 puntos (perfil: `undersampling`)

**Ruta:** `filters/UndersamplingFilter.java`

```java
@Component
@Profile("undersampling")
public class UndersamplingFilter implements BlueprintsFilter {
    @Override
    public Blueprint apply(Blueprint bp) {
        List<Point> in = bp.getPoints();
        if (in.size() <= 2) return bp;
        List<Point> out = new ArrayList<>();
        for (int i = 0; i < in.size(); i++) {
            if (i % 2 == 0) out.add(in.get(i));
        }
        return new Blueprint(bp.getAuthor(), bp.getName(), out);
    }
}
```

**Algoritmo:** conserva los elementos en posiciones pares (0, 2, 4, …). Si el blueprint tiene 2 o menos puntos no se transforma (mínimo estructural). Complejidad: $O(n)$.

**Ejemplo:**

| Entrada | Salida |
|---|---|
| `(1,1),(2,2),(3,3),(4,4),(5,5),(6,6)` | `(1,1),(3,3),(5,5)` |
| `(0,0),(1,0),(2,0),(3,0)` | `(0,0),(2,0)` |

---

## 4. Tabla de perfiles y beans activos

| Perfil activo | Bean inyectado | Descripción |
|---|---|---|
| `inmemory` (defecto) | `IdentityFilter` | Sin transformación |
| `redundancy` | `RedundancyFilter` | Elimina duplicados consecutivos |
| `undersampling` | `UndersamplingFilter` | 1 de cada 2 puntos |
| `postgres` | `IdentityFilter` | Sin transformación (usa BD) |

> Los perfiles de persistencia (`inmemory`, `postgres`) y los de filtro (`redundancy`, `undersampling`) son **ortogonales** — se pueden combinar:
> ```powershell
> mvn "spring-boot:run" "-Dspring-boot.run.profiles=postgres,redundancy"
> ```

---

## 5. Cobertura del filtro en el servicio

El filtro se aplica en **todos los métodos de lectura**, no solo al obtener un blueprint individual:

```java
// getAllBlueprints
public Set<Blueprint> getAllBlueprints() {
    return persistence.getAllBlueprints().stream()
            .map(filter::apply)
            .collect(Collectors.toSet());
}

// getBlueprintsByAuthor
public Set<Blueprint> getBlueprintsByAuthor(String author) throws BlueprintNotFoundException {
    return persistence.getBlueprintsByAuthor(author).stream()
            .map(filter::apply)
            .collect(Collectors.toSet());
}

// getBlueprint (individual)
public Blueprint getBlueprint(String author, String name) throws BlueprintNotFoundException {
    return filter.apply(persistence.getBlueprint(author, name));
}
```

---

## 6. Comandos de ejecución

```powershell
# Perfil por defecto — IdentityFilter (sin transformación)
mvn "spring-boot:run"

# RedundancyFilter — elimina puntos duplicados
mvn "spring-boot:run" "-Dspring-boot.run.profiles=redundancy"

# UndersamplingFilter — 1 de cada 2 puntos
mvn "spring-boot:run" "-Dspring-boot.run.profiles=undersampling"

# Combinado: PostgreSQL + RedundancyFilter
mvn "spring-boot:run" "-Dspring-boot.run.profiles=postgres,redundancy"
```

---

## 7. Evidencia de pruebas

### Prueba 1 — `RedundancyFilter` (perfil `redundancy`)

**Datos persistidos (POST):**
```json
{
  "author": "test",
  "name": "redundancy_demo",
  "points": [
    {"x":1,"y":1}, {"x":1,"y":1},
    {"x":2,"y":2}, {"x":2,"y":2}, {"x":2,"y":2},
    {"x":3,"y":3}
  ]
}
```
→ 6 puntos guardados, con duplicados consecutivos.

**Respuesta al GET (filtro aplicado):**
```json
{
  "code": 200,
  "message": "execute ok",
  "data": {
    "author": "test",
    "name": "redundancy_demo",
    "points": [
      {"x": 1, "y": 1},
      {"x": 2, "y": 2},
      {"x": 3, "y": 3}
    ]
  },
  "_links": { ... }
}
```
→ **6 puntos → 3 puntos.** Los dos `(1,1)` y los tres `(2,2)` se redujeron a uno cada uno.

---

### Prueba 2 — `UndersamplingFilter` (perfil `undersampling`)

**Datos persistidos (POST):**
```json
{
  "author": "test",
  "name": "under_demo",
  "points": [
    {"x":1,"y":1}, {"x":2,"y":2}, {"x":3,"y":3},
    {"x":4,"y":4}, {"x":5,"y":5}, {"x":6,"y":6}
  ]
}
```
→ 6 puntos guardados, sin duplicados.

**Respuesta al GET (filtro aplicado):**
```json
{
  "code": 200,
  "message": "execute ok",
  "data": {
    "author": "test",
    "name": "under_demo",
    "points": [
      {"x": 1, "y": 1},
      {"x": 3, "y": 3},
      {"x": 5, "y": 5}
    ]
  },
  "_links": { ... }
}
```
→ **6 puntos → 3 puntos.** Se conservaron los índices 0, 2, 4 (valores `1,3,5`).

---

## 8. Estructura de archivos resultante

```
filters/
├── BlueprintsFilter.java          ← interfaz (sin cambios)
├── IdentityFilter.java            ← @Profile("!redundancy & !undersampling")
├── RedundancyFilter.java          ← @Profile("redundancy")
└── UndersamplingFilter.java       ← @Profile("undersampling")

services/
└── BlueprintsServices.java        ← filtro aplicado en los 3 métodos GET
```

---

## 9. Resumen de cambios

| Archivo | Cambio |
|---|---|
| `filters/IdentityFilter.java` | Añadido `@Profile("!redundancy & !undersampling")` |
| `filters/RedundancyFilter.java` | Implementación completa con `@Profile("redundancy")` |
| `filters/UndersamplingFilter.java` | Implementación completa con `@Profile("undersampling")` |
| `services/BlueprintsServices.java` | Filtro aplicado en `getAllBlueprints()` y `getBlueprintsByAuthor()` |
