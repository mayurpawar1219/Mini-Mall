# Mini D-Mart — Grocery Store Application

## Project Overview
Mini D-Mart is a full-stack grocery store application designed to seamlessly connect customers with store operations. Customers can browse products, manage their shopping carts, place orders for store pickup, and process secure payments. Internally, the platform provides distinct capabilities for staff to manage inventory, prepare orders, and process returns/exchanges, while providing store administrators with an eagle-eye view of all transactions, audit logs, and KPI dashboards.

## Key Features
* **Customer Authentication:** Secure registration and login using stateless JWTs.
* **Role-Based Access Control (RBAC):** Strict separation of privileges across Customer, Staff, and Admin roles.
* **Product Catalog:** Real-time product browsing categorized by departments.
* **Inventory Management:** Accurate tracking of available versus reserved stock with concurrent safety.
* **Shopping Cart & Checkout:** Persistent cart logic scaling to seamless checkout.
* **Stripe Payments:** Real-time, secure payment processing and automated intent verification.
* **Order Lifecycle:** End-to-end tracking (Placed, Confirmed, Preparing, Ready for Pickup, Completed, Cancelled).
* **Scheduled Pickup Slots:** Capacity-limited pickup slot reservations for store pickup.
* **Returns & Exchanges:** Customer-initiated return/exchange workflows processed and validated by staff.
* **Dashboards:** Real-time financial and operational metrics available to staff and administrators.
* **Audit Logging:** System-wide tracking of critical administrative/staff actions.

## Technology Stack
**Frontend:**
- React 18
- Vite
- Tailwind CSS
- React Router

**Backend:**
- Java 17
- Spring Boot 3.4.1
- Spring Security & JWT Token Auth (io.jsonwebtoken 0.12.5)
- Spring Data JPA / Hibernate
- MySQL 8.x
- Flyway (Database Migrations)
- Stripe Java SDK (v24.22.0)
- Maven
- Docker

## System Architecture
```text
  Customer / Staff / Admin
            ↓
  React Frontend (Vercel)
            ↓
  Spring Boot REST API (Render)
            ↓
  Spring Security (JWT Validation / RBAC)
            ↓
  Aiven MySQL Database (via JPA/Hibernate)
```
**External Integrations:**
- **Stripe:** Used during checkout to securely process payments without touching the application backend directly.

## Project Structure
```text
Mini_Dmart/
├── backend/                  # Spring Boot backend source code
│   ├── src/main/java/com/minidmart/
│   │   ├── config/           # Security, CORS, Stripe configurations
│   │   ├── controller/       # REST API endpoints
│   │   ├── dto/              # Data Transfer Objects
│   │   ├── entity/           # JPA entities (Order, Product, etc.)
│   │   ├── repository/       # Spring Data Repositories
│   │   ├── security/         # JWT Filters & Providers
│   │   └── service/          # Core business logic
│   └── src/main/resources/
│       ├── db/migration/     # Flyway SQL migrations
│       └── application.yml   # Application properties
├── frontend/                 # React frontend source code
│   └── src/
│       ├── api/              # Axios API client setup
│       ├── components/       # Reusable UI components
│       ├── context/          # React Context (Auth)
│       └── pages/            # View components grouped by role
├── .env.example              # Sample environment configuration
├── README.md                 # Project Documentation
└── SECURITY.md               # Security Findings & Controls
```

## Database Design
The schema uses a relational structure managed by Flyway:
* `users`: Authentication details and RBAC role.
* `categories` & `products`: Catalog definitions.
* `inventory`: Tracks `available_quantity` and `reserved_quantity` tied 1:1 to products.
* `carts` & `cart_items`: Ephemeral shopping items bound to a user.
* `pickup_slots`: Time-boxed reservations with capacity constraints.
* `orders` & `order_items`: Immutable historical records of purchases tied to the user and their selected pickup slot.
* `return_requests` & `exchange_requests`: Post-purchase workflows tied to specific `order_items`.
* `audit_logs`: Append-only records tracking admin/staff operational events.

## API Documentation
Core APIs are RESTful. Examples include:

