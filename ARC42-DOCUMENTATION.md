# Shortly - Arc42 Architekturdokumentation

**Architekturdokumentation eines Cloud-Native URL-Shortener-Service**

---

## 1. Einführung und Ziele

### 1.1 Aufgabenstellung

Shortly ist ein URL-Shortener-Service, der lange URLs in kurze, 6-stellige alphanumerische Codes umwandelt. Der Service ermöglicht:

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

| Rolle      | Erwartungshaltung                                                           |
|------------|-----------------------------------------------------------------------------|
| Entwickler | Lernprojekt für Spring Boot 4.0, Java 25, GraalVM Native Image und Angular |
| Betreiber  | Einfaches Deployment via Kubernetes/Helm, gutes Monitoring                  |

---

## 2. Randbedingungen

### 2.1 Technische Randbedingungen

| Randbedingung            | Erläuterung                                                                      |
|--------------------------|----------------------------------------------------------------------------------|
| **Java 25**              | Neueste Java-Version mit modernen Features                                       |
| **Spring Boot 4.0**      | Cutting-Edge Framework-Version                                                   |
| **GraalVM Native Image** | Kompilierung zu nativem Binary für schnellen Start und geringen Memory-Footprint |
| **Angular**              | Moderne SPA mit TypeScript, Signals und OpenAPI-Codegenerierung                  |
| **Redis**                | In-Memory-Datenbank für schnelle Zugriffe und TTL-Support                        |
| **Kubernetes**           | Zielplattform für Deployment                                                     |

### 2.2 Organisatorische Randbedingungen

- WIP-Status: Noch kein Produktiv-Einsatz
- Kein Authentication/Authorization implementiert (API ist öffentlich)
- Solo-Projekt ohne Team-Konventionen

---

## 3. Kontextabgrenzung

### 3.1 Fachlicher Kontext

**Akteure:**
- **User (Browser)**: Konsumiert Redirect-Funktionalität und interagiert mit der Web-UI
- **Frontend (Angular SPA)**: Stellt UI für URL-Shortening bereit
- **Backend (Spring Boot)**: Verarbeitet API-Requests und führt Redirects aus
- **Redis**: Persistiert ShortLinks mit TTL-Support

**Interaktionen:**
- Browser → Frontend: Lädt SPA, navigiert zu ShortLinks
- Frontend → Backend: API-Calls für URL-Shortening (`POST /api/shorten`)
- Browser → Backend: Direkter Redirect-Aufruf (`GET /{code}`)
- Backend → Redis: Speichert und lädt ShortLinks

### 3.2 Technischer Kontext

**Deployment-Architektur:**
- **Ingress (NGINX)**: Routing-Layer mit Pattern-basiertem Routing
  - `/api/**` → Backend
  - `/[A-Za-z0-9]{6}` → Backend (Shortcode-Redirects)
  - `/*` → Frontend (Fallback)
- **Frontend Service**: NGINX-Server mit Angular SPA (Port 80)
- **Backend Service**: Spring Boot Native Binary (Port 8080)
- **Redis Service**: In-Memory-Datenbank (Port 6379)

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
| **Angular**          | Moderne SPA-Architektur mit TypeScript, Signals und starkem Tooling          |
| **Redis**            | Sehr schnelle In-Memory-DB, native TTL-Unterstützung für Ablaufdaten         |
| **GraalVM Native**   | ~65MB Image-Size (mit UPX), schneller Start (<1s), geringer Memory-Footprint |
| **Base62-Encoding**  | 62^6 = ~56 Mrd. mögliche Codes, URL-safe (keine Sonderzeichen)               |
| **OpenAPI-Codegen**  | Typsichere Frontend/Backend-Integration, automatische Synchronisation        |
| **Stateless Design** | Horizontal skalierbar, keine Session-Verwaltung nötig                        |

### 4.2 Architektur-Prinzipien

**Backend:**
- **Layered Architecture** (3-Schicht): Klare Trennung von Web, Business-Logik, Persistence
- **RESTful API**: HTTP-Verben + Statuscodes für API-Design
- **Dependency Injection**: Spring-Container managed alle Komponenten

