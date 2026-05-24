# QUY TẮC KIẾN TRÚC SƯ AI & LẬP TRÌNH

> **Mục đích:** Giao thức vận hành điều chỉnh cách AI tương tác với cơ sở mã nguồn.

---

## 1. KHỞI TẠO/ĐỒNG BỘ NGỮ CẢNH

- Kiểm tra `ARCH_MAP.md` tại thư mục `.agent` của dự án.
- **NẾU KHÔNG TỒN TẠI:** Thực hiện quét toàn bộ dự án và tạo `ARCH_MAP.md` dựa trên mẫu kiến trúc chuẩn bên dưới (tạo cả `.agent` nếu chưa tồn tại ).
- **NẾU ĐÃ TỒN TẠI:** Đọc để đồng bộ ngữ cảnh trước khi tiếp tục.

---

## 2. NGÔN NGỮ & TÀI LIỆU

- **TẤT CẢ** chú thích, docstring, log và giải thích trong **MỌI** file phải bằng **TIẾNG VIỆT**.
- **Phong cách:** Tự nhiên, súc tích, giọng điệu kỹ sư với kỹ sư.
- **Trọng tâm:** Giải thích _"TẠI SAO"_ đằng sau logic, không chỉ _"CÁI GÌ"_.

---

### Định nghĩa "Logic không tầm thường" (Non-trivial logic)

> Các đoạn mã **bắt buộc** phải có chú thích tiếng Việt bao gồm nhưng không giới hạn:
>
> - **Hàm xử lý dữ liệu phức tạp:** Các hàm thực hiện biến đổi, lọc, tổng hợp dữ liệu đa tầng hoặc đa nguồn.
> - **Thuật toán tự viết:** Mọi thuật toán không thuộc thư viện chuẩn, bao gồm vòng lặp lồng nhau, đệ quy, hoặc tối ưu hóa thủ công.
> - **Code có tác động liên thông:** Các đoạn mã thay đổi trạng thái, cấu trúc dữ liệu, hoặc giao tiếp giữa nhiều module khác nhau.
> - **Logic điều kiện phức tạp:** Các khối `if/else` lồng nhau, `switch` nhiều nhánh, hoặc điều kiện kết hợp phức tạp.
> - **Xử lý lỗi không thuần túy:** Các khối `try/catch`, `defer`, hoặc xử lý rollback cần giải thích chiến lược phục hồi.
>
> _Không cần chú thích cho:_ getter/setter đơn giản, gọi hàm thư viện chuẩn một dòng, hoặc khai báo biến hiển nhiên.

---

## 3. LUỒNG THỰC THI

### TRƯỚC Khi Lập trình

- Tham chiếu `ARCH_MAP.md`.
- Cảnh báo về các tác động phụ tiềm ẩn lên các thành phần liên kết.
- Mọi thay đổi đều phải thông qua sự cho phép của tôi trước khi thực hiện thay đổi trên mã nguồn (trừ các tác vụ như dịch thuật hoặc comment).

### SAU Khi Lập trình

- Cập nhật `ARCH_MAP.md` ngay lập tức với các thay đổi, thực thể mới, hoặc luồng đã sửa đổi.

### BẢO TRÌ

- Tóm tắt định kỳ các mục "Thay đổi Gần đây" để tránh phình token.
- Lưu trữ các mục lộ trình lỗi thời vào `ARCHIVE.md` riêng nếu cần.

---

### Quản lý Phiên bản cho ARCH_MAP

- Mỗi lần cập nhật `ARCH_MAP.md`, AI **phải** ghi lại mục mới vào phần **Nhật ký Thay đổi** với định dạng:
  ```
  - `[YYYY-MM-DD]` - `[Loại thay đổi]`: Tóm tắt ngắn gọn lý do thay đổi (tối đa 2 dòng).
  ```
- **Các loại thay đổi:** `[Thêm]` | `[Sửa]` | `[Xóa]` | `[Tái cấu trúc]` | `[Sửa lỗi]`.
- Mục nhật ký phải nêu rõ: _thành phần nào_ bị ảnh hưởng, _lý do_ thay đổi, và _tác động_ đến luồng dữ liệu liên quan.
- Mục đích: Khi xem lại lịch sử Git, bạn có thể theo dõi sự tiến hóa của kiến trúc dự án mà không cần đọc toàn bộ diff.

---

## 4. PHONG CÁCH MÃ NGUỒN

- Duy trì cú pháp **tiếng Anh** hiện có cho mã thực tế (biến, hàm, lớp).
- Tất cả **siêu dữ liệu, chú thích, và tài liệu** phải bằng **tiếng Việt**.
- Tuân theo các quy tắc linting và định dạng đã thiết lập của dự án.

---

## 5. DANH SÁCH KIỂM TRA TUÂN THỦ

Trước khi gửi bất kỳ thay đổi mã nào, xác minh:

- [ ] `ARCH_MAP.md` đã được đọc và hiểu.
- [ ] Chú thích tiếng Việt có mặt cho mọi logic không tầm thường.
- [ ] Cảnh báo tác động phụ đã được ghi lại nếu áp dụng.
- [ ] `ARCH_MAP.md` đã được cập nhật để phản ánh trạng thái hiện tại.
- [ ] Không có giải thích tiếng Anh nào tồn tại trong chú thích hoặc docstring (toàn bộ dự án).

---

_Cập nhật lần cuối: `[YYYY-MM-DD]`_
_Được duy trì bởi: Giao thức Kiến trúc sư AI v1.0_
