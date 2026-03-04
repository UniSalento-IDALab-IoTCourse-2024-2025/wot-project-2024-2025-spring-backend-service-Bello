# ChillChain — Backend Service

**carrier-management-service** is the Spring Boot backend powering the ChillChain refrigerated transport sharing platform. It exposes a REST API for fleet management, shipment booking, IoT telemetry ingestion, and anomaly notification, secured with JWT-based role authentication.

## Tech Stack

- **Java 21** + **Spring Boot 3** (Web, Security, Data MongoDB, Integration)
- **MongoDB** — document store for users, vehicles, trips, shipments, telemetry, and notifications
- **MQTT** (Mosquitto) — real-time ingestion of telemetry data and anomaly alerts from Python microservices
- **Google Maps Platform** — Routes API for path computation, Geocoding API for address resolution
- **Argon2** — password hashing
- **JWT** — stateless authentication with role-based access control

## Architecture Overview

```
┌──────────────┐   MQTT    ┌───────────────────────────┐   REST    ┌──────────────┐
│  Fridge      ├──────────►│  carrier-management-svc   │◄─────────┤  React       │
│  Streamer    │           │  (Spring Boot)            │          │  Frontend    │
│  (port 8002) │           │  port 8081                │          │  port 5173   │
└──────────────┘           │                           │          └──────────────┘
                           │  ┌─────────────────────┐  │
┌──────────────┐   MQTT    │  │  MqttListenerService │  │
│  Anomaly     ├──────────►│  │  - telemetry ingest  │  │
│  Detector    │           │  │  - anomaly → notif.  │  │
│  (port 8003) │           │  └─────────────────────┘  │
└──────────────┘           │                           │
                           │  ┌─────────────────────┐  │
                           │  │     MongoDB          │  │
                           │  │  - user, vehicle     │  │
                           │  │  - trip, shipment    │  │
                           │  │  - telemetry (TTL)   │  │
                           │  │  - notification      │  │
                           │  └─────────────────────┘  │
                           └───────────────────────────┘
```

## User Roles

| Role | Description | Accessible endpoints |
|------|-------------|---------------------|
| **ADMIN** | Fleet and trip management | Vehicles CRUD, trips CRUD, shipments management |
| **TECHNICIAN** | Monitoring and alerts | Telemetry history, notifications, mark-as-read |
| **CLIENT** | Shipment booking | Retrieve compatible trips, select and book a trip |

## API Endpoints

### Public (no authentication)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/carrier/authenticate` | Login, returns JWT + role |
| `POST` | `/api/carrier/register` | Register a new user |
| `POST` | `/api/carrier/retrieveTrips` | Find compatible trips for a shipment |
| `POST` | `/api/carrier/selectTrip` | Confirm booking on a selected trip |

### Admin (role: ADMIN)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/carrier/addVehicle` | Register a new vehicle |
| `POST` | `/api/carrier/deleteVehicle` | Delete vehicle + cascade trips/shipments |
| `GET` | `/api/carrier/trips` | List all trips |
| `POST` | `/api/carrier/deleteTrip` | Delete trip + cascade shipments |
| `POST` | `/api/carrier/shipmentsByTrip` | List shipments for a given trip |
| `POST` | `/api/carrier/deleteShipment` | Delete shipment, restore trip capacity |

### Technician (role: TECHNICIAN)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/carrier/vehicles` | List all vehicles (shared with ADMIN) |
| `GET` | `/api/carrier/notifications` | List anomaly notifications |
| `POST` | `/api/carrier/notifications/read/{id}` | Mark a notification as read |
| `GET` | `/api/carrier/telemetry/{vehicleName}` | Telemetry history for a vehicle |

### Simulation Control (public)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/carrier/trip/startSimulation/{vehicleName}` | Start telemetry streaming for a vehicle |
| `POST` | `/api/carrier/trip/stopSimulation` | Stop active telemetry streaming |
| `GET` | `/api/carrier/simulation/status` | Current simulation status |

## MQTT Topics

The backend subscribes to two MQTT topic patterns via Spring Integration:

| Topic Pattern | Channel | Purpose |
|---------------|---------|---------|
| `fridge/+/telemetry` | `mqttInputChannel` | Receive telemetry data points, persist to MongoDB with TTL |
| `fridge/+/anomalies` | `mqttAnomalyChannel` | Receive anomaly alerts, create notification documents |

The `+` wildcard matches any vehicle name (e.g., `fridge/FiatDucato01/telemetry`).

## MongoDB Collections

| Collection | Description | Notes |
|------------|-------------|-------|
| `user` | Accounts with email, hashed password, role | Argon2 hashing |
| `vehicle` | Fleet registry with dimensions (cm), weight (kg), price/km | Dimensions stored as integers in cm |
| `trip` | Routes with polyline, distance, duration, remaining capacity | Capacity updated on shipment add/delete |
| `shipment` | Booked parcels linked to trips | Dimensions in cm, weight in kg |
| `telemetry` | Sensor readings from refrigeration units | TTL-indexed for automatic expiration |
| `notification` | Anomaly alerts with read/unread status | Created by MqttListenerService |

## Key Services

**MqttListenerService** — Listens on both MQTT channels. Telemetry messages are deserialized and persisted with the vehicle name extracted from the topic. Anomaly messages trigger the creation of a `Notification` document.

**TripRoutingService** — Integrates with Google Maps Routes API to compute optimal paths. Used when creating new dedicated trips and when recalculating routes with intermediate waypoints for shared trips.

**GeoUtils** — Haversine distance calculation between geographic coordinates.

**PolylineUtils** — Decodes Google encoded polylines and computes point-to-polyline distance for spatial matching of shipment pickup/delivery points against existing trip routes.

## Running Locally

The service is designed to run as part of the ChillChain Docker Compose stack:

```bash
docker compose up -d
```

This starts MongoDB, Mosquitto, the Spring Boot backend (port 8081), the React frontend (port 5173), and the Python microservices (ports 8002, 8003).

For standalone development:

```bash
# Requires MongoDB on localhost:27017 and Mosquitto on localhost:1883
./mvnw spring-boot:run
```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `FRIDGE_API_URL` | `http://fridge-streamer:8002` | Fridge Streamer service URL |
| `GOOGLE_MAPS_API_KEY` | — | Google Maps Platform API key (required) |
| `SPRING_DATA_MONGODB_URI` | `mongodb://mongo:27017/chillchain` | MongoDB connection string |

## Response Format

All endpoints return a consistent wrapper:

```json
{
  "message": "Description of the result",
  "statusCode": 200,
  "body": { "..." }
}
```

Error responses follow the same structure with appropriate HTTP status codes (400, 401, 404, 409, 500).