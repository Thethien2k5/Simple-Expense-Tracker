# Simple Expense Tracker

Ứng dụng quản lý chi tiêu đơn giản, tự động đọc thông báo biến động số dư từ ứng dụng ngân hàng để ghi chú giao dịch hoàn toàn offline.

## 🌟 Tính năng chính

- **Tự động bắt thông báo:** Sử dụng `BankNotificationListenerService` lắng nghe biến động số dư từ các ứng dụng ngân hàng/ví điện tử (Vietcombank, Techcombank, MB Bank, TNEX, Momo, v.v.).
- **Phân tích thông minh:** Bộ phân tích (Parser) linh hoạt bằng Regex đọc trực tiếp từ JSON, hỗ trợ cập nhật động cấu hình nhận diện ngân hàng mà không cần build lại app.
- **Quản lý đa tài khoản:** Hỗ trợ thống kê giao dịch theo từng tài khoản ngân hàng riêng biệt.
- **Hoàn toàn Offline & Bảo mật:** Dữ liệu giao dịch được lưu hoàn toàn trên bộ nhớ thiết bị của bạn thông qua Room Database. Không có bất kỳ kết nối gửi dữ liệu nào lên server (No Backend) để bảo vệ tính riêng tư.
- **Giao diện hiện đại & mượt mà:**
  - Xây dựng 100% bằng Jetpack Compose.
  - Sử dụng hệ thống Double Navigation Drawers (Menu Cài đặt bên trái, Panel Thông báo bên phải).
  - Hỗ trợ chế độ Custom Dark Theme độc quyền.
- **Đa ngôn ngữ trực tiếp (Kotlin thuần):** Quản lý chuỗi thông qua `CompositionLocal` thay vì `strings.xml`, cho phép chuyển đổi ngôn ngữ ứng dụng tức thì mà không cần khởi động lại Activity.

## 🛠 Ngôn ngữ & Công nghệ

- **Ngôn ngữ:** Kotlin
- **Kiến trúc:** Clean Architecture + MVVM
- **UI Framework:** Jetpack Compose (100%)
- **Dependency Injection:** Dagger Hilt
- **Local Storage:** Room Database (Lưu trữ giao dịch), DataStore Preferences (Cài đặt, Ngôn ngữ, Theme)
- **Background Processing:** NotificationListenerService
- **Asynchronous Programming:** Coroutines & StateFlow

## 🏗 Cấu trúc Dự án (Architecture)

Dự án tuân theo mô hình **Clean Architecture** để đảm bảo tính phân tách mã nguồn và dễ mở rộng, bao gồm các package chính:

- `di/`: Các Module Dependency Injection bằng Hilt cung cấp App, DB, DataStore, Repository.
- `data/`: Tầng dữ liệu chứa Local Database (Room), DAOs, Entity, Mapper và Repository Implementations.
- `domain/`: Tầng nghiệp vụ cốt lõi chứa Domain Models, UseCases, Interfaces, cấu hình hệ thống (`ConfigManager`) và `NotificationParser`.
- `ui/`: Tầng giao diện chứa các thành phần Compose UI (Dashboard, Ledger, Notification, Settings, Theme).
- `service/`: Tầng Background Service chạy ngầm bắt và xử lý thông báo.

## 🔄 Sơ đồ Luồng (Flow Diagrams)

```mermaid
graph TD
    subgraph UI_Layer ["UI Layer (Tầng Giao Diện)"]
        DashboardScreen["DashboardScreen (Màn hình chính)"] --> DashboardViewModel["DashboardViewModel"]
        LedgerScreen["LedgerScreen (Sổ thu chi)"] --> LedgerViewModel["LedgerViewModel"]
    end
    subgraph Domain_Layer ["Domain Layer (Tầng Nghiệp Vụ)"]
        DashboardViewModel --> GetTransactionsUseCase["GetTransactionsUseCase (Lấy giao dịch)"]
        DashboardViewModel --> GetBankAccountsUseCase["GetBankAccountsUseCase (Lấy TK)"]
        GetTransactionsUseCase --> TransactionRepository["TransactionRepository (Interface)"]
        GetBankAccountsUseCase --> BankAccountRepository["BankAccountRepository (Interface)"]
        BankNotificationListenerService["BankNotificationListenerService (Lắng nghe TB)"] --> NotificationParser["NotificationParser (Phân tích TB)"]
    end
    subgraph Data_Layer ["Data Layer (Tầng Dữ Liệu)"]
        TransactionRepository -.-> TransactionRepositoryImpl["TransactionRepositoryImpl (Thực thi)"]
        BankAccountRepository -.-> BankAccountRepositoryImpl["BankAccountRepositoryImpl (Thực thi)"]
        TransactionRepositoryImpl --> RoomDatabase["Room Database (CSDL Cục bộ)"]
        BankAccountRepositoryImpl --> RoomDatabase
    end
```

