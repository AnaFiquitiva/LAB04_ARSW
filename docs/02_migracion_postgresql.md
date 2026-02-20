# Punto 2 — Migración a Persistencia en PostgreSQL

**Laboratorio 4 · Arquitecturas de Software (ARSW)**  
Escuela Colombiana de Ingeniería Julio Garavito  
Fecha: 19 de febrero de 2026

---

## 1. Objetivo

Reemplazar la persistencia en memoria (`InMemoryBlueprintPersistence`) por una implementación real sobre **PostgreSQL 16**, utilizando **Spring Data JPA** y **Hibernate 6**, manteniendo la compatibilidad con el perfil `inmemory` ya existente gracias al sistema de **Spring Profiles**.

---

## 2. Tecnologías utilizadas

| Tecnología | Versión | Rol |
|---|---|---|
| PostgreSQL | 16-alpine (Docker) | Motor de base de datos relacional |
| Spring Data JPA | 3.3.9 (via Boot) | Capa de abstracción ORM |
| Hibernate | 6.x (auto) | Implementación JPA |
| Docker Desktop | 28.4.0 | Contenerización de la base de datos |
| Spring Profiles | — | Conmutación entre backends de persistencia |

---

## 3. Infraestructura — Docker Compose

Se creó el archivo `docker-compose.yml` en la raíz del proyecto para levantar PostgreSQL sin instalación local:

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
    restart: unless-stopped

volumes:
  blueprints_data:
```

**Datos de conexión:**

| Parámetro | Valor |
|---|---|
| Host | `localhost` |
| Puerto | `5432` |
| Base de datos | `blueprintsdb` |
| Usuario | `blueprints` |
| Contraseña | `blueprints123` |

**Comando para levantar el contenedor:**
```powershell
docker compose up -d
```

---

## 4. Dependencias Maven agregadas

En `pom.xml` se añadieron dos dependencias:

```xml
<!-- ORM / JPA -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- Driver PostgreSQL (solo en runtime) -->
<dependency>
  <groupId>org.postgresql</groupId>
  <artifactId>postgresql</artifactId>
  <scope>runtime</scope>
</dependency>
```

---

## 5. Clases creadas

### 5.1 `PointEmbeddable` — Objeto embebible JPA

**Ruta:** `persistence/entity/PointEmbeddable.java`

```java
@Embeddable
public class PointEmbeddable {
    private int x;
    private int y;
    // constructores, getters
}
```

Representa un punto (x, y) almacenado en la tabla `blueprint_points` como `@ElementCollection`. Se requirió constructor sin argumentos por la especificación JPA.

---

### 5.2 `BlueprintEntity` — Entidad JPA principal

**Ruta:** `persistence/entity/BlueprintEntity.java`

```java
@Entity
@Table(name = "blueprints",
       uniqueConstraints = @UniqueConstraint(columnNames = {"author","name"}))
public class BlueprintEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String author;
    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "blueprint_points",
                     joinColumns = @JoinColumn(name = "blueprint_id"))
    @OrderColumn(name = "point_order")
    private List<PointEmbeddable> points;
}
```

**Tablas generadas por Hibernate (DDL automático):**

```
blueprints
  ├── id         BIGSERIAL PRIMARY KEY
  ├── author     VARCHAR
  └── name       VARCHAR
  └── UNIQUE(author, name)

blueprint_points
  ├── blueprint_id  BIGINT → FK blueprints.id
  ├── point_order   INTEGER
  ├── x             INTEGER
  └── y             INTEGER
```

> **Nota técnica:** Se usó `FetchType.EAGER` para evitar `LazyInitializationException` durante la serialización JSON fuera de sesión Hibernate.

---

### 5.3 `BlueprintJpaRepository` — Repositorio Spring Data

**Ruta:** `persistence/impl/BlueprintJpaRepository.java`

```java
public interface BlueprintJpaRepository extends JpaRepository<BlueprintEntity, Long> {
    Optional<BlueprintEntity>     findByAuthorAndName(String author, String name);
    List<BlueprintEntity>         findByAuthor(String author);
    boolean                       existsByAuthorAndName(String author, String name);
}
```

Spring Data genera automáticamente la implementación SQL en tiempo de arranque.

---

### 5.4 `PostgresBlueprintPersistence` — Implementación del contrato

**Ruta:** `persistence/impl/PostgresBlueprintPersistence.java`

```java
@Repository
@Profile("postgres")
@Transactional
public class PostgresBlueprintPersistence implements BlueprintPersistence {