**Frontend:**
- **Single-Page-Application**: Angular mit Client-Side-Routing
- **Component-basiert**: Standalone Components ohne NgModule
- **Signal-basiert**: Modernes reaktives State-Management
- **OpenAPI-generiert**: Typsichere Backend-Kommunikation

**Integration:**
- **API-First**: Backend definiert OpenAPI-Spec, Frontend generiert Client
- **Dynamische URLs**: `window.location.origin` für flexible Deployments
- **Error-Handling**: RFC 7807 ProblemDetail für standardisierte Fehler

---

## 5. Bausteinsicht

### 5.1 Backend-Architektur

**Layer-Struktur:**

1. **Web Layer**
   - REST-Controller mit API-Endpoints und Redirect-Logik
   - Validierung mit Bean Validation und Custom Annotations
   - Exception-Handling mit RFC 7807 ProblemDetail

2. **Service Layer**
   - ShortLinkService: Kern-Business-Logik
   - ShortCodeGenerator: Base62-Code-Generierung
   - TTL-Berechnung und Click-Tracking

3. **Persistence Layer**
   - Redis-Integration via StringRedisTemplate
   - ShortLink-Entity mit TTL-Support
   - Atomare Click-Tracking-Operationen

**Wichtige Komponenten:**

| Komponente              | Verantwortlichkeit                                           |
|-------------------------|--------------------------------------------------------------|
| Controller              | REST-API-Endpoints: `/{code}`, `/api/shorten`, `/api/stats/{code}`, `/api/{code}` |
| ShortLinkService        | Business-Logik, ShortCode-Generierung mit Kollisionsvermeidung, atomares Click-Tracking |
| Base62Generator         | Generiert 6-stellige alphanumerische Codes (62^6 = ~57 Mrd. Kombinationen) |
| ShortLink Entity        | Datenmodell mit `@RedisHash` und `@TimeToLive` für automatisches Ablaufen |
| GlobalExceptionHandler  | Zentrale Exception-Behandlung mit RFC 7807 Format            |

**Besonderheiten:**
- Mixed Routing: `/api` für Admin-Endpoints, `/{code}` für Redirects
- Redis INCR für atomares Click-Tracking ohne Race-Conditions
- Custom Validation: `@ValidShortCode` für 6-Zeichen-Format

### 5.2 Frontend-Architektur

**Angular-Struktur:**

1. **Generated Layer (OpenAPI)**
   - ControllerService: Typsichere API-Client-Methoden
   - Models: DTOs und Interfaces (z.B. ShortLinkDto)
   - Configuration: Dynamische basePath-Konfiguration

2. **Application Layer**
   - Creation Component: Hauptkomponente für URL-Shortening
   - Routing: Angular Router für SPA-Navigation
   - Error-Handling: RFC 7807 ProblemDetail-Parsing

3. **Infrastructure Layer**
   - NGINX: Statischer Webserver für Production
   - Build-Pipeline: Angular CLI mit Tree-Shaking

**Wichtige Komponenten:**

| Komponente                    | Verantwortlichkeit                                          |
|-------------------------------|-------------------------------------------------------------|
| Creation Component            | Formular-Handling, Validierung, Signal-basiertes State-Management |
| OpenAPI-Generated Services    | Typsichere Backend-Kommunikation, automatische Synchronisation |
| NGINX-Konfiguration           | SPA-Routing mit Fallback, Gzip-Kompression |

**Besonderheiten:**
- Standalone Components (ohne NgModule)
- RxJS nur für HTTP-Calls, Signals für UI-State
- SSR-Safe durch `window`-Checks
- Dependency Injection via `inject()`

### 5.3 Datenmodell

**ShortLink-Entity:**

| Feld         | Typ    | Beschreibung                                    |
|--------------|--------|-------------------------------------------------|
| shortCode    | String | Eindeutiger 6-stelliger Identifier (Primary Key) |
| originalUrl  | String | Ziel-URL für Redirect                           |
| clicks       | int    | Anzahl der Zugriffe (atomar inkrementiert)      |
| ttl          | Long   | Time-To-Live in Sekunden (null = permanent)     |

