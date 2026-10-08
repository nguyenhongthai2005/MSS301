# FUCinemaBookingSystem – Cinema Ticket Booking System

Hệ thống đặt vé xem phim trực tuyến theo kiến trúc Microservices với API Gateway.

## Tiến độ thực hiện

### ✅ Milestone F0 – Hạ tầng & Khởi tạo dự án (Hoàn thành)
- **TODO 0.1:** Cấu hình Docker Compose cho 3 cơ sở dữ liệu (SQL Server 2022, MongoDB 7, MySQL 8).
- **TODO 0.2:** Script khởi tạo database `cinema_customer` và `cinema_booking`.
- **TODO 0.3:** Khởi tạo 3 backend services (`customer-service`, `movie-service`, `booking-service`) với Spring Boot 4.1.0, Java 21 và đầy đủ dependencies.
- **TODO 0.4:** Khởi tạo `api-gateway` với Spring Cloud Gateway Server Web MVC và OAuth2 Resource Server.
- **TODO 0.5:** Cấu hình `application.properties` (cổng dịch vụ, chuỗi kết nối DataSource).
- **TODO 0.6:** Triển khai bộ xử lý ngoại lệ tập trung (`GlobalExceptionHandler`, `ApiException`, `ErrorResponse`) cho cả 3 service.

---

## Danh mục dịch vụ & Cổng chạy
| Dịch vụ | Cổng | Cơ sở dữ liệu | Ghi chú |
|---|---|---|---|
| `api-gateway` | 9000 | - | Single Entry Point, xác thực JWT & phân quyền |
| `customer-service` | 8081 | SQL Server 2022 (`cinema_customer`) | Quản lý tài khoản, khách hàng, phát hành JWT |
| `movie-service` | 8082 | MongoDB 7 (`cinema_movie`) | Quản lý danh mục phim, phòng, suất chiếu |
| `booking-service` | 8083 | MySQL 8 (`cinema_booking`) | Quản lý giao dịch đặt vé, OpenFeign sang movie-service |

---

## Hướng dẫn khởi động hạ tầng Database
```bash
cd fu-cinema
docker compose up -d
```
