# Saga Design – Watch Management Microservices

> **[TODO-4.1]** Tài liệu thiết kế Saga Pattern (Orchestration) cho hệ thống quản lý đồng hồ.  
> **Microservices:** `MSWatch` (Saga Orchestrator) & `MSBrand` (Saga Participant).

---

## 1. Thành phần tham gia (Participants)

Hệ thống áp dụng mô hình **Database per Service** (mỗi service chỉ kết nối trực tiếp database của chính nó):

| Microservice | Vai trò trong Saga | Database sở hữu | Dữ liệu quản lý tham gia Saga |
|---|---|---|---|
| **`MSWatch`** | **Saga Orchestrator** (Điều phối) | `DBWatch` | Bảng `Watches`, `WatchCategories`, `SagaStates`, `SagaLogs` |
| **`MSBrand`** | **Saga Participant** (Tham gia) | `DBBrand` | Bảng `BrandWatches` (quan hệ giữa Watch và Brand) |

---

## 2. Đặc tả chi tiết 3 chu trình Saga (Orchestration)

### 2.1. Saga CREATE_WATCH (Thêm mới đồng hồ)

Khi Admin thêm đồng hồ mới, dữ liệu phải được lưu vào `DBWatch.Watches` đồng thời liên kết thương hiệu phải được lưu vào `DBBrand.BrandWatches`.

| Bước | Tên Local Transaction | Service / DB | Thao tác thực hiện | Compensating Transaction (Bồi hoàn) | Idempotent? |
|:---:|---|---|---|---|:---:|
| **1** | `CREATE_WATCH` | MSWatch / DBWatch | INSERT bản ghi mới vào bảng `Watches` | `DELETE_WATCH` (gọi `deleteIfExists` xoá watch vừa insert) | ✔ |
| **2** | `ASSIGN_BRAND` | MSBrand / DBBrand | Gán watch vào brand qua API `PUT /internal/brands/{brandId}/watches/{watchId}` | `UNASSIGN_BRAND` (gọi `DELETE /internal/brands/watches/{watchId}`) | ✔ |

> **Thứ tự bồi hoàn (LIFO):** Nếu bước 2 (`ASSIGN_BRAND`) thất bại hoặc gặp lỗi mạng ➔ Chạy bồi hoàn bước 1: `DELETE_WATCH`.

---

### 2.2. Saga UPDATE_WATCH (Cập nhật đồng hồ)

Khi Admin cập nhật thông tin đồng hồ và có thể đổi sang thương hiệu khác.

| Bước | Tên Local Transaction | Service / DB | Thao tác thực hiện | Compensating Transaction (Bồi hoàn) | Idempotent? |
|:---:|---|---|---|---|:---:|
| **1** | `UPDATE_WATCH` | MSWatch / DBWatch | Lưu snapshot `before`, ghi đè dữ liệu mới vào bảng `Watches` | `RESTORE_WATCH` (ghi đè lại dữ liệu từ snapshot `before`) | ✔ |
| **2** | `ASSIGN_BRAND` | MSBrand / DBBrand | Cập nhật brand mới vào bảng `BrandWatches` | `ASSIGN_BRAND_BACK` (gán lại `oldBrandId` ban đầu) | ✔ |

> **Thứ tự bồi hoàn (LIFO):** Nếu bước 2 thất bại ➔ Chạy bồi hoàn bước 1 (`RESTORE_WATCH`) để khôi phục trạng thái cũ.

---

### 2.3. Saga DELETE_WATCH (Xoá đồng hồ) – Pivot Step

Khi Admin xoá một chiếc đồng hồ, cần gỡ liên kết khỏi `DBBrand.BrandWatches` và xoá cứng bản ghi khỏi `DBWatch.Watches`.

| Bước | Tên Local Transaction | Service / DB | Thao tác thực hiện | Compensating Transaction (Bồi hoàn) | Idempotent? |
|:---:|---|---|---|---|:---:|
| **1** | `UNASSIGN_BRAND` | MSBrand / DBBrand | Gỡ watch khỏi thương hiệu qua API `DELETE /internal/brands/watches/{watchId}` | `ASSIGN_BRAND_BACK` (gán lại `currentBrandId` cho watch) | ✔ |
| **2** | `DELETE_WATCH` | MSWatch / DBWatch | Hard delete bản ghi khỏi bảng `Watches` | **— PIVOT TRANSACTION —** (Không có bồi hoàn) | — |

> ⚠️ **Vì sao `DELETE_WATCH` là Pivot Transaction?**
> * Trong SQL Server, cột `WatchID` là khoá chính tự tăng (`IDENTITY`). Khi đã xoá cứng (hard-delete) một dòng, không thể INSERT lại một dòng mới mà giữ nguyên được đúng giá trị `WatchID` cũ một cách an toàn.
> * Do đó, một khi `DELETE_WATCH` đã commit, Saga **không thể quay lui (irreversible)** nữa.
> * **Quy tắc thiết kế:** Bước có rủi ro cao (gọi qua HTTP tới MSBrand) phải được thực hiện **trước** (khi còn bồi hoàn được); bước pivot không thể bù trừ bắt buộc phải đặt ở vị trí **cuối cùng**.

---

## 3. Máy trạng thái Saga (State Machine)

