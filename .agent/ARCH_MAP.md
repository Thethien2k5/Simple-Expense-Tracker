# ARCH_MAP - BẢN ĐỒ KIẾN TRÚC HỆ THỐNG

> **Mục đích:** Tài liệu sống đóng vai trò là nguồn sự thật duy nhất cho bối cảnh kỹ thuật của dự án. AI phải đọc trước mỗi tác vụ lập trình và cập nhật ngay sau đó.

---

## 1. Tổng quan Công nghệ (Tech Stack)

- **Ngôn ngữ:** Kotlin
- **Framework UI:** Jetpack Compose (100%)
- **Kiến trúc:** Clean Architecture + MVVM
- **Dependency Injection:** Hilt
- **Cơ sở dữ liệu:** Room
- **Xử lý nền:** WorkManager (đã cấu hình dependency)
- **Điều hiện hướng:** Jetpack Navigation Compose (đã cấu hình dependency)

---

## 2. Bản đồ Thư mục & Trách nhiệm

- `app/src/main/java/com/T2V/simple_expense_tracker/`:
  - `ExpenseTrackerApp.kt`: Lớp Application khởi tạo Hilt.
  - `MainActivity.kt`: Entry point với hệ thống Double Navigation Drawers (Tùy chỉnh bên trái & Thông báo bên phải) bao bọc nội dung chính (Dashboard).
  - `/di`: Các Module của Dagger Hilt để cung cấp các phụ thuộc (`DatabaseModule` và `RepositoryModule` đã triển khai).
  - `/data`: Tầng dữ liệu, bao gồm:
    - `/local`: Quản lý Room Database (`AppDatabase`), các DAO (`/dao`) và thực thể cơ sở dữ liệu (`/entity`).
    - `/mapper`: Ánh xạ dữ liệu giữa Database Entities và Domain Models.
    - `/repository`: Triển khai các Repository interface từ tầng domain.
  - `/domain`: Tầng nghiệp vụ cốt lõi (Business Logic):
    - `/model`: Định nghĩa cấu trúc dữ liệu thuần túy (Domain Models).
    - `/repository`: Khai báo các interface của Repository.
    - `/usecase`: Định nghĩa các kịch bản sử dụng (Use Cases) của hệ thống.
  - `/ui`: Tầng giao diện người dùng (MVVM) sử dụng Jetpack Compose:
    - `/dashboard`: Màn hình chính với 5 section (Số dư, Chờ xử lý, Gần đây, Chi tiết, Thống kê), bao gồm các components biểu đồ (`ChartComponents`) và dialogs.
    - `/ledger`: Giao diện danh sách thông báo gốc (NotificationPanel - Drawer phải).
    - `/settings`: Giao diện Tùy chỉnh (SettingsPanel - Drawer trái).
    - `/theme`: Bảng màu thiết kế Material3 Custom Dark Theme, typography (tương thích Geist).
  - `/service` *(Chưa triển khai)*: Dịch vụ nghe thông báo (`NotificationListenerService`) và các tác vụ nền (`WorkManager`).

---

## 3. Các Thực thể Dữ liệu Cốt lõi

- **BankAccount** / **BankAccountEntity**: Thông tin về tài khoản ngân hàng được theo dõi.
- **Transaction** / **TransactionEntity**: Chi tiết về một giao dịch tài chính (số tiền, ngày tháng, nội dung).
- **Category** / **CategoryEntity**: Danh mục chi tiêu (ví dụ: Ăn uống, Di chuyển).
- **AutoRule** / **AutoRuleEntity**: Quy tắc heuristic để tự động phân loại giao dịch từ thông báo.
- **RawNotification** / **RawNotificationEntity**: Lưu trữ thông tin thô của thông báo hệ thống nhận được trước khi xử lý phân tích.

---

## 4. Luồng Dữ liệu Quan trọng

- **Luồng Phân tích Thông báo (Dự kiến):** `NotificationListenerService` -> `ParseNotificationUseCase` -> `SaveTransactionUseCase` -> Room DB.
- **Luồng Hiển thị:** Room DB -> Repository -> UseCases -> ViewModel (StateFlow) -> Compose UI. Dữ liệu được theo dõi thời gian thực thông qua Kotlin Flows.

---

## 5. Quy ước & Ràng buộc

- **Ngôn ngữ:** Tất cả chú thích và giải thích trong mã nguồn phải bằng **Tiếng Việt**.
- **UI:** Không sử dụng XML layouts, 100% Jetpack Compose.
- **Dependency Injection:** Sử dụng Hilt để quản lý sự phụ thuộc giữa các lớp.
- **Địa phương hóa:** Ứng dụng hoạt động 100% local, không yêu cầu internet cho xử lý cơ bản.

---

## 6. Nhật ký Thay đổi & Lộ trình

- `[2024-05-22]` - `[Khởi tạo]`: Thiết lập cấu trúc dự án theo Clean Architecture, nâng cấp Version Catalog và cấu hình Hilt/Room.
- `[2024-05-22]` - `[Thêm]`: Triển khai toàn bộ tầng dữ liệu (Entities, DAOs, AppDatabase) theo sơ đồ DBML.
- `[2026-05-25]` - `[Cập nhật]`: Triển khai các Hilt Modules và UseCases cơ bản của Domain.
- `[2026-05-25]` - `[Hoàn thành UI]`: Hoàn thiện toàn bộ giao diện dựa trên thiết kế mẫu. Xây dựng MainActivity với 2 Drawers, cấu hình Custom Dark Theme, và tạo chi tiết 5 Sections trong Dashboard. Liên kết ViewModel để hiển thị luồng dữ liệu thời gian thực.
- `[TODO]`:
  1. Triển khai dịch vụ `NotificationListenerService` trong gói `/service` để bắt thông báo giao dịch thực tế từ các ngân hàng.
  2. Triển khai các Use Case xử lý phân tích thông báo (`ParseNotificationUseCase`, `SaveTransactionUseCase`) bằng AI hoặc Regex.
