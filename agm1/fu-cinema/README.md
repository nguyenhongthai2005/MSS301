# FUCinemaBookingSystem – Cinema Ticket Booking System

Hệ thống đặt vé xem phim trực tuyến theo kiến trúc Microservices với API Gateway.
- **Môn học:** MSS301 – Microservices Architecture
- **Tác giả:** Nguyen Hong Thai (nguyenhongthai06092005@gmail.com)
- **Repository:** [https://github.com/nguyenhongthai2005/MSS301](https://github.com/nguyenhongthai2005/MSS301)

---

## 1. Danh mục dịch vụ & Cổng mạng

| Dịch vụ | Cổng | Cơ sở dữ liệu | Trách nhiệm chính |
|---|---|---|---|
| `api-gateway` | **9000** | - | Single Entry Point, xác thực JWT, bảo vệ header (Anti-Spoofing), phân quyền Role-based |
| `customer-service` | **8081** | SQL Server 2022 (`cinema_customer`) | Quản lý khách hàng, xác thực tài khoản (BCrypt), phát hành JWT token |
| `movie-service` | **8082** | MongoDB 7 (`cinema_movie`) | Quản lý thể loại, phòng chiếu, phim, lên lịch suất chiếu và tính giờ kết thúc |
| `booking-service` | **8083** | MySQL 8 (`cinema_booking`) | Sơ đồ ghế, đặt vé, OpenFeign sang movie-service, lịch sử đặt vé, hủy vé, thống kê doanh thu |

---

## 2. Tài khoản thử nghiệm (Test Accounts)

### 2.1. Tài khoản Quản trị viên (Admin)
- **Email chính thức:** `admin@fucinema.com` | **Mật khẩu:** `@@abc123@@`
- **Email rút gọn:** `admin` | **Mật khẩu:** `123`
- *Ghi chú:* Tài khoản Admin được cấu hình trực tiếp qua `application.properties`, có quyền quản trị toàn bộ hệ thống (`ROLE_ADMIN`).

### 2.2. Tài khoản Khách hàng (Customers)
Dữ liệu mẫu được nạp tự động qua Flyway Migration trên SQL Server:
- `an@gmail.com` | Mật khẩu: `123456` (Trạng thái: `ACTIVE`, `ROLE_CUSTOMER`)
- `binh@gmail.com` | Mật khẩu: `123456` (Trạng thái: `ACTIVE`, `ROLE_CUSTOMER`)
- `chi@gmail.com` | Mật khẩu: `123456` (Trạng thái: `INACTIVE` – dùng test chặn đăng nhập BR02)

---

## 3. Hướng dẫn khởi chạy hệ thống

### Bước 1: Khởi động 3 cơ sở dữ liệu qua Docker Compose
```bash
cd fu-cinema
docker compose up -d
```
Đợi container `cinema-sqlserver` chuyển sang trạng thái `healthy` trước khi chạy các service backend.

### Bước 2: Khởi động 4 dịch vụ (mỗi service 1 terminal)
```bash
# Terminal 1: customer-service (Port 8081)
cd fu-cinema/customer-service
mvn spring-boot:run

# Terminal 2: movie-service (Port 8082)
cd fu-cinema/movie-service
mvn spring-boot:run

# Terminal 3: booking-service (Port 8083)
cd fu-cinema/booking-service
mvn spring-boot:run

# Terminal 4: api-gateway (Port 9000)
cd fu-cinema/api-gateway
mvn spring-boot:run
```

Kiểm tra trạng thái Gateway:
```bash
curl http://localhost:9000/actuator/health
# Kết quả: {"status":"UP"}
```

---

## 4. Hướng dẫn kiểm thử với Postman Collection Runner

1. Mở Postman, chọn **Import** và nạp 2 file trong thư mục `fu-cinema/postman/`:
   - `FUCinemaBookingSystem.postman_collection.json`
   - `FUCinema-Local.postman_environment.json`
2. Chọn Environment: **FUCinema-Local**.
3. Chạy thử nghiệm:
   - **Direct Port Testing:** Hỗ trợ kiểm thử trực tiếp từng service backend tại các cổng 8081, 8082, 8083 để cô lập lỗi.
   - **Gateway Testing (Port 9000):** Toàn bộ API được bảo vệ bởi Spring Security OAuth2 Resource Server. Token JWT được chuyển tiếp thông tin định danh qua header `X-User-Id`, `X-User-Email`, `X-User-Role` và ngăn chặn triệt để client giả mạo header (`UserHeaderFilter`).
4. Để chạy tự động (Collection Runner):
   - Chuột phải vào collection `FUCinemaBookingSystem` -> Chọn **Run collection**.
   - Chọn môi trường **FUCinema-Local** -> Nhấn **Run FUCinemaBookingSystem**.
   - Tất cả các test case đều đạt kết quả **Passed** (0 Failed).

---

## 5. Danh mục tính năng đã hoàn thiện

- ✅ **F0:** Hạ tầng Docker Compose (SQL Server, MongoDB, MySQL), khởi tạo cấu trúc dự án và Global Exception Handler tập trung.
- ✅ **F1:** Xác thực JWT HS256, mã hóa mật khẩu BCrypt, đăng nhập Admin và Customer.
- ✅ **F2:** Khách hàng đăng ký tài khoản, xem/sửa thông tin cá nhân và đổi mật khẩu.
- ✅ **F3:** Quản trị viên quản lý danh sách khách hàng, tìm kiếm phân trang và xóa mềm (Soft Delete).
- ✅ **F4:** Quản trị viên quản lý thể loại phim và phòng chiếu với DataSeeder MongoDB, ràng buộc xóa (BR03).
- ✅ **F5:** Quản trị viên quản lý thông tin phim, truy vấn động với `MongoTemplate` và `Criteria`.
- ✅ **F6:** Quản trị viên quản lý suất chiếu, tự động tính giờ kết thúc, kiểm tra trùng giờ cùng phòng chiếu (Overlap Guard).
- ✅ **F7:** Khách hàng xem sơ đồ ghế và đặt vé xem phim qua OpenFeign (Snapshot Pattern).
- ✅ **F8:** Khách hàng xem lịch sử đặt vé, hủy vé trước giờ chiếu 2 tiếng (BR12) và giải phóng ghế.
- ✅ **F9:** Quản trị viên kết xuất báo cáo thống kê doanh thu theo khoảng thời gian và xếp hạng theo doanh thu phim giảm dần.
- ✅ **F10:** API Gateway định tuyến tập trung cổng 9000, lọc header giả mạo và phân quyền Role-based (`ROLE_ADMIN`, `ROLE_CUSTOMER`).
- ✅ **F11:** Postman Collection và Environment kiểm thử hoàn chỉnh 100%.
