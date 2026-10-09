# KẾ HOẠCH TRIỂN KHAI VÀ PHÂN TÍCH CHI TIẾT PART 5: API DOCUMENTATION (SWAGGER & SPRINGDOC OPENAPI)

> **Môn học:** MSS301 - Microservices with Spring Boot  
> **Giai đoạn:** Slot 8 / Part 5 — API Documentation & Aggregation  
> **Thời gian tạo:** 2026-10-09  
> **Mục tiêu:** Tích hợp Springdoc OpenAPI & Swagger UI cho 3 services (`product-service`, `inventory-service`, `order-service`) và tổng hợp API docs tập trung tại `api-gateway`.

---

## 1. TỔNG QUAN VÀ KIẾN TRÚC HỆ THỐNG

### 1.1. Luồng hoạt động API Documentation trong Microservices
```
                       ┌──────────────────────────────────────────────┐
                       │               Client / Browser               │
                       └──────────────────────┬───────────────────────┘
                                              │ GET /swagger-ui.html
                                              │ (Không cần JWT Token)
                                              ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                               API GATEWAY (Port 9000)                                  │
│                                                                                        │
│  - Swagger UI tập trung tại: /swagger-ui.html                                          │
│  - Dropdown Menu:                                                                      │
│      [1] Product Service   --> /aggregate/product-service/v3/api-docs                  │
│      [2] Order Service     --> /aggregate/order-service/v3/api-docs                    │
│      [3] Inventory Service --> /aggregate/inventory-service/v3/api-docs                  │
│                                                                                        │
│  - Routes rewrite: setPath("/api-docs") forward đến từng Service đích                 │
│  - SecurityConfig: permitAll cho /swagger-ui/**, /v3/api-docs/**, /aggregate/**        │
└──────────────┬──────────────────────────────┬─────────────────────────────┬────────────┘
               │                              │                             │
               │ (rewrite: /api-docs)         │ (rewrite: /api-docs)        │ (rewrite: /api-docs)
               ▼                              ▼                             ▼
   ┌───────────────────────┐      ┌───────────────────────┐     ┌───────────────────────┐
   │    Product Service    │      │     Order Service     │     │   Inventory Service   │
   │      (Port 8080)      │      │      (Port 8081)      │     │      (Port 8082)      │
   ├───────────────────────┤      ├───────────────────────┤     ├───────────────────────┤
   │ - springdoc-webmvc-ui │      │ - springdoc-webmvc-ui │     │ - springdoc-webmvc-ui │
   │ - OpenAPIConfig bean  │      │ - OpenAPIConfig bean  │     │ - OpenAPIConfig bean  │
   │ - CorsConfig WebMvc   │      │ - CorsConfig WebMvc   │     │ - CorsConfig WebMvc   │
   │ - Docs tại: /api-docs │      │ - Docs tại: /api-docs │     │ - Docs tại: /api-docs │
   └───────────────────────┘      └───────────────────────┘     └───────────────────────┘
```

---

## 2. PHÂN TÍCH HIỆN TRẠNG MÃ NGUỒN VÀ CÁC ĐIỂM CẦN LƯU Ý ĐẶC BIỆT

Qua việc rà soát trực tiếp cây thư mục và mã nguồn tại `e:\A_Ki8\mss\MSS301\slot8`, phát hiện các điểm sai lệch và bẫy lỗi quan trọng:

