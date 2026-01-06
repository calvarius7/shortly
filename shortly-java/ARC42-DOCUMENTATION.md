# Shortly - Arc42 Architekturdokumentation

**Backend-fokussierte Dokumentation eines URL-Shortener-Service**

---

## 1. Einführung und Ziele

### 1.1 Aufgabenstellung

Shortly ist ein URL-Shortener-Service, der lange URLs in kurze, 6-stellige alphanumerische Codes umwandelt. Der Service
ermöglicht:

- Erstellen von Kurz-URLs mit optionalem Ablaufdatum
- Weiterleitung von Kurz-URLs zur Original-URL
- Tracking von Klick-Statistiken
- Löschen von Kurz-URLs

### 1.2 Qualitätsziele

| Priorität | Qualitätsziel      | Szenario                                            |
|-----------|--------------------|-----------------------------------------------------|
| 1         | **Performance**    | Redirects müssen in <100ms erfolgen (Redis-Caching) |
| 2         | **Skalierbarkeit** | Horizontal skalierbar durch Stateless-Design        |
| 3         | **Wartbarkeit**    | Hohe Testabdeckung (>80%), Code-Quality-Tools       |
| 4         | **Betreibbarkeit** | Cloud-Native mit Health Checks, Prometheus-Metriken |
| 5         | **Security**       | Input-Validierung, Container-Hardening (Distroless) |

### 1.3 Stakeholder

| Rolle      | Erwartungshaltung                                              |
|------------|----------------------------------------------------------------|
| Entwickler | Lernprojekt für Spring Boot 4.0, Java 25, GraalVM Native Image |
| Betreiber  | Einfaches Deployment via Kubernetes/Helm, gutes Monitoring     |

---

## 2. Randbedingungen

### 2.1 Technische Randbedingungen

| Randbedingung            | Erläuterung                                                                      |
|--------------------------|----------------------------------------------------------------------------------|
| **Java 25**              | Neueste Java-Version mit modernen Features                                       |
| **Spring Boot 4.0**      | Cutting-Edge Framework-Version                                                   |
| **GraalVM Native Image** | Kompilierung zu nativem Binary für schnellen Start und geringen Memory-Footprint |
| **Redis**                | In-Memory-Datenbank für schnelle Zugriffe und TTL-Support                        |
| **Kubernetes**           | Zielplattform für Deployment                                                     |

### 2.2 Organisatorische Randbedingungen

- WIP-Status: Noch kein Produktiv-Einsatz
- Kein Authentication/Authorization implementiert (API ist öffentlich)
- Solo-Projekt ohne Team-Konventionen

---

## 3. Kontextabgrenzung

### 3.1 Fachlicher Kontext

```
┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│   Browser   │────────▶│   Shortly    │────────▶│    Redis    │
│   (User)    │◀────────│   Backend    │◀────────│  Database   │
└─────────────┘         └──────────────┘         └─────────────┘
      │                        │
      │                        │
      ▼                        ▼
  HTTP GET /{code}        Store & Retrieve
  HTTP POST /api/shorten  Short Links
  HTTP DELETE /api/{code}
```

**Externe Schnittstellen:**

- **User (Browser)**: Konsumiert Redirect-Funktionalität und API
- **Redis**: Persistiert ShortLinks mit TTL-Support

### 3.2 Technischer Kontext

```
┌──────────────────────────────────────────────────────┐
│                   Kubernetes Cluster                  │
│                                                        │
│  ┌──────────────────────────────────────────────┐   │
│  │              Ingress (NGINX)                  │   │
│  │  - /api/** → Backend                          │   │
│  │  - /{6chars} → Backend (Redirect)             │   │
│  │  - /* → Frontend                              │   │
│  └──────────────────────────────────────────────┘   │
│         │                          │                  │
│         ▼                          ▼                  │
│  ┌─────────────┐           ┌─────────────┐          │
│  │  Frontend   │           │   Backend   │          │
│  │   (NGINX)   │           │  (Spring)   │          │
│  │   Port 80   │           │  Port 8080  │          │
│  └─────────────┘           └─────────────┘          │
│                                    │                  │
│                                    ▼                  │
│                             ┌─────────────┐          │
│                             │    Redis    │          │
│                             │  Port 6379  │          │
│                             └─────────────┘          │
└──────────────────────────────────────────────────────┘
```

**Kommunikationskanäle:**

- HTTP/REST zwischen User und Services
- Redis-Protocol zwischen Backend und Redis
- Alle Services im gleichen K8s-Cluster (ClusterIP)

---

## 4. Lösungsstrategie

### 4.1 Technologieentscheidungen

| Entscheidung         | Begründung                                                                   |
|----------------------|------------------------------------------------------------------------------|
| **Spring Boot**      | De-facto Standard für Java-Enterprise-Anwendungen, umfangreiches Ökosystem   |
| **Redis**            | Sehr schnelle In-Memory-DB, native TTL-Unterstützung für Ablaufdaten         |
| **GraalVM Native**   | ~65MB Image-Size (mit UPX), schneller Start (<1s), geringer Memory-Footprint |
| **Base62-Encoding**  | 62^6 = ~56 Mrd. mögliche Codes, URL-safe (keine Sonderzeichen)               |
| **Stateless Design** | Horizontal skalierbar, keine Session-Verwaltung nötig                        |

### 4.2 Top-Level-Zerlegung

