# Postman Sample Requests - POS System

This file provides ready-to-use sample requests (URL, method, headers, body, and descriptions) for testing the `pos-system` APIs in Postman.

## Base Setup

- **Base URL**: `http://localhost:8080/pos-system`
- **OpenAPI JSON**: `http://localhost:8080/pos-system/v3/api-docs`
- **Swagger UI**: `http://localhost:8080/pos-system/swagger-ui.html`
- **Authentication**: Basic Auth
  - Username: `admin`
  - Password: `admin123`
- **Common Headers**:
  - `Content-Type: application/json` (for POST/PUT)
  - `Accept: application/json`

> Note: According to current app config, most API endpoints require authentication.

---

## 1) Customer APIs

### 1.1 Create Customer
- **Description**: Create a new customer.
- **Method**: `POST`
- **URL**: `http://localhost:8080/pos-system/customer`
- **Headers**:
  - `Authorization: Basic <base64(admin:admin123)>`
  - `Content-Type: application/json`
  - `Accept: application/json`
- **Body (raw JSON)**:
```json
{
  "id": "C001",
  "name": "Nimal Perera",
  "address": "Colombo",
  "email": "nimal@example.com"
}
```

### 1.2 Update Customer
- **Description**: Update an existing customer.
- **Method**: `PUT`
- **URL**: `http://localhost:8080/pos-system/customer`
- **Headers**:
  - `Authorization: Basic <base64(admin:admin123)>`
  - `Content-Type: application/json`
  - `Accept: application/json`
- **Body (raw JSON)**:
```json
{
  "id": "C001",
  "name": "Nimal S. Perera",
  "address": "Kandy",
  "email": "nimal@example.com"
}
```

### 1.3 Get Customer by ID
- **Description**: Retrieve one customer by ID.
- **Method**: `GET`
- **URL**: `http://localhost:8080/pos-system/customer?id=C001`
- **Headers**:
  - `Authorization: Basic <base64(admin:admin123)>`
  - `Accept: application/json`

### 1.4 Get All Customers
- **Description**: Retrieve all customers.
- **Method**: `GET`
- **URL**: `http://localhost:8080/pos-system/customer`
- **Headers**:
  - `Authorization: Basic <base64(admin:admin123)>`
  - `Accept: application/json`

### 1.5 Delete Customer
- **Description**: Delete customer by ID.
- **Method**: `DELETE`
- **URL**: `http://localhost:8080/pos-system/customer?id=C001`
- **Headers**:
  - `Authorization: Basic <base64(admin:admin123)>`

---

## 2) Item APIs

### 2.1 Create Item
- **Description**: Create a new item.
- **Method**: `POST`
- **URL**: `http://localhost:8080/pos-system/item`
- **Headers**:
  - `Authorization: Basic <base64(admin:admin123)>`
  - `Content-Type: application/json`
  - `Accept: application/json`
- **Body (raw JSON)**:
```json
{
  "code": "I001",
  "description": "Rice 1kg",
  "unitPrice": 250.00
}
```

### 2.2 Update Item
- **Description**: Update an existing item.
- **Method**: `PUT`
- **URL**: `http://localhost:8080/pos-system/item`
- **Headers**:
  - `Authorization: Basic <base64(admin:admin123)>`
  - `Content-Type: application/json`
  - `Accept: application/json`
- **Body (raw JSON)**:
```json
{
  "code": "I001",
  "description": "Rice 1kg - Premium",
  "unitPrice": 275.00
}
```

### 2.3 Get Item by Code
- **Description**: Retrieve one item by code.
- **Method**: `GET`
- **URL**: `http://localhost:8080/pos-system/item?code=I001`
- **Headers**:
  - `Authorization: Basic <base64(admin:admin123)>`
  - `Accept: application/json`

### 2.4 Get All Items
- **Description**: Retrieve all items.
- **Method**: `GET`
- **URL**: `http://localhost:8080/pos-system/item`
- **Headers**:
  - `Authorization: Basic <base64(admin:admin123)>`
  - `Accept: application/json`

### 2.5 Delete Item
- **Description**: Delete item by code.
- **Method**: `DELETE`
- **URL**: `http://localhost:8080/pos-system/item?code=I001`
- **Headers**:
  - `Authorization: Basic <base64(admin:admin123)>`

---

## 3) Order APIs

### 3.1 Create Order
- **Description**: Create a new order with order details.
- **Method**: `POST`
- **URL**: `http://localhost:8080/pos-system/order`
- **Headers**:
  - `Authorization: Basic <base64(admin:admin123)>`
  - `Content-Type: application/json`
  - `Accept: application/json`
- **Body (raw JSON)**:
```json
{
  "orderId": "O001",
  "date": "2026-11-01",
  "customerId": "C001",
  "orderDetails": [
    {
      "orderId": "O001",
      "itemCode": "I001",
      "qty": 2,
      "unitPrice": 250.00
    },
    {
      "orderId": "O001",
      "itemCode": "I002",
      "qty": 1,
      "unitPrice": 450.00
    }
  ]
}
```

### 3.2 Get Order by ID
- **Description**: Retrieve one order by ID.
- **Method**: `GET`
- **URL**: `http://localhost:8080/pos-system/order?id=O001`
- **Headers**:
  - `Authorization: Basic <base64(admin:admin123)>`
  - `Accept: application/json`

### 3.3 Get All Orders
- **Description**: Retrieve all orders.
- **Method**: `GET`
- **URL**: `http://localhost:8080/pos-system/order`
- **Headers**:
  - `Authorization: Basic <base64(admin:admin123)>`
  - `Accept: application/json`

---

## 4) Quick Validation Notes

- Customer ID format: `C001`, `C002`, ...
- Item code format: `I001`, `I002`, ...
- Order ID format: `O001`, `O002`, ...
- Use valid email format for customer email field.

---

## 5) Postman Tips

- In Postman, set **Authorization** tab to **Basic Auth** and provide username/password once at collection level.
- For POST/PUT requests, choose **Body -> raw -> JSON**.
- If you get `401 Unauthorized`, re-check credentials and whether the app is running under the same context path and port.

