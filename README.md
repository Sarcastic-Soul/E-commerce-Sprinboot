# 🛒 BrutShop — Full-Stack E-Commerce Platform

A **Neo-brutalist styled full-stack e-commerce application** built with **React 19 + Vite** and **Spring Boot 3**. It features robust JWT auth, role-based access, an order management system, dynamic product filtering, and a beautifully brutal UI.

## 📸 Preview

![Preview](./Screenshot.png)

## 🔧 Tech Stack

### Frontend
- **React 19** (Vite)
- **Tailwind CSS** (Neo-brutalist theme)
- **Lucide Icons** & **Recharts** (Admin Data Visualization)
- **React Router DOM**
- **Axios** for API calls & **Sonner** for toast notifications
- **JWT** token storage and AuthContext
- **Vitest & React Testing Library** for frontend component testing

### Backend
- **Spring Boot 3**
- **Spring Security + JWT** (Role-based authorization: `USER`, `ADMIN`)
- **PostgreSQL** (Local pgAdmin for Dev, NeonDB for Prod)
- **Cloudinary** for image uploads
- **JUnit 5 & Mockito** for unit and integration testing
- **DevSeeder** to auto-populate dummy products and users

## ✨ Features

### 👨‍💻 Auth & Security
- JWT-based login/signup with auto-login
- Synchronous AuthContext session management (no refresh glitches)
- Role-based protected routes

### 🛍 Products & Discovery
- Product grid with **Debounced Search**
- **Dynamic Brutalist Filter Drawer** (Filter by Category, Price Range, and Sorting)
- Product detail view with Cloudinary integration
- Admin-only add/edit/delete capabilities

### 🛒 Cart & 📦 Orders
- Sliding cart drawer with dynamic Context badge
- DTO-based cart synchronization
- **Checkout System:** Convert carts into immutable orders 
- **Order History:** Users can track their past orders, totals, and statuses

### 📊 Admin Dashboard
- Global platform statistics (Total Users, Products, Orders)
- **Interactive Line Chart** mapping order trends over time using Recharts
- Secure, admin-only data aggregation endpoints

## 🔌 Database & Environments

This project utilizes Spring Profiles to manage separate development and production environments.

**Development (`application-dev.properties`):**
Connects to a local PostgreSQL instance via pgAdmin.
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/brutshop_dev
spring.datasource.username=postgres
spring.datasource.password=your_local_password
```

**Production (`application-prod.properties`):**
Ready to connect to **NeonDB** using environment variables.
```properties
spring.datasource.url=${NEON_DB_URL}
spring.datasource.username=${NEON_DB_USER}
spring.datasource.password=${NEON_DB_PASS}
```

## 🚀 Getting Started

### Backend

```bash
cd Backend

# The app runs the 'dev' profile by default
./mvnw spring-boot:run
```
> 🔁 `DevSeeder.java` inserts sample products and default users (`admin`/`admin123` and `user`/`user123`) on first boot.

### Frontend

```bash
cd Frontend
npm install
npm run dev
```

### 🧪 Running Tests
- **Backend:** Run `./mvnw test` in the `/Backend` directory.
- **Frontend:** Run `npm run test` in the `/Frontend` directory.

## 🧠 Folder Structure

```
E-commerce/
├── Backend/
│   ├── src/main/java/com/anish/e_commerce/
│   └── src/test/java/com/anish/e_commerce/
├── Frontend/
│   ├── src/components/
│   ├── src/pages/
│   └── src/context/
└── README.md
```

## 📝 License

MIT — Feel free to modify and use. Credit appreciated.

> Made by [Anish Kumar](https://github.com/Sarcastic-Soul)
