# H8 EMS Deployment & Operations Guide

## 1. Quick Startup (Local / Developer)
Run the startup script:
```cmd
run_project.bat
```
Or execute manually:
```cmd
# 1. Start Docker services (PostgreSQL PostGIS, Redis, Kafka, Keycloak)
docker-compose up -d

# 2. Start Web Server
python -m http.server 8088 --directory web
```

---

## 2. Ports & Service Topology

| Service | Port | Endpoint / Healthcheck | Description |
| :--- | :--- | :--- | :--- |
| **Web Frontend** | `8088` | `http://localhost:8088/` | Static Dispatcher, Crew PWA, and ED Hub |
| **API Gateway** | `8080` | `http://localhost:8080/actuator/health` | Central gateway routing requests to services |
| **Incident Service** | `8081` | `http://localhost:8081/actuator/health` | Emergency intake, MPDS severity, salted caller hash |
| **Dispatch Service** | `8082` | `http://localhost:8082/actuator/health` | Candidate ranking, scoring engine, unit reservations |
| **Tracking Service** | `8083` | `http://localhost:8083/actuator/health` | Vehicle GPS ingest & spatial Redis queries |
| **Routing Service** | `8084` | `http://localhost:8084/actuator/health` | GraphHopper travel time matrix & ETA calculations |
| **Hospital Service** | `8085` | `http://localhost:8085/actuator/health` | ED bed capacities, trauma bays, AlertHub SSE |
| **Redeployment Service**| `8086` | `http://localhost:8086/actuator/health` | MEXCLP double-standard coverage optimizer |
| **Audit Service** | `8087` | `http://localhost:8087/actuator/health` | SHA-256 tamper-evident cryptographic hash ledger |
| **PostgreSQL (PostGIS)**| `5432` | `pg_isready -U h8` | Spatial relational store |
| **Redis** | `6379` | `redis-cli ping` | Distributed locks & fast capacity cache |
| **Kafka Broker** | `9092` | `localhost:9092` | Event-driven transactional outbox message bus |

---

## 3. Production Deployment (Docker Compose / Cloud VPS)
To deploy the entire stack in production on Ubuntu / Debian / AWS / GCP:

```bash
# 1. Clone repository
git clone <repo-url> /opt/ambulance
cd /opt/ambulance

# 2. Launch infrastructure and services
docker-compose -f docker-compose.yml up -d --build

# 3. Check container health
docker-compose ps
```

---

## 4. Web Application Access
* **Platform Portal**: `http://<SERVER_IP>:8088/`
* **Dispatcher Command Center**: `http://<SERVER_IP>:8088/dispatcher/`
* **Paramedic Crew Mobile PWA**: `http://<SERVER_IP>:8088/crew/`
* **Hospital ED Tactical Hub**: `http://<SERVER_IP>:8088/ed/`
