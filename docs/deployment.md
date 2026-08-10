# Deployment Guide - Smart Hostel Management System

---

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Local Development Setup](#2-local-development-setup)
3. [Docker Deployment](#3-docker-deployment)
4. [Production Deployment with MySQL](#4-production-deployment-with-mysql)
5. [Environment Variables](#5-environment-variables)
6. [Troubleshooting](#6-troubleshooting)

---

## 1. Prerequisites

| Software | Version | Purpose |
|----------|---------|---------|
| Java JDK | 17+ | Backend runtime |
| Node.js | 18+ | Frontend development |
| Docker | 24+ | Containerized deployment |
| MySQL | 8.0+ | Production database |
| Maven | 3.8+ | Backend build |
| Git | Latest | Version control |

---

## 2. Local Development Setup

### 2.1 Clone the Repository

```bash
git clone https://github.com/your-username/smart-hostel-management.git
cd smart-hostel-management
```

### 2.2 Backend Setup

The backend uses H2 in-memory database for development by default, so no MySQL setup is needed for basic development.

```bash
cd backend

# Build the project
mvn clean install -DskipTests

# Run with H2 (default)
mvn spring-boot:run
```

Backend runs at `http://localhost:8080`

For development with MySQL, edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/hostel_db?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
```

### 2.3 Frontend Setup

```bash
cd frontend

# Install dependencies
npm install

# Create .env file
echo "VITE_API_BASE_URL=http://localhost:8080/api" > .env

# Start development server
npm start
```

Frontend runs at `http://localhost:3000`

### 2.4 Access the Application

| Component | URL |
|-----------|-----|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080/api |
| H2 Console | http://localhost:8080/h2-console |

### 2.5 Default Credentials

| Role | Email | Password |
|------|-------|----------|
| Admin | admin@hostel.com | admin123 |
| Warden | warden@hostel.com | warden123 |
| Student | student@hostel.com | student123 |

---

## 3. Docker Deployment

### 3.1 Prerequisites

- Docker Engine 24+
- Docker Compose 2.20+

### 3.2 Build and Run with Docker Compose

```bash
# From project root
docker-compose up -d

# View logs
docker-compose logs -f

# Check service status
docker-compose ps
```

### 3.3 Access the Application

| Service | URL |
|---------|-----|
| Frontend | http://localhost |
| Backend API | http://localhost:8080/api |
| MySQL | localhost:3307 (exposed, root/root_password) |

### 3.4 Useful Docker Commands

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (reset database)
docker-compose down -v

# Rebuild a specific service
docker-compose up -d --build backend

# Rebuild all services
docker-compose up -d --build

# View logs for a specific service
docker-compose logs -f backend

# Execute commands inside a container
docker-compose exec mysql mysql -u root -p hostel_db

# Check container health
docker ps
```

### 3.5 Docker Architecture

```
┌──────────────────────────────────────────────────────┐
│                    Docker Network                      │
│                  hostel-network                        │
│                                                        │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────┐  │
│  │   Frontend    │    │   Backend    │    │  MySQL   │  │
│  │   Nginx:80    │───▶│  Java:8080   │───▶│  :3306   │  │
│  │   React SPA   │    │  Spring Boot │    │  DB      │  │
│  └──────────────┘    └──────────────┘    └──────────┘  │
│                                                        │
└──────────────────────────────────────────────────────┘
```

---

## 4. Production Deployment with MySQL

### 4.1 Setting Up MySQL

```bash
# Connect to MySQL
mysql -u root -p

# Create database
CREATE DATABASE IF NOT EXISTS hostel_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

# Create dedicated user
CREATE USER 'hostel_user'@'%' IDENTIFIED BY 'strong_password';
GRANT ALL PRIVILEGES ON hostel_db.* TO 'hostel_user'@'%';
FLUSH PRIVILEGES;

# Import schema
USE hostel_db;
SOURCE /path/to/database/schema.sql;
EXIT;
```

### 4.2 Backend Production Configuration

Create `application-prod.properties`:

```properties
# Server
server.port=8080

# Database
spring.datasource.url=${SPRING_DATASOURCE_URL}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

# JWT
app.jwt.secret=${APP_JWT_SECRET}
app.jwt.expiration=${APP_JWT_EXPIRATION:86400000}

# File Upload
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# H2 (disable in production)
spring.h2.console.enabled=false
```

### 4.3 Building for Production

```bash
# Build backend JAR
cd backend
mvn clean package -DskipTests -Pproduction

# Build frontend
cd frontend
npm ci
npm run build
```

### 4.4 Running Backend JAR

```bash
java -jar backend/target/hostel-management-*.jar \
  --spring.profiles.active=prod \
  --spring.datasource.url=jdbc:mysql://localhost:3306/hostel_db?useSSL=true&serverTimezone=UTC \
  --spring.datasource.username=hostel_user \
  --spring.datasource.password=strong_password \
  --app.jwt.secret=your_256bit_jwt_secret_key
```

### 4.5 Serving Frontend with Nginx (Non-Docker)

```nginx
server {
    listen 80;
    server_name your-domain.com;
    root /var/www/hostel-frontend;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /uploads/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
    }

    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;
}
```

### 4.6 Running as a Systemd Service

Create `/etc/systemd/system/hostel-backend.service`:

```ini
[Unit]
Description=Smart Hostel Backend
After=network.target mysql.service

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/opt/hostel
Environment=SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/hostel_db?useSSL=false&serverTimezone=UTC
Environment=SPRING_DATASOURCE_USERNAME=hostel_user
Environment=SPRING_DATASOURCE_PASSWORD=strong_password
Environment=APP_JWT_SECRET=your_256bit_jwt_secret_key
Environment=APP_JWT_EXPIRATION=86400000
ExecStart=/usr/bin/java -jar /opt/hostel/app.jar --spring.profiles.active=prod
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable hostel-backend
sudo systemctl start hostel-backend
sudo systemctl status hostel-backend
```

---

## 5. Environment Variables

### 5.1 Backend

| Variable | Description | Default | Required |
|----------|-------------|---------|:--------:|
| `SPRING_DATASOURCE_URL` | MySQL JDBC connection URL | `jdbc:mysql://localhost:3306/hostel_db` | Yes |
| `SPRING_DATASOURCE_USERNAME` | Database username | `root` | Yes |
| `SPRING_DATASOURCE_PASSWORD` | Database password | - | Yes |
| `APP_JWT_SECRET` | JWT signing key (min 256-bit) | - | Yes |
| `APP_JWT_EXPIRATION` | JWT token expiry in ms | `86400000` (24h) | No |
| `SERVER_PORT` | Application port | `8080` | No |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `default` | No |
| `UPLOAD_DIR` | File upload directory | `./uploads` | No |

### 5.2 Frontend

| Variable | Description | Default | Required |
|----------|-------------|---------|:--------:|
| `VITE_API_BASE_URL` | Backend API base URL | `http://localhost:8080/api` | Yes |

### 5.3 Docker

| Variable | Description | Default |
|----------|-------------|---------|
| `MYSQL_ROOT_PASSWORD` | MySQL root password | `root_password` |
| `MYSQL_DATABASE` | MySQL database name | `hostel_db` |

---

## 6. Troubleshooting

### 6.1 Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| Port 8080 already in use | Another process using the port | `netstat -ano \| findstr :8080` then `taskkill /PID <PID> /F` |
| MySQL connection refused at startup | MySQL not ready | Docker handles this with `depends_on` healthcheck; locally, wait for MySQL to start |
| npm install fails | Network/version issues | Clear cache: `npm cache clean --force` |
| Maven build fails | Dependency issues | `mvn clean install -U` to force update snapshots |
| JWT token expired | Token expiration too short | Increase `APP_JWT_EXPIRATION` (default: 86400000 ms = 1 day) |
| CORS error in browser | Frontend origin not allowed | Add frontend URL to CORS config in backend `WebConfig.java` |
| 404 on page refresh (React) | SPA routing not configured | Ensure nginx has `try_files $uri $uri/ /index.html;` |
| H2 console not accessible | Disabled in profile | Enable `spring.h2.console.enabled=true` in dev profile |
| File upload fails | Size limit exceeded | Increase `spring.servlet.multipart.max-file-size` |
| Docker build fails | Network/Dockerfile issues | Check Docker logs: `docker-compose logs --tail=50` |

### 6.2 Checking Logs

```bash
# Backend logs (Docker)
docker-compose logs -f backend

# Backend logs (systemd)
sudo journalctl -u hostel-backend -f

# Backend logs (direct)
tail -f backend/logs/application.log

# Nginx access logs
sudo tail -f /var/log/nginx/access.log

# Nginx error logs
sudo tail -f /var/log/nginx/error.log

# MySQL logs
sudo tail -f /var/log/mysql/error.log
```

### 6.3 Health Checks

```bash
# Check backend is running
curl http://localhost:8080/api/auth/login

# Check database connectivity
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@hostel.com","password":"admin123"}'

# Check frontend is serving
curl http://localhost:80

# Check Docker container health
docker ps --filter "name=hostel-"
```

### 6.4 Database Connection Strings

| Environment | Connection String |
|-------------|------------------|
| Local (H2) | `jdbc:h2:mem:hostel_db` |
| Local (MySQL) | `jdbc:mysql://localhost:3306/hostel_db?useSSL=false&serverTimezone=UTC` |
| Docker | `jdbc:mysql://mysql:3306/hostel_db?useSSL=false&serverTimezone=UTC` |
| Production | `jdbc:mysql://your-host:3306/hostel_db?useSSL=true&serverTimezone=UTC` |

### 6.5 Quick Verification Script

```bash
echo "=== Checking Backend ==="
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/auth/login
echo ""

echo "=== Checking Frontend ==="
curl -s -o /dev/null -w "%{http_code}" http://localhost:80
echo ""

echo "=== Docker Containers ==="
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```
