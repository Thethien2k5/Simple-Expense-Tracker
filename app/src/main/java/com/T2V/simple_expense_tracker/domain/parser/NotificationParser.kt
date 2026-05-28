package com.T2V.simple_expense_tracker.domain.parser

/**
 * Cấu trúc dữ liệu sau khi thông báo được phân tích cú pháp.
 */
data class ParsedData(
    val bankName: String,
    val accountNumber: String,
    val amount: Double,
    val isCredit: Boolean, // true = Nhận tiền (+), false = Chuyển tiền (-)
    val counterparty: String,
    val content: String,
    val timestamp: Long
)

/**
 * Bộ phân tách Regex thông minh để bóc tách thông tin giao dịch từ thông báo thô của ngân hàng.
 */
object NotificationParser {

    fun parse(bankName: String, content: String, timestamp: Long): ParsedData? {
        if (content.isBlank()) return null

        var amount = 0.0
        var isCredit = true

        // 1. Phân tích Số tiền (amount) & Chiều giao dịch (isCredit)
        // Tìm dạng: +50,000 VND, -120.000 đ, GD: +50000d, tru 20.000d, cong 100.000d
        val amountRegex = """(?:(?:GD|giao dịch)\s*)?([\+\-])\s*([0-9.,]+)\s*(?:VND|đ|d|vnd|Vnd)""".toRegex(RegexOption.IGNORE_CASE)
        val amountMatch = amountRegex.find(content)

        if (amountMatch != null) {
            val sign = amountMatch.groupValues[1]
            val amountStr = amountMatch.groupValues[2].replace(".", "").replace(",", "")
            amount = amountStr.toDoubleOrNull() ?: 0.0
            isCredit = (sign == "+")
        } else {
            // Trường hợp dùng chữ "tru" / "trừ" hoặc "cong" / "cộng" / "nhan" / "nhận"
            val textAmountRegex = """(?:trừ|tru)\s*([0-9.,]+)\s*(?:VND|đ|d|vnd)""".toRegex(RegexOption.IGNORE_CASE)
            val textAmountMatch = textAmountRegex.find(content)
            if (textAmountMatch != null) {
                val amountStr = textAmountMatch.groupValues[1].replace(".", "").replace(",", "")
                amount = amountStr.toDoubleOrNull() ?: 0.0
                isCredit = false
            } else {
                // Trường hợp dùng chữ "tăng" / "tang" hoặc "cộng" / "cong" / "nhận" / "nhan"
                val textAmountRegex3 = """(?:tăng|tang|cộng|cong|nhận|nhan)\s*([0-9.,]+)""".toRegex(RegexOption.IGNORE_CASE)
                val textAmountMatch3 = textAmountRegex3.find(content)
                if (textAmountMatch3 != null) {
                    val amountStr = textAmountMatch3.groupValues[1].replace(".", "").replace(",", "")
                    amount = amountStr.toDoubleOrNull() ?: 0.0
                    isCredit = true
                } else {
                    // Trường hợp dùng chữ "giảm" / "giam" hoặc "trừ" / "tru"
                    val textAmountRegex4 = """(?:giảm|giam|trừ|tru)\s*([0-9.,]+)""".toRegex(RegexOption.IGNORE_CASE)
                    val textAmountMatch4 = textAmountRegex4.find(content)
                    if (textAmountMatch4 != null) {
                        val amountStr = textAmountMatch4.groupValues[1].replace(".", "").replace(",", "")
                        amount = amountStr.toDoubleOrNull() ?: 0.0
                        isCredit = false
                    } else {
                        // Quét số kèm tiền tệ thô: 100,000 VND
                        val rawAmountRegex = """([0-9.,]+)\s*(?:VND|đ|d|vnd)""".toRegex(RegexOption.IGNORE_CASE)
                        val rawAmountMatch = rawAmountRegex.find(content)
                        if (rawAmountMatch != null) {
                            val amountStr = rawAmountMatch.groupValues[1].replace(".", "").replace(",", "")
                            amount = amountStr.toDoubleOrNull() ?: 0.0
                            // Mặc định là chi tiêu (-) nếu chứa các từ khóa thanh toán/trừ
                            val isExpense = content.contains("trừ", ignoreCase = true) || 
                                            content.contains("tru", ignoreCase = true) || 
                                            content.contains("giảm", ignoreCase = true) ||
                                            content.contains("giam", ignoreCase = true) ||
                                            content.contains("thanh toan", ignoreCase = true) || 
                                            content.contains("chuyển khoản đi", ignoreCase = true) ||
                                            content.contains("-")
                            isCredit = !isExpense
                        }
                    }
                }
            }
        }

        if (amount == 0.0) return null

        // 2. Phân tích Số tài khoản (accountNumber)
        // Quét bằng từ khóa: TK, tài khoản, tai khoan, so tk, account
        val accountRegex = """(?:TK|tài khoản|tai khoan|so tk|account)\s*([0-9xX]+)""".toRegex(RegexOption.IGNORE_CASE)
        var accountNumber = "DEFAULT_ACC"
        val accountMatch = accountRegex.find(content)
        if (accountMatch != null) {
            accountNumber = accountMatch.groupValues[1]
        } else {
            // Quét chuỗi số độc lập có từ 8 đến 15 chữ số
            val rawAccountRegex = """\b([0-9]{8,15})\b""".toRegex()
            val rawAccountMatch = rawAccountRegex.find(content)
            if (rawAccountMatch != null) {
                accountNumber = rawAccountMatch.groupValues[1]
            }
        }

        // 3. Phân tích Nội dung giao dịch (content)
        // Quét bằng các từ khóa: ND, nội dung, noi dung, nd, ref, lý do
        val contentRegex = """(?:ND|nội dung|noi dung|nd|ref|ly do|lý do)\s*[:\-]?\s*(.+)""".toRegex(RegexOption.IGNORE_CASE)
        var parsedContent = "Giao dịch qua thông báo ngân hàng"
        val contentMatch = contentRegex.find(content)
        if (contentMatch != null) {
            parsedContent = contentMatch.groupValues[1].trim()
            // Loại bỏ thông tin số dư tiếp theo trong nội dung nếu có
            val balanceKeywords = listOf("số dư", "so du", "sd:", "balance")
            for (keyword in balanceKeywords) {
                val index = parsedContent.lowercase().indexOf(keyword)
                if (index != -1) {
                    parsedContent = parsedContent.substring(0, index).trim()
                    break
                }
            }
            parsedContent = parsedContent.trim().removeSuffix(".")
        }

        // 4. Phân tích Đối tác (counterparty)
        var counterparty = "Chưa rõ đối tác"
        
        // Danh sách các từ khóa giao dịch thông dụng cần loại trừ
        val excludeKeywords = listOf(
            "chuyen", "chuyển", "tien", "tiền", "thanh toan", "thanh toán", 
            "an trua", "an sang", "mua", "nap", "nạp", "tra no", "trả nợ", 
            "luong", "lương", "thang", "tháng", "gd", "ck", "banking", "ib", "mb",
            "chuyen khoan", "chuyển khoản", "nop", "nộp", "rut", "rút", "mat", "mặt"
        )

        // Cách 1: Quét các từ chỉ định người gửi/nhận như "từ", "tu", "tới", "toi", "gui", "nhan", "bởi", "by"
        val transferRegex = """(?:từ|tu|bởi|by|tới|toi|gửi|gui|nhận|nhan|khach hang|kh)\s+([A-Z0-9\s]{3,30}?)(?:\s+chuyen|\s+ck|\s+nap|\s+thanh|\s+toi|\s+tai|$)""".toRegex(RegexOption.IGNORE_CASE)
        val transferMatch = transferRegex.find(parsedContent)
        
        if (transferMatch != null) {
            val potentialName = transferMatch.groupValues[1].trim()
            if (potentialName.isNotBlank() && !excludeKeywords.any { potentialName.lowercase().contains(it) }) {
                counterparty = potentialName
            }
        }
        
        // Cách 2: Quét chuỗi gồm 2 đến 4 từ viết hoa liên tiếp trong nội dung (ví dụ: NGUYEN VAN B, LE VAN AN)
        if (counterparty == "Chưa rõ đối tác") {
            val nameRegex = """\b([A-Z]{2,10}(?:\s+[A-Z]{1,10}){1,3})\b""".toRegex()
            val matches = nameRegex.findAll(parsedContent).map { it.value.trim() }.toList()
            for (match in matches) {
                val lowerMatch = match.lowercase()
                val isExcluded = excludeKeywords.any { keyword -> 
                    lowerMatch == keyword || 
                    lowerMatch.startsWith(keyword + " ") || 
                    lowerMatch.endsWith(" " + keyword) ||
                    lowerMatch.contains(" " + keyword + " ")
                }
                if (!isExcluded && match.split(" ").size >= 2) {
                    counterparty = match
                    break
                }
            }
        }
        
        // Cách 3: Nếu vẫn chưa ra, lấy 2-3 từ đầu tiên của nội dung chuyển tiền làm tên tạm (nếu không trùng từ khóa loại trừ)
        if (counterparty == "Chưa rõ đối tác") {
            val words = parsedContent.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (words.size >= 2) {
                val candidate = words.take(3).joinToString(" ")
                if (!excludeKeywords.any { candidate.lowercase().contains(it) }) {
                    counterparty = candidate
                }
            }
        }

        if (counterparty.isBlank()) {
            counterparty = "Giao dịch nội địa"
        }

        return ParsedData(
            bankName = bankName,
            accountNumber = accountNumber,
            amount = amount,
            isCredit = isCredit,
            counterparty = counterparty,
            content = parsedContent,
            timestamp = timestamp
        )
    }
}