| Dịch vụ | Hiện trạng trong Codebase | Lưu ý / Bẫy lỗi phát hiện (Gotchas) | Giải pháp xử lý |
| :--- | :--- | :--- | :--- |
| **`product-service`** | 1. Package gốc là `com.fudn.product_service` (có dấu gạch dưới `_`).<br>2. Cấu hình dùng `application.yaml` (không có file `.properties`). | **Bẫy 1:** Trong tài liệu hướng dẫn ghi `package com.fudn.productservice.config`. Nếu làm đúng như tài liệu thì Spring Boot Component Scan sẽ **bỏ qua**, dẫn tới không load được Bean `productServiceAPI`.<br>**Bẫy 2:** Không có sẵn `application.properties` để bỏ comment. | 1. Đặt đúng package `com.fudn.product_service.config` cho `OpenAPIConfig` và `CorsConfig`.<br>2. Bổ sung cấu hình `springdoc` trực tiếp vào `application.yaml` (hoặc tạo `application.properties`). |
| **`inventory-service`** | Package gốc là `com.fudn.inventoryservice`. Đã có `application.properties`. | Chưa có dependencies `springdoc` và cấu hình đường dẫn. | Bổ sung dependencies vào `pom.xml`, thêm 2 config vào `application.properties`, tạo 2 file config. |
| **`order-service`** | Package gốc là `com.fudn.orderservice`. Đã có `application.properties`. | Chưa có dependencies `springdoc` và cấu hình đường dẫn. | Bổ sung dependencies vào `pom.xml`, thêm 2 config vào `application.properties`, tạo 2 file config. |
| **`api-gateway`** | Đang có sẵn test `ApiGatewaySecurityTests` kiểm tra `/actuator/health` được public. | **Bẫy 3:** Nếu ghi đè `freeResourceUrls` theo tài liệu mà quên `/actuator/health`, test `healthEndpointIsPublic()` hiện có sẽ bị **FAIL (401)**. | Bổ sung đầy đủ cả các URL swagger VÀ `/actuator/health`, `/actuator/health/**` vào `freeResourceUrls`. |

---

## 3. DANH SÁCH CHI TIẾT 16 TODO CẦN THỰC HIỆN

### NHÓM A: Product Service (Port 8080)
1. **`DOC-1`** · `product-service/pom.xml`: Thêm 2 dependencies `springdoc-openapi-starter-webmvc-ui` và `springdoc-openapi-starter-webmvc-api` (version 2.5.0).
2. **`DOC-2`** · `product-service/src/main/resources/application.yaml`: Cấu hình `springdoc.swagger-ui.path=/swagger-ui.html` và `springdoc.api-docs.path=/api-docs`.
3. **`DOC-3`** · Tạo mới `product-service/src/main/java/com/fudn/product_service/config/OpenAPIConfig.java`: Khai báo Bean `productServiceAPI` trả về OpenAPI với title `"Product Service API"` và version `"v0.0.1"`.
4. **`DOC-4`** · Tạo mới `product-service/src/main/java/com/fudn/product_service/config/CorsConfig.java`: Cho phép CORS trên pattern `/api/**` để Swagger UI từ Gateway có thể gọi trực tiếp.

### NHÓM B: Inventory Service (Port 8082)
5. **`DOC-5`** · `inventory-service/pom.xml`: Thêm 2 dependencies `springdoc-openapi-starter-webmvc-ui` và `springdoc-openapi-starter-webmvc-api` (version 2.5.0).
6. **`DOC-6`** · `inventory-service/src/main/resources/application.properties`: Thêm cấu hình Swagger UI path và API Docs path.
7. **`DOC-7`** · Tạo mới `inventory-service/src/main/java/com/fudn/inventoryservice/config/OpenAPIConfig.java`: Khai báo Bean `inventoryServiceAPI` với title `"Inventory Service API"` và version `"v0.0.1"`.
8. **`DOC-8`** · Tạo mới `inventory-service/src/main/java/com/fudn/inventoryservice/config/CorsConfig.java`: Cho phép CORS trên pattern `/api/**`.

### NHÓM C: Order Service (Port 8081)
9. **`DOC-9`** · `order-service/pom.xml`: Thêm 2 dependencies `springdoc-openapi-starter-webmvc-ui` và `springdoc-openapi-starter-webmvc-api` (version 2.5.0).
10. **`DOC-10`** · `order-service/src/main/resources/application.properties`: Thêm cấu hình Swagger UI path và API Docs path.
11. **`DOC-11`** · Tạo mới `order-service/src/main/java/com/fudn/orderservice/config/OpenAPIConfig.java`: Khai báo Bean `orderServiceAPI` với title `"Order Service API"` và version `"v0.0.1"`.
12. **`DOC-12`** · Tạo mới `order-service/src/main/java/com/fudn/orderservice/config/CorsConfig.java`: Cho phép CORS trên pattern `/api/**`.

