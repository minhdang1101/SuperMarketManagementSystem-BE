# Supermarket Management System

Hệ thống quản lý siêu thị gồm hai ứng dụng:

- **Backend:** Spring Boot, Spring Security, JWT, JPA/Hibernate và SQL Server.
- **Frontend:** React, Vite và Tailwind CSS.

## 1. Mục tiêu project

Supermarket Management System (SMMS) được xây dựng nhằm số hóa các nghiệp vụ vận hành hằng ngày của một siêu thị trên cùng một hệ thống. Project giúp quản lý tập trung dữ liệu bán hàng, hàng hóa, tồn kho, nhà cung cấp, nhân viên và khách hàng; đồng thời giảm thao tác thủ công, hạn chế sai lệch dữ liệu và hỗ trợ người quản lý theo dõi tình hình kinh doanh.

Các mục tiêu chính:

- Hỗ trợ thu ngân thực hiện bán hàng, áp dụng khuyến mãi và thanh toán nhanh chóng.
- Theo dõi sản phẩm, danh mục, barcode và số lượng tồn kho theo thời gian thực.
- Quản lý quy trình nhập hàng từ nhà cung cấp, đơn mua hàng và phiếu nhận hàng.
- Quản lý nhân viên, ca làm việc, khách hàng thành viên và phân quyền theo vai trò.
- Cung cấp dashboard, lịch sử bán hàng và báo cáo để hỗ trợ ra quyết định.
- Tích hợp các dịch vụ mở rộng như VNPay, email và trợ lý AI.
- Cung cấp REST API có xác thực JWT để frontend và các client khác có thể tích hợp.

## 2. Chức năng chính

- Đăng ký, đăng nhập, đặt lại mật khẩu và quản lý hồ sơ cá nhân.
- Phân quyền người dùng theo các vai trò `ADMIN`, `MANAGER` và `CASHIER`.
- Quản lý sản phẩm, danh mục, barcode, nhà cung cấp và chương trình khuyến mãi.
- Quản lý đơn đặt hàng, nhận hàng, điều chỉnh tồn kho và báo cáo tồn kho.
- Bán hàng tại quầy, quản lý khách hàng thành viên và xem lịch sử giao dịch.
- Quản lý nhân viên, ca làm việc và cấu hình hệ thống.
- Dashboard và báo cáo phục vụ theo dõi hoạt động kinh doanh.
- Thanh toán VNPay sandbox, gửi email và trò chuyện với trợ lý AI khi đã cấu hình dịch vụ tương ứng.
- Swagger UI hỗ trợ xem và thử REST API trực tiếp trên trình duyệt.

## 3. Yêu cầu môi trường

Cài đặt các công cụ sau trước khi chạy dự án:

| Công cụ | Phiên bản |
| --- | --- |
| Java JDK | 17 |
| Node.js | `^20.19.0` hoặc `>=22.12.0` |
| npm | Đi kèm với Node.js |
| SQL Server | SQL Server 2019 trở lên được khuyến nghị |
| Git | Phiên bản mới nhất |

Không cần cài Maven riêng vì backend đã có Maven Wrapper.

Kiểm tra môi trường:

```bash
java -version
node --version
npm --version
git --version
```

## 4. Tải source code

Đặt hai repository trong cùng một thư mục cha để dễ thao tác:

```text
workspace/
├── SuperMarketManagementSystem-BE/
└── supermarket-FE/
```

```bash
git clone https://github.com/minhdang1101/SuperMarketManagementSystem-BE.git
git clone https://github.com/minhdang1101/supermarket-FE.git
```

## 5. Chuẩn bị SQL Server

1. Khởi động SQL Server và bảo đảm TCP/IP đã được bật ở cổng `1433`.
2. Mở SQL Server Management Studio (SSMS) hoặc Azure Data Studio.
3. Tạo database bằng câu lệnh:

```sql
IF DB_ID(N'SMMS') IS NULL
    CREATE DATABASE [SMMS];
GO
```

Ứng dụng sử dụng `spring.jpa.hibernate.ddl-auto=update`, vì vậy các bảng sẽ được tự động tạo/cập nhật khi backend kết nối database thành công. Database `SMMS` vẫn phải được tạo trước.

## 6. Cấu hình và chạy Backend

Di chuyển vào thư mục backend:

```bash
cd SuperMarketManagementSystem-BE
```

Tạo file cấu hình cục bộ từ file mẫu.

PowerShell:

```powershell
Copy-Item .env.example .env
```

macOS/Linux:

```bash
cp .env.example .env
```

Mở `.env` và cập nhật tối thiểu các giá trị sau:

```properties
DB_URL=jdbc:sqlserver://localhost:1433;databaseName=SMMS;encrypt=false
DB_USERNAME=sa
DB_PASSWORD=your_local_database_password
JWT_SECRET=replace_with_a_long_random_secret
```

Các nhóm cấu hình còn lại chỉ cần thiết khi sử dụng tính năng tương ứng:

| Nhóm | Biến môi trường | Mục đích |
| --- | --- | --- |
| Email | `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | Gửi email từ hệ thống |
| VNPay | `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET`, `VNPAY_PAY_URL`, `VNPAY_RETURN_URL`, `VNPAY_API_URL`, `VNPAY_VERSION`, `VNPAY_COMMAND`, `VNPAY_ORDER_TYPE` | Thanh toán VNPay sandbox |
| AI | `AI_PROVIDER`, `AI_API_URL`, `AI_API_KEY`, `AI_MODEL`, `AI_TIMEOUT_SECONDS` | Trợ lý AI; mặc định dùng Gemini |

Ví dụ cấu hình AI có thể bổ sung vào `.env`:

```properties
AI_PROVIDER=gemini
AI_API_URL=https://generativelanguage.googleapis.com/v1beta
AI_API_KEY=your_gemini_api_key
AI_MODEL=gemini-2.5-flash
AI_TIMEOUT_SECONDS=30
```

> Không commit file `.env` hoặc các khóa bí mật lên Git.

Chạy backend trên Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Chạy backend trên macOS/Linux:

```bash
./mvnw spring-boot:run
```

Backend khởi động tại `http://localhost:8080`. Có thể kiểm tra tài liệu API tại:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

### Nạp dữ liệu demo

Sau khi backend đã khởi động thành công ít nhất một lần và Hibernate đã tạo schema:

1. Mở SSMS và chọn database `SMMS`.
2. Chạy một trong hai script:
   - `src/main/resources/data-seed-login.sql`: chỉ tạo role và tài khoản đăng nhập.
   - `src/main/resources/data-seed-all.sql`: tạo bộ dữ liệu demo đầy đủ.
3. Khởi động lại backend nếu cần.

Các tài khoản demo chính:

| Username | Password | Role |
| --- | --- | --- |
| `admin` | `password` | ADMIN |
| `manager` | `password` | MANAGER |
| `cashier` | `password` | CASHIER |

Script dữ liệu đầy đủ còn tạo thêm các tài khoản phục vụ demo nghiệp vụ. Tất cả tài khoản demo đang hoạt động trong script này dùng mật khẩu `password`.

## 7. Cấu hình và chạy Frontend

Mở terminal thứ hai và di chuyển vào thư mục frontend:

```bash
cd supermarket-FE
```

Tạo file môi trường:

PowerShell:

```powershell
Copy-Item .env.example .env
```

macOS/Linux:

```bash
cp .env.example .env
```

Cấu hình mặc định dùng proxy phát triển của Vite:

```properties
VITE_API_BASE_URL=/api/v1
```

Cài dependency theo đúng `package-lock.json` và chạy FE:

```bash
npm ci
npm run dev
```

Mở <http://localhost:5173> trên trình duyệt. Khi chạy ở chế độ development, Vite sẽ chuyển tiếp các request `/api` tới `http://localhost:8080`, vì vậy backend phải đang chạy.

## 8. Thứ tự chạy nhanh

Mỗi dịch vụ cần một terminal riêng:

1. Khởi động SQL Server.
2. Terminal 1: chạy backend bằng `./mvnw spring-boot:run` hoặc `.\mvnw.cmd spring-boot:run`.
3. Nếu là lần chạy đầu, nạp một trong các script dữ liệu demo vào database `SMMS`.
4. Terminal 2: chạy frontend bằng `npm run dev`.
5. Truy cập <http://localhost:5173> và đăng nhập bằng tài khoản demo.

## 9. Kiểm tra và build

Backend:

```bash
# Windows
.\mvnw.cmd test
.\mvnw.cmd clean package

# macOS/Linux
./mvnw test
./mvnw clean package
```

File JAR sau khi build nằm trong thư mục `target/`. Chạy JAR bằng lệnh:

```bash
java -jar target/SuperMarketManagementSystem-0.0.1-SNAPSHOT.jar
```

Frontend:

```bash
npm run lint
npm run build
npm run preview
```

Kết quả build frontend nằm trong thư mục `dist/`.

## 10. Xử lý lỗi thường gặp

### Backend không kết nối được SQL Server

- Kiểm tra SQL Server đang chạy và database `SMMS` đã tồn tại.
- Kiểm tra lại `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` trong `.env`.
- Bật SQL Server Authentication nếu sử dụng tài khoản `sa`.
- Bật TCP/IP và cổng `1433`, sau đó khởi động lại SQL Server.
- Nếu môi trường yêu cầu mã hóa, bổ sung các tham số chứng chỉ phù hợp vào `DB_URL`; cấu hình local mẫu đang dùng `encrypt=false`.

### FE gọi API thất bại

- Kiểm tra backend đang chạy tại `http://localhost:8080`.
- Giữ `VITE_API_BASE_URL=/api/v1` khi chạy `npm run dev` để sử dụng Vite proxy.
- Khởi động lại Vite sau khi thay đổi `.env`.
- Nếu đổi cổng backend, cập nhật `target` trong `vite.config.js`.

### Cổng đã được sử dụng

- Backend mặc định dùng cổng `8080`.
- Frontend mặc định dùng cổng `5173`.
- Dừng tiến trình đang chiếm cổng hoặc cập nhật cấu hình của dịch vụ tương ứng. Nếu đổi cổng FE, cần bảo đảm origin mới được backend cho phép.

### Maven Wrapper không chạy trên macOS/Linux

Cấp quyền thực thi rồi chạy lại:

```bash
chmod +x mvnw
./mvnw spring-boot:run
```