**Redis-Storage:**
- Key-Format: `links:{shortCode}`
- Automatisches Löschen via Redis TTL-Mechanismus
- Atomare Updates für Click-Counter

---

## 6. Laufzeitsicht

### 6.1 Szenario: URL verkürzen (Frontend → Backend)

**Ablauf:**
1. User gibt URL im Frontend-Formular ein
2. Frontend validiert Input (nicht leer, gültige URL)
3. Frontend sendet `POST /api/shorten` mit `ShortLinkDto` (URL + Ablaufdatum)
4. Backend validiert Request mit Bean Validation
5. Service generiert ShortCode (Base62, 6 Zeichen) mit Kollisionsprüfung
6. Service berechnet TTL aus Ablaufdatum
7. Service speichert ShortLink in Redis
8. Backend gibt ShortCode zurück (HTTP 201 Created)
9. Frontend konstruiert vollständige URL (`window.location.origin/{code}`)
10. Frontend zeigt ShortLink mit Copy-Button an

### 6.2 Szenario: Shortlink auflösen (Redirect)

**Ablauf:**
1. User klickt auf ShortLink oder gibt URL im Browser ein (`/{code}`)
2. Ingress routet Request zum Backend (Pattern-Match: 6 alphanumerische Zeichen)
3. Backend validiert ShortCode-Format
4. Service lädt ShortLink aus Redis
5. Service inkrementiert Click-Counter (atomar via Redis INCR)
6. Backend sendet HTTP 302 Redirect mit Original-URL
7. Browser folgt Redirect automatisch zur Ziel-URL

### 6.3 Szenario: Statistiken abrufen

**Ablauf:**
1. Frontend sendet `GET /api/stats/{code}`
2. Backend lädt ShortLink aus Redis
3. Backend gibt Statistiken zurück (Klicks, Original-URL, Ablaufdatum)
4. Frontend zeigt Statistiken an

---

## 7. Verteilungssicht

### 7.1 Kubernetes-Deployment

**Services:**
- **shortly-frontend**: ClusterIP auf Port 80 (NGINX + Angular)
- **shortly-backend**: ClusterIP auf Port 8080 (GraalVM Native Binary)
- **shortly-redis**: ClusterIP auf Port 6379 (Redis Standalone)

**Ingress-Routing:**
- Pattern-basiertes Routing für Shortcodes: `/[A-Za-z0-9]{6}`
- Prefix-basiertes Routing für API: `/api`
- Fallback für Frontend: `/`

**Helm-Chart:**
- Parametrisierbar via `values.yaml`
- Separate Image-Tags für Frontend/Backend
- Redis mit optionaler Persistenz (PVC)

### 7.2 Container-Images

**Backend-Image:**
- Base: GraalVM Native Image + Distroless
- Size: ~65MB (nach UPX-Kompression)
- Startup: <1 Sekunde
- Memory: ~128MB Requests, 512MB Limits

**Frontend-Image:**
- Base: NGINX Alpine
- Size: ~50MB
- Konfiguration: SPA-Routing, Gzip-Kompression

**Redis-Image:**
- Base: Redis 8 Alpine
- Persistenz: Optional via Volume Mount
- Size: ~35MB

---

## 8. Querschnittliche Konzepte

### 8.1 Validierung

**Backend:**
- Jakarta Bean Validation (Hibernate Validator)
- Request-Body: `@Valid` auf DTO-Parameter
- Path-Parameter: Custom `@ValidShortCode` Annotation

**Frontend:**
- Template-driven Validation
- Client-Side-Validierung vor API-Call
- Server-Side-Fehler werden geparst und angezeigt

**Error-Format:**
- RFC 7807 ProblemDetail für standardisierte Fehler
- HTTP 400 Bad Request bei Validierungsfehlern

### 8.2 Exception-Handling

