<div align="center">

# 🛒 BrutShop

**Neo-brutalist full-stack e-commerce** — React 19 + Spring Boot 3, shipped as a single Docker monolith.

[![CI/CD](https://img.shields.io/github/actions/workflow/status/Sarcastic-Soul/E-commerce-Sprinboot/deploy.yml?branch=main&style=flat-square&logo=githubactions&logoColor=white&label=CI%2FCD)](https://github.com/Sarcastic-Soul/E-commerce-Sprinboot/actions/workflows/deploy.yml)
[![Live Demo](https://img.shields.io/badge/demo-live-success?style=flat-square&logo=render&logoColor=white)](https://springboot-ecommerce-latest-ctgu.onrender.com/)
[![API Docs](https://img.shields.io/badge/API-Swagger-85EA2D?style=flat-square&logo=swagger&logoColor=black)](https://springboot-ecommerce-latest-ctgu.onrender.com/swagger-ui/index.html)
[![License](https://img.shields.io/badge/license-MIT-blue?style=flat-square)](LICENSE)

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![React](https://img.shields.io/badge/React-19-61DAFB?style=flat-square&logo=react&logoColor=black)
![Vite](https://img.shields.io/badge/Vite-6-646CFF?style=flat-square&logo=vite&logoColor=white)
![Tailwind CSS](https://img.shields.io/badge/Tailwind-4-06B6D4?style=flat-square&logo=tailwindcss&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=flat-square&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=flat-square&logo=redis&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white)

### [🔗 Live App](https://springboot-ecommerce-latest-ctgu.onrender.com/) · [📖 API Docs](https://springboot-ecommerce-latest-ctgu.onrender.com/swagger-ui/index.html) · [📺 Demo Video](https://youtu.be/o3MAQWqXyqA)

JWT auth with refresh tokens · role-based access · Razorpay checkout · wishlist & restock notifications · Redis caching · rate limiting · admin analytics

![Preview](./Screenshot.png)

</div>

> ⚠️ **Free-tier hosting** — the server sleeps when idle. First visit takes **2–5 minutes** to wake up. Watch the [demo video](https://youtu.be/o3MAQWqXyqA) if you'd rather not wait.

### 🔑 Demo Accounts

| Role | Username | Password |
|---|---|---|
| Standard user | `user` | `user123` |
| Admin | `admin` | `admin123` |

💡 The login page has **one-click buttons** for both — no typing required.

---

## 🏛 Architecture

```mermaid
graph TD
    Client[Browser / Client] -->|HTTPS| RateLimiter[Bucket4j Rate Limiter]
    RateLimiter -->|JWT Auth| Security[Spring Security Filter Chain]

    subgraph Spring Boot Backend
        Security --> Controllers[REST Controllers]
        Controllers --> Services[Business Logic Services]
        Services --> Repositories[Spring Data JPA]

        Services -.->|Cache Read/Write| Redis[(Redis Cache)]
        Services -.->|Upload Images| Cloudinary[Cloudinary API]
    end

    Repositories --> Database[(PostgreSQL Database)]
```

- React compiles into the Spring Boot JAR at build time → **one deployable artifact**.
- Vite proxies `/api` to `localhost:8080` in dev — no CORS setup needed locally.

---

## 🔧 Tech Stack

| Layer | Technologies |
|---|---|
| **Frontend** | React 19 (Vite 8), Tailwind CSS 4, React Router 7, Axios, Recharts, Phosphor Icons, Sonner |
| **Backend** | Spring Boot 4.1 (Java 21+, Java 25 in Docker), Spring Security + JWT, Spring Data JPA, Bucket4j |
| **Data** | PostgreSQL (Neon in prod), Redis (API cache + refresh-token TTL) |
| **Integrations** | Razorpay (payments, test mode), Cloudinary (image hosting) |
| **Testing** | JUnit 5, Mockito, Spring Security Test |
| **DevOps** | Docker (multi-stage), GitHub Actions, Render |

---

## ✨ Features

**🔐 Auth & Security**
- Short-lived JWT access tokens + Redis-backed refresh tokens.
- Role-based route protection (`ADMIN` / `USER`), enforced client- and server-side.
- Per-user ownership checks on carts and notifications.
- IP-based rate limiting (30 req/min) on public endpoints.

**🛍 Products & Discovery**
- Server-side pagination, debounced search, Redis-cached reads.
- Filter drawer: category, price range, availability, sorting.
- Wishlist with instant UI updates.

**🛒 Cart & Orders**
- Sliding cart drawer with live badge count.
- Razorpay checkout with server-side signature verification.
- Stock deducts only after payment is verified, from the order snapshot.
- Order history: `PENDING` · `COMPLETED` · `REJECTED`.

**🔔 Notifications**
- Restocking a product from `0` → `≥1` notifies everyone who wishlisted it.

**📊 Admin Dashboard**
- Totals for users, products, orders, revenue, out-of-stock items.
- Recharts line chart of order trends over time.

---

## 💻 Local Setup

**Prerequisites:** Java 21 · Node 18+ · pnpm · PostgreSQL (database `brutshop`) · Redis (`docker run -p 6379:6379 redis`)

```bash
# 1. Backend → localhost:8080
cp backend/secrets.properties.example backend/secrets.properties    # optional, see below
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 2. Frontend → localhost:3000
cd frontend && pnpm install && pnpm dev
```

- `-Dspring-boot.run.profiles=dev` is **required** — the app defaults to `prod`.
- Starts fine with blank secrets; only image upload and checkout are affected.
- `DevSeeder` seeds the demo accounts and 15 sample products on first boot.

---

## 🔑 Environment Variables

Local values live in `backend/secrets.properties` (gitignored + dockerignored); production uses real env vars on Render. **Env vars override the file.**

| Variable | Scope | Purpose |
|---|---|---|
| `JWT_SECRET` | ⚠️ **Prod required** | Signing key, 32+ chars. Unset → new random key each restart, dropping every session. Generate: `openssl rand -base64 48` |
| `DB_URL` `DB_USER` `DB_PASS` | Prod | PostgreSQL — dev uses localhost defaults |
| `REDIS_URL` | Prod | Redis — dev uses localhost defaults |
| `SPRING_PROFILES_ACTIVE` | Prod | Set to `prod` |
| `CLOUDINARY_CLOUD_NAME` `_API_KEY` `_API_SECRET` | Optional | Image upload |
| `RAZORPAY_KEY_ID` `_KEY_SECRET` | Optional | Checkout + signature verification. The frontend reads the key ID from `/api/payments/config`, so rotating keys needs no rebuild |

**GitHub Actions secrets:** `DOCKER_USERNAME` · `DOCKER_PASSWORD` · `RENDER_DEPLOY_HOOK`

---

## 🚀 Deployment

Push to `main` → GitHub Actions builds the monolith image → pushes to Docker Hub → triggers the Render deploy hook → Render pulls and runs it with the env vars above.

---

## 🧪 Tests

```bash
cd backend && ./mvnw test
```

27 tests across services, business logic, and endpoint authorization (JUnit 5 · Mockito · Spring Security Test).

---

<div align="center">

**[MIT](LICENSE)** © [Anish Kumar](https://github.com/Sarcastic-Soul)

</div>
