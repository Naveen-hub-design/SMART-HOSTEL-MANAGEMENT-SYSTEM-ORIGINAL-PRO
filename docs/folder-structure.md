# Folder Structure - Smart Hostel Management & Student Community Platform

```
smart-hostel-management/
|
+-- README.md                              # Project overview and quick-start guide
+-- .gitignore                             # Git ignore rules
+-- docker-compose.yml                     # Docker Compose configuration for all services
|
+-- backend/                               # Spring Boot Backend Application
|   +-- pom.xml                            # Maven project configuration
|   +-- Dockerfile                         # Multi-stage Docker build
|   +-- .dockerignore                      # Docker ignore rules
|   |
|   +-- src/
|       +-- main/
|       |   +-- java/com/hostel/
|       |   |   +-- SmartHostelApplication.java         # Spring Boot entry point
|       |   |   |
|       |   |   +-- config/                # Configuration classes
|       |   |   |   +-- SecurityConfig.java             # Spring Security configuration
|       |   |   |   +-- JwtAuthenticationFilter.java    # JWT authentication filter
|       |   |   |   +-- JwtUtil.java                    # JWT token utility
|       |   |   |   +-- WebConfig.java                  # CORS configuration
|       |   |   |   +-- AppConfig.java                  # General bean configuration
|       |   |   |
|       |   |   +-- entity/                # JPA entity classes
|       |   |   |   +-- User.java                       # Base user entity
|       |   |   |   +-- Student.java                    # Student profile entity
|       |   |   |   +-- Admin.java                      # Admin profile entity
|       |   |   |   +-- Warden.java                     # Warden profile entity
|       |   |   |   +-- HostelBlock.java                # Hostel block entity
|       |   |   |   +-- Room.java                       # Room entity
|       |   |   |   +-- LeaveRequest.java               # Leave request entity
|       |   |   |   +-- Complaint.java                  # Complaint entity
|       |   |   |   +-- Notice.java                     # Notice entity
|       |   |   |   +-- MessFeedback.java               # Mess feedback entity
|       |   |   |   +-- LostAndFound.java               # Lost and found entity
|       |   |   |   +-- MarketplaceItem.java            # Marketplace item entity
|       |   |   |
|       |   |   +-- dto/                   # Data Transfer Objects
|       |   |   |   +-- LoginRequest.java               # Login credentials
|       |   |   |   +-- LoginResponse.java              # Login response with JWT
|       |   |   |   +-- RegisterRequest.java            # Registration request
|       |   |   |   +-- StudentProfileDTO.java          # Student profile response
|       |   |   |   +-- RoomDTO.java                    # Room response
|       |   |   |   +-- LeaveRequestDTO.java            # Leave request DTO
|       |   |   |   +-- ComplaintDTO.java               # Complaint DTO
|       |   |   |   +-- NoticeDTO.java                  # Notice DTO
|       |   |   |   +-- MessFeedbackDTO.java            # Mess feedback DTO
|       |   |   |   +-- LostAndFoundDTO.java            # Lost and found DTO
|       |   |   |   +-- MarketplaceItemDTO.java         # Marketplace item DTO
|       |   |   |   +-- DashboardDTO.java               # Dashboard statistics DTO
|       |   |   |   +-- ReportDTO.java                  # Report data DTO
|       |   |   |
|       |   |   +-- repository/            # Spring Data JPA repositories
|       |   |   |   +-- UserRepository.java
|       |   |   |   +-- StudentRepository.java
|       |   |   |   +-- AdminRepository.java
|       |   |   |   +-- WardenRepository.java
|       |   |   |   +-- HostelBlockRepository.java
|       |   |   |   +-- RoomRepository.java
|       |   |   |   +-- LeaveRequestRepository.java
|       |   |   |   +-- ComplaintRepository.java
|       |   |   |   +-- NoticeRepository.java
|       |   |   |   +-- MessFeedbackRepository.java
|       |   |   |   +-- LostAndFoundRepository.java
|       |   |   |   +-- MarketplaceItemRepository.java
|       |   |   |
|       |   |   +-- service/               # Business logic services
|       |   |   |   +-- AuthService.java
|       |   |   |   +-- StudentService.java
|       |   |   |   +-- AdminService.java
|       |   |   |   +-- WardenService.java
|       |   |   |   +-- RoomService.java
|       |   |   |   +-- LeaveService.java
|       |   |   |   +-- ComplaintService.java
|       |   |   |   +-- NoticeService.java
|       |   |   |   +-- MessFeedbackService.java
|       |   |   |   +-- LostAndFoundService.java
|       |   |   |   +-- MarketplaceService.java
|       |   |   |
|       |   |   +-- controller/            # REST API controllers
|       |   |   |   +-- AuthController.java
|       |   |   |   +-- StudentController.java
|       |   |   |   +-- RoomController.java
|       |   |   |   +-- LeaveController.java
|       |   |   |   +-- ComplaintController.java
|       |   |   |   +-- NoticeController.java
|       |   |   |   +-- WardenController.java
|       |   |   |   +-- AdminController.java
|       |   |   |   +-- MessFeedbackController.java
|       |   |   |   +-- LostAndFoundController.java
|       |   |   |   +-- MarketplaceController.java
|       |   |   |   +-- AIController.java
|       |   |   |
|       |   |   +-- exception/             # Exception handling
|       |   |   |   +-- GlobalExceptionHandler.java
|       |   |   |   +-- ResourceNotFoundException.java
|       |   |   |   +-- BadRequestException.java
|       |   |   |   +-- UnauthorizedException.java
|       |   |   |   +-- DuplicateResourceException.java
|       |   |   |
|       |   |   +-- util/                  # Utility classes
|       |   |       +-- ApiResponse.java
|       |   |       +-- Constants.java
|       |   |
|       |   +-- resources/
|       |       +-- application.properties
|       |       +-- application-prod.properties
|       |       +-- application-docker.properties
|       |
|       +-- test/
|           +-- java/com/hostel/
|               +-- controller/
|               |   +-- AuthControllerTest.java
|               |   +-- StudentControllerTest.java
|               |   +-- RoomControllerTest.java
|               +-- service/
|               |   +-- AuthServiceTest.java
|               |   +-- StudentServiceTest.java
|               |   +-- RoomServiceTest.java
|               |   +-- LeaveServiceTest.java
|               |   +-- ComplaintServiceTest.java
|               |   +-- MessFeedbackTest.java
|               |   +-- LostAndFoundTest.java
|               |   +-- MarketplaceTest.java
|               |   +-- AdminServiceTest.java
|               +-- repository/
|                   +-- UserRepositoryTest.java
|                   +-- RoomRepositoryTest.java
|
+-- frontend/                              # React Frontend Application
|   +-- package.json                       # npm dependencies and scripts
|   +-- package-lock.json                  # Dependency lock file
|   +-- .env                               # Environment variables
|   +-- Dockerfile                         # Multi-stage Docker build
|   +-- nginx.conf                         # Nginx configuration for SPA
|   +-- .dockerignore                      # Docker ignore rules
|   +-- tailwind.config.js                 # Tailwind CSS configuration
|   +-- postcss.config.js                  # PostCSS configuration
|   |
|   +-- public/
|   |   +-- index.html                     # HTML entry point
|   |   +-- favicon.ico
|   |   +-- manifest.json
|   |
|   +-- src/
|       +-- index.js                       # React entry point
|       +-- App.js                         # Root component with router
|       +-- index.css                      # Global CSS with Tailwind
|       |
|       +-- assets/
|       |   +-- images/
|       |   +-- icons/
|       |
|       +-- context/
|       |   +-- AuthContext.js             # Authentication state
|       |
|       +-- hooks/
|       |   +-- useAuth.js
|       |   +-- useApi.js
|       |
|       +-- services/
|       |   +-- api.js                     # Axios instance with JWT interceptor
|       |   +-- authService.js
|       |   +-- studentService.js
|       |   +-- roomService.js
|       |   +-- leaveService.js
|       |   +-- complaintService.js
|       |   +-- noticeService.js
|       |   +-- wardenService.js
|       |   +-- adminService.js
|       |   +-- messFeedbackService.js
|       |   +-- lostAndFoundService.js
|       |   +-- marketplaceService.js
|       |
|       +-- utils/
|       |   +-- validators.js
|       |   +-- constants.js
|       |
|       +-- layouts/
|       |   +-- AuthLayout.js
|       |   +-- DashboardLayout.js
|       |   +-- AdminLayout.js
|       |   +-- components/
|       |       +-- Sidebar.js
|       |       +-- Navbar.js
|       |       +-- Footer.js
|       |
|       +-- components/
|       |   +-- common/
|       |   |   +-- Button.js
|       |   |   +-- Input.js
|       |   |   +-- Select.js
|       |   |   +-- Modal.js
|       |   |   +-- Card.js
|       |   |   +-- Badge.js
|       |   |   +-- Table.js
|       |   |   +-- LoadingSpinner.js
|       |   |   +-- FileUpload.js
|       |   |   +-- ConfirmDialog.js
|       |   |
|       |   +-- forms/
|       |       +-- LoginForm.js
|       |       +-- RegisterForm.js
|       |       +-- LeaveForm.js
|       |       +-- ComplaintForm.js
|       |       +-- FeedbackForm.js
|       |       +-- MarketplaceForm.js
|       |       +-- RoomForm.js
|       |       +-- BlockForm.js
|       |
|       +-- pages/
|           +-- auth/
|           |   +-- LoginPage.js
|           |   +-- RegisterPage.js
|           |
|           +-- student/
|           |   +-- StudentDashboard.js
|           |   +-- StudentProfile.js
|           |   +-- MyRoom.js
|           |   +-- LeaveApplication.js
|           |   +-- MyLeaves.js
|           |   +-- FileComplaint.js
|           |   +-- MyComplaints.js
|           |   +-- MessFeedbackPage.js
|           |   +-- LostAndFoundPage.js
|           |   +-- MarketplacePage.js
|           |   +-- NoticeBoard.js
|           |
|           +-- warden/
|           |   +-- WardenDashboard.js
|           |   +-- ManageLeaves.js
|           |   +-- ManageComplaints.js
|           |   +-- StudentList.js
|           |   +-- PostNotice.js
|           |
|           +-- admin/
|           |   +-- AdminDashboard.js
|           |   +-- ManageBlocks.js
|           |   +-- ManageRooms.js
|           |   +-- ManageWardens.js
|           |   +-- ManageStudents.js
|           |   +-- ManageNotices.js
|           |   +-- ReportsPage.js
|           |
|           +-- common/
|               +-- NotFoundPage.js
|               +-- UnauthorizedPage.js
|
+-- database/                              # Database files
|   +-- schema.sql                         # Complete MySQL schema with sample data
|   +-- ER-Diagram.txt                     # Text-based ER diagram
|
+-- docs/                                  # Project documentation
    +-- api-documentation.md               # REST API documentation
    +-- deployment.md                      # Deployment guide
    +-- project-documentation.md           # Complete project report
    +-- folder-structure.md                # This file
```

## Directory Descriptions

### backend/
Spring Boot Java application with layered architecture. Contains configuration, entities, DTOs, repositories, services, controllers, exception handlers, and utilities. The `uploads/` directory stores uploaded files.

### frontend/
React SPA with Create React App. Contains components organized by type (common, forms), page components organized by role (auth, student, warden, admin), services for API communication, context for state management, and utility functions.

### database/
Contains the MySQL schema definition with all 12 tables, indexes, constraints, and sample seed data. Also includes a text-based ER diagram showing entity relationships.

### docs/
Comprehensive project documentation including API reference, deployment guide, detailed project report, and folder structure reference.