### NHÓM D: API Gateway (Port 9000)
13. **`DOC-13`** · `api-gateway/pom.xml`: Thêm 2 dependencies `springdoc-openapi-starter-webmvc-ui` và `springdoc-openapi-starter-webmvc-api` (version 2.5.0).
14. **`DOC-14`** · `api-gateway/src/main/resources/application.properties`: Khai báo danh sách 3 URL aggregate (`urls[0]`, `urls[1]`, `urls[2]`).
15. **`DOC-15`** · `api-gateway/src/main/java/com/fudn/gateway/routes/Routes.java`: Khai báo 3 RouterFunction beans để route `/aggregate/{service}/v3/api-docs` sang các service tương ứng kèm rewrite filter `setPath("/api-docs")`.
16. **`DOC-16`** · `api-gateway/src/main/java/com/fudn/gateway/config/SecurityConfig.java`:
    - `DOC-16a`: Khai báo `freeResourceUrls` (Swagger + Aggregate + Actuator Health).
    - `DOC-16b`: Cập nhật `securityFilterChain` cho phép `freeResourceUrls` permitAll, tích hợp CORS source.
    - `DOC-16c`: Khai báo Bean `corsConfigurationSource()`.

---

## 4. CHI TIẾT MÃ NGUỒN CẦN TRIỂN KHAI CHO TỪNG BƯỚC

### 4.1. Nhóm A: Product Service

#### [DOC-1] File: `product-service/pom.xml`
Thêm vào thẻ `<dependencies>`:
```xml
		<!-- SpringDoc OpenAPI Swagger UI -->
		<dependency>
			<groupId>org.springdoc</groupId>
			<artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
			<version>2.5.0</version>
		</dependency>
		<dependency>
			<groupId>org.springdoc</groupId>
			<artifactId>springdoc-openapi-starter-webmvc-api</artifactId>
			<version>2.5.0</version>
		</dependency>
```

#### [DOC-2] File: `product-service/src/main/resources/application.yaml`
Thêm đoạn sau vào file:
```yaml
springdoc:
  swagger-ui:
    path: /swagger-ui.html
  api-docs:
    path: /api-docs
```

#### [DOC-3] File mới: `product-service/src/main/java/com/fudn/product_service/config/OpenAPIConfig.java`
```java
package com.fudn.product_service.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI productServiceAPI() {
        return new OpenAPI()
                .info(new Info().title("Product Service API")
                        .description("This is the REST API for Product Service")
                        .version("v0.0.1")
                        .license(new License().name("Apache 2.0")))
                .externalDocs(new ExternalDocumentation()
                        .description("Product Service Wiki Documentation")
                        .url("https://fudn-product-service-dummy-url.com/docs"));
    }
}
```

#### [DOC-4] File mới: `product-service/src/main/java/com/fudn/product_service/config/CorsConfig.java`
```java
package com.fudn.product_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowedOriginPatterns("*")
                .allowCredentials(false);
    }
}
```

---

### 4.2. Nhóm B: Inventory Service

#### [DOC-5] File: `inventory-service/pom.xml`
Thêm vào `<dependencies>`:
```xml
		<!-- SpringDoc OpenAPI Swagger UI -->
		<dependency>
			<groupId>org.springdoc</groupId>
			<artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
			<version>2.5.0</version>
		</dependency>
		<dependency>
			<groupId>org.springdoc</groupId>
			<artifactId>springdoc-openapi-starter-webmvc-api</artifactId>
			<version>2.5.0</version>
		</dependency>
```

#### [DOC-6] File: `inventory-service/src/main/resources/application.properties`
Thêm vào cuối file:
```properties
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.api-docs.path=/api-docs
```

