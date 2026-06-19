# Mini Project API Documentation

A stateless Spring Boot REST API with JWT-based authentication, role-based access control (RBAC), and resource ownership enforcement. Built with Spring Boot 3+, Spring Security 6, Spring Data JPA, MySQL, and JJWT.

## Tech Stack

- **Language:** Java
- **Framework:** Spring Boot 3+
- **Security:** Spring Security 6 (JWT-based, stateless)
- **Persistence:** Spring Data JPA + MySQL
- **Async:** Spring `@Async` for background tasks
- **Email:** Spring Mail (SMTP via Mailtrap Sandbox for testing)

## Roles

| Role | Description |
|---|---|
| `ADMIN` | Full access — can manage any product, override vendor ownership rules, place/view orders |
| `VENDOR` | Can create products, and update/delete only their own products |
| `CUSTOMER` | Can view products and place orders |

## Authentication

All protected endpoints require a JWT in the request header:

```
Authorization: Bearer <token>
```

Tokens are obtained via `POST /api/auth/login` and contain the user's username and role-derived authority (`ROLE_ADMIN`, `ROLE_VENDOR`, or `ROLE_CUSTOMER`).

---

## 1. Auth Module

Base path: `/api/auth` — all endpoints in this module are public (no token required).

### Register

**POST** `/api/auth/register`

Creates a new user account.

**Request body:**
```json
{
  "username": "vendor1",
  "password": "password123",
  "role": "VENDOR"
}
```

`role` must be one of: `ADMIN`, `VENDOR`, `CUSTOMER`.

**Responses:**
| Status | Meaning |
|---|---|
| 200 / 201 | User created successfully |
| 400 | Validation failure (missing fields) |
| 409 / 400 | Username already exists |

### Login

**POST** `/api/auth/login`

**Request body:**
```json
{
  "username": "vendor1",
  "password": "password123"
}
```