```
┌───────────────────────────────────────────────┐
│              Controller Layer                  │
│  - REST-Endpoints                              │
│  - Input-Validierung                           │
│  - Exception-Handling                          │
└───────────────────────────────────────────────┘
                    │
                    ▼
┌───────────────────────────────────────────────┐
│              Service Layer                     │
│  - Business-Logik                              │
│  - ShortCode-Generierung                       │
│  - TTL-Berechnung                              │
│  - Click-Tracking                              │
└───────────────────────────────────────────────┘
                    │
                    ▼
┌───────────────────────────────────────────────┐
│           Persistence Layer                    │
│  - Spring Data Redis Repository                │
│  - ShortLink-Entity mit TTL                    │
└───────────────────────────────────────────────┘
```

### 4.3 Architektur-Stil

- **Layered Architecture** (3-Schicht): Klare Trennung von Web, Business-Logik, Persistence
- **RESTful API**: HTTP-Verben + Statuscodes für API-Design
- **Dependency Injection**: Spring-Container managed alle Komponenten

### 4.4 Frontend-Integration

**Technologie:** Angular (Single-Page-Application)

**API-Client-Generierung:**
- OpenAPI-Codegenerierung aus Backend-Spezifikation
- Dynamische `basePath`-Konfiguration über `window.location.origin`
- TypeScript-Client für typsichere Backend-Kommunikation

**App Creation:**
- Custom-Initialisierung (app.creation) für Konfiguration und Bootstrap
- SPA-Routing mit Angular Router
- NGINX als statischer Webserver im Production-Deployment

---

## 5. Bausteinsicht

### 5.1 Whitebox-Gesamtsystem

```
┌─────────────────────────────────────────────────────────┐
│                  Shortly Backend                         │
│                                                           │
│  ┌────────────────────────────────────────────────┐    │
│  │            Web Layer                            │    │
│  │  ┌──────────────────────────────────────────┐  │    │
│  │  │ Controller                                │  │    │
│  │  │ - GET /{shortCode}                        │  │    │
│  │  │ - POST /api/shorten                       │  │    │
│  │  │ - GET /api/stats/{shortCode}              │  │    │
│  │  │ - DELETE /api/{shortCode}                 │  │    │
│  │  └──────────────────────────────────────────┘  │    │
│  │  ┌──────────────────────────────────────────┐  │    │
│  │  │ GlobalExceptionHandler                    │  │    │
│  │  │ - MethodArgumentNotValidException         │  │    │
│  │  │ - ConstraintViolationException            │  │    │
│  │  └──────────────────────────────────────────┘  │    │
│  │  ┌──────────────────────────────────────────┐  │    │
│  │  │ WebConfig                                 │  │    │
│  │  │ - CORS (nur @Profile("dev"))              │  │    │
│  │  └──────────────────────────────────────────┘  │    │
│  └────────────────────────────────────────────────┘    │
│                         │                                │
│                         ▼                                │
│  ┌────────────────────────────────────────────────┐    │
│  │            Service Layer                        │    │
│  │  ┌──────────────────────────────────────────┐  │    │
│  │  │ ShortLinkService                          │  │    │
│  │  │ - create(url, expiresAt)                  │  │    │
│  │  │ - findById(shortCode)                     │  │    │
│  │  │ - deleteById(shortCode)                   │  │    │
│  │  │ - calculateTtl(expiresAt)                 │  │    │
│  │  └──────────────────────────────────────────┘  │    │
│  │  ┌──────────────────────────────────────────┐  │    │
│  │  │ ShortCodeGenerator (Interface)            │  │    │
│  │  │ - LENGTH = 6                              │  │    │
│  │  │ - generate()                              │  │    │
│  │  └──────────────────────────────────────────┘  │    │
│  │  ┌──────────────────────────────────────────┐  │    │
│  │  │ Base62Generator (Impl)                    │  │    │
│  │  │ - CHARACTERS = "0-9a-zA-Z"                │  │    │
│  │  │ - ThreadLocalRandom                       │  │    │
│  │  └──────────────────────────────────────────┘  │    │
│  └────────────────────────────────────────────────┘    │
│                         │                                │
│                         ▼                                │
│  ┌────────────────────────────────────────────────┐    │
│  │         Persistence Layer                       │    │
│  │  ┌──────────────────────────────────────────┐  │    │
│  │  │ ShortLinkRepository                       │  │    │
│  │  │ extends CrudRepository<ShortLink, String> │  │    │
│  │  └──────────────────────────────────────────┘  │    │
│  │  ┌──────────────────────────────────────────┐  │    │
│  │  │ ShortLink (Entity)                        │  │    │
│  │  │ @RedisHash("links")                       │  │    │
│  │  │ - @Id shortCode: String                   │  │    │
│  │  │ - originalUrl: String                     │  │    │
│  │  │ - clicks: int                             │  │    │
│  │  │ - @TimeToLive ttl: Long                   │  │    │
│  │  └──────────────────────────────────────────┘  │    │
│  └────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────┘
```

### 5.2 Komponenten-Details

#### 5.2.1 Controller (Web Layer)

**Verantwortlichkeit:**

- REST-API-Endpoints bereitstellen
- HTTP-Request-Validierung
- HTTP-Response-Mapping

**Schnittstellen:**

| Endpoint                 | Methode | Beschreibung                    |
|--------------------------|---------|---------------------------------|
| `/{shortCode}`           | GET     | Redirect zur Original-URL (302) |
| `/api/shorten`           | POST    | Erstellt neuen Shortlink        |
| `/api/stats/{shortCode}` | GET     | Liefert Statistiken (Klicks)    |
| `/api/{shortCode}`       | DELETE  | Löscht Shortlink                |