## Data Flow - Sequence Diagram (Notification Processing)
```mermaid
sequenceDiagram
    participant OS as "Android OS (HĐH)"
    participant Service as "NotificationListener (Dịch vụ)"
    participant Parser as "NotificationParser (Bộ phân tích)"
    participant Repo as "Data Repositories (Kho dữ liệu)"
    participant DB as "Room Database (CSDL)"

    OS->>Service: onNotificationPosted() (Có thông báo mới)
    Service->>Parser: isBankNotification() (Kiểm tra TB Ngân hàng)
    alt Is Valid Bank Notification (TB Hợp lệ)
        Service->>Repo: insertNotification() (Lưu TB thô)
        Repo->>DB: Save Raw Notification (Ghi vào DB)
        Service->>Parser: parseMultiTier() (Phân tích nội dung 3 tầng)
        Parser-->>Service: ParsedData (Số tiền, Tài khoản, Số dư)
        Service->>Service: Mutex.withLock() (Khóa luồng đồng bộ)
        Service->>Repo: getBankAccount() (Lấy thông tin Tài khoản)
        Repo-->>Service: BankAccount info (Trả về TK)
        Service->>Repo: updateBankAccount() (Cập nhật số dư mới)
        Service->>Repo: insertTransaction() (Lưu giao dịch chi tiêu)
        Repo->>DB: Save Transaction (Ghi vào DB)
    end
```

## Entity Relationship Diagram (ERD)
```mermaid
erDiagram
    BankAccount ||--o{ Transaction : "has (có)"
    RawNotification ||--o| Transaction : "creates (tạo ra)"
    BankAccount {
        Long id PK
        String bankName "Tên ngân hàng"
        String accountNumber "Số tài khoản"
        Double balance "Số dư hiện tại"
    }
    RawNotification {
        Long id PK
        String bankName "Tên ngân hàng"
        String fullContent "Nội dung gốc đầy đủ"
        Boolean isProcessed "Trạng thái đã xử lý"
    }
    Transaction {
        Long id PK
        Long bankAccountId FK
        Long rawNotificationId FK
        Double amount "Số tiền giao dịch"
        String counterparty "Đối tác/Người gửi nhận"
    }
```

## 🚀 Cài đặt & Chạy ứng dụng

1. Clone kho lưu trữ này về máy:
   ```bash
   git clone https://github.com/Thethien2k5/Simple-Expense-Tracker.git
   ```
2. Mở dự án bằng **Android Studio** (Phiên bản mới nhất hỗ trợ Kotlin mới và Jetpack Compose).
3. Đợi Gradle đồng bộ (Sync) và tải về các thư viện cần thiết.
4. Chạy ứng dụng trên máy ảo (Emulator) hoặc thiết bị Android thật.

> **Lưu ý Cấp quyền:** Lần đầu mở app, ứng dụng sẽ yêu cầu quyền đọc thông báo (Notification Access). Vui lòng cấp quyền này trong Cài đặt hệ thống để tính năng tự động lắng nghe và phân tích các giao dịch ngân hàng có thể hoạt động.

## 📅 Lộ trình phát triển (Roadmap)

- [x] Thiết lập cấu trúc dự án chuẩn Clean Architecture, MVVM, Room, Hilt.
- [x] Phát triển giao diện UI 100% bằng Jetpack Compose với Double Drawers.
- [x] Cài đặt `BankNotificationListenerService` & parser tự động sử dụng Regex.
- [x] Cơ chế cấu hình Parser động thông qua `ConfigManager` lưu trữ JSON.
- [x] Quản lý Đa ngôn ngữ và Theme bằng `CompositionLocal`.
- [x] Tích hợp tính năng tự động chuẩn hóa Unicode khi quét Regex. 
- [ ] Tích hợp AI (Gemini Nano hoặc LLM nhẹ) vào `NotificationParser` để tự động bóc tách nội dung giao dịch thông minh hơn nếu Regex truyền thống thất bại. *(Phương án này đã bị loại bỏ trong quá trình phát triển)*