#### [DOC-7] File mới: `inventory-service/src/main/java/com/fudn/inventoryservice/config/OpenAPIConfig.java`
```java
package com.fudn.inventoryservice.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI inventoryServiceAPI() {
        return new OpenAPI()
                .info(new Info().title("Inventory Service API")
                        .description("This is the REST API for Inventory Service")
                        .version("v0.0.1")
                        .license(new License().name("Apache 2.0")))
                .externalDocs(new ExternalDocumentation()
                        .description("Inventory Service Wiki Documentation")
                        .url("https://fudn-inventory-service-dummy-url.com/docs"));
    }
}
```

#### [DOC-8] File mới: `inventory-service/src/main/java/com/fudn/inventoryservice/config/CorsConfig.java`
```java
package com.fudn.inventoryservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowedOriginPatterns("*")
                .allowCredentials(false);
    }
}
```

---

### 4.3. Nhóm C: Order Service

#### [DOC-9] File: `order-service/pom.xml`
Thêm vào `<dependencies>`:
```xml
		<!-- SpringDoc OpenAPI Swagger UI -->
		<dependency>
			<groupId>org.springdoc</groupId>
			<artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
			<version>2.5.0</version>
		</dependency>
		<dependency>
			<groupId>org.springdoc</groupId>
			<artifactId>springdoc-openapi-starter-webmvc-api</artifactId>
			<version>2.5.0</version>
		</dependency>
```

#### [DOC-10] File: `order-service/src/main/resources/application.properties`
Thêm vào cuối file:
```properties
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.api-docs.path=/api-docs
```

#### [DOC-11] File mới: `order-service/src/main/java/com/fudn/orderservice/config/OpenAPIConfig.java`
```java
package com.fudn.orderservice.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI orderServiceAPI() {
        return new OpenAPI()
                .info(new Info().title("Order Service API")
                        .description("This is the REST API for Order Service")
                        .version("v0.0.1")
                        .license(new License().name("Apache 2.0")))
                .externalDocs(new ExternalDocumentation()
                        .description("Order Service Wiki Documentation")
                        .url("https://fudn-order-service-dummy-url.com/docs"));
    }
}
```

#### [DOC-12] File mới: `order-service/src/main/java/com/fudn/orderservice/config/CorsConfig.java`
```java
package com.fudn.orderservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowedOriginPatterns("*")
                .allowCredentials(false);
    }
}
```

---

### 4.4. Nhóm D: API Gateway

#### [DOC-13] File: `api-gateway/pom.xml`
Thêm vào `<dependencies>`:
```xml
        <!-- SpringDoc OpenAPI Swagger UI -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>2.5.0</version>
        </dependency>
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-api</artifactId>
            <version>2.5.0</version>
        </dependency>
```

#### [DOC-14] File: `api-gateway/src/main/resources/application.properties`
Thêm vào cuối file:
```properties
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.enabled=true
springdoc.api-docs.enabled=true
springdoc.swagger-ui.urls[0].name=Product Service
springdoc.swagger-ui.urls[0].url=/aggregate/product-service/v3/api-docs
springdoc.swagger-ui.urls[1].name=Order Service
springdoc.swagger-ui.urls[1].url=/aggregate/order-service/v3/api-docs
springdoc.swagger-ui.urls[2].name=Inventory Service
springdoc.swagger-ui.urls[2].url=/aggregate/inventory-service/v3/api-docs
```

#### [DOC-15] File: `api-gateway/src/main/java/com/fudn/gateway/routes/Routes.java`
Thêm các imports:
```java
import static org.springframework.cloud.gateway.server.mvc.filter.FilterFunctions.setPath;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;
```
Và thêm 3 Bean routes (DOC-15a, DOC-15b, DOC-15c):
```java
    @Bean
    public RouterFunction<ServerResponse> productServiceSwaggerRoute() {
        return route("product_service_swagger")
                .route(path("/aggregate/product-service/v3/api-docs"),
                        http(productServiceUrl))
                .filter(setPath("/api-docs"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> orderServiceSwaggerRoute() {
        return route("order_service_swagger")
                .route(path("/aggregate/order-service/v3/api-docs"),
                        http(orderServiceUrl))
                .filter(setPath("/api-docs"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> inventoryServiceSwaggerRoute() {
        return route("inventory_service_swagger")
                .route(path("/aggregate/inventory-service/v3/api-docs"),
                        http(inventoryServiceUrl))
                .filter(setPath("/api-docs"))
                .build();
    }
```
*(Ghi chú: Dùng `productServiceUrl`, `orderServiceUrl`, `inventoryServiceUrl` giúp hỗ trợ chạy test WireMock linh động).*