    private final BlueprintJpaRepository jpaRepository;
    // ...

    @Override
    public void saveBlueprint(Blueprint bp) throws BlueprintPersistenceException {
        if (jpaRepository.existsByAuthorAndName(bp.getAuthor(), bp.getName()))
            throw new BlueprintPersistenceException("Blueprint already exists");
        jpaRepository.save(toEntity(bp));
    }
    // + getBlueprint, getBlueprintsByAuthor, getAllBlueprints, addPoint
}
```

**Patrón de conversión:**

```
Blueprint (dominio)  ←→  BlueprintEntity (JPA)
Point (record)       ←→  PointEmbeddable (@Embeddable)
```

---

## 6. Configuración de perfiles Spring

### `application.properties` (perfil `inmemory`, por defecto)

```properties
spring.profiles.active=inmemory
spring.jpa.open-in-view=false

# Excluye JPA/DataSource para no requerir BD cuando no es necesario
spring.autoconfigure.exclude=\
  org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,\
  org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,\
  org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration
```

### `application-postgres.properties` (perfil `postgres`)

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/blueprintsdb
spring.datasource.username=blueprints
spring.datasource.password=blueprints123
spring.datasource.driver-class-name=org.postgresql.Driver

spring.autoconfigure.exclude=        ← re-habilita JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

### Anotaciones de perfil en los beans

```java
// Solo activa cuando el perfil NO es "postgres"
@Profile("!postgres")
public class InMemoryBlueprintPersistence implements BlueprintPersistence { ... }

// Solo activa cuando el perfil ES "postgres"
@Profile("postgres")
public class PostgresBlueprintPersistence implements BlueprintPersistence { ... }
```

---

## 7. Comandos de ejecución

```powershell
# 1. Levantar base de datos
docker compose up -d

# 2. Ejecutar con perfil PostgreSQL
mvn spring-boot:run -Dspring-boot.run.profiles=postgres

# 3. Ejecutar en memoria (por defecto, sin BD)
mvn spring-boot:run
```

---

## 8. Verificación — Evidencia de pruebas

### Tablas creadas en PostgreSQL

Después del primer arranque con perfil `postgres`, Hibernate creó las tablas automáticamente (`ddl-auto=update`):

```
blueprintsdb=# \dt
             List of relations
 Schema |       Name        | Type  |  Owner
--------+-------------------+-------+----------
 public | blueprint_points  | table | blueprints
 public | blueprints        | table | blueprints
```

### Endpoints probados con HTTP

| Método | Ruta | Resultado |
|---|---|---|
| `POST` | `/api/v1/blueprints` | `201 Created` — blueprint guardado en BD |
| `GET` | `/api/v1/blueprints` | `200 OK` — lista desde BD |
| `GET` | `/api/v1/blueprints/{author}` | `200 OK` — filtra por autor |
| `GET` | `/api/v1/blueprints/{author}/{name}` | `200 OK` — registro exacto |
| `PUT` | `/api/v1/blueprints/{author}/{name}/points` | `202 Accepted` — punto agregado |

---

## 9. Problemas encontrados y soluciones

| Problema | Causa | Solución |
|---|---|---|
| `LazyInitializationException` al serializar JSON | `@ElementCollection` usa LAZY por defecto | `FetchType.EAGER` + `@Transactional` |
| `Failed to determine a suitable driver class` en perfil inmemory | Spring intentaba crear `DataSource` sin configuración | Excluir autoconfiguraciones JPA/DataSource en `application.properties` |
| Advertencia dialect Hibernate | Se especificaba `PostgreSQLDialect` explícitamente | Se eliminó; Hibernate 6 auto-detecta el dialecto |
| Advertencia `version:` en docker-compose | Atributo obsoleto en Docker Compose v2 | Se eliminó la clave `version` del YAML |

---

## 10. Estructura de archivos resultante

```
persistence/
├── BlueprintNotFoundException.java
├── BlueprintPersistence.java          ← interfaz (sin cambios)
├── BlueprintPersistenceException.java
├── entity/
│   ├── BlueprintEntity.java           ← NUEVO
│   └── PointEmbeddable.java           ← NUEVO
└── impl/
    ├── BlueprintJpaRepository.java    ← NUEVO
    ├── InMemoryBlueprintPersistence.java  ← @Profile("!postgres") agregado
    └── PostgresBlueprintPersistence.java  ← NUEVO
```
