# RideLink

RideLink is a Java-based microservice project designed for a ride-sharing assignment. It contains separate services for account management, driver management, ride handling, and fare/payment processing.

## Project Purpose

This project demonstrates a typical distributed system setup using Spring Boot and H2 database. Each service is independent and communicates with the others through REST APIs.

## Services

1. Account Service
   - User registration and authentication
   - JWT-based security
   - Role-based access

2. Driver Service
   - Driver profiles and vehicle data
   - Availability management
   - Driver lookup logic

3. Ride Service
   - Ride request flow
   - Driver assignment
   - Ride lifecycle management

4. Fare and Payment Service
   - Fare calculation
   - Payment simulation
   - Receipts and finalization

## Tech Stack

- Java 22
- Spring Boot 4.1.1
- Maven
- H2 Database
- Docker
- REST APIs

## Running the Project

### Prerequisites

- Java 22
- Maven
- H2 database support (embedded in each service)

### Run the services

Run each Spring Boot service in order.

## Assignment Context

This project is intended as a basic academic assignment implementation of a ride-sharing microservice system. It focuses on backend service design, inter-service communication, authentication, and database separation.

## Notes

- This version is kept simple and assignment-focused.
- GitHub-specific repository branding and deployment references have been removed for portability and reuse.

```bash
# 1. Account Service — auth for everything else
cd account-service/account-service
./mvnw spring-boot:run          # → http://localhost:8081
 
# 2. Driver & Vehicle Service — no dependencies
cd driver-service/driver-service
./mvnw spring-boot:run          # → http://localhost:8082
 
# 3. Ride Management Service — depends on Driver Service
cd ride-service/ride-service
./mvnw spring-boot:run          # → http://localhost:8083
 
# 4. Fare & Payment Service — depends on Ride Service
cd fare-payment-service/fare-payment-service
./mvnw spring-boot:run          # → http://localhost:8084
```
 
> 📁 Each service's Maven project lives one folder deeper than its top-level directory name (a leftover from the original Spring Initializr scaffold) — adjust the `cd` path if yours has been flattened since.
 
Each service uses an in-memory H2 database. The database is initialized automatically by Spring Boot and is available through the configured H2 datasource.
 
### 📚 Swagger UI
 
Once a service is running, its live API docs are at:
 
```
http://localhost:<port>/swagger-ui/index.html
```
 
---
 
## 📡 API Overview
 
### 🔐 Account Service — `:8081`
 
| Method | Endpoint | Description |
|--------|----------|--------------|
| `POST` | `/api/auth/register` | Register a passenger or driver account |
| `POST` | `/api/auth/login` | Log in, receive a JWT |
| `GET` | `/api/accounts/{id}` | View a profile (password never returned) |
| `PATCH` | `/api/accounts/{id}` | Update a profile |
| `PATCH` | `/api/accounts/{id}/status?status=` | Admin-only: activate/suspend an account |
 
### 🚗 Driver & Vehicle Service — `:8082`
 
| Method | Endpoint | Description |
|--------|----------|--------------|
| `POST` | `/api/drivers` | Register a new driver/vehicle profile |
| `GET` | `/api/drivers/available` | List currently available drivers |
| `PATCH` | `/api/drivers/{id}/availability` | Toggle availability (also used internally to reserve a driver on assignment) |
 
### 🧭 Ride Management Service — `:8083`
 
| Method | Endpoint | Description |
|--------|----------|--------------|
| `POST` | `/api/rides` | Request a ride (auto-assigns an available driver) |
| `GET` | `/api/rides/{id}` | View a ride's details |
| `PATCH` | `/api/rides/{id}/complete` | Complete a ride — triggers final fare calculation |
 
### 💳 Fare & Payment Service — `:8084`
 
| Method | Endpoint | Description |
|--------|----------|--------------|
| `POST` | `/api/fares/estimate` | Estimate a fare for a pickup/destination |
| `POST` | `/api/payments/finalize` | Interservice: calculate final fare, record a simulated payment |
| `GET` | `/api/payments/{rideId}/status` | Check a payment's status |
| `GET` | `/api/payments/{rideId}/receipt` | Retrieve a completed payment's receipt |
 
---
 
## 🧪 Testing the Full Flow
 
The whole point is proving the services actually *talk* to each other. Here's the golden path:
 
```
1. Register a passenger and a driver     → Account Service
2. Driver goes online                     → Driver & Vehicle Service
3. Passenger requests a ride              → Ride Service calls Driver Service, assigns a driver
4. Driver completes the ride              → Ride Service calls Fare & Payment Service
5. Final fare is calculated & recorded    → Fare & Payment Service
6. Receipt is retrieved                    → Fare & Payment Service
```
 
...and the negative scenarios, all returning the same clean error shape:
 
```json
{ "code": "SOME_STABLE_ERROR_CODE", "message": "A human-readable explanation, no stack traces." }
```
 
| Scenario | Response |
|---|---|
| No available driver | `409` — `NO_DRIVER_AVAILABLE` |
| Driver Service unreachable | `503` — `DRIVER_SERVICE_UNAVAILABLE` |
| Invalid ride status transition | `409` — `INVALID_STATUS_TRANSITION` |
| Ride already paid | `409` — `DUPLICATE_PAYMENT` |
| Fare & Payment Service unreachable during completion | `503` — `PAYMENT_FAILED` (ride stays unpaid, never silently marked complete) |
 
---
 
## 🐳 Project Layout
 
```
Ridelink/
├── account-service/          # Account Service (auth, JWT, roles)
├── driver-service/           # Driver & Vehicle Service
├── ride-service/             # Ride Management Service
├── fare-payment-service/     # Fare & Payment Service
├── docker-compose.yml        # Starts MongoDB and the services
└── README.md                 # Assignment overview
```
 
---
 
## 📌 Notes
 
- This project focuses on the required backend microservice logic for the assignment.
- No real payments, maps, or third-party services are involved anywhere. Everything is simulated, on purpose.
- Each service maintains its **own** database — no table, collection, or connection is shared across services, by design.
 
