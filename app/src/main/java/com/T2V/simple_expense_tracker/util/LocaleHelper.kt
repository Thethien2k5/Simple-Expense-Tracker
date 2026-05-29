package com.T2V.simple_expense_tracker.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {
    fun setLocale(context: Context, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration()
        config.setLocale(locale)

        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }
}
//*
//
//Android quản lý ngôn ngữ theo `Configuration` (Cấu hình). Khi bạn muốn đổi từ Tiếng Việt
//→
//→ Tiếng Anh, bạn không thể chỉ đổi một cái biến là xong, mà bạn phải nói với Android: *"Hãy thay đổi toàn bộ cấu hình ngôn ngữ của app này"*.

// Để gọn gàng, Thông kêu AI tạo ra LocaleHelper. Hàm setLocale này làm 3 việc chính bên trong:
// Bước 1: Tạo một đối tượng Locale
//Nó lấy cái code bạn truyền vào (ví dụ: "en") và tạo ra một đối tượng Locale("en"). Đây là cách nói với Java/Kotlin: "Tôi muốn dùng ngôn ngữ tiếng Anh".
//
//Bước 2: Cập nhật Configuration (Cấu hình)
//Nó truy cập vào resources.configuration của app và ghi đè ngôn ngữ mới vào đó. Nó giống như việc bạn thay đổi cài đặt trong menu của điện thoại, nhưng làm bằng code.
//
//Bước 3: Cập nhật Context
//Nó gọi hàm createConfigurationContext() để tạo ra một cái "ngữ cảnh" mới đã được áp dụng ngôn ngữ mới. Điều này đảm bảo rằng khi bạn gọi stringResource(R.string.welcome), Android sẽ biết phải vào folder values-en để lấy chữ ra.
// */