```
        ┌────────────────────────────────────────────────────────┐
        │                                                        │
        ▼                                                        ▼
    [PENDING] ──► [EXECUTING] ────────────────────────────► [COMPLETED]
                      │
                      │ (gặp lỗi / timeout)
                      ▼
                   [FAILED]
                      │
                      ▼
               [COMPENSATING] ──(bù trừ xong)────────────► [COMPENSATED]
                      │
                      └─ (thử lại 3 lần vẫn lỗi) ────────► [DỪNG Ở COMPENSATING]
                                                           (cần can thiệp thủ công)
```

* **`PENDING`:** Khởi tạo Saga, sinh `SagaId` (UUID).
* **`EXECUTING`:** Đang lần lượt thực thi các bước local transaction.
* **`COMPLETED`:** Tất cả các bước đều commit thành công. Dữ liệu 2 database nhất quán.
* **`FAILED`:** Một bước bị lỗi (hoặc timeout mạng). Chuyển sang chu trình bồi hoàn.
* **`COMPENSATING`:** Đang chạy các giao dịch bù trừ theo thứ tự LIFO (ngược lại).
* **`COMPENSATED`:** Tất cả các bước đã được bồi hoàn thành công. Dữ liệu 2 database trở về trạng thái nhất quán ban đầu.
* **Kẹt ở `COMPENSATING`:** Nếu thao tác bồi hoàn thất bại sau 3 lần retry, hệ thống dừng lại và log cảnh báo để quản trị viên xử lý, tuyệt đối không tự ý đánh dấu `COMPENSATED`.

---

## 4. Luồng xử lý thất bại điển hình (Ví dụ: Tạo Watch với Brand 999)

```
Client (Postman)         MSWatch (Orchestrator)          DBWatch                 MSBrand (Port 8082)
      │                            │                        │                             │
      │──POST /api/watches (b=999)►│                        │                             │
      │                            │──[1] CREATE_WATCH─────►│                             │
      │                            │   INSERT Watches (id=11) (COMMIT)                    │
      │                            │◄──OK (WatchID=11)──────│                             │
      │                            │                                                      │
      │                            │──[2] ASSIGN_BRAND: PUT /internal/brands/999/watches/11──►│
      │                            │◄──HTTP 404 Brand Not Found───────────────────────────│
      │                            │                                                      │
      │                            │ ⚠️ Bước 2 FAILED -> Chuyển sang COMPENSATING         │
      │                            │                                                      │
      │                            │──[C1] DELETE_WATCH────►│                             │
      │                            │   DELETE Watches WHERE WatchID=11 (COMMIT)           │
      │                            │◄──OK───────────────────│                             │
      │                            │                                                      │
      │                            │ ✔ Hoàn tất bù trừ -> status = COMPENSATED            │
      │◄──HTTP 400 Bad Request─────│                                                      │
          {"sagaStatus": "COMPENSATED", "failedStep": "ASSIGN_BRAND"}
```

* **Bằng chứng nhận biết:** WatchID 11 đã thực sự được INSERT vào SQL Server rồi mới bị xoá. Chiếc đồng hồ tạo thành công tiếp theo sẽ có `WatchID = 12` (ID bị nhảy cóc do auto-increment của SQL Server).

---

## 5. Quyết định thiết kế cốt lõi (TODO-4.2)

### 5.1. Vì sao không dùng `@Transactional` phân tán?
* Annotation `@Transactional` của Spring chỉ quản lý transaction cục bộ trong **một** database context (thông qua một `PlatformTransactionManager`).
* Lời gọi REST HTTP qua OpenFeign sang `MSBrand` nằm hoàn toàn bên ngoài transaction context của `MSWatch`.
* Nếu bước gọi `MSBrand` bị lỗi, Spring chỉ rollback được `DBWatch`, không thể tự động hoàn tác dữ liệu đã lỡ ghi bên `DBBrand`. Hơn nữa, giữ một transaction DB mở trong lúc chờ response mạng là anti-pattern nghiêm trọng (gây nghẽn Connection Pool).

### 5.2. Vì sao không dùng 2PC (Two-Phase Commit / XA)?
* **Hiệu năng & Khả năng mở rộng:** 2PC bắt buộc phải lock tài nguyên trên cả hai database xuyên suốt 2 pha (Prepare và Commit), làm tăng độ trễ và giảm thông lượng (throughput).
* **Single Point of Failure:** Bộ điều phối giao dịch (Transaction Coordinator) là điểm lỗi đơn lẻ. Nếu coordinator sập trong pha commit, các participant bị treo lock.
* **Không tương thích Microservices hiện đại:** Các dịch vụ giao tiếp qua HTTP/REST API không hỗ trợ chuẩn giao thức XA.

### 5.3. Tính chất của Compensating Transaction
* **Idempotent (Cùng kết quả dù gọi nhiều lần):** Do mạng có thể timeout và hệ thống có cơ chế Retry tối đa 3 lần, các API bồi hoàn (như `DELETE /internal/brands/watches/{id}`) phải an toàn khi gọi lặp lại (nếu bản ghi không tồn tại vẫn trả về HTTP 200 `NOTHING_TO_DO`).
* **LIFO Order (Vào sau ra trước):** Bước nào commit sau cùng thì phải được hoàn tác đầu tiên để tránh vi phạm ràng buộc dữ liệu phụ thuộc.
* **Validation trước Saga:** Các kiểm tra validation nghiệp vụ (tên > 10 ký tự, giá >= 100, ngày tạo = hôm nay, kiểm tra danh mục) được thực hiện **trước** khi mở Saga để tránh tạo rác nhật ký `SagaStates`.