**Response (200):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "vendor1",
  "role": "VENDOR"
}
```

*(Exact response field names depend on your `JwtResponse` DTO — adjust if different.)*

**Responses:**
| Status | Meaning |
|---|---|
| 200 | Login successful, token returned |
| 401 / 403 | Invalid username or password |

---

## 2. Product Module

Base path: `/api/products`

Products are created by vendors and contain a reference to their owning vendor. Vendors may only update or delete their own products; admins may modify any product. Read access is public.

### Product object shape (response)

```json
{
  "id": 1,
  "name": "Wireless Mouse",
  "description": "Ergonomic wireless mouse with USB-C charging",
  "price": 25.99,
  "stock": 100,
  "category": "Electronics",
  "createdAt": "2026-06-19T10:15:30",
  "updatedAt": "2026-06-19T10:15:30",
  "vendorId": 2,
  "vendorUsername": "vendor1"
}
```

### List all products

**GET** `/api/products`

**Auth:** None required (public)

**Response (200):** array of product objects.

### Get a single product

**GET** `/api/products/{id}`

**Auth:** None required (public)

**Responses:**
| Status | Meaning |
|---|---|
| 200 | Product found |
| 404 | No product with that ID |

### Create a product

**POST** `/api/products`

**Auth:** `VENDOR` or `ADMIN`

**Request body:**
```json
{
  "name": "Wireless Mouse",
  "description": "Ergonomic wireless mouse with USB-C charging",
  "price": 25.99,
  "stock": 100,
  "category": "Electronics"
}
```

Validation: `name` required (non-blank), `price` required and must be > 0, `stock` required and must be ≥ 0.

The product's `vendor` is automatically set to the authenticated user — it cannot be set or overridden by the request body.

**Responses:**
| Status | Meaning |
|---|---|
| 201 | Product created |
| 400 | Validation error |
| 401 / 403 | Missing/invalid token, or role is not VENDOR/ADMIN |

### Update a product

**PUT** `/api/products/{id}`

**Auth:** `VENDOR` (only if they own the product) or `ADMIN` (any product)

**Request body:** same shape as Create.

**Responses:**
| Status | Meaning |
|---|---|
| 200 | Updated successfully |
| 400 | Validation error |
| 403 | Authenticated as VENDOR but does not own this product |
| 404 | Product not found |

### Delete a product

**DELETE** `/api/products/{id}`

**Auth:** `VENDOR` (only if they own the product) or `ADMIN` (any product)

**Responses:**
| Status | Meaning |
|---|---|
| 204 | Deleted successfully, no content returned |
| 403 | Authenticated as VENDOR but does not own this product |
| 404 | Product not found |

---

## 3. Order Module

Base path: `/api/orders`

Customers place orders against existing products. Before an order is created, the Order module verifies — via an internal call into the Product module — that the product exists and that requested quantity does not exceed available stock. On success, stock is decremented and an order record is created with status `PENDING`.

### Order object shape (response)

```json
{
  "id": 5,
  "productId": 1,
  "productName": "Wireless Mouse",
  "customerId": 3,
  "customerUsername": "customer1",
  "quantity": 2,
  "status": "PENDING",
  "createdAt": "2026-06-19T10:20:00"
}
```

### Order statuses

| Status | Meaning |
|---|---|
| `PENDING` | Default status when an order is placed |
| `CONFIRMED` | Reserved for future use (no endpoint currently transitions to this) |
| `CANCELLED` | Reserved for future use (no endpoint currently transitions to this) |

### Create an order

**POST** `/api/orders`

**Auth:** `CUSTOMER` or `ADMIN`

**Request body:**
```json
{
  "productId": 1,
  "quantity": 2
}
```

Validation: `productId` required, `quantity` required and must be ≥ 1.

**Business rules enforced:**
- Product must exist (otherwise 404)
- Requested quantity must not exceed current stock (otherwise 400)
- On success, the product's stock is decremented by the order quantity
- An async email notification is triggered (see Notification Module below) — this does not delay the HTTP response

**Responses:**
| Status | Meaning |
|---|---|
| 201 | Order created, stock decremented |
| 400 | Insufficient stock, or validation error |
| 401 / 403 | Missing/invalid token, or role is not CUSTOMER/ADMIN |
| 404 | Product not found |

### View my orders

**GET** `/api/orders/my`

**Auth:** `CUSTOMER` or `ADMIN`

Returns only the orders placed by the authenticated user.

**Response (200):** array of order objects belonging to the caller.

---

## 4. Notification Module

There is no public endpoint for this module — it runs automatically as a side effect of order creation.

**Trigger:** Immediately after an order is successfully saved in `POST /api/orders`.

**Behavior:** An async task (`@Async`) sends an email via SMTP (Mailtrap Sandbox in the current test configuration) containing:

```
Subject: Order Confirmation
Body: Order placed successfully order ID <orderId>
```

Because the task is asynchronous, the HTTP response for order creation returns immediately and does not wait for the email to be sent. If the email fails to send (e.g. SMTP misconfiguration), the failure is logged server-side and does **not** roll back or affect the already-created order.

**Current limitation:** the recipient address is currently a fixed test address, not the customer's real email, since the `User` entity does not yet store an email field.

---

## Error Response Conventions

Most error responses follow Spring's default structure (via `ResponseStatusException` or Spring Security's exception handling):

```json
{
  "timestamp": "2026-06-19T10:25:00.000+00:00",
  "status": 403,
  "error": "Forbidden",
  "message": "You do not have permission to modify this product",
  "path": "/api/products/2"
}
```

| Status | Used for |
|---|---|
| 400 | Validation failures, business rule violations (e.g. insufficient stock) |
| 401 | Missing or invalid JWT |
| 403 | Valid JWT but insufficient role, or not the resource owner |
| 404 | Resource does not exist |

---

## Quick Reference: Endpoint Summary

| Method | Path | Auth Required | Notes |
|---|---|---|---|
| POST | `/api/auth/register` | None | |
| POST | `/api/auth/login` | None | |
| GET | `/api/products` | None | |
| GET | `/api/products/{id}` | None | |
| POST | `/api/products` | VENDOR, ADMIN | |
| PUT | `/api/products/{id}` | VENDOR (owner), ADMIN | |
| DELETE | `/api/products/{id}` | VENDOR (owner), ADMIN | |
| POST | `/api/orders` | CUSTOMER, ADMIN | Verifies stock via Product module |
| GET | `/api/orders/my` | CUSTOMER, ADMIN | Returns only the caller's orders |