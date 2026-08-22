# Mini D-Mart — Grocery Store Application

A full-stack grocery store application with customer shopping, store pickup/delivery, returns/exchanges, and staff/admin management.

## Tech Stack

### Backend
- Java 17, Spring Boot 3.x, Spring Security, JWT, Spring Data JPA, Hibernate, MySQL 8.x

### Frontend
- React 18, Vite, Tailwind CSS, shadcn/ui, TanStack Query, React Router

## Project Structure

```
Mini_Dmart/
├── backend/          # Spring Boot REST API
├── frontend/         # React SPA
├── README.md
├── SECURITY.md
└── .env.example
```

## Setup

### Prerequisites
- Java 17+
- Maven 3.9+
- Node.js 18+
- MySQL 8.x

### Backend
```bash
cd backend
# Configure database in src/main/resources/application.yml or set environment variables
mvn spring-boot:run
```

### Frontend
```bash
cd frontend
npm install
npm run dev
```

## Environment Variables

See `.env.example` for all required environment variables.

## Documentation

- [SECURITY.md](./SECURITY.md) — Security design and findings
