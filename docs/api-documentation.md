# API Documentation - Smart Hostel Management System

---

## Base URL

```
http://localhost:8080/api
```

## Authentication Header

All protected endpoints require:

```
Authorization: Bearer <jwt_token>
```

## Standard Response Format

### Success Response
```json
{
  "status": 200,
  "message": "Success",
  "data": { ... }
}
```

### Error Response
```json
{
  "status": 400,
  "message": "Error description",
  "errors": ["field-specific errors"]
}
```

---

## Table of Contents

1. [Authentication Controller](#1-authentication-controller)
2. [Student Controller](#2-student-controller)
3. [Room Controller](#3-room-controller)
4. [Leave Controller](#4-leave-controller)
5. [Complaint Controller](#5-complaint-controller)
6. [Notice Controller](#6-notice-controller)
7. [Warden Controller](#7-warden-controller)
8. [Admin Controller](#8-admin-controller)
9. [Mess Feedback Controller](#9-mess-feedback-controller)
10. [Lost & Found Controller](#10-lost--found-controller)
11. [Marketplace Controller](#11-marketplace-controller)
12. [AI Controller](#12-ai-controller)

---

## 1. Authentication Controller

| Method | URL | Auth | Description |
|--------|-----|:----:|-------------|
| POST | `/auth/login` | No | Login and receive JWT token |
| POST | `/auth/register` | No | Register a new student account |

### POST /auth/login

Authenticate user credentials and return a JWT token.

**Request Body:**
```json
{
  "email": "student@hostel.com",
  "password": "student123"
}
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "id": 3,
  "name": "Rahul Kumar",
  "email": "student@hostel.com",
  "role": "STUDENT"
}
```

**Response (401 Unauthorized):**
```json
{
  "status": 401,
  "message": "Invalid email or password"
}
```

### POST /auth/register

Register a new student.

**Request Body:**
```json
{
  "name": "New Student",
  "email": "new.student@hostel.com",
  "password": "password123",
  "phone": "9876543210",
  "enrollmentNo": "ENR2024002",
  "parentContact": "9876543211",
  "address": "456, New Street, City",
  "dateOfBirth": "2001-05-15",
  "gender": "MALE"
}
```

**Response (201 Created):**
```json
{
  "message": "Registration successful. Please login.",
  "userId": 4
}
```

**Response (409 Conflict):**
```json
{
  "status": 409,
  "message": "Email already registered"
}
```

---

## 2. Student Controller

| Method | URL | Auth | Description |
|--------|-----|:----:|-------------|
| GET | `/students/profile` | STUDENT | Get logged-in student's profile |
| PUT | `/students/profile` | STUDENT | Update student profile |
| GET | `/students/room` | STUDENT | Get allocated room details |
| PUT | `/students/change-password` | STUDENT | Change account password |

### GET /students/profile

**Response (200 OK):**
```json
{
  "id": 1,
  "userId": 3,
  "name": "Rahul Kumar",
  "email": "student@hostel.com",
  "phone": "9876543212",
  "enrollmentNo": "ENR2024001",
  "parentContact": "9876543213",
  "address": "123, Main Street, Delhi",
  "dateOfBirth": "2000-01-15",
  "gender": "MALE",
  "roomId": 1,
  "roomNo": "A-101",
  "blockName": "A Wing - Boys Hostel"
}
```

### PUT /students/profile

**Request Body:**
```json
{
  "phone": "9876543214",
  "parentContact": "9876543215",
  "address": "Updated Address, Delhi"
}
```

**Response (200 OK):**
```json
{
  "message": "Profile updated successfully",
  "phone": "9876543214"
}
```

### GET /students/room

**Response (200 OK):**
```json
{
  "id": 1,
  "roomNo": "A-101",
  "blockName": "A Wing - Boys Hostel",
  "blockCode": "A-BLOCK",
  "floor": 1,
  "capacity": 2,
  "occupants": 1,
  "status": "OCCUPIED",
  "rent": 5000
}
```

### PUT /students/change-password

**Request Body:**
```json
{
  "currentPassword": "oldPassword123",
  "newPassword": "newPassword456"
}
```

**Response (200 OK):**
```json
{
  "message": "Password changed successfully"
}
```

---

## 3. Room Controller

| Method | URL | Auth | Description |
|--------|-----|:----:|-------------|
| GET | `/rooms` | Yes | List all rooms with optional filters |
| GET | `/rooms/{id}` | Yes | Get room details by ID |
| POST | `/rooms` | ADMIN | Create a new room |
| PUT | `/rooms/{id}` | ADMIN | Update room details |
| DELETE | `/rooms/{id}` | ADMIN | Delete a room |
| POST | `/rooms/allocate` | ADMIN | Allocate room to student |
| POST | `/rooms/vacate` | ADMIN | Vacate a student's room |

**Query Parameters for GET /rooms:**

| Param | Type | Description |
|-------|------|-------------|
| `blockId` | Long | Filter by block |
| `status` | String | AVAILABLE, OCCUPIED, MAINTENANCE |
| `floor` | Integer | Filter by floor number |

### POST /rooms

**Request Body:**
```json
{
  "roomNo": "A-301",
  "blockId": 1,
  "floor": 3,
  "capacity": 2,
  "rent": 5500
}
```

**Response (201 Created):**
```json
{
  "id": 16,
  "roomNo": "A-301",
  "blockId": 1,
  "message": "Room created successfully"
}
```

### POST /rooms/allocate

**Request Body:**
```json
{
  "studentId": 1,
  "roomId": 2
}
```

**Response (200 OK):**
```json
{
  "message": "Room allocated successfully",
  "studentName": "Rahul Kumar",
  "roomNo": "A-102"
}
```

### POST /rooms/vacate

**Request Body:**
```json
{
  "studentId": 1
}
```

**Response (200 OK):**
```json
{
  "message": "Room vacated successfully"
}
```

---

## 4. Leave Controller

| Method | URL | Auth | Description |
|--------|-----|:----:|-------------|
| POST | `/leaves/apply` | STUDENT | Apply for leave or gate pass |
| GET | `/leaves/my-leaves` | STUDENT | Get current student's leave requests |
| GET | `/leaves/pending` | WARDEN | Get pending leaves for warden's block |
| PUT | `/leaves/{id}/approve` | WARDEN | Approve a leave request |
| PUT | `/leaves/{id}/reject` | WARDEN | Reject a leave request |

### POST /leaves/apply

**Request Body:**
```json
{
  "fromDate": "2025-12-20",
  "toDate": "2025-12-25",
  "reason": "Going home for winter break"
}
```

**Response (201 Created):**
```json
{
  "id": 2,
  "message": "Leave request submitted successfully"
}
```

### PUT /leaves/{id}/approve

**Request Body:**
```json
{
  "remarks": "Approved. Enjoy your holidays."
}
```

**Response (200 OK):**
```json
{
  "message": "Leave approved successfully",
  "status": "APPROVED"
}
```

### PUT /leaves/{id}/reject

**Request Body:**
```json
{
  "remarks": "Insufficient attendance. Please meet warden."
}
```

**Response (200 OK):**
```json
{
  "message": "Leave rejected",
  "status": "REJECTED"
}
```

---

## 5. Complaint Controller

| Method | URL | Auth | Description |
|--------|-----|:----:|-------------|
| POST | `/complaints` | STUDENT | File a new complaint |
| GET | `/complaints/my-complaints` | STUDENT | Get current student's complaints |
| GET | `/complaints/pending` | WARDEN | Get pending complaints for warden's block |
| PUT | `/complaints/{id}/status` | WARDEN/ADMIN | Update complaint status |

### POST /complaints

**Request Body:**
```json
{
  "title": "Broken Fan",
  "description": "The ceiling fan in room A-101 is not working properly.",
  "imageUrl": null
}
```

**Response (201 Created):**
```json
{
  "id": 2,
  "message": "Complaint registered successfully"
}
```

### PUT /complaints/{id}/status

**Request Body:**
```json
{
  "status": "IN_PROGRESS"
}
```

**Allowed Status Values:** `PENDING`, `IN_PROGRESS`, `RESOLVED`, `REJECTED`

**Response (200 OK):**
```json
{
  "message": "Complaint status updated to IN_PROGRESS"
}
```

---

## 6. Notice Controller

| Method | URL | Auth | Description |
|--------|-----|:----:|-------------|
| GET | `/notices` | No | List all active notices |
| POST | `/notices` | ADMIN/WARDEN | Create a new notice |
| PUT | `/notices/{id}` | ADMIN | Update an existing notice |
| DELETE | `/notices/{id}` | ADMIN | Delete a notice |

**Query Parameters for GET /notices:**

| Param | Type | Description |
|-------|------|-------------|
| `role` | String | Filter by target role (ALL, STUDENT, WARDEN) |

### POST /notices

**Request Body:**
```json
{
  "title": "Holiday Notice",
  "content": "Hostel will remain open during winter break.",
  "targetRole": "ALL",
  "expiresAt": "2026-01-15T23:59:59"
}
```

**Response (201 Created):**
```json
{
  "id": 2,
  "message": "Notice posted successfully"
}
```

---

## 7. Warden Controller

| Method | URL | Auth | Description |
|--------|-----|:----:|-------------|
| GET | `/wardens` | ADMIN | List all wardens |
| GET | `/wardens/dashboard` | WARDEN | Get warden's dashboard statistics |
| PUT | `/wardens/{id}/assign-block` | ADMIN | Assign a block to a warden |

### GET /wardens/dashboard

**Response (200 OK):**
```json
{
  "blockName": "A Wing - Boys Hostel",
  "totalRooms": 6,
  "occupiedRooms": 1,
  "availableRooms": 5,
  "totalStudents": 1,
  "pendingLeaves": 1,
  "pendingComplaints": 1,
  "complaintsByStatus": {
    "PENDING": 1,
    "IN_PROGRESS": 0,
    "RESOLVED": 0,
    "REJECTED": 0
  }
}
```

### PUT /wardens/{id}/assign-block

**Request Body:**
```json
{
  "blockId": 2
}
```

**Response (200 OK):**
```json
{
  "message": "Block assigned successfully"
}
```

---

## 8. Admin Controller

| Method | URL | Auth | Description |
|--------|-----|:----:|-------------|
| GET | `/admin/dashboard` | ADMIN | System-wide dashboard statistics |
| POST | `/admin/wardens` | ADMIN | Create a new warden |
| GET | `/admin/wardens` | ADMIN | List all wardens |
| DELETE | `/admin/wardens/{id}` | ADMIN | Delete a warden |
| POST | `/admin/blocks` | ADMIN | Create a new hostel block |
| GET | `/admin/blocks` | ADMIN | List all hostel blocks |
| PUT | `/admin/blocks/{id}` | ADMIN | Update a hostel block |
| DELETE | `/admin/blocks/{id}` | ADMIN | Delete a hostel block |
| GET | `/admin/reports` | ADMIN | Generate hostel reports |

### GET /admin/dashboard

**Response (200 OK):**
```json
{
  "totalStudents": 1,
  "totalWardens": 1,
  "totalBlocks": 3,
  "totalRooms": 15,
  "occupiedRooms": 1,
  "availableRooms": 14,
  "pendingLeaves": 1,
  "pendingComplaints": 1,
  "occupancyRate": "6.67%"
}
```

### POST /admin/wardens

**Request Body:**
```json
{
  "name": "Mrs. Patel",
  "email": "warden2@hostel.com",
  "password": "warden123",
  "phone": "9876543220",
  "blockId": 2,
  "qualification": "M.A. Student Affairs"
}
```

**Response (201 Created):**
```json
{
  "id": 2,
  "message": "Warden created successfully"
}
```

### POST /admin/blocks

**Request Body:**
```json
{
  "name": "D Wing - Girls Hostel",
  "code": "D-BLOCK",
  "address": "Main Campus, East Side"
}
```

**Response (201 Created):**
```json
{
  "id": 4,
  "message": "Block created successfully"
}
```

### GET /admin/reports

**Query Parameters:**

| Param | Type | Description |
|-------|------|-------------|
| `type` | String | OCCUPANCY, LEAVE, COMPLAINT |
| `blockId` | Long | Filter by block (optional) |
| `fromDate` | String | Start date (YYYY-MM-DD) |
| `toDate` | String | End date (YYYY-MM-DD) |

**Response (200 OK) - Occupancy Report:**
```json
{
  "reportType": "OCCUPANCY",
  "generatedAt": "2025-12-01T12:00:00",
  "data": [
    {
      "blockName": "A Wing - Boys Hostel",
      "totalRooms": 6,
      "occupiedRooms": 1,
      "availableRooms": 5,
      "occupancyRate": "16.67%"
    }
  ]
}
```

---

## 9. Mess Feedback Controller

| Method | URL | Auth | Description |
|--------|-----|:----:|-------------|
| POST | `/mess-feedback` | STUDENT | Submit daily mess feedback |
| GET | `/mess-feedback/my-feedback` | STUDENT | Get my feedback history |
| GET | `/mess-feedback/averages` | Yes | Get average ratings |

### POST /mess-feedback

**Request Body:**
```json
{
  "date": "2025-12-01",
  "foodQualityRating": 4,
  "tasteRating": 3,
  "cleanlinessRating": 5,
  "comments": "Good food overall."
}
```

All ratings are on a scale of 1 to 5.

**Response (201 Created):**
```json
{
  "id": 2,
  "message": "Feedback submitted successfully"
}
```

### GET /mess-feedback/averages

**Query Parameters:**

| Param | Type | Description |
|-------|------|-------------|
| `fromDate` | String | Start date (YYYY-MM-DD) |
| `toDate` | String | End date (YYYY-MM-DD) |

**Response (200 OK):**
```json
{
  "averageFoodQuality": 4.0,
  "averageTaste": 3.0,
  "averageCleanliness": 5.0,
  "overallAverage": 4.0,
  "totalResponses": 1,
  "period": {
    "from": "2025-12-01",
    "to": "2025-12-01"
  }
}
```

---

## 10. Lost & Found Controller

| Method | URL | Auth | Description |
|--------|-----|:----:|-------------|
| POST | `/lost-and-found` | STUDENT | Report a lost or found item |
| GET | `/lost-and-found` | No | List all items |
| PUT | `/lost-and-found/{id}/status` | STUDENT/WARDEN/ADMIN | Update item status |

**Query Parameters for GET /lost-and-found:**

| Param | Type | Description |
|-------|------|-------------|
| `status` | String | LOST, FOUND, RESOLVED |
| `category` | String | Filter by category |

### POST /lost-and-found

**Request Body:**
```json
{
  "title": "Blue Water Bottle",
  "description": "Milton blue water bottle lost in the mess hall.",
  "category": "BOTTLE",
  "location": "Main Mess Hall",
  "status": "LOST",
  "contactInfo": "Contact: rahul@hostel.com"
}
```

**Response (201 Created):**
```json
{
  "id": 2,
  "message": "Item reported successfully"
}
```

### PUT /lost-and-found/{id}/status

**Allowed Status Values:** `LOST`, `FOUND`, `RESOLVED`

**Request Body:**
```json
{
  "status": "RESOLVED"
}
```

**Response (200 OK):**
```json
{
  "message": "Item status updated to RESOLVED"
}
```

---

## 11. Marketplace Controller

| Method | URL | Auth | Description |
|--------|-----|:----:|-------------|
| POST | `/marketplace` | STUDENT | Add item for sale |
| GET | `/marketplace` | No | List all items |
| GET | `/marketplace/my-items` | STUDENT | List my items |
| PUT | `/marketplace/{id}/sold` | STUDENT | Mark item as sold |
| DELETE | `/marketplace/{id}` | STUDENT | Delete my listing |

**Query Parameters for GET /marketplace:**

| Param | Type | Description |
|-------|------|-------------|
| `category` | String | Filter by category |
| `status` | String | AVAILABLE, SOLD |

### POST /marketplace

**Request Body:**
```json
{
  "title": "Used Textbooks - Computer Science",
  "description": "Set of 5 CS textbooks in good condition.",
  "price": 1500,
  "category": "BOOKS",
  "imageUrl": null
}
```

**Response (201 Created):**
```json
{
  "id": 2,
  "message": "Item listed for sale successfully"
}
```

---

## 12. AI Controller

| Method | URL | Auth | Description |
|--------|-----|:----:|-------------|
| POST | `/ai/chat` | Yes | Send message to AI chatbot |
| POST | `/ai/suggest-room` | ADMIN | Get AI-powered room suggestions |
| POST | `/ai/analyze-complaints` | ADMIN | Analyze complaint patterns |
| GET | `/ai/occupancy-prediction` | ADMIN | Predict occupancy trends |

### POST /ai/chat

Send a message to the AI-powered hostel assistant chatbot.

**Request Body:**
```json
{
  "message": "What is the procedure to apply for leave?",
  "context": "student"
}
```

**Response (200 OK):**
```json
{
  "reply": "To apply for leave, go to the Leave section in your dashboard, click 'Apply Leave', fill in the date range and reason, then submit. Your warden will review and approve/reject it.",
  "timestamp": "2025-12-01T12:00:00"
}
```

### POST /ai/suggest-room

Get AI-powered room allocation suggestions based on student preferences and compatibility.

**Request Body:**
```json
{
  "studentId": 1,
  "preferences": {
    "floor": 1,
    "blockId": 1,
    "quietStudy": true
  }
}
```

**Response (200 OK):**
```json
{
  "suggestions": [
    {
      "roomId": 2,
      "roomNo": "A-102",
      "blockName": "A Wing - Boys Hostel",
      "floor": 1,
      "compatibilityScore": 92.5,
      "reason": "Same floor preference, available, quiet block location"
    }
  ]
}
```

### POST /ai/analyze-complaints

Analyze complaint data to identify patterns and suggest preventive actions.

**Request Body:**
```json
{
  "blockId": 1,
  "fromDate": "2025-01-01",
  "toDate": "2025-12-31"
}
```

**Response (200 OK):**
```json
{
  "analysis": {
    "totalComplaints": 45,
    "mostCommonCategory": "Electrical",
    "trendingIssues": ["Fan repair requests increased by 30%"],
    "suggestions": ["Schedule preventive maintenance for ceiling fans"]
  }
}
```

### GET /ai/occupancy-prediction

Predict future occupancy trends based on historical data.

**Response (200 OK):**
```json
{
  "predictions": [
    {
      "month": "2026-01",
      "predictedOccupancy": 72.5,
      "trend": "increasing"
    },
    {
      "month": "2026-02",
      "predictedOccupancy": 78.1,
      "trend": "increasing"
    }
  ],
  "confidence": 85.3
}
```

---

## Error Codes

| HTTP Status | Description |
|-------------|-------------|
| 200 | Request succeeded |
| 201 | Resource created successfully |
| 400 | Bad request or validation error |
| 401 | Missing or invalid JWT token |
| 403 | Valid JWT but insufficient role permissions |
| 404 | Resource not found |
| 409 | Duplicate resource (email already exists) |
| 413 | File upload too large |
| 500 | Internal server error |

## HTTP Status Code Summary

| Code | Usage |
|------|-------|
| 200 | Successful GET, PUT, DELETE operations |
| 201 | Successful POST (resource created) |
| 400 | Validation errors, bad request body |
| 401 | Missing/invalid/expired JWT token |
| 403 | Valid JWT but insufficient role permissions |
| 404 | Resource ID not found |
| 409 | Unique constraint violation |
| 500 | Server-side exception |
