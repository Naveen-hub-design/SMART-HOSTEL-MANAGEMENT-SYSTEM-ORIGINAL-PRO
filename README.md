<div align="center">
  <h1>Smart Hostel Management & Student Community Platform</h1>
  <p><em>A comprehensive web-based platform for managing hostel operations including student management, room allocation, leave tracking, complaints, mess feedback, lost & found, marketplace, and AI-powered features.</em></p>

  <p>
    <img src="https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=java" alt="Java 17">
    <img src="https://img.shields.io/badge/Spring_Boot-3.1-brightgreen?style=flat-square&logo=spring" alt="Spring Boot 3.1">
    <img src="https://img.shields.io/badge/React-18-blue?style=flat-square&logo=react" alt="React 18">
    <img src="https://img.shields.io/badge/MySQL-8.0-blue?style=flat-square&logo=mysql" alt="MySQL 8.0">
    <img src="https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker" alt="Docker">
    <img src="https://img.shields.io/badge/JWT-auth-yellow?style=flat-square" alt="JWT Auth">
    <img src="https://img.shields.io/badge/license-MIT-green?style=flat-square" alt="License MIT">
  </p>
</div>

---

## Features

### Student Module
- **Profile Management** – Register using enrollment number, manage personal details
- **Room Viewing & Allocation** – View allocated room, request changes
- **Leave Application** – Apply for leave/gate pass, track approval status
- **Complaint Registration** – File complaints with image uploads
- **Mess Feedback** – Rate food quality, taste, and cleanliness daily
- **Lost & Found** – Report lost items or claim found items
- **Marketplace** – Buy/sell second-hand items within hostel
- **Notice Board** – View notices and circulars
- **AI Chatbot** – Get instant answers to hostel-related queries

### Warden Module
- **Dashboard** – Block-wise statistics overview
- **Leave Management** – Approve/reject leave requests
- **Complaint Management** – Update complaint status
- **Student List** – View students in allocated block
- **Notices** – Post block-specific notices

### Admin Module
- **Dashboard** – System-wide analytics and reports
- **Block Management** – CRUD operations on hostel blocks
- **Room Management** – Add/edit rooms, allocate/vacate
- **Warden Management** – Assign/remove wardens to blocks
- **Student Management** – View all students, handle special cases
- **Notice Management** – Post global notices
- **Reports** – Generate occupancy, leave, complaint reports
- **AI Room Suggestions** – Smart room allocation recommendations
- **Complaint Analysis** – Pattern detection and preventive insights
- **Occupancy Prediction** – Trend forecasting for capacity planning

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| **Frontend** | React 18, Create React App, Tailwind CSS, React Router 6 |
| **Backend** | Java 17, Spring Boot 3.1, Spring Data JPA, Spring Security |
| **Database** | MySQL 8.0 (production), H2 (development) |
| **Auth** | JWT (JSON Web Token), BCrypt Password Hashing |
| **Build** | Maven (Backend), npm (Frontend) |
| **DevOps** | Docker, Docker Compose |

---

## Screenshots

<!-- TODO: Add actual screenshots -->
| Module | Screenshot |
|--------|-----------|
| Login Page | *Screenshot pending* |
| Student Dashboard | *Screenshot pending* |
| Warden Dashboard | *Screenshot pending* |
| Admin Dashboard | *Screenshot pending* |
| Room Allocation | *Screenshot pending* |
| Leave Management | *Screenshot pending* |
| Complaints | *Screenshot pending* |
| Mess Feedback | *Screenshot pending* |
| Marketplace | *Screenshot pending* |
| AI Chatbot | *Screenshot pending* |

---

## Quick Start Guide

### Prerequisites

- Java 17+
- Node.js 18+
- npm 9+
- MySQL 8.0+ (optional for local dev with H2)
- Maven 3.8+
- Git
- Docker & Docker Compose (for containerized setup)

### Clone

```bash
git clone https://github.com/your-username/smart-hostel-management.git
cd smart-hostel-management
```

### Backend Setup (with H2 - no MySQL needed)

```bash
cd backend
mvn clean install -DskipTests
mvn spring-boot:run
```

Backend runs at `http://localhost:8080`

### Frontend Setup

```bash
cd frontend
npm install
npm start
```

Frontend runs at `http://localhost:3000`

### Access

| Component | URL |
|-----------|-----|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080/api |
| H2 Console | http://localhost:8080/h2-console |

---

## Docker Quick Start

```bash
# Build and start all services
docker-compose up -d --build

# Access the application
# Frontend: http://localhost
# Backend: http://localhost:8080/api

# Stop all services
docker-compose down
```

---

## Default Credentials

| Role | Email | Password |
|------|-------|----------|
| Admin | admin@hostel.com | admin123 |
| Warden | warden@hostel.com | warden123 |
| Student | student@hostel.com | student123 |

---

## Project Structure

```
smart-hostel-management/
+-- backend/           # Spring Boot REST API (Java 17)
+-- frontend/          # React 18 SPA
+-- database/          # MySQL schema and ER diagram
+-- docs/              # Project documentation
+-- docker-compose.yml # Multi-service Docker setup
+-- README.md          # This file
```

---

## API Documentation

Complete API documentation with request/response examples is available:

- [API Documentation](docs/api-documentation.md)
- [Deployment Guide](docs/deployment.md)
- [Project Documentation](docs/project-documentation.md)
- [Folder Structure](docs/folder-structure.md)

---

## Database

The database consists of 12 tables with proper relationships, indexes, and constraints.

- [Database Schema](database/schema.sql)
- [ER Diagram](database/ER-Diagram.txt)

---

## Contributors

- **Project Lead** – [Your Name]

---

## License

This project is licensed under the MIT License.
# SMART-HOSTEL-MANAGEMENT-SYSTEM-ORIGINAL-PRO