**Code-Location:** `src/main/java/neusta/shortly/web/Controller.java`

**Besonderheiten:**

- Kein `@RequestMapping`-Prefix am Controller (mixed Routing: `/api` und `/{code}`)
- `@ValidShortCode` Custom-Annotation für Path-Parameter-Validierung
- OpenAPI/Swagger-Annotationen für API-Dokumentation

#### 5.2.2 ShortLinkService (Service Layer)

**Verantwortlichkeit:**

- Business-Logik für ShortLink-Verwaltung
- ShortCode-Generierung mit Kollisionsvermeidung
- Click-Tracking (Inkrement bei jedem Aufruf)
- TTL-Berechnung aus `expiresAt`

**Code-Location:** `src/main/java/neusta/shortly/service/ShortLinkService.java`

**Refactoring (seit 0599852):**
- Umstellung von `CrudRepository` auf `StringRedisTemplate` für bessere Redis-Integration
- Direkte Nutzung von Redis-Operationen für optimierte Performance
- Atomare Click-Tracking-Operationen via Redis INCR

**Algorithmus ShortCode-Generierung:**
- Stream-basierte Generierung mit Kollisionsprüfung
- Generiert Codes, bis einer gefunden wird, der noch nicht existiert
- Potentiell langsam bei hoher Kollisionsrate (>50% DB-Auslastung)
- Alternative für Production: Sequenz-basierte Kodierung mit Base62

**Click-Tracking:**
- Seit Refactoring (0599852) über atomare Redis INCR-Befehle
- Thread-Safe ohne Race-Conditions

#### 5.2.3 Base62Generator (Service Layer)

**Verantwortlichkeit:**

- Generierung von 6-stelligen Base62-Codes

**Code-Location:** `src/main/java/neusta/shortly/service/Base62Generator.java`

**Alphabet:** `0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ`

**Mögliche Codes:** 62^6 = 56.800.235.584 (~57 Milliarden)

**Random-Strategie:**

- `ThreadLocalRandom` für Thread-Safety ohne Synchronisation
- Gleichverteilung über den Coderaum

**Alternative Strategien:**

1. **Sequenz + Base62-Kodierung**: Deterministisch, keine Kollisionen
2. **Hash-basiert**: URL-Hash als Basis (reproduzierbar)
3. **UUID-Prefix**: UUID auf 6 Zeichen kürzen (höhere Kollisionsgefahr)

#### 5.2.4 ShortLink (Entity)

**Code-Location:** `src/main/java/neusta/shortly/model/ShortLink.java`

**Felder:**
- `shortCode` (String): Eindeutiger 6-stelliger Identifier
- `originalUrl` (String): Ziel-URL für Redirect
- `clicks` (int): Anzahl der Zugriffe
- `ttl` (Long): Time-To-Live in Sekunden

**Besonderheiten:**
- `@RedisHash("links")`: Wird als Redis-Key `links:{shortCode}` gespeichert
- `@TimeToLive`: Redis-native Ablaufzeit (automatisches Löschen)
- `ttl=null`: Kein Ablaufdatum (bleibt für immer)

#### 5.2.5 GlobalExceptionHandler

**Verantwortlichkeit:**

- Zentrale Exception-Behandlung
- Einheitliches Error-Format (RFC 7807 ProblemDetail)

**Code-Location:** `src/main/java/neusta/shortly/web/GlobalExceptionHandler.java`

**Behandelte Exceptions:**

- `MethodArgumentNotValidException`: Request-Body-Validierung fehlgeschlagen
- `ConstraintViolationException`: Path-/Query-Parameter-Validierung fehlgeschlagen

**Response-Format:** RFC 7807 ProblemDetail mit `type`, `title`, `status`, `detail`-Feldern



---

## 5.3 Frontend-Baustein

### 5.3.1 Angular-Application (Architektur)

**Struktur:**
- **OpenAPI-generierter Code:** Services, Models, API-Clients (automatisch generiert)
- **Angular-Core:** Routing, Module, Shared-Components (Angular-Magic)
- **Custom-Components:** Eigenentwickelte UI-Komponenten (z.B. Creation-Component)

**Technologie-Stack:**
- Angular Standalone Components (TypeScript-basiert)
- OpenAPI-generierter API-Client (`ControllerService`)
- Angular Signals für reaktives State-Management
- RxJS für asynchrone Operationen

**Code-Location:** `shortly-frontend/`

### 5.3.2 OpenAPI-Client-Generierung

**Automatische Generierung:**
- Services: `ControllerService` für alle Backend-Endpoints
- Models: `ShortLinkDto` und andere DTOs
- TypeScript-Interfaces für typsichere Request/Response-Objekte

**Konfiguration (d857a2f):**
- Dynamische `basePath`-Ableitung aus `window.location.origin`
- Ermöglicht flexibles Deployment ohne Hardcoding von URLs
- Funktioniert sowohl lokal (localhost) als auch in Production (K8s-Ingress)

**Vorteile:**
- Typsicherheit zwischen Frontend und Backend
- Automatische Synchronisation bei API-Änderungen
- Reduzierte Fehlerquote durch Code-Generation
- Kein manuelles Mapping von API-Responses nötig

### 5.3.3 Creation-Component (Custom-Implementierung)

**Verantwortlichkeit:**
- Hauptkomponente für URL-Shortening-Funktionalität
- Formular-Handling und Validierung
- Error-Handling und User-Feedback
- Clipboard-Integration

