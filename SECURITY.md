# Security — Mini D-Mart

## 1. Security Overview
This document outlines the security architecture, threat mitigations, and compliance measures actively implemented within the Mini D-Mart grocery application. The system embraces a defense-in-depth strategy, combining strict network-level policies, robust application-level Role-Based Access Control (RBAC), and secure external payment integrations.

## 2. Authentication
Authentication is entirely stateless, utilizing **JSON Web Tokens (JWT)**.
- **Login/Register:** Customers authenticate via `/api/auth/login`. Successful authentication yields a short-lived access token (15 mins) and a long-lived refresh token (7 days).
- **Token Handling:** The backend strictly verifies the JWT signature on every incoming request via the `JwtAuthenticationFilter`. Tokens are transferred via the `Authorization: Bearer <token>` HTTP header.

## 3. Authorization / RBAC
The system implements strict Role-Based Access Control enforcing the principle of least privilege across three explicit roles:
- **CUSTOMER:** Can browse products, manage their personal cart, execute checkouts, and view *only their own* order history and profile.
- **STAFF:** Granted elevated privileges to view all active orders, mutate order status (e.g., mark as ready for pickup), adjust product inventory, and process return/exchange requests. Cannot modify core store configurations or view audit logs.
- **ADMIN:** Unrestricted operational access. Can promote users, modify categories/products, generate reports, and inspect the unalterable system Audit Logs.
- **Implementation:** Enforced via Spring Security `SecurityFilterChain` matchers (e.g., `/api/admin/**` restricted to `ADMIN`) and method-level `@PreAuthorize` annotations in controllers.

## 4. Password Security
- All user passwords are cryptographically hashed using **BCrypt** (`BCryptPasswordEncoder` in Spring Security) with a work factor of 12.
- Plaintext passwords are never stored in the database or written to logs.

## 5. API Security
- **Protected Endpoints:** All API endpoints are secure by default (`.anyRequest().authenticated()`). Only explicit public routes (e.g., product browsing, login) are permitted without a token.
- **Input Validation:** Incoming request bodies are aggressively validated using Jakarta Bean Validation (`@Valid`, `@NotNull`, `@Min`, etc.) to prevent malformed data from executing logic.
- **CORS:** Cross-Origin Resource Sharing is strictly bound to the known frontend URL (e.g., the Vercel domain), preventing cross-site interaction from malicious domains.
- **Error Handling:** A global `@ControllerAdvice` exception handler intercepts errors, returning standardized JSON `ApiResponse` objects rather than exposing internal Spring stack traces.

## 6. Secrets Management
- All configuration secrets are externalized to environment variables and injected at deployment (Render/Vercel dashboards).
- **Stripe Secret Keys:** `STRIPE_SECRET_KEY` remains strictly on the backend. The frontend only receives the safely exposable Publishable Key.
- **Database Credentials & JWT Secrets:** Are never committed. A `.env.example` file uses dummy placeholders to guide local setup without exposing production keys.

## 7. Database Security
- **SQL Injection Prevention:** All database interactions utilize Spring Data JPA/Hibernate, which inherently uses parameterized queries, eliminating SQL injection vectors.
- **Optimistic Locking:** The `inventory` table utilizes a `@Version` field to prevent race conditions and concurrent double-spend attacks during simultaneous checkouts.
- **Network Isolation:** The Aiven MySQL database is configured strictly for authenticated connections over SSL/TLS.

## 8. Audit Logging
The application maintains a dedicated `audit_logs` table. High-risk actions performed by `STAFF` or `ADMIN` roles (e.g., modifying inventory, promoting a user) generate an immutable audit record containing:
- The actor's User ID.
- The action performed.
- The affected entity type and ID.
- A precise timestamp.

## 9. Payment Security
- The system integrates seamlessly with **Stripe**.
- Credit card data is **never** touched, processed, or stored by the backend or database.
- Payment Intents are created on the backend and fulfilled securely via the Stripe React SDK on the frontend.

## 10. Deployment Security
- **Vercel (Frontend):** Serves the compiled static React SPA over enforced HTTPS.
- **Render (Backend):** Serves the Spring Boot application over HTTPS. Environment variables ensure secrets remain localized to the Render container.
- **Aiven (Database):** Hosted securely with SSL mode enabled for all JDBC connections (`sslMode=REQUIRED` / `DISABLED` local toggle via env vars).

## 11. Security Testing
The application underwent the following security validations during development:
- **Unauthorized API Access:** Verified that missing/invalid JWTs yield a 401 Unauthorized response.
- **Role-Based Access Restrictions:** Verified that a `CUSTOMER` attempting to access `/api/admin/orders` correctly receives a 403 Forbidden response.
- **Invalid Authentication:** Verified that incorrect passwords yield immediate 401 rejections without exposing user enumeration vulnerabilities.

## 12. Security Findings / Known Risks
- **Token Revocation:** JWTs are stateless. If a refresh token is stolen, it cannot be revoked before its 7-day expiration since there is no server-side token blacklist (e.g., Redis).
- **Rate Limiting:** The backend currently lacks aggressive rate-limiting (e.g., Bucket4j). A malicious actor could brute-force the login endpoint, relying solely on Render's basic infrastructure protections.

## 13. Responsible Disclosure
If you discover a security vulnerability within this project, please do not disclose it publicly. Submit an issue securely via GitHub or contact the repository owner directly to coordinate a responsible patch and disclosure.
