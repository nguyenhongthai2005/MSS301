# MSS301 - Slot 13: Saga Pattern (Orchestration) Demo

Hệ thống demo Saga Pattern (Orchestration) xử lý giao dịch phân tán giữa 3 Microservices với mô hình **Database-per-Service** (Polyglot Persistence) trên nền tảng **Java 21 · Spring Boot 4.1.1 (Spring Framework 7) · Docker Compose · Postman**.

Tài liệu hướng dẫn chi tiết toàn diện: [Saga_guide.md](./Saga_guide.md)

---

## 1. Kiến trúc hệ sinh thái & Database-per-Service

| Service | Port | Database | Công nghệ & Driver | Nhiệm vụ chính |
|---|---|---|---|---|
| `order-service` | **8081** | **MySQL 8.4** (`orderdb`) | Spring Data JPA, `mysql-connector-j` | **Saga Orchestrator**, Quản lý Order & SagaLog |
| `inventory-service` | **8082** | **MongoDB 8.0** (`inventorydb`) | Spring Data MongoDB (Replica Set `rs0`) | Quản lý Product & Reservation (`reserve` / `release`) |
| `payment-service` | **8083** | **SQL Server 2022** (`paymentdb`) | Spring Data JPA, `mssql-jdbc` | Quản lý Wallet & Payment (`pay` / `refund`) |

---

## 2. Danh sách TODO đã hoàn thành (10/10)

- [x] **TODO-0**: Cấu hình `docker-compose.yml` cho 3 Database (`MySQL 8.4`, `SQL Server 2022`, `MongoDB 8.0 Replica Set rs0`) kèm healthchecks.
- [x] **TODO-1**: `inventory-service` - `@Document` `Product` & `Reservation`, `DataSeeder` nạp sẵn `P1`, `P2`.
- [x] **TODO-2**: `inventory-service` - `MongoTransactionManager`, `reserve` (trừ kho) & `release` (hoàn tác cộng kho) đảm bảo **Idempotent**.
- [x] **TODO-3**: `payment-service` - Entity `Wallet` & `Payment`, khởi tạo ví `C1=5000, C2=100` qua `data.sql` idempotent.
- [x] **TODO-4**: `payment-service` - `pay` (trừ tiền) & `refund` (hoàn tiền) đảm bảo **Idempotent**.
- [x] **TODO-5**: `order-service` - Entity `Order` (bảng `orders`), `SagaLog`, `OrderStatus` & `SagaStatus` (6 trạng thái chuẩn).
- [x] **TODO-6**: `order-service` - `OrderService` với mỗi method là một local transaction commit tức thì xuống MySQL.
- [x] **TODO-7**: `order-service` - Cấu hình `RestClientConfig`, `InventoryClient` & `PaymentClient`.
- [x] **TODO-8**: `order-service` - `OrderSagaOrchestrator` điều phối tuần tự, LIFO compensation stack, Retry 3 lần qua `RetryTemplate` Spring Framework 7.
- [x] **TODO-9**: `order-service` - `OrderController` expose `POST /api/orders`, `GET /api/orders`, `GET /api/orders/{id}/saga-logs`.
- [x] **TODO-10**: Multi-stage `Dockerfile` cho 3 service và tích hợp toàn bộ vào `docker-compose.yml`.
- [x] **TODO-11**: File Postman Collection `Saga_Postman_Collection.json` đầy đủ các kịch bản kiểm thử KB1 ➔ KB8.

---

## 3. Hướng dẫn chạy và kiểm thử

### Cách 1: Chạy toàn bộ hệ thống bằng Docker Compose (Khuyên dùng)
```bash
# Khởi động toàn bộ 3 database và 3 microservices
docker compose up -d --build

# Kiểm tra trạng thái các container
docker compose ps

# Theo dõi log của Saga Orchestrator
docker compose logs -f order-service
```

### Cách 2: Chạy Database trên Docker và chạy Service trên IDE
```bash
# Khởi động 3 database
docker compose up -d mysql sqlserver mongodb

# Chờ 3 container chuyển sang trạng thái (healthy)
docker compose ps
```
Sau đó chạy từng Service (`InventoryServiceApplication`, `PaymentServiceApplication`, `OrderServiceApplication`) trong IntelliJ hoặc VS Code.

---

## 4. Kiểm thử với Postman

1. Mở Postman Desktop.
2. Bấm **Import** ➔ Chọn file [`Saga_Postman_Collection.json`](./Saga_Postman_Collection.json).
3. Chuột phải vào collection **MSS301 - Saga Demo (Spring Boot 4.1.1)** ➔ Chọn **Run collection**.
4. Bỏ tick thư mục *Thủ công*, bấm **Run** để chạy tự động kiểm thử các kịch bản:
   * **KB1**: Happy path (`CONFIRMED / COMPLETED`).
   * **KB2**: Thanh toán thất bại (402) ➔ Hoàn tác kho (`CANCELLED / COMPENSATED`).
   * **KB3**: Kho hết hàng (409) ➔ Huỷ đơn.
   * **KB4**: Sản phẩm không tồn tại (404) ➔ Huỷ đơn.
   * **KB5**: Ví không tồn tại (404) ➔ Hoàn tác kho ➔ Huỷ đơn.
   * **KB7**: Kiểm tra tính Khả trùng (Idempotent) khi gọi lại nhiều lần.
