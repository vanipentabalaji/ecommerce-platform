# Ecommerce Microservices Architecture

A modern microservices-based e-commerce platform built with Spring Boot, Apache Kafka, and MySQL. This project demonstrates event-driven architecture with inter-service communication, AI-powered features, and comprehensive order/payment/product management.

## 🏗️ Architecture Overview

This project consists of three independent microservices that communicate asynchronously via Apache Kafka:

```
┌─────────────────────────────────────────────────────────────┐
│                     API Gateway / Client                     │
└────────────┬─────────────────┬──────────────┬─────────────────┘
             │                 │              │
      ┌──────▼──────┐   ┌──────▼──────┐  ┌───▼──────────┐
      │   Order      │   │  Payment    │  │   Product    │
      │  Service     │   │   Service   │  │   Service    │
      │  (8082)      │   │  (8083)     │  │   (8081)     │
      │  (DB)        │   │(Stateless)  │  │  (DB)        │
      └──────┬───────┘   └──────┬──────┘  └───┬──────────┘
             │                  │             │
             └──────────────────┼─────────────┘
                    │
                    │ Kafka Topics
                    │
             ┌──────▼──────────┐
             │  Apache Kafka   │
             │  (localhost:    │
             │   9092)         │
             └─────────────────┘
                    │
          ┌─────────┴─────────┐
          │                   │
    ┌─────▼────┐         ┌────▼────┐
    │ MySQL    │         │ MySQL   │
    │ Order    │         │ Product │
    │  DB      │         │   DB    │
    └──────────┘         └─────────┘
```

## 📦 Microservices

### 1. **Order Service** (Port: 8082)
Handles order creation, management, and tracking. Integrates with Google Gemini AI for intelligent order processing.

**Key Features:**
- Create and place orders
- Cancel orders with automatic stock restoration
- Track order status (PENDING → PLACED/FAILED/CANCELLED)
- AI-powered chat assistant for order management
- Real-time status updates via Kafka events

**Database:** `order_service_db`

**Primary Endpoints:**
- `POST /orders/place-order` - Create a new order
- `POST /orders/cancel-order` - Cancel an existing order
- `POST /ai/chat` - AI chat for order inquiries

**Key Technologies:**
- Spring Boot 3.3.4
- Spring Data JPA
- MySQL Connector
- Apache Kafka
- Spring AI (Google Gemini)
- Lombok

### 2. **Payment Service** (Port: 8083)
Processes payments for orders and manages refunds for cancelled orders. **Stateless service** - no database, operates purely on Kafka events.

**Key Features:**
- Consumes order creation events and processes payments
- Consumes order cancellation events and processes refunds
- Publishes payment status events (success/failure) back to Kafka
- Event-driven architecture (no persistence layer)
- Configurable payment simulation

**Database:** None (Stateless, event-driven service)

**Key Technologies:**
- Spring Boot 3.5.11
- Spring Web (REST endpoints for health checks)
- Apache Kafka (primary data source)
- Spring Kafka Test Suite (testing)
- Lombok

**Kafka Topics Consumed:**
- `order-created` → Processes payment → publishes `payment-success` or `payment-failed`
- `order-cancelled` → Processes refund

### 3. **Product Service** (Port: 8081)
Manages product catalog and inventory/stock management.

**Key Features:**
- Create and manage products
- View all products or specific products
- Real-time stock management
- Automatic stock reduction when orders are placed
- Stock restoration when orders are cancelled
- REST API for external integrations

**Database:** `product_service_db`

**Primary Endpoints:**
- `POST /products/create-product` - Add a new product
- `GET /products/get-all-products` - Retrieve all products
- `GET /products/{id}` - Get product by ID
- `PUT /products/{productId}/reduce-stock?quantity=X` - Update inventory

**Key Technologies:**
- Spring Boot 3.5.11
- Spring Data JPA
- MySQL Connector
- Apache Kafka
- Lombok

## 🔄 Event Flow

### Order Placement Flow:
```
1. Client → POST /orders/place-order (ProductID, Quantity)
2. Order Service:
   - Fetches product details from Product Service
   - Validates stock availability
   - Creates order in DB (Status: PENDING)
   - Publishes OrderCreatedEvent to Kafka (order-created topic)
3. Payment Service:
   - Consumes OrderCreatedEvent
   - Processes payment
   - Publishes PaymentSuccessEvent (payment-success topic)
4. Order Service:
   - Consumes PaymentSuccessEvent
   - Updates order status to PLACED
5. Product Service:
   - Consumes OrderCreatedEvent
   - Reduces product stock
```