#### [DOC-16] File: `api-gateway/src/main/java/com/fudn/gateway/config/SecurityConfig.java`
Cập nhật file thành:
```java
package com.fudn.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // DOC-16a: Gom cac resource duoc permitAll (Swagger + Aggregator + Actuator Health)
    private final String[] freeResourceUrls = {
            "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
            "/swagger-resources/**", "/aggregate/**",
            "/actuator/health", "/actuator/health/**"
    };

    // DOC-16b: Cấu hình phân quyền và CORS
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(freeResourceUrls).permitAll()
                        .anyRequest().authenticated())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .build();
    }

    // DOC-16c: Bean cấu hình CORS
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST"));
        configuration.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

---

## 5. KẾ HOẠCH VÀ KỊCH BẢN KIỂM THỬ (TESTING & VERIFICATION)

### 5.1. Kiểm thử biên dịch (Compilation Test)
Thực hiện lệnh compile trên từng service để đảm bảo không lỗi syntax hay thiếu thư viện:
```powershell
cd e:\A_Ki8\mss\MSS301\slot8\product-service;   .\mvnw.cmd test-compile
cd e:\A_Ki8\mss\MSS301\slot8\inventory-service; .\mvnw.cmd test-compile
cd e:\A_Ki8\mss\MSS301\slot8\order-service;     .\mvnw.cmd test-compile
cd e:\A_Ki8\mss\MSS301\slot8\api-gateway;       .\mvnw.cmd test-compile
```

### 5.2. Chạy Automated Tests hiện có
Kiểm tra test hiện có ở API Gateway không bị ảnh hưởng:
```powershell
cd e:\A_Ki8\mss\MSS301\slot8\api-gateway; .\mvnw.cmd test
```
Đảm bảo tất cả test:
- `healthEndpointIsPublic()`: Status 200 OK
- `requestWithoutTokenShouldReturn401()`: Status 401 Unauthorized
- `requestWithValidJwtShouldBeRoutedToProductService()`: Status 200 OK
- `postOrderWithValidJwtShouldBeRoutedToOrderService()`: Status 201 Created
- `inventoryRouteShouldForwardQueryParams()`: Status 200 OK

### 5.3. Kiểm thử tích hợp Swagger (Manual E2E)
Khi khởi động hệ sinh thái microservices:
1. Mở trình duyệt vào `http://localhost:9000/swagger-ui.html` không cần đăng nhập/JWT token.
2. Kiểm tra Dropdown trên góc phải có đủ 3 options:
   - **Product Service** (trỏ tới `/aggregate/product-service/v3/api-docs`)
   - **Order Service** (trỏ tới `/aggregate/order-service/v3/api-docs`)
   - **Inventory Service** (trỏ tới `/aggregate/inventory-service/v3/api-docs`)
3. Chọn từng service và xác nhận hiển thị chính xác tiêu đề, mô tả và version `v0.0.1`.
4. Gọi trực tiếp `http://localhost:9000/api/products` không token -> trả về HTTP `401 Unauthorized`.

---

## 6. CHIẾN LƯỢC COMMIT MESSAGE CHUẨN CONVENTIONAL COMMITS

Lịch sử git của repository tuân thủ chặt chẽ chuẩn **Conventional Commits**:
- `feat(scope): message`
- `build(scope): message`
- `chore(scope): message`
- `docs: message`

Tùy vào quy trình làm việc của bạn (commit từng bước hoặc commit tổng), dưới đây là 2 kịch bản commit chuẩn xác:

### KỊCH BẢN 1: Commit theo từng Service (Khuyến nghị - Rõ ràng, dễ Review)

