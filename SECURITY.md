# Security — Mini D-Mart

## Overview

This document describes the security design, measures, and findings for the Mini D-Mart application.

> This document will be completed during the security hardening phase.

## Authentication
- JWT-based stateless authentication
- BCrypt password hashing (Spring Security BCryptPasswordEncoder)
- Short-lived access tokens (15 min) + long-lived refresh tokens (7 days)

## Authorization
- Role-Based Access Control (RBAC): CUSTOMER, STAFF, ADMIN
- Spring Security filter chain with URL-level rules
- Method-level authorization via @PreAuthorize
- Service-layer ownership verification

## API Security
- CORS restricted to frontend origin
- CSRF disabled (stateless JWT API)
- Input validation via Jakarta Bean Validation
- Parameterized queries via JPA/Hibernate (SQL injection prevention)
- Global exception handler (no stack traces in production)

## Secret Management
- All secrets externalized via environment variables
- No hardcoded credentials in source code
- `.env.example` provided with placeholder values

## Audit Logging
- Critical actions logged to audit_logs table
- Tracks: who, what, when, IP address, entity affected

## Security Findings
> To be completed after security review.