**Code-Location:** `shortly-frontend/src/app/creation/creation.ts`

**State-Management mit Angular Signals:**
- `loading`: Loading-State für UI-Feedback während API-Calls
- `error`: Fehlermeldungen aus API oder Validierung
- `result`: Erfolgreich generierter Shortlink
- `urlToShorten`: User-Input (zwei-Wege-Binding)

**Kern-Funktionalität `createPost()`:**

1. **Validierung:** Prüfung auf leere URL
2. **Payload-Erstellung:** ShortLinkDto mit 7-Tage-Ablaufzeit
3. **API-Call:** Typsicherer Call über generierten `ControllerService`
4. **URL-Konstruktion:** Dynamische Basis-URL via `window.location.origin` für flexible Deployments
5. **Error-Handling:** RFC-7807-ProblemDetail-Parsing über `ApiErrorUtil`

**Clipboard-Funktionalität `copy()`:**
- Moderne Clipboard-API für Copy-to-Clipboard
- User-Feedback via Alert
- Error-Handling für unsichere Kontexte

**Besonderheiten:**
- **Standalone Component:** Moderne Angular-Architektur ohne NgModule
- **Signal-basiert:** Reaktives State-Management ohne RxJS-Observables für UI-State
- **RxJS-Integration:** Nur für HTTP-Calls (`.pipe()`, `finalize()`)
- **Dependency Injection:** `inject()` statt Constructor-Injection
- **SSR-Safe:** `window`-Check für Server-Side-Rendering-Kompatibilität

**Integration mit generiertem Code:**
- Nutzt generierten `ControllerService` und `ShortLinkDto`-Interface
- Konsumiert typsichere API-Methoden ohne manuelle HTTP-Calls

### 5.3.4 Deployment

**Build-Prozess:**
- Angular CLI kompiliert TypeScript zu JavaScript
- Tree-Shaking für minimale Bundle-Größe
- Statische Assets (HTML, CSS, JS) werden erzeugt

**Production-Serving:**
- NGINX serviert die kompilierten Dateien
- SPA-Routing mit Fallback auf `index.html` (alle Routen → Angular-Router)
- Gzip-Kompression für optimale Performance

---

## 6. Laufzeitsicht

### 6.1 Szenario: Shortlink erstellen

```
┌────────┐         ┌────────────┐         ┌──────────────────┐         ┌───────────┐
│ Client │         │ Controller │         │ ShortLinkService │         │   Redis   │
└────────┘         └────────────┘         └──────────────────┘         └───────────┘
    │                     │                         │                         │
    │ POST /api/shorten   │                         │                         │
    │ {url, expiresAt}    │                         │                         │
    ├────────────────────▶│                         │                         │
    │                     │ @Valid                  │                         │
    │                     │ validate(dto)           │                         │
    │                     ├────────────┐            │                         │
    │                     │            │            │                         │
    │                     │◀───────────┘            │                         │
    │                     │                         │                         │
    │                     │ create(url, expiresAt)  │                         │
    │                     ├────────────────────────▶│                         │
    │                     │                         │ generate()              │
    │                     │                         ├──────────┐              │
    │                     │                         │          │              │
    │                     │                         │◀─────────┘              │
    │                     │                         │                         │
    │                     │                         │ existsById(code)?       │
    │                     │                         ├────────────────────────▶│
    │                     │                         │                         │
    │                     │                         │◀────────────────────────┤
    │                     │                         │ false                   │
    │                     │                         │                         │
    │                     │                         │ save(ShortLink)         │
    │                     │                         ├────────────────────────▶│
    │                     │                         │                         │
    │                     │◀────────────────────────┤                         │
    │                     │ ShortLink               │                         │
    │                     │                         │                         │
    │◀────────────────────┤                         │                         │
    │ 201 Created         │                         │                         │
    │ {shortCode}         │                         │                         │
```

**Schritte:**

1. Client sendet POST-Request mit URL und optionalem `expiresAt`
2. Controller validiert Input mit Bean-Validation
3. Service generiert ShortCode (bis kein Konflikt)
4. Service berechnet TTL aus `expiresAt`
5. Repository speichert in Redis
6. Controller gibt ShortCode zurück (201 Created)

### 6.2 Szenario: Shortlink auflösen (Redirect)

```
┌────────┐         ┌────────────┐         ┌──────────────────┐         ┌───────────┐
│ Client │         │ Controller │         │ ShortLinkService │         │   Redis   │
└────────┘         └────────────┘         └──────────────────┘         └───────────┘
    │                     │                         │                         │
    │ GET /abc123         │                         │                         │
    ├────────────────────▶│                         │                         │
    │                     │ @ValidShortCode         │                         │
    │                     │ validate("abc123")      │                         │
    │                     ├──────────┐              │                         │
    │                     │          │              │                         │
    │                     │◀─────────┘              │                         │
    │                     │                         │                         │
    │                     │ findById("abc123")      │                         │
    │                     ├────────────────────────▶│                         │
    │                     │                         │                         │
    │                     │                         │ findById("abc123")      │
    │                     │                         ├────────────────────────▶│
    │                     │                         │                         │
    │                     │                         │◀────────────────────────┤
    │                     │                         │ ShortLink               │
    │                     │                         │                         │
    │                     │                         │ clicks++                │
    │                     │                         ├──────┐                  │
    │                     │                         │      │                  │
    │                     │                         │◀─────┘                  │
    │                     │                         │                         │
    │                     │                         │ save(ShortLink)         │
    │                     │                         ├────────────────────────▶│
    │                     │                         │                         │
    │                     │◀────────────────────────┤                         │
    │                     │ Optional<ShortLink>     │                         │
    │                     │                         │                         │
    │◀────────────────────┤                         │                         │
    │ 302 Found           │                         │                         │
    │ Location: {url}     │                         │                         │
    │                     │                         │                         │
    │ GET {url}           │                         │                         │
    ├─────────────────────────────────────────────────────────────────────────▶
```

