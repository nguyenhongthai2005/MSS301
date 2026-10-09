# Testing Inventory Service bằng Postman

> Dựa trên `inventory-service.md`. Áp dụng cho service `inventory-service` (package `com.fudn.inventoryservice`).
> **Base URL:** `http://localhost:8082` | **Endpoint:** `GET /api/inventory?skuCode={sku}&quantity={quantity}`

---

## 0. Checklist chuẩn bị trước khi test

- [ ] Docker container `mysql` đang chạy (`docker ps` thấy container `mysql` status `Up`)
- [ ] Database `inventory_service` đã tồn tại trong MySQL
- [ ] `inventory-service` đã chạy thành công (`.\mvnw spring-boot:run`), log không có lỗi Flyway
- [ ] Log xác nhận Flyway đã áp dụng `V1__init.sql` và `V2__add_inventory.sql` (dòng `Successfully applied 2 migrations`)
- [ ] Bảng `t_inventory` đã tồn tại và có 4 SKU mẫu (`iphone_15`, `pixel_8`, `galaxy_24`, `oneplus_12` với 100 số lượng)
- [ ] Postman đã cài đặt, đã import file `Inventory_Service.postman_collection.json`

---

## 1. Test Cases

| # | Test Case | Method | Params | Kỳ vọng Status | Kết quả mong đợi |
|---|---|---|---|---|---|
| 1 | Còn hàng (Đủ số lượng) | GET | `skuCode=iphone_15&quantity=100` | `200 OK` | `true` |
| 2 | Hết hàng (Không đủ số lượng) | GET | `skuCode=iphone_15&quantity=200` | `200 OK` | `false` |
| 3 | SKU không tồn tại | GET | `skuCode=unknown_device&quantity=1` | `200 OK` | `false` |
| 4 | Thiếu param bắt buộc | GET | `skuCode=iphone_15` (thiếu `quantity`) | `400 Bad Request` | Spring error JSON |
| 5 | Sai kiểu param | GET | `skuCode=iphone_15&quantity=abc` | `400 Bad Request` | Spring error JSON |
| 6 | Sai HTTP Method | POST | `/api/inventory` | `405 Method Not Allowed` | Spring error JSON |
