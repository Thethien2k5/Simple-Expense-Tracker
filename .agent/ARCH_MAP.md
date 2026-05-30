# ARCH_MAP - BẢN ĐỒ KIẾN TRÚC HỆ THỐNG

> **Mục đích:** Tài liệu sống đóng vai trò là nguồn sự thật duy nhất cho bối cảnh kỹ thuật của dự án. AI phải đọc trước mỗi tác vụ lập trình và cập nhật ngay sau đó.

---

## 1. Tổng quan Công nghệ (Tech Stack)

- **Ngôn ngữ:** Kotlin
- **Framework UI:** Jetpack Compose (100%)
- **Kiến trúc:** Clean Architecture + MVVM
- **Dependency Injection:** Hilt
- **Cơ sở dữ liệu:** Room (Local DB), DataStore (Preferences)
- **Xử lý nền:** `BankNotificationListenerService` để đọc thông báo, cấu hình sẵn WorkManager (nếu cần thiết).
- **Điều hướng:** Tích hợp thông qua cơ chế View-State và hệ thống Modal Drawer thay vì NavHost phức tạp.

---

## 2. Bản đồ Thư mục & Trách nhiệm

- `app/src/main/java/com/T2V/simple_expense_tracker/`:
  - `ExpenseTrackerApp.kt`: Lớp Application khởi tạo Hilt.
  - `MainActivity.kt`: Entry point với hệ thống Double Navigation Drawers (Settings bên trái & Notification Panel bên phải) bao bọc nội dung chính (Dashboard).
  - `/di`: Các Module của Dagger Hilt cung cấp phụ thuộc:
    - `AppModule.kt`: Các phụ thuộc cấp ứng dụng.
    - `DatabaseModule.kt`: Cung cấp Room Database và DAOs.
    - `DataStoreModule.kt`: Cung cấp DataStore cho Preferences (Theme, Language).
    - `RepositoryModule.kt`: Cung cấp các Repositories.
  - `/data`: Tầng dữ liệu, bao gồm:
    - `/local`: Quản lý Room Database (`AppDatabase`), các DAO (`BankAccountDao`, `RawNotificationDao`, `TransactionDao`) và thực thể cơ sở dữ liệu (`/entity`).
    - `/mapper`: Ánh xạ dữ liệu giữa Database Entities và Domain Models.
    - `/repository`: Triển khai các Repository interface từ tầng domain (BankAccount, Language, RawNotification, Theme, Transaction).
  - `/domain`: Tầng nghiệp vụ cốt lõi (Business Logic):
    - `/config`: Cấu hình hệ thống (`ConfigManager`).
    - `/model`: Định nghĩa cấu trúc dữ liệu thuần túy (Domain Models như `Transaction`, `BankAccount`, `RawNotification`).
    - `/parser`: `NotificationParser` chịu trách nhiệm phân tích nội dung thông báo ngân hàng để bóc tách thông tin giao dịch.
    - `/repository`: Khai báo các interface của Repository.
    - `/usecase`: Các kịch bản sử dụng (Lấy thông báo, tài khoản, giao dịch...).
  - `/ui`: Tầng giao diện người dùng (MVVM) sử dụng Jetpack Compose:
    - `/dashboard`: Màn hình chính với các section (Số dư, Gần đây, Chi tiết, Thống kê), biểu đồ (`ChartComponents`) và `TransactionDetailDialog`.
    - `/ledger`: Giao diện danh sách thông báo gốc / sổ cái phụ (NotificationPanel).
    - `/notification`: Xử lý các UI liên quan đến luồng nhận thông báo mới (Cảnh báo số dư bất thường `AnomalyConfirmationDialog`, Nhập giao dịch thủ công `ManualParseScreen`, và `NotificationActionViewModel`).
    - `/settings`: Giao diện Cài đặt hệ thống (SettingsPanel).
    - `/theme`: Bảng màu thiết kế Material3 Custom Dark Theme, typography. Bao gồm cả `AppStrings.kt` (quản lý đa ngôn ngữ 100% Kotlin qua CompositionLocal).
  - `/service`: Dịch vụ chạy ngầm (`BankNotificationListenerService`) lắng nghe thông báo biến động số dư từ ngân hàng.
  - `/util`: Các lớp tiện ích chung (như `LocaleHelper`).

---

## 3. Sơ đồ Thực thể Cơ sở dữ liệu (ERD)