**Backend:**
- Global Exception Handler (`@RestControllerAdvice`)
- Zentrale Behandlung von Validation-Exceptions
- RFC 7807 ProblemDetail Response-Format

**Frontend:**
- ApiErrorUtil für ProblemDetail-Parsing
- Error-Signals für UI-Feedback
- User-freundliche Fehlermeldungen

### 8.3 Logging & Monitoring

**Backend:**
- Spring Boot Actuator: `/actuator/health`, `/actuator/prometheus`
- Health Checks: Liveness und Readiness Probes
- Prometheus-Metrics für Monitoring

**Kubernetes:**
- Liveness-Probe: Pod-Restart bei Failure
- Readiness-Probe: Kein Traffic bei Not-Ready
- ServiceMonitor: Optional für Prometheus-Operator

### 8.4 Security

**Aktueller Stand:**
- Keine Authentication/Authorization
- Input-Validierung gegen Injection-Angriffe
- CORS nur im Dev-Profil aktiviert

**Container-Security:**
- Distroless Base-Image (minimale Angriffsfläche)
- Non-Root-User
- Keine Shell im Container

**TODO für Production:**
- API-Key-Authentication für Admin-Endpoints
- Rate-Limiting im Ingress
- HTTPS/TLS-Terminierung

### 8.5 Testing

**Backend:**
- Unit-Tests: JUnit 5 + Mockito
- Integration-Tests: Spring Boot Test + Testcontainers
- Mutation-Tests: Pitest
- Performance-Tests: K6
- Coverage: >80% (enforced by JaCoCo)

**Frontend:**
- Unit-Tests: Jasmine/Karma
- E2E-Tests: Möglich mit Playwright/Cypress

---

## 9. Architekturentscheidungen

### ADR-1: Redis als Datenbank

**Entscheidung:** Redis als Primary-Database

**Begründung:**
- Sub-Millisekunden-Latenz für Lookups
- Native TTL-Unterstützung
- Einfache Spring-Data-Integration
- Atomare Operationen für Click-Tracking

**Konsequenzen:**
- (+) Sehr schnelle Redirects (<5ms)
- (+) Automatisches Cleanup abgelaufener Links
- (~) Persistenz optional (konfigurierbar)
- (-) Single-Point-of-Failure ohne Redis-Cluster

**Alternativen:**
- PostgreSQL: Klassische Persistenz, aber langsamer

### ADR-2: GraalVM Native Image

**Entscheidung:** GraalVM Native-Image-Kompilierung

**Begründung:**
- Startup-Zeit: <1s (vs. 5-10s JVM)
- Image-Size: 65MB mit UPX (vs. 200MB+ JVM)
- Bessere Container-Density

**Konsequenzen:**
- (+) Schnellerer Start und Scaling
- (+) Geringerer Memory-Footprint
- (-) Längere Build-Zeit (3-8min)
- (-) Reflection benötigt Hints

### ADR-3: Base62-Random-Generierung

**Entscheidung:** Random Base62-Generierung mit Kollisionsprüfung

**Begründung:**
- Einfache Implementierung
- Gleichverteilung über Coderaum
- Stateless (keine DB-Sequence)

**Konsequenzen:**
- (+) Keine zentrale Koordination nötig
- (+) Uniform-Random (keine Patterns)
- (-) Potentielle Kollisionen bei hoher Auslastung

**Alternativen:**
- Sequenz-basiert: Schneller, braucht aber Koordination
- Hash-basiert: Deterministisch, höhere Kollisionsgefahr

### ADR-4: Angular + OpenAPI-Codegenerierung

**Entscheidung:** Angular SPA mit OpenAPI-generiertem API-Client

**Begründung:**
- Typsicherheit zwischen Frontend und Backend
- Automatische Synchronisation bei API-Änderungen
- Moderne Angular-Features (Signals, Standalone Components)
- Dynamische URL-Konfiguration für flexible Deployments

**Konsequenzen:**
- (+) Reduzierte Fehlerquote durch Typsicherheit
- (+) Kein manuelles API-Client-Coding
- (-) Build-Prozess etwas komplexer