### Order Cancellation Flow:
```
1. Client → POST /orders/cancel-order (OrderID)
2. Order Service:
   - Updates order status to CANCELLED
   - Publishes OrderCancelledEvent to Kafka
3. Product Service:
   - Consumes OrderCancelledEvent
   - Restores product stock
4. Payment Service:
   - Consumes OrderCancelledEvent
   - Processes refund
```

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Maven 3.6+
- MySQL 8.0+
- Apache Kafka 3.0+
- Google Gemini API Key (for AI features in Order Service)

### Installation & Setup

#### 1. Clone the Repository
```bash
git clone <repository-url>
cd ecommerce-microservices
```

#### 2. Set Up Databases

Create two MySQL databases (Order Service and Product Service only - Payment Service is stateless):
```sql
CREATE DATABASE order_service_db;
CREATE DATABASE product_service_db;
```

#### 3. Configure Services

Update `application.properties` in each service:

**Order Service** (`order-service/src/main/resources/application.properties`):
```properties
spring.datasource.url=jdbc:mysql://localhost:3307/order_service_db
spring.datasource.username=root
spring.datasource.password=your_password
spring.ai.google.genai.api-key=your_gemini_api_key
spring.kafka.bootstrap-servers=localhost:9092
```

**Product Service** (`product-service/src/main/resources/application.properties`):
```properties
spring.datasource.url=jdbc:mysql://localhost:3307/product_service_db
spring.datasource.username=root
spring.datasource.password=your_password
spring.kafka.bootstrap-servers=localhost:9092
```

**Payment Service** (`payment-service/src/main/resources/application.properties`):
```properties
spring.application.name=payment-service
server.port=8083
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=payment-group-1
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
```
*Note: No database configuration needed - Payment Service is stateless*

#### 4. Start Kafka
```bash
# Start Zookeeper
bin/zookeeper-server-start.sh config/zookeeper.properties

# Start Kafka Server
bin/kafka-server-start.sh config/server.properties
```

#### 5. Build and Run Services

**Product Service:**
```bash
cd product-service/product-service
mvn clean package
mvn spring-boot:run
```

**Order Service:**
```bash
cd order-service/order-service
mvn clean package
mvn spring-boot:run
```

**Payment Service:**
```bash
cd payment-service/payment-service
mvn clean package
mvn spring-boot:run
```

## 📋 API Documentation

### Order Service APIs

#### Create Order
```http
POST /orders/place-order
Content-Type: application/json

{
  "productId": 1,
  "quantity": 5
}

Response: "Order placed successfully with ID: 101"
```

#### Cancel Order
```http
POST /orders/cancel-order
Content-Type: application/json

{
  "orderId": 101
}

Response: "Order 101 cancelled successfully"
```

#### AI Chat Assistant
```http
POST /ai/chat?prompt=Order%202%20iPhones

Supported Intents:
- PLACE_ORDER: "Order 2 iPhones"
- CHECK_STATUS: "What is status of order 123?"
- CHECK_STOCK: "Is iPhone available?"
- ORDER_HISTORY: "Show my recent orders"
- CANCEL_ORDER: "Cancel order 123"
- CHECK_AMOUNT: "What is the price of iPhone?"

Response: JSON with intent, extracted details, and action results
```

### Product Service APIs

#### Create Product
```http
POST /products/create-product
Content-Type: application/json

{
  "name": "iPhone 15",
  "price": 999.99,
  "stock": 100
}
```

#### Get All Products
```http
GET /products/get-all-products

Response: Array of Product objects
```

#### Get Product by ID
```http
GET /products/{id}

Response: Product object
```

#### Reduce Stock
```http
PUT /products/{productId}/reduce-stock?quantity=5

Response: "Stock updated successfully"
```

## 📊 Data Models

### Order Entity
```java
@Entity
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long productId;
    private Integer quantity;
    private Double amount;
    
    @Enumerated(EnumType.STRING)
    private OrderStatus status;  // PENDING, PLACED, FAILED, CANCELLED
}
```

### Product Entity
```java
@Entity
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private Double price;
    private Integer stock;
}
```

## 🔌 Kafka Topics