| Method | Endpoint | Purpose | Authentication |
|--------|----------|---------|----------------|
| POST   | `/api/auth/login` | Authenticate and retrieve JWT | Public |
| POST   | `/api/auth/register` | Register a new customer | Public |
| GET    | `/api/products` | Browse product catalog | Public |
| POST   | `/api/checkout` | Process Stripe payment & generate order | CUSTOMER |
| GET    | `/api/orders/{id}`| View order details | CUSTOMER |
| PATCH  | `/api/admin/orders/{id}/status` | Update order state | STAFF / ADMIN |
| GET    | `/api/admin/audit-logs` | Retrieve system action logs | ADMIN |

## Authentication & RBAC
Authentication relies entirely on stateless **JWT tokens** sent as `Bearer` tokens in the `Authorization` header.
- **Roles:** The application enforces 3 strict roles: `CUSTOMER`, `STAFF`, and `ADMIN`.
- **Enforcement:** Enforced at the controller level via Spring Security (`SecurityConfig` request matchers) and `@PreAuthorize` method annotations.
- **Tokens:** Short-lived access tokens and long-lived refresh tokens are utilized.

## Business Logic
- **Inventory Reservation:** When an order is placed, stock is immediately shifted from `available_quantity` to `reserved_quantity`. Stock is only fully deducted when the order completes, or restored if the order is cancelled.
- **Order Lifecycle:** Placed → Confirmed → Preparing → Ready for Pickup → Completed. Any deviations (e.g., Cancellation) reverse inventory holds.
- **Returns & Exchanges:** Customers can request a return. Staff verify the item condition and approve/reject via the staff portal, generating a refund or adjusting inventory automatically.

## Environment Variables
The application relies heavily on `.env` variables to prevent hardcoded secrets. View `.env.example` for the required configuration template. **Never commit actual database passwords, JWT secrets, or Stripe secret keys into the repository.**

## Local Development Setup
### Prerequisites
- Node.js 18+
- Java 17+ and Maven
- MySQL 8.x

### Backend Configuration
1. Initialize a MySQL database named `minidmart`.
2. Navigate to `backend/` and create an `.env` file using the template in `.env.example`.
3. Run the application (Flyway will automatically create the tables):
```bash
cd backend
mvn clean install
mvn spring-boot:run
```

### Frontend Configuration
1. Navigate to `frontend/` and create an `.env` file containing `VITE_API_URL` and `VITE_STRIPE_PUBLISHABLE_KEY`.
2. Start the Vite server:
```bash
cd frontend
npm install
npm run dev
```

## Deployment
- **Frontend:** Hosted statically on **Vercel** (`https://mini-mall-cyan.vercel.app/`).
- **Backend:** Spring Boot REST API hosted on **Render**. A multi-stage Dockerfile constructs and serves the application.
- **Database:** Hosted remotely on **Aiven MySQL**.

*Production environment variables are securely injected via the Vercel and Render dashboards.*

## Testing
- **Backend Unit Tests:** The project maintains a suite of JUnit 5 tests covering business logic within core services (`OrderServiceTest`, `CheckoutServiceTest`, `ReturnServiceTest`, `DashboardServiceTest`, etc.).
- **Build Verification:** Tested locally and inside Docker to ensure compile-time and runtime integrity.
- **Manual End-to-End Validation:** Workflows for checkout, RBAC boundaries, login, and inventory deduction were manually verified.

## Security
- **JWT Authentication:** Completely stateless authentication, preventing session hijacking.
- **Role-Based Access Control:** Hardened backend endpoints using `.anyRequest().authenticated()`.
- **Database Protections:** Parameterized queries natively handled by JPA to prevent SQL injection.
- **CORS Policies:** Configured securely to allow only authorized frontend origins.
- **Audit Logging:** Administrative actions generate unalterable logs tracing the acting user.

## Known Limitations
- Refunds generated by return requests must be handled via Stripe. Wait times or failure conditions for Stripe refunds are not aggressively retried.
- Refresh tokens are verified via JWT signature but are not blacklisted/stored in the database; a compromised refresh token remains valid until it expires.
- Concurrent inventory reservation relies heavily on database-level constraints. In extremely high-throughput scenarios, optimistic locking `version` exceptions may occur and require the user to retry their cart checkout.

## Future Improvements
- Implement a Redis-backed refresh token blacklist to allow forcing a user log out.
- Add email notifications (e.g., via SendGrid) for order status updates.
- Introduce advanced product search (e.g., Elasticsearch) instead of relational querying for larger catalogs.
