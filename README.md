# 🛒 BrutShop — Full-Stack E-Commerce Platform

A full-stack Neo-brutalist styled e-commerce web application built with **React 19 + Vite** frontend and **Spring Boot** backend. Features include JWT-based authentication, role-based access, image upload, product management, a stylish UI inspired by the Neo-brutalism design aesthetic, and more.

## 📸 Preview

![Preview](./Screenshot.png)

## 🔧 Tech Stack

### Frontend
- **React 19** (Vite)
- **Tailwind CSS** (with Neo-brutalist theme)
- **Lucide Icons**
- **React Router DOM**
- **Axios** for API calls
- **Sonner** for toast notifications
- **JWT** token storage and AuthContext
- **Custom Dark Mode** using ThemeContext

### Backend
- **Spring Boot 3**
- **Spring Security** with JWT
- **Role-based authorization (`USER`, `ADMIN`)**
- **PostgreSQL** database
- **Multipart image upload support**
- **DevSeeder** to auto-populate 5 dummy products
- **CORS** configuration for frontend integration

## ✨ Features

### 👨‍💻 Auth
- JWT-based login/signup
- Auto-login after signup
- AuthContext manages session globally
- Redirects based on auth status
- Role-based protected routes (e.g., `/add-product`, `/update-product/:id` for `ADMIN` only)

### 🛍 Product
- View product grid
- Search products (debounced)
- View product details with image and metadata
- Add/Edit/Delete product (for Admin)
- Image upload support with validation (max 2MB)

### 🛒 Cart
- Add/remove/update quantity
- Neo-brutalist styled sliding cart drawer
- Auth check before adding to cart
- Full toast notifications for cart actions

### 🖼 Images
- Images are stored as byte arrays (`byte[]`) in the database
- Rendered via base64 conversion on frontend
- Fallback images from `https://picsum.photos`

## 🚀 Getting Started

### 🔧 Prerequisites
- Node.js ≥ 18
- Java 17+
- PostgreSQL running locally
- Maven

## 📦 Backend Setup

1. Clone the repo and `cd` into the `Backend` directory:
   ```bash
   git clone https://github.com/Sarcastic-Soul/E-commerce-Sprinboot.git
   cd E-commerce/Backend
   ```

2. Configure PostgreSQL in `application.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/ecommerce
   spring.datasource.username=postgres
   spring.datasource.password=yourpassword
   ```

3. Run the Spring Boot app:
   ```bash
   ./mvnw spring-boot:run
   ```

> ✅ On first run, `DevSeeder.java` inserts 5 dummy products (with images from `/resources/static/`). Make sure the image files exist!

## 🌐 Frontend Setup

1. `cd` into the frontend directory:
   ```bash
   cd ../Frontend
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Start development server:
   ```bash
   npm run dev
   ```

4. Vite will launch the app at:
   ```
   http://localhost:3000
   ```

## 🔒 Auth & Roles

- JWT is stored in `localStorage` and auto-decoded to populate `user` state
- Roles (`USER` / `ADMIN`) are checked for protected routes using `ProtectedRoute`
- Backend endpoints like `/api/product` require `ADMIN` role via `@PreAuthorize`

## 🔥 DevSeeder (Image Upload Support)

- Dummy products are inserted with base64-encoded images from:
  ```
  src/main/resources/static/
  ├── backpack.jpg
  ├── keyboard.jpg
  ├── plushie.jpg
  ├── tshirt.jpg
  └── earphones.jpg
  ```

> ⚠️ Ensure these images exist or warnings will show during seeding.

## 🧠 Folder Structure

```
E-commerce/
├── Backend/
│   ├── src/main/java/com/anish/e_commerce/
│   ├── src/main/resources/
│   └── pom.xml
├── Frontend/
│   ├── src/components/
│   ├── src/pages/
│   ├── src/context/
│   ├── src/api/
│   └── vite.config.js
└── README.md
```

## 📝 License

MIT — Feel free to modify and use. Credit appreciated.

> Made with 💀 and too much coffee by [Anish Kumar](https://github.com/Sarcastic-Soul)