#### Bước 1: Commit Product Service (DOC-1 → DOC-4)
```bash
git add slot8/product-service/pom.xml slot8/product-service/src/main/resources/application.yaml slot8/product-service/src/main/java/com/fudn/product_service/config/
git commit -m "feat(product): add openapi springdoc config and cors support"
```

#### Bước 2: Commit Inventory Service (DOC-5 → DOC-8)
```bash
git add slot8/inventory-service/pom.xml slot8/inventory-service/src/main/resources/application.properties slot8/inventory-service/src/main/java/com/fudn/inventoryservice/config/
git commit -m "feat(inventory): add openapi springdoc config and cors support"
```

#### Bước 3: Commit Order Service (DOC-9 → DOC-12)
```bash
git add slot8/order-service/pom.xml slot8/order-service/src/main/resources/application.properties slot8/order-service/src/main/java/com/fudn/orderservice/config/
git commit -m "feat(order): add openapi springdoc config and cors support"
```

#### Bước 4: Commit API Gateway (DOC-13 → DOC-16)
```bash
git add slot8/api-gateway/pom.xml slot8/api-gateway/src/main/resources/application.properties slot8/api-gateway/src/main/java/com/fudn/gateway/routes/Routes.java slot8/api-gateway/src/main/java/com/fudn/gateway/config/SecurityConfig.java
git commit -m "feat(gateway): aggregate swagger docs and configure security cors"
```

#### Bước 5: Commit Tài liệu Kế hoạch (File Markdown)
```bash
git add slot8/PLAN_PART5_SWAGGER_DOCS.md
git commit -m "docs(slot8): add part 5 swagger implementation plan and analysis"
```

---

### KỊCH BẢN 2: Commit Đơn lẻ cho toàn bộ Part 5 (Single Batch Commit)
Nếu bạn muốn đóng gói toàn bộ slot8 trong một lần commit duy nhất:
```bash
git add slot8/
git commit -m "feat(docs): implement springdoc openapi swagger aggregation across microservices"
```
Hoặc:
```bash
git commit -m "feat(slot8): implement part 5 api documentation and gateway aggregation"
```

---

## 7. CHECKLIST HOÀN THÀNH

- [x] DOC-1: Thêm dependency SpringDoc vào `product-service/pom.xml`
- [x] DOC-2: Cấu hình `springdoc` trong `product-service/src/main/resources/application.yaml`
- [x] DOC-3: Tạo `OpenAPIConfig.java` trong `com.fudn.product_service.config`
- [x] DOC-4: Tạo `CorsConfig.java` trong `com.fudn.product_service.config`
- [x] DOC-5: Thêm dependency SpringDoc vào `inventory-service/pom.xml`
- [x] DOC-6: Cấu hình `springdoc` trong `inventory-service/src/main/resources/application.properties`
- [x] DOC-7: Tạo `OpenAPIConfig.java` trong `com.fudn.inventoryservice.config`
- [x] DOC-8: Tạo `CorsConfig.java` trong `com.fudn.inventoryservice.config`
- [x] DOC-9: Thêm dependency SpringDoc vào `order-service/pom.xml`
- [x] DOC-10: Cấu hình `springdoc` trong `order-service/src/main/resources/application.properties`
- [x] DOC-11: Tạo `OpenAPIConfig.java` trong `com.fudn.orderservice.config`
- [x] DOC-12: Tạo `CorsConfig.java` trong `com.fudn.orderservice.config`
- [x] DOC-13: Thêm dependency SpringDoc vào `api-gateway/pom.xml`
- [x] DOC-14: Cấu hình `springdoc.swagger-ui.urls` trong `api-gateway/src/main/resources/application.properties`
- [x] DOC-15: Thêm 3 RouterFunction Beans và filter rewrite vào `api-gateway/.../Routes.java`
- [x] DOC-16: Cấu hình `SecurityConfig.java` cho Gateway (freeResourceUrls + CORS)
- [x] Test compile và build test các services (Product Service `compile: SUCCESS`, Inventory Service `test-compile: SUCCESS`, Order Service `test-compile: SUCCESS`, API Gateway `test: 5/5 PASSED`).