**Schritte:**

1. Client ruft GET /{shortCode} auf
2. Controller validiert ShortCode-Format (6 Zeichen)
3. Service lädt ShortLink aus Redis
4. Service inkrementiert Click-Counter
5. Service speichert Update zurück
6. Controller sendet 302-Redirect mit Original-URL
7. Browser folgt Redirect automatisch


---

## 7. Verteilungssicht

### 7.1 Infrastruktur Kubernetes

```
┌─────────────────────────────────────────────────────────────┐
│                    Kubernetes Cluster                        │
│                                                               │
│  ┌────────────────────────────────────────────────────────┐ │
│  │                   Ingress Controller                    │ │
│  │  - nginx.ingress.kubernetes.io                          │ │
│  │  - Regex-Support für Shortcode-Pattern                  │ │
│  └────────────────────────────────────────────────────────┘ │
│         │                          │                          │
│         ▼                          ▼                          │
│  ┌─────────────┐           ┌────────────────┐               │
│  │  Frontend   │           │    Backend     │               │
│  │   Service   │           │    Service     │               │
│  │ ClusterIP   │           │  ClusterIP     │               │
│  │   Port 80   │           │   Port 8080    │               │
│  └─────────────┘           └────────────────┘               │
│         │                          │                          │
│         ▼                          ▼                          │
│  ┌─────────────┐           ┌────────────────┐               │
│  │  Frontend   │           │    Backend     │               │
│  │    Pods     │           │     Pods       │               │
│  │  (NGINX)    │           │  (Spring)      │               │
│  │  Replicas:1 │           │  Replicas:1    │               │
│  └─────────────┘           └────────────────┘               │
│                                    │                          │
│                                    ▼                          │
│                            ┌────────────────┐                │
│                            │     Redis      │                │
│                            │    Service     │                │
│                            │   ClusterIP    │                │
│                            │   Port 6379    │                │
│                            └────────────────┘                │
│                                    │                          │
│                                    ▼                          │
│                            ┌────────────────┐                │
│                            │   Redis Pod    │                │
│                            │  (Standalone)  │                │
│                            └────────────────┘                │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

### 7.2 Ingress-Routing

**Konfiguration:** `deploy/helm/shortly/templates/ingress.yaml`

```yaml
paths:
  # API-Endpoints → Backend
  - path: /api
    pathType: Prefix
    backend:
      service:
        name: shortly-backend
        port: 8080

  # Shortcode-Pattern: 6 alphanumerische Zeichen → Backend
  - path: /[A-Za-z0-9]{6}
    pathType: ImplementationSpecific
    backend:
      service:
        name: shortly-backend
        port: 8080

  # Alles andere → Frontend
  - path: /
    pathType: Prefix
    backend:
      service:
        name: shortly-frontend
        port: 80