| Topic | Producer | Consumer | Purpose |
|-------|----------|----------|---------|
| `order-created` | Order Service | Payment Service, Product Service | Triggers payment processing and stock reduction |
| `payment-success` | Payment Service | Order Service | Updates order status to PLACED |
| `payment-failed` | Payment Service | Order Service | Updates order status to FAILED |
| `order-cancelled` | Order Service | Product Service, Payment Service | Triggers stock restoration and refund |

## 🔐 Security Considerations

- **TODO:** Implement authentication/authorization (JWT, OAuth2)
- **TODO:** Add API rate limiting
- **TODO:** Encrypt sensitive data (passwords, API keys)
- **TODO:** Implement proper error handling and validation
- Currently uses basic configuration - suitable for development only

## 📈 Features & Enhancements

### Implemented ✅
- ✅ Event-driven microservices architecture
- ✅ Inter-service communication via Kafka
- ✅ Order management (create, cancel, track)
- ✅ Product inventory management
- ✅ Payment processing (simulated)
- ✅ AI-powered order assistant with Google Gemini
- ✅ MySQL persistence
- ✅ REST APIs

### Future Enhancements 🚀
- [ ] User authentication and authorization
- [ ] Order filtering by userId
- [ ] Advanced payment gateway integration (Stripe, PayPal)
- [ ] Real refund processing logic
- [ ] API rate limiting and throttling
- [ ] Circuit breaker pattern for resilience
- [ ] Distributed tracing (Sleuth + Zipkin)
- [ ] Service mesh (Istio) integration
- [ ] Containerization (Docker) and orchestration (Kubernetes)
- [ ] Comprehensive API documentation (Swagger/OpenAPI)
- [ ] Advanced logging and monitoring
- [ ] Database transaction management
- [ ] Cache layer (Redis)
- [ ] Performance optimization and indexing

## 🛠️ Tech Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Runtime | Java | 17 |
| Framework | Spring Boot | 3.3.4 / 3.5.11 |
| Persistence | JPA/Hibernate | Spring Data JPA |
| Message Queue | Apache Kafka | 3.0+ |
| Database | MySQL | 8.0+ |
| Build Tool | Maven | 3.6+ |
| Coding | Lombok | Latest |
| AI | Spring AI + Google Gemini | 1.1.3 |

## 📝 Project Structure

```
ecommerce-microservices/
├── order-service/
│   ├── src/main/java/com/ecommerce/order_service/
│   │   ├── controller/     # REST Controllers
│   │   ├── service/        # Business Logic
│   │   ├── entity/         # JPA Entities
│   │   ├── repository/     # Data Access
│   │   ├── consumer/       # Kafka Consumers
│   │   ├── event/          # Event Classes
│   │   └── dto/            # Data Transfer Objects
│   └── src/main/resources/ # Configuration
│
├── payment-service/
│   ├── src/main/java/com/ecommerce/payment_service/
│   │   ├── service/        # Payment Logic
│   │   ├── consumer/       # Kafka Consumers
│   │   └── event/          # Event Classes
│   └── src/main/resources/ # Configuration
│
├── product-service/
│   ├── src/main/java/com/ecommerce/product_service/
│   │   ├── controller/     # REST Controllers
│   │   ├── service/        # Business Logic
│   │   ├── entity/         # JPA Entities
│   │   ├── repository/     # Data Access
│   │   ├── consumer/       # Kafka Consumers
│   │   └── event/          # Event Classes
│   └── src/main/resources/ # Configuration
│
└── README.md
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 👨‍💻 Author

Developed as a demonstration of microservices architecture using Spring Boot and event-driven design patterns.

## 📞 Support

For issues, questions, or suggestions, please open an issue in the repository.

## 🎯 Key Learnings

This project demonstrates:
- **Microservices Architecture:** Breaking down a monolithic application into independent services
- **Event-Driven Communication:** Using Kafka for asynchronous inter-service communication
- **API Design:** RESTful API design principles
- **Database Management:** Using JPA/Hibernate for ORM
- **AI Integration:** Leveraging modern AI APIs (Google Gemini) for enhanced features
- **Spring Boot Best Practices:** Proper use of annotations, dependency injection, and configuration
- **Kafka Patterns:** Producers, consumers, and topic-based messaging

---

**Note:** This is a development/demonstration project. For production use, implement proper authentication, error handling, monitoring, and security measures.
