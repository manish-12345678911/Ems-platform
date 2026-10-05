# Chapter 11: Production Deployment, Operations & Disaster Recovery Manual

## 11.1 Infrastructure Requirements & Hardware Sizing

### 11.1.1 Minimum & Recommended Cloud Specifications

| Tier | Environment | Target Scale | Hardware Profile | Recommended Cloud Instance |
| :--- | :--- | :--- | :--- | :--- |
| **Development / Test** | Local / Staging | 1-20 Units | 4 vCPU, 8 GB RAM, 50 GB NVMe | DigitalOcean Basic ($24/mo) |
| **Metropolitan Production** | Regional City (Jaipur) | 14-100 Units | 8 vCPU, 16 GB RAM, 160 GB NVMe | Hetzner CPX41 (~€24/mo) / AWS c6i.2xlarge |
| **National Enterprise** | State-Wide Multi-Region | 500+ Units | High-Availability K8s Cluster | AWS EKS / GCP GKE Multi-Zone |

---

## 11.2 Production Startup Procedures

### 11.2.1 Operating System Preparation (Ubuntu 24.04 LTS)
```bash
# 1. Update system packages
sudo apt update && sudo apt upgrade -y

# 2. Install Docker Engine & Docker Compose Plugin
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# 3. Optimize kernel parameters for high-throughput networking & Redis
sudo tee -a /etc/sysctl.conf <<EOF
vm.overcommit_memory = 1
net.core.somaxconn = 65535
fs.file-max = 2097152
EOF
sudo sysctl -p
```

### 11.2.2 Secrets & Environment Variables Configuration
Production configurations must never commit plaintext passwords. Deploy a production `.env` file:
```bash
# Security & Database Credentials
PG_PASSWORD=prod_vault_pg_secure_9871
KC_ADMIN_PASSWORD=prod_vault_kc_admin_5432
HASH_SALT=prod_vault_salted_secret_hmac_256

# Active Profiles
SPRING_PROFILES_ACTIVE=docker
KAFKA_BOOTSTRAP_SERVERS=kafka:29092
REDIS_HOST=redis
REDIS_PORT=6379
```

### 11.2.3 Launching the Production Container Mesh
```bash
# 1. Clone repository to /opt/ambulance
sudo git clone <repo-url> /opt/ambulance
cd /opt/ambulance

# 2. Compile and package fat executable JARs
./mvnw clean package -DskipTests

# 3. Launch the container mesh in detached mode
docker compose up -d --build

# 4. Verify healthy state of all containers
docker compose ps
```

---

## 11.3 Observability, Monitoring & Health Probes

### 11.3.1 Actuator Healthcheck Verification Matrix
All microservices expose standardized Spring Boot Actuator endpoints accessible on internal networks:

```
+-------------------------------------------------------------------------------+
|                        SERVICE HEALTH CHECK MATRIX                            |
+----------------------+-----------+--------------------------------------------+
| Microservice         | Port      | Health Check URL                           |
+----------------------+-----------+--------------------------------------------+
| API Gateway          | 8080      | http://localhost:8080/actuator/health      |
| Incident Service     | 8081      | http://localhost:8081/actuator/health      |
| Dispatch Service     | 8082      | http://localhost:8082/actuator/health      |
| Tracking Service     | 8083      | http://localhost:8083/actuator/health      |
| Routing Service      | 8084      | http://localhost:8084/actuator/health      |
| Hospital Service     | 8085      | http://localhost:8085/actuator/health      |
| Redeployment Service | 8086      | http://localhost:8086/actuator/health      |
| Audit Service        | 8087      | http://localhost:8087/actuator/health      |
+----------------------+-----------+--------------------------------------------+
```

### 11.3.2 Prometheus & Grafana Observability
The platform bundles pre-configured monitoring configurations:
* **Prometheus (:9090)**: Automatically scrapes microservice JVM metrics, garbage collection latency, HTTP request duration percentiles, and Kafka consumer group lag.
* **Grafana Dashboard (:3000)**: Visualizes real-time metrics:
  * Total active emergency calls per hour.
  * Mean candidate scoring latency ($P_{50}, P_{95}, P_{99}$).
  * Unit status distribution (`AVAILABLE` vs `TRANSPORTING`).
  * Hospital diversion status and emergency department bed occupancy.

---

## 11.4 Backup, Disaster Recovery & High-Availability Runbooks

### 11.4.1 Automated Daily Database Backups
```bash
#!/usr/bin/env bash
set -e
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_DIR="/var/backups/h8_ems"
mkdir -p "$BACKUP_DIR"

# Execute pg_dump with PostGIS schema preservation
docker exec -t h8-postgres-1 pg_dump -U h8 -d h8 -F c -b -v \
  -f "/tmp/h8_backup_${TIMESTAMP}.dump"

docker cp "h8-postgres-1:/tmp/h8_backup_${TIMESTAMP}.dump" "${BACKUP_DIR}/"
gzip "${BACKUP_DIR}/h8_backup_${TIMESTAMP}.dump"

# Retain backups for 30 days
find "$BACKUP_DIR" -type f -mtime +30 -name "*.dump.gz" -exec rm {} \;
```

### 11.4.2 Point-In-Time Restoration Procedure
```bash
# 1. Stop mutating services
docker stop h8-dispatch-service-1 h8-incident-service-1

# 2. Restore PostgreSQL database from dump
gunzip /var/backups/h8_ems/h8_backup_20261005_090000.dump.gz
docker cp /var/backups/h8_ems/h8_backup_20261005_090000.dump h8-postgres-1:/tmp/
docker exec -it h8-postgres-1 pg_restore -U h8 -d h8 --clean --if-exists /tmp/h8_backup_20261005_090000.dump

# 3. Restart microservices
docker start h8-incident-service-1 h8-dispatch-service-1
```

### 11.4.3 Zero-Downtime Rolling Update Workflow
1. Package new JARs: `./mvnw clean package -DskipTests`.
2. Rebuild target service container: `docker compose build dispatch-service`.
3. Re-create container with zero gateway disruption: `docker compose up -d --no-deps dispatch-service`.
4. Validate health: `curl -s http://localhost:8082/actuator/health`.