```

**Routing-Logik:**

1. `/api/shorten` → Backend (Prefix-Match)
2. `/abc123` → Backend (Regex-Match)
3. `/*` → Frontend (Prefix-Match, Fallback)

### 7.3 Container-Images

#### Backend-Image

**Base:** GraalVM Native Image + Distroless

**Dockerfile:** `shortly-java/Dockerfile`

**Build-Stages:**

1. **Builder-Stage:**
    - GraalVM Native-Image-Compiler
    - Maven-Build mit `native:compile`
    - UPX-Kompression (170MB → 65MB)
    - Build-Args: `-H:+StripDebugInfo`

2. **Runtime-Stage:**
    - Distroless Java-Base (minimal, keine Shell)
    - User: nonroot
    - Binary: `/app/shortly`
    - Port: 8080

**Image-Size:** ~65MB (nach UPX-Kompression)

**Startup-Zeit:** <1 Sekunde

**Memory:** ~128MB (Requests), 512MB (Limits)

#### Frontend-Image

**Base:** NGINX + Angular-Build

**Location:** `shortly-frontend/Dockerfile`

**Besonderheiten:**

- Angular-Build im Multi-Stage-Dockerfile
- NGINX-Konfiguration für SPA-Routing
- Kleineres Image (~50MB)

---

## 8. Querschnittliche Konzepte

### 8.1 Validierung

**Framework:** Jakarta Bean Validation (Hibernate Validator)

**Strategie:**

- **Request-Body:** `@Valid` auf DTO-Parameter + Bean-Validation-Annotations
- **Path-Parameter:** Custom-Annotation `@ValidShortCode` mit `ConstraintValidator`

**Validierungsregeln:**
- `ShortLinkDto.url`: `@NotBlank`, `@URL`
- `ShortLinkDto.expiresAt`: Optional
- `shortCode`: Exakt 6 Zeichen (Custom-Validator `@ValidShortCode`)

**Error-Handling:** `GlobalExceptionHandler` wandelt Validation-Exceptions in RFC 7807 ProblemDetail (HTTP 400)

### 8.2 Exception-Handling

**Strategie:** Global Exception Handler mit `@RestControllerAdvice`

**Code-Location:** `src/main/java/neusta/shortly/web/GlobalExceptionHandler.java`

**Behandelte Exceptions:**

- `MethodArgumentNotValidException` → 400 Bad Request
- `ConstraintViolationException` → 400 Bad Request

**Response-Format:** RFC 7807 ProblemDetail mit detaillierten Fehlermeldungen

**Vorteile:**
- Einheitliches Error-Format
- Zentrale Fehlerbehandlung (DRY)
- Standard-konform (RFC 7807)

### 8.3 Logging & Monitoring

**Logging:**

- Spring Boot Default (Logback)
- Log-Level: INFO (Production), DEBUG (Development)
- Keine explizite Log-Konfiguration (Defaults ausreichend)

**Monitoring:**

- **Actuator-Endpoints:** `/actuator/health`, `/actuator/prometheus`
- **Health Checks:**
    - Liveness: `/actuator/health/liveness` (Pod-Restart bei Failure)
    - Readiness: `/actuator/health/readiness` (kein Traffic bei Not-Ready)
- **Metrics:** Prometheus-Format für Scraping durch Prometheus
- **ServiceMonitor:** Optional für Prometheus-Operator

**Error-Tracking:**

- Sentry-Integration vorbereitet (`pom.xml`)
- Noch nicht konfiguriert (kein DSN)

### 8.4 Security

**Aktueller Stand:**

- Keine Authentication/Authorization
- API ist öffentlich zugänglich
- Input-Validierung gegen Injection-Angriffe

**Container-Security:**

- Distroless Base-Image (minimal attack surface)
- Non-Root-User
- Read-Only-Filesystem (könnte aktiviert werden)

**TODO für Production:**

- API-Key-Authentication für Admin-Endpoints (create/delete)
- Rate-Limiting im Ingress
- HTTPS/TLS-Terminierung

### 8.5 Testing

**Test-Strategie:**

| Test-Typ          | Framework                         | Coverage-Ziel          |
|-------------------|-----------------------------------|------------------------|
| Unit-Tests        | JUnit 5 + Mockito                 | >80% Line Coverage     |
| Integration-Tests | Spring Boot Test + Testcontainers | API-Endpoints          |
| Web-Layer-Tests   | MockMvc                           | Controller-Validierung |
| Mutation-Tests    | Pitest                            | Test-Qualität          |
| Performance-Tests | K6                                | Load-Testing           |

**Code-Quality-Tools:**

- **Checkstyle:** Google Code Style
- **PMD:** Static Code Analysis (Priority 1-3)
- **SpotBugs:** Bug-Detection (Medium/High)
- **JaCoCo:** Coverage-Report + Enforcement (80%)

**Test-Execution:**

```bash
mvn verify                 # Unit + Integration Tests + Coverage
mvn checkstyle:check       # Code-Style
mvn pmd:check              # Static Analysis
mvn spotbugs:check         # Bug Detection
mvn pitest:mutationCoverage # Mutation Testing
k6 run k6-load-test.js     # Performance Testing
```

**Coverage-Requirement:** 80% Line + Branch Coverage (enforced by JaCoCo)

**Neue Tests (003d605):**
- Performance-Tests mit K6 für Load-Testing
- Redis-backed Click-Tracking-Tests
- Validierung der atomaren Operationen unter Last

---

## 9. Architekturentscheidungen

### ADR-1: Redis als Datenbank

**Kontext:**

- URL-Shortener benötigt schnelle Lookups
- TTL-Funktionalität für Ablaufdaten
- Einfache Key-Value-Struktur

**Entscheidung:** Redis als Primary-Database

**Begründung:**

- Sub-Millisekunden-Latenz für Lookups
- Native TTL-Unterstützung (`EXPIRE`-Command)
- Einfache Spring-Data-Integration
- In-Memory = hohe Performance

**Konsequenzen:**

- (+) Sehr schnelle Redirects (<5ms)
- (+) Automatisches Cleanup abgelaufener Links
- (~) Keine Persistenz (bei Neustart Datenverlust) per default, kann aber per Parameter eingeschaltet werden
- (-) Single-Point-of-Failure (ohne Redis-Cluster)

**Alternativen:**

- PostgreSQL: Klassische Persistenz, aber langsamer und größer

### ADR-2: GraalVM Native Image

**Kontext:**

- Container-Umgebung mit vielen kurzen Lifecycles
- Startup-Zeit und Memory-Footprint relevant
- Java 25 + Spring Boot 4.0 unterstützen Native-Image

**Entscheidung:** GraalVM Native-Image-Kompilierung

**Begründung:**

- Startup-Zeit: <1s (vs. 5-10s für JVM)
- Memory: ~128MB (vs. 512MB+ für JVM)
- Image-Size: 65MB mit UPX (vs. 200MB+ JVM-Image)
- Bessere Container-Density

**Konsequenzen:**

- (+) Schnellerer Start (Pod-Restart, Scaling)
- (+) Geringerer Memory-Footprint
- (+) Kleinere Container-Images
- (-) Längere Build-Zeit (3-8min)
- (-) Reflection/Dynamic-Proxies bräuchten Hints

### ADR-3: Base62-Random-Generierung

**Kontext:**

- ShortCodes müssen eindeutig sein
- 6 Zeichen = guter Kompromiss (Länge vs. Coderaum)
- Verschiedene Generierungs-Strategien möglich

**Entscheidung:** Random Base62-Generierung mit Kollisionsprüfung

**Begründung:**

- Einfache Implementierung
- Gleichverteilung über Coderaum
- Keine Abhängigkeit von Counter/Sequence
- Thread-safe durch ThreadLocalRandom

**Konsequenzen:**

- (+) Stateless (keine DB-Sequence nötig)
- (+) Uniform-Random (keine Patterns erkennbar)
- (-) Potentielle Kollisionen (bei >50% Auslastung), in der Praxis jedoch unwahrscheinlich

**Alternativen:**

- Sequenz-basiert: Schneller, aber braucht Koordination (Redis-INCR)
- Hash-basiert: Deterministisch, aber höhere Kollisionsgefahr

### ADR-4: Mixed Routing (mit/ohne /api)

**Kontext:**

- Shortcodes sollen kurz bleiben: `/abc123` statt `/api/abc123`
- Admin-APIs sollen unter `/api` liegen
- Ingress muss beides routen können

**Entscheidung:** Kein globaler `@RequestMapping`-Prefix am Controller

**Begründung:**

- Shortcodes direkt unter Root-Path (`/{shortCode}`)
- API-Endpoints unter `/api` für klare Trennung
- Ingress-Regex für Shortcode-Pattern

**Konsequenzen:**

- (+) Kurze Shortlinks (User-Experience)
- (+) Klare API-Struktur
- (-) Komplexere Ingress-Konfiguration
- (-) Potentielle Konflikte (z.B. `/status` vs. Shortcode)

**Routing-Regeln:**

1. `/api/**` → Backend (Prefix-Match, höchste Priorität)
2. `/[A-Za-z0-9]{6}` → Backend (Regex-Match)
3. `/**` → Frontend (Prefix-Match, Fallback)

---

## 10. Qualitätsanforderungen

### 10.1 Qualitätsszenarien

| ID | Qualitätsziel  | Szenario           | Maßnahmen                                      |
|----|----------------|--------------------|------------------------------------------------|
| Q1 | Performance    | Redirect in <100ms | Redis-Caching, Native-Image                    |
| Q2 | Verfügbarkeit  | 99% Uptime (SLA)   | Health-Checks, Auto-Restart, Readiness-Probe   |
| Q3 | Skalierbarkeit | 10.000 req/s       | Stateless-Design, Horizontal-Scaling (HPA)     |
| Q4 | Wartbarkeit    | Feature in <2h     | Layered-Architecture, Unit-Tests, Clean-Code   |
| Q5 | Testbarkeit    | >80% Coverage      | JUnit, Mockito, Testcontainers, JaCoCo         |
| Q6 | Betreibbarkeit | Deployment <5min   | Helm-Charts, Health-Checks, Prometheus-Metrics |

### 10.2 Metriken

**Performance:**

- Redirect-Latenz: P50 < 50ms, P99 < 100ms
- API-Response-Time: P50 < 100ms, P99 < 500ms

**Verfügbarkeit:**

- Liveness-Probe: Fehlschlag → Pod-Restart
- Readiness-Probe: Fehlschlag → kein Traffic

**Code-Quality:**

- Line-Coverage: >80% (enforced)
- Branch-Coverage: >80% (enforced)
- Mutation-Score: >70% (Pitest)

---

## 11. Risiken und technische Schulden

### 11.1 Risiken

| Risk-ID | Risiko                        | Wahrscheinlichkeit | Impact  | Mitigation                               | Status      |
|---------|-------------------------------|--------------------|---------|------------------------------------------|-------------|
| R1      | Redis-Ausfall → Totalausfall  | Mittel             | Hoch    | Redis-Cluster, Persistent-Volume         | ✅ ERLEDIGT |
| R2      | Kollisions-Performance        | Niedrig            | Mittel  | Wechsel zu Sequenz-basierter Generierung | ABGLEHNT    |
| R3      | Click-Tracking Race-Condition | Hoch               | Niedrig | Redis INCR verwenden                     | ✅ ERLEDIGT |
| R4      | Keine Authentication          | Hoch               | Mittel  | API-Key für Admin-Endpoints              | OFFEN       |
| R5      | Native-Image-Build-Fehler     | Niedrig            | Hoch    | Reflection-Hints pflegen                 | OFFEN       |

**Mitigierte Risiken:**
- **R1 (9ec5755):** Redis-Persistenz aktiviert, Datenverlust-Risiko reduziert
- **R3 (0599852):** Race-Conditions durch atomare Redis-INCR-Operationen behoben

### 11.2 Technische Schulden

| TD-ID | Beschreibung                      | Impact                     | Aufwand | Priorität | Status      |
|-------|-----------------------------------|----------------------------|---------|-----------|-------------|
| TD1   | Click-Tracking nicht atomar       | Daten-Inkonsistenz         | 2h      | Mittel    | ✅ ERLEDIGT |
| TD2   | Keine Authentication              | Security-Lücke             | 8h      | Hoch      | OFFEN       |
| TD3   | Redis ohne Persistenz             | Datenverlust               | 1h      | Mittel    | ✅ ERLEDIGT |
| TD4   | Keine Rate-Limiting               | DoS-Anfälligkeit           | 2h      | Mittel    | ✅ ERLEDIGT |
| TD5   | ShortCode-Generierung ineffizient | Performance bei hoher Last | 4h      | Niedrig   | OFFEN       |

**Erledigte Schulden:**

- **TD1 (0599852):** Click-Tracking jetzt atomar via Redis INCR-Operationen
- **TD3 (9ec5755):** Redis-Persistenz für Production-Umgebung aktiviert
- **TD4 (9ec5755):** Rate-Limiting-Annotationen im Ingress hinzugefügt

**Priorisierung für Production:**

1. TD2: Authentication (Blocker für Production) - OFFEN
2. ~~TD3: Redis-Persistenz~~ - ERLEDIGT
3. ~~TD4: Rate-Limiting~~ - ERLEDIGT
4. ~~TD1: Atomares Click-Tracking~~ - ERLEDIGT
5. TD5: Generator-Optimierung (Performance-Reserve) - OFFEN

---

## 12. Glossar

| Begriff            | Definition                                          |
|--------------------|-----------------------------------------------------|
| **ShortCode**      | 6-stelliger alphanumerischer Code (z.B. "abc123")   |
| **ShortLink**      | Mapping von ShortCode zu Original-URL               |
| **Base62**         | Encoding mit 0-9, a-z, A-Z (62 Zeichen)             |
| **TTL**            | Time-To-Live (Ablaufzeit in Sekunden)               |
| **Redirect**       | HTTP 302-Weiterleitung zur Original-URL             |
| **Click-Tracking** | Zählen der Zugriffe auf einen ShortLink             |
| **Native-Image**   | Ahead-of-Time-kompiliertes Binary (GraalVM)         |
| **Distroless**     | Minimales Container-Image ohne OS-Tools             |
| **Health-Check**   | Endpoint zur Prüfung der Service-Verfügbarkeit      |
| **Actuator**       | Spring-Boot-Modul für Monitoring-Endpoints          |
| **Ingress**        | Kubernetes-Ressource für HTTP-Routing               |
| **ClusterIP**      | Kubernetes-Service-Typ (nur intern erreichbar)      |
| **HPA**            | Horizontal Pod Autoscaler (automatische Skalierung) |
| **ServiceMonitor** | Prometheus-Ressource für Metrics-Scraping           |

---

## Anhang

### A1. Verwendete Technologien

| Technologie        | Version  | Zweck                    |
|--------------------|----------|--------------------------|
| Java               | 25       | Programmiersprache       |
| Spring Boot        | 4.0.0    | Application-Framework    |
| Spring Data Redis  | 4.0.x    | Redis-Integration        |
| Redis              | 8-alpine | In-Memory-Datenbank      |
| GraalVM            | 25       | Native-Image-Compiler    |
| UPX                | 5.0.2    | Binary-Kompression       |
| Lombok             | Latest   | Boilerplate-Reduktion    |
| Jakarta Validation | 3.x      | Input-Validierung        |
| Springdoc OpenAPI  | 3.0.0    | API-Dokumentation        |
| JUnit 5            | 5.11.x   | Unit-Testing             |
| Testcontainers     | Latest   | Integration-Testing      |
| JaCoCo             | 0.8.14   | Coverage-Reporting       |
| Pitest             | 1.22.0   | Mutation-Testing         |
| Checkstyle         | 3.3.1    | Code-Style               |
| PMD                | 3.28.0   | Static-Analysis          |
| SpotBugs           | 4.9.8    | Bug-Detection            |
| K6                 | Latest   | Performance-Testing      |
| Angular            | Latest   | Frontend-Framework       |
| TypeScript         | Latest   | Frontend-Sprache         |
| RxJS               | Latest   | Reactive Programming     |
| OpenAPI Generator  | Latest   | Client-Code-Generierung  |
| Kubernetes         | 1.28+    | Container-Orchestrierung |
| Helm               | 3.x      | Kubernetes-Paketmanager  |
| NGINX Ingress      | Latest   | HTTP-Routing             |
| NGINX              | Latest   | Frontend-Webserver       |

### A2. Nützliche Links

- **Projekt-Repository:** (GitLab-URL)
- **OpenAPI-Spec:** `/api-docs` (Swagger-UI: `/swagger-ui.html`)
- **Actuator-Endpoints:** `/actuator`
- **Prometheus-Metrics:** `/actuator/prometheus`
- **Helm-Chart:** `deploy/helm/shortly/`

### A3. Build & Deployment

**Lokal bauen:**

```bash
cd shortly-java
./mvnw clean package -DskipTests
```

**Native-Image bauen:**

```bash
./mvnw -Pnative native:compile
```

**Docker-Image bauen:**

```bash
docker build -t shortly-backend:latest .
```

**Helm-Deployment:**

```bash
cd deploy/helm
helm install shortly ./shortly -f values.yaml
```

**Lokales Testing mit Docker Compose:**

```bash
docker-compose up
```

---

## Changelog

### Version 1.1 (2026-01-06)
- **Frontend-Dokumentation** hinzugefügt (Abschnitt 4.4, 5.3)
- **Backend-Refactorings** dokumentiert:
- **Infrastruktur-Updates** dokumentiert:
- **CI/CD-Verbesserungen** dokumentiert:
- **Testing** erweitert:
- **Technische Schulden** aktualisiert:
  - TD1 (Click-Tracking) ✅ ERLEDIGT
  - TD3 (Redis-Persistenz) ✅ ERLEDIGT
  - TD4 (Rate-Limiting) ✅ ERLEDIGT
- **Risiken** aktualisiert:
  - R1 (Redis-Ausfall) mitigiert
  - R3 (Race-Conditions) behoben

### Version 1.0 (Initial)
- Initiale Dokumentation des Shortly-Projekts
- Backend-fokussierte Arc42-Dokumentation

---

**Dokument-Version:** 1.1
**Stand:** 2026-01-06
**Autor:** Lucas Immoor (Lernprojekt)
