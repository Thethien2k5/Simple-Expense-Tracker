# ARCH_MAP - BẢN ĐỒ KIẾN TRÚC HỆ THỐNG

> **Mục đích:** Tài liệu sống đóng vai trò là nguồn sự thật duy nhất cho bối cảnh kỹ thuật của dự án. AI phải đọc trước mỗi tác vụ lập trình và cập nhật ngay sau đó.

---

## 1. Tổng quan Công nghệ (Tech Stack)

- **Ngôn ngữ:** Kotlin
- **Framework UI:** Jetpack Compose (100%)
- **Kiến trúc:** Clean Architecture + MVVM
- **Dependency Injection:** Hilt
- **Cơ sở dữ liệu:** Room
- **Xử lý nền:** WorkManager
- **Điều hướng:** Jetpack Navigation Compose

---

## 2. Bản đồ Thư mục & Trách nhiệm

- `app/src/main/java/com/T2V/simple_expense_tracker/`:
  - `/di`: Các Module của Dagger Hilt để cung cấp các phụ thuộc (Dependencies).
  - `/service`: Dịch vụ nghe thông báo (NotificationListenerService) và các tác vụ nền (WorkManager).
  - `/data`: Tầng dữ liệu, bao gồm Room Database, DAOs, và triển khai Repository.
  - `/domain`: Tầng nghiệp vụ cốt lõi (Business Logic), bao gồm Models, Repository Interfaces và UseCases.
  - `/presentation`: Tầng giao diện người dùng (MVVM), bao gồm ViewModels và các màn hình Compose.
  - `/presentation/component`: Các thành phần Compose có thể tái sử dụng.
  - `/presentation/theme`: Định nghĩa màu sắc, phông chữ và chủ đề của ứng dụng.

---

## 3. Các Thực thể Dữ liệu Cốt lõi

- **BankAccount:** Thông tin về tài khoản ngân hàng được theo dõi.
- **Transaction:** Chi tiết về một giao dịch tài chính (số tiền, ngày tháng, nội dung).
- **Category:** Danh mục chi tiêu (ví dụ: Ăn uống, Di chuyển).
- **Rule:** Quy tắc heuristic để tự động phân loại giao dịch từ thông báo.

---

## 4. Luồng Dữ liệu Quan trọng

- **Luồng Phân tích Thông báo:** NotificationListenerService -> ParseNotificationUseCase -> SaveTransactionUseCase -> Room DB.
- **Luồng Hiển thị:** Room DB -> Repository -> ViewModel (StateFlow) -> Compose UI.

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
- `[TODO]`: Thiết lập Hilt Modules để cung cấp các thực thể Database và DAO cho toàn ứng dụng.
