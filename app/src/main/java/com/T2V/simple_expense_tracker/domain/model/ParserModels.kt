package com.T2V.simple_expense_tracker.domain.model

/**
 * Kết quả phân tích thông báo ngân hàng ở tầng domain.
 */
data class ParsedData(
    val bankName: String,
    val accountNumber: String,
    val amount: Double,
    val isCredit: Boolean, // true = Nhận tiền (+), false = Chuyển tiền (-)
    val counterparty: String,
    val content: String,
    val timestamp: Long,
    val balance: Double? = null
)

/**
 * Cấu hình định nghĩa cho một ngân hàng.
 */
data class BankConfig(
    val name: String,
    val color: String,
    val packageKeywords: List<String>,
    val titleKeywords: List<String>,
    val parserConfig: BankParserConfig? = null
)

/**
 * Cấu hình phân tích Regex riêng cho từng ngân hàng.
 */
data class BankParserConfig(
    val amountPatterns: List<AmountPattern>,
    val accountPatterns: List<String>,
    val contentPatterns: List<String>,
    val balancePatterns: List<String>,
    val transferPatterns: List<String>
)

data class AmountPattern(
    val pattern: String,
    val type: String,
    val signGroup: Int = 0,
    val amountGroup: Int = 1
)

/**
 * Kết quả trả về từ quá trình phân tích, bao gồm trạng thái:
 * - SUCCESS: Phân tích thành công hoàn toàn bằng Regex.
 * - REJECTED: Thông báo không hợp lệ (quảng cáo, SMS, không phải biến động số dư, hoặc không tách được số liệu).
 */
enum class ParseResult {
    SUCCESS,
    REJECTED
}

data class NotificationParseOutput(
    val result: ParseResult,
    val parsedData: ParsedData? = null,
    val rawContent: String = "",
    val bankName: String = ""
)