### ADR-5: Mixed Routing (mit/ohne /api)

**Entscheidung:** Kein globaler Prefix für Shortcode-Endpoints

**Begründung:**
- Shortcodes direkt unter Root (`/{code}`) für kurze URLs
- API-Endpoints unter `/api` für klare Trennung
- Ingress-Regex für Pattern-basiertes Routing

**Konsequenzen:**
- (+) Kurze Shortlinks (User-Experience)
- (+) Klare API-Struktur
- (-) Komplexere Ingress-Konfiguration

---

## 10. Qualitätsanforderungen

### 10.1 Qualitätsszenarien

| ID | Qualitätsziel  | Szenario           | Maßnahmen                                      |
|----|----------------|--------------------|------------------------------------------------|
| Q1 | Performance    | Redirect in <100ms | Redis-Caching, Native-Image                    |
| Q2 | Verfügbarkeit  | 99% Uptime         | Health-Checks, Auto-Restart, Readiness-Probe   |
| Q3 | Skalierbarkeit | 10.000 req/s       | Stateless-Design, Horizontal-Scaling (HPA)     |
| Q4 | Wartbarkeit    | Feature in <2h     | Layered-Architecture, Unit-Tests, Clean-Code   |
| Q5 | Testbarkeit    | >80% Coverage      | JUnit, Mockito, Testcontainers, JaCoCo         |
| Q6 | Betreibbarkeit | Deployment <5min   | Helm-Charts, Health-Checks, Prometheus-Metrics |

---

## 11. Risiken und technische Schulden

### 11.1 Risiken

| Risk-ID | Risiko                        | Wahrscheinlichkeit | Impact  | Mitigation                               | Status      |
|---------|-------------------------------|--------------------|---------|------------------------------------------|-------------|
| R1      | Redis-Ausfall → Totalausfall  | Mittel             | Hoch    | Redis-Cluster, Persistent-Volume         | ✅ ERLEDIGT |
| R2      | Kollisions-Performance        | Niedrig            | Mittel  | Wechsel zu Sequenz-basierter Generierung | ABGELEHNT   |
| R3      | Click-Tracking Race-Condition | Hoch               | Niedrig | Redis INCR verwenden                     | ✅ ERLEDIGT |
| R4      | Keine Authentication          | Hoch               | Mittel  | API-Key für Admin-Endpoints              | OFFEN       |
| R5      | Native-Image-Build-Fehler     | Niedrig            | Hoch    | Reflection-Hints pflegen                 | ✅ ERLEDIGT |

### 11.2 Technische Schulden

| TD-ID | Beschreibung                      | Impact                     | Priorität | Status      |
|-------|-----------------------------------|----------------------------|-----------|-------------|
| TD1   | Click-Tracking nicht atomar       | Daten-Inkonsistenz         | Mittel    | ✅ ERLEDIGT |
| TD2   | Keine Authentication              | Security-Lücke             | Hoch      | OFFEN       |
| TD3   | Redis ohne Persistenz             | Datenverlust               | Mittel    | ✅ ERLEDIGT |
| TD4   | Keine Rate-Limiting               | DoS-Anfälligkeit           | Mittel    | ✅ ERLEDIGT |
| TD5   | ShortCode-Generierung ineffizient | Performance bei hoher Last | Niedrig   | OFFEN       |

**Priorisierung für Production:**
1. TD2: Authentication (Blocker) - OFFEN
2. TD5: Generator-Optimierung (Performance-Reserve) - OFFEN

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
| **SPA**            | Single-Page-Application (Angular)                   |
| **OpenAPI**        | API-Spezifikationsformat für REST-APIs              |
| **Ingress**        | Kubernetes-Ressource für HTTP-Routing               |
| **HPA**            | Horizontal Pod Autoscaler (automatische Skalierung) |

---

**Dokument-Version:** 2.0
**Stand:** 2026-01-30
**Autor:** Lucas Immoor (Lernprojekt)