```dbml
// Simple Expense Tracker - Sơ đồ kiến trúc DB (Tối ưu Local-only)

// Thông tin các tài khoản cần theo dõi
Table BankAccount {
  id integer [pk, increment, note: 'Khóa chính']
  bankName varchar(100) [not null, note: 'Tên ngân hàng (ví dụ: Vietcombank, Momo, Tnex)']
  accountNumber varchar(50) [note: 'Số TK (ẩn 4 số cuối)']
  iconRes varchar(100) [note: 'Tên file icon trong drawable']
  colorHex varchar(7) [default: '#1976D2', note: 'Mã màu nhận diện thương hiệu']
}

// Lưu nguyên thông báo (bóc tách, đối chiếu, list)
Table RawNotification {
  id integer [pk, increment, note: 'Khóa chính']
  bankName varchar(100) [not null, note: 'Tên nguồn thông báo']
  fullContent text [not null, note: 'Toàn bộ nội dung gốc của thông báo']
  receivedAt integer [not null, note: 'Thời điểm nhận thông báo (Timestamp)']
  isProcessed boolean [default: false, note: 'Trạng thái đã được bóc tách hay chưa']
}

// Nội dung sạch hiển thị dạng bảng cho người dùng xem
Table Transaction {
  id integer [pk, increment, note: 'Khóa chính']
  rawNotificationId integer [ref: > RawNotification.id, note: 'Liên kết tới thông báo gốc']
  bankAccountId integer [not null, ref: > BankAccount.id, note: 'Ngân hàng thực hiện giao dịch']
  amount real [not null, note: 'Số tiền (số dương: nhận, số âm: chuyển)']
  counterparty varchar(255) [note: 'Tên người gửi hoặc người nhận']
  content text [note: 'Mô tả ngắn gọn về giao dịch']
  timestamp integer [not null, note: 'Thời điểm giao dịch thực tế']
}
```

---

## 4. Luồng Dữ liệu Quan trọng

- **Luồng Bắt Thông Báo Ngân Hàng:** `BankNotificationListenerService` đọc thông báo -> Gửi Intent Broadcast -> `NotificationActionViewModel` -> `NotificationParser` bóc tách dữ liệu -> Nếu có bất thường/không bóc tách được thì hiển thị Dialog xác nhận / Nhập tay -> `TransactionRepository` -> Room DB.
- **Luồng Hiển thị Giao diện:** Room DB -> Repository -> UseCases -> ViewModel (StateFlow) -> Compose UI. Dữ liệu được theo dõi thời gian thực thông qua Kotlin Flows.
- **Luồng Đa Ngôn Ngữ & Giao diện:** Cấu hình đọc từ DataStore -> Flow truyền tới `MainActivity` -> `CompositionLocalProvider` (Cung cấp `LocalAppStrings`, `MaterialTheme`) -> Toàn bộ Compose Components tái render ngay lập tức mà không cần Android Context (Resource).

---

## 5. Quy ước & Ràng buộc

- **Ngôn ngữ:** Tất cả chú thích và giải thích trong mã nguồn phải bằng **Tiếng Việt**.
- **UI:** Không sử dụng XML layouts, 100% Jetpack Compose.
- **Dependency Injection:** Sử dụng Hilt để quản lý sự phụ thuộc giữa các lớp.
- **Đa ngôn ngữ (Localization):** Quản lý 100% bằng Kotlin thông qua `CompositionLocal` (`LocalAppStrings`), không phụ thuộc vào `res/values/strings.xml` của Android (chỉ giữ lại XML cho tên ứng dụng ở màn hình nền).
- **Địa phương hóa:** Ứng dụng hoạt động 100% local, không yêu cầu internet, không có server backend.

---

## 6. Nhật ký Thay đổi & Lộ trình

- `[2024-05-22]` - `[Khởi tạo]`: Thiết lập cấu trúc dự án theo Clean Architecture, nâng cấp Version Catalog và cấu hình Hilt/Room. Trải qua bước tạo Entity, DAO, AppDatabase.
- `[2026-05-25]` - `[Cập nhật]`: Triển khai các Hilt Modules và UseCases cơ bản của Domain. Hoàn thiện toàn bộ giao diện (Double Drawers, Custom Dark Theme).
- `[2026-05-26]` - `[Refactor]`: Gỡ bỏ hoàn toàn tính năng Phân loại giao dịch (Category) và Quy tắc tự động (AutoRule) từ DB lên UI để tinh gọn hệ thống.
- `[2026-05-30]` - `[Hoàn thiện Notification]`: Tích hợp đầy đủ `BankNotificationListenerService`, parser xử lý nội dung thông báo ngân hàng (`NotificationParser`) và giao diện kiểm soát thông báo (`AnomalyConfirmationDialog`, `ManualParseScreen`, `NotificationActionViewModel`).
- `[2026-05-30]` - `[Chuyển đổi Ngôn Ngữ & Theme]`: Chặn xoay ngang màn hình (portrait). Thay thế 100% màu hardcode bằng màu của theme (`MaterialTheme.colorScheme`). Loại bỏ hoàn toàn đa ngôn ngữ XML (bỏ các folder `values-XX`), cấu trúc lại toàn bộ string dưới dạng object `AppStrings` thuần Kotlin qua `CompositionLocal`. Cải thiện UI Setting Panel với Gradient Icon.
- `[TODO]`:
  1. Tích hợp AI (Gemini Nano hoặc LLM nhẹ) vào `NotificationParser` để tự động bóc tách nội dung giao dịch thông minh hơn nếu hệ thống Regex truyền thống không đủ linh hoạt.
  2. Bổ sung tính năng sao lưu (Backup/Restore) cơ sở dữ liệu Room lên Google Drive/Local Storage.
