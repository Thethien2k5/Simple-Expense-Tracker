// Top-level build file where you can add configuration options common to all sub-projects/modules.
// Tệp cấu hình build cấp gốc nơi bạn có thể thêm các tùy chọn cấu hình dùng chung cho tất cả các dự án con/mô-đun.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // Thêm plugin Hilt để hỗ trợ Dependency Injection ở cấp dự án
    alias(libs.plugins.hilt.android) apply false
    // Thêm plugin Kapt để hỗ trợ xử lý Annotation (cần thiết cho Room và Hilt)
    alias(libs.plugins.kotlin.kapt) apply false
}