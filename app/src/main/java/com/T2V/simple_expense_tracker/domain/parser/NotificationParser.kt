package com.T2V.simple_expense_tracker.domain.parser

import android.content.Context
import android.util.Log
import com.T2V.simple_expense_tracker.domain.config.ConfigManager
import com.T2V.simple_expense_tracker.domain.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bộ phân tích thông báo ngân hàng theo kiến trúc 3 tầng:
 * 1. Regex từ JSON (ưu tiên config riêng của ngân hàng, fallback sang globalParserConfig).
 * 2. ML Kit Entity Extraction (nếu Regex không khớp).
 * 3. Kích hoạt xử lý thủ công bởi người dùng (nếu ML Kit cũng thất bại).
 *
 * Đọc cấu hình từ ConfigManager (Internal Storage) để hỗ trợ cập nhật Regex động.
 */
@Singleton
class NotificationParser @Inject constructor(
    @ApplicationContext private val context: Context,
    private val configManager: ConfigManager
) {
    companion object {
        private const val TAG = "NotificationParser"
    }

    // Cấu hình đã tải
    private val banks = mutableListOf<BankConfig>()
    private val smsBlacklistPackages = mutableListOf<String>()
    private val promotionalKeywords = mutableListOf<String>()
    private var globalParserConfig: BankParserConfig? = null
    private var globalExpenseKeywords = mutableListOf<String>()
    private var globalBalanceKeywords = mutableListOf<String>()
    private var globalExcludeKeywords = mutableListOf<String>()
    private var globalNamePattern: String = ""

    
    init {
        loadConfiguration()
    }

    // ============================================================================
    // PHẦN 1: TẢI CẤU HÌNH
    // ============================================================================

    /**
     * Tải toàn bộ cấu hình từ ConfigManager (Internal Storage JSON động).
     */
    private fun loadConfiguration() {
        try {
            val root = configManager.loadConfig()

            // 1. Tải danh sách SMS blacklist packages
            val smsArray = root.optJSONArray("smsBlacklistPackages")
            if (smsArray != null) {
                for (i in 0 until smsArray.length()) {
                    smsBlacklistPackages.add(smsArray.getString(i))
                }
            }

            // 2. Tải danh sách đen từ khóa quảng cáo
            val promoArray = root.optJSONArray("promotionalKeywords")
            if (promoArray != null) {
                for (i in 0 until promoArray.length()) {
                    promotionalKeywords.add(promoArray.getString(i))
                }
            }

            // 3. Tải danh sách ngân hàng (kèm parserConfig riêng nếu có)
            val banksArray = root.optJSONArray("banks")
            if (banksArray != null) {
                for (i in 0 until banksArray.length()) {
                    val bankObj = banksArray.getJSONObject(i)
                    val name = bankObj.optString("name", "")
                    val color = bankObj.optString("color", "#757575")
                    val pkgKeywords = jsonArrayToStringList(bankObj.optJSONArray("packageKeywords"))
                    val titleKeywords = jsonArrayToStringList(bankObj.optJSONArray("titleKeywords"))
                    val parserConfig = parseBankParserConfig(bankObj.optJSONObject("parserConfig"))
                    banks.add(BankConfig(name, color, pkgKeywords, titleKeywords, parserConfig))
                }
            }

            // 4. Tải globalParserConfig
            val globalObj = root.optJSONObject("globalParserConfig")
            if (globalObj != null) {
                globalParserConfig = parseBankParserConfig(globalObj)
                globalExpenseKeywords.addAll(jsonArrayToStringList(globalObj.optJSONArray("expenseKeywords")))
                globalBalanceKeywords.addAll(jsonArrayToStringList(globalObj.optJSONArray("balanceKeywords")))
                globalExcludeKeywords.addAll(jsonArrayToStringList(globalObj.optJSONArray("excludeKeywords")))
                globalNamePattern = globalObj.optString("namePattern", "")
            }

            Log.d(TAG, "Đã tải cấu hình thành công. Số ngân hàng: ${banks.size}, Promotional keywords: ${promotionalKeywords.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi tải cấu hình từ JSON.", e)
        }
    }

    /**
     * Tải lại cấu hình từ Internal Storage (gọi khi JSON được cập nhật động).
     */
    fun reloadConfiguration() {
        banks.clear()
        smsBlacklistPackages.clear()
        promotionalKeywords.clear()
        globalExpenseKeywords.clear()
        globalBalanceKeywords.clear()
        globalExcludeKeywords.clear()
        loadConfiguration()
    }

    private fun parseBankParserConfig(obj: JSONObject?): BankParserConfig? {
        if (obj == null) return null
        val amountPatterns = mutableListOf<AmountPattern>()
        val amtArray = obj.optJSONArray("amountPatterns")
        if (amtArray != null) {
            for (i in 0 until amtArray.length()) {
                val a = amtArray.getJSONObject(i)
                amountPatterns.add(
                    AmountPattern(
                        pattern = a.optString("pattern", ""),
                        type = a.optString("type", ""),
                        signGroup = a.optInt("signGroup", 0),
                        amountGroup = a.optInt("amountGroup", 1)
                    )
                )
            }
        }
        return BankParserConfig(
            amountPatterns = amountPatterns,
            accountPatterns = jsonArrayToStringList(obj.optJSONArray("accountPatterns")),
            contentPatterns = jsonArrayToStringList(obj.optJSONArray("contentPatterns")),
            balancePatterns = jsonArrayToStringList(obj.optJSONArray("balancePatterns")),
            transferPatterns = jsonArrayToStringList(obj.optJSONArray("transferPatterns"))
        )
    }

    private fun jsonArrayToStringList(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            list.add(arr.getString(i))
        }
        return list
    }



    // ============================================================================
    // PHẦN 3: LỌC THÔNG BÁO (Blacklist + Whitelist)
    // ============================================================================

    /**
     * Kiểm tra xem thông báo có phải biến động số dư hợp lệ hay không.
     * Quy trình lọc 3 bước:
     * 1. Bỏ qua nếu ứng dụng gửi là SMS (nằm trong smsBlacklistPackages).
     * 2. Bỏ qua nếu ứng dụng gửi không phải ngân hàng/ví đã biết.
     * 3. Bỏ qua nếu nội dung chứa từ khóa quảng cáo (promotionalKeywords).
     */
    fun isBankNotification(packageName: String, title: String, text: String): Boolean {
        val lowerPkg = packageName.lowercase()
        val lowerText = text.lowercase()

        // Bước 1: Chặn mọi thông báo từ ứng dụng SMS
        if (smsBlacklistPackages.any { lowerPkg.contains(it.lowercase()) }) {
            Log.d(TAG, "Bỏ qua thông báo từ SMS: $packageName")
            return false
        }

        // Bước 2: Chỉ cho phép thông báo từ ngân hàng/ví đã biết
        val isKnownBank = isFromKnownBank(packageName, title)
        if (!isKnownBank) {
            Log.d(TAG, "Bỏ qua thông báo từ ứng dụng không xác định: $packageName | $title")
            return false
        }

        // Bước 3: Loại bỏ thông báo quảng cáo/khuyến mãi
        if (containsPromotionalKeywords(lowerText)) {
            Log.d(TAG, "Bỏ qua thông báo quảng cáo/khuyến mãi: $text")
            return false
        }

        return true
    }

    /**
     * Kiểm tra xem thông báo có phải từ ứng dụng ngân hàng/ví đã biết hay không.
     */
    private fun isFromKnownBank(packageName: String, title: String): Boolean {
        val lowerPkg = packageName.lowercase()
        val lowerTitle = title.lowercase()
        return banks.any { bank ->
            bank.packageKeywords.any { lowerPkg.contains(it.lowercase()) } ||
                    bank.titleKeywords.any { lowerTitle.contains(it.lowercase()) }
        }
    }

    /**
     * Kiểm tra xem nội dung thông báo có chứa từ khóa quảng cáo/khuyến mãi hay không.
     */
    private fun containsPromotionalKeywords(lowerText: String): Boolean {
        return promotionalKeywords.any { lowerText.contains(it.lowercase()) }
    }

    // ============================================================================
    // PHẦN 4: NHẬN DIỆN NGÂN HÀNG
    // ============================================================================

    /**
     * Nhận dạng tên ngân hàng từ Package Name hoặc Tiêu đề thông báo.
     */
    fun detectBankName(packageName: String, title: String): String {
        val lowerPkg = packageName.lowercase()
        val lowerTitle = title.lowercase()
        for (bank in banks) {
            val matchesPkg = bank.packageKeywords.any { lowerPkg.contains(it.lowercase()) }
            val matchesTitle = bank.titleKeywords.any { lowerTitle.contains(it.lowercase()) }
            if (matchesPkg || matchesTitle) {
                return bank.name
            }
        }
        return title.takeIf { it.isNotBlank() } ?: "Ngân hàng khác"
    }

    /**
     * Lấy màu sắc đặc trưng của ngân hàng.
     */
    fun getBankColor(bankName: String): String {
        return banks.find { it.name.equals(bankName, ignoreCase = true) }?.color ?: "#757575"
    }

    // ============================================================================
    // PHẦN 5: PHÂN TÍCH 3 TẦNG
    // ============================================================================

    /**
     * Phân tích thông báo theo kiến trúc 3 tầng.
     * @return NotificationParseOutput chứa kết quả và trạng thái phân tích.
     */
    suspend fun parseMultiTier(bankName: String, content: String, timestamp: Long): NotificationParseOutput {
        if (content.isBlank()) {
            return NotificationParseOutput(ParseResult.REJECTED)
        }

        // CHUẨN HÓA UNICODE VỀ DẠNG DỰNG SẴN (NFC)
        // Mục đích: Đồng bộ hóa các ký tự tiếng Việt (VD: "Số dư" dạng tổ hợp và "Số dư" dạng dựng sẵn)
        // để Regex nhận diện chính xác mà không bị lọt từ khóa.
        val normalizedContent = java.text.Normalizer.normalize(content, java.text.Normalizer.Form.NFC)

        var regexResult: ParsedData? = null
        val bankConfig = banks.find { it.name.equals(bankName, ignoreCase = true) }
        
        if (bankConfig?.parserConfig != null) {
            regexResult = parseWithRegex(bankName, normalizedContent, timestamp, bankConfig.parserConfig)
        }
        
        if (regexResult == null && globalParserConfig != null) {
            regexResult = parseWithRegex(bankName, normalizedContent, timestamp, globalParserConfig!!)
        }

        if (regexResult != null) {
            Log.d(TAG, "Phân tích thành công bằng Regex cho $bankName")
            return NotificationParseOutput(ParseResult.SUCCESS, regexResult, normalizedContent, bankName)
        }

        Log.d(TAG, "Phân tích thất bại cho $bankName. Từ chối.")
        return NotificationParseOutput(ParseResult.REJECTED, null, normalizedContent, bankName)
    }

    // ============================================================================
    // PHẦN 5.1: PHÂN TÍCH BẰNG REGEX
    // ============================================================================

    /**
     * Phân tích thông báo bằng Regex.
     * Ưu tiên sử dụng parserConfig riêng của ngân hàng, fallback sang globalParserConfig.
     */
    private fun parseWithRegex(bankName: String, content: String, timestamp: Long, config: BankParserConfig): ParsedData? {
        // 1. Phân tích Số tiền (amount) & Chiều giao dịch (isCredit)
        var amount = 0.0
        var isCredit = true
        var matchedAmount = false

        for (patternConfig in config.amountPatterns) {
            try {
                val regex = patternConfig.pattern.toRegex(RegexOption.IGNORE_CASE)
                val match = regex.find(content)
                if (match != null) {
                    when (patternConfig.type) {
                        "sign_amount" -> {
                            val sign = match.groupValues[patternConfig.signGroup]
                            val amountStr = match.groupValues[patternConfig.amountGroup].replace(".", "").replace(",", "")
                            val parsedVal = amountStr.toDoubleOrNull()
                            if (parsedVal != null) {
                                amount = parsedVal
                                isCredit = (sign == "+")
                                matchedAmount = true
                                break
                            }
                        }
                        "fixed_expense" -> {
                            val amountStr = match.groupValues[patternConfig.amountGroup].replace(".", "").replace(",", "")
                            val parsedVal = amountStr.toDoubleOrNull()
                            if (parsedVal != null) {
                                amount = parsedVal
                                isCredit = false
                                matchedAmount = true
                                break
                            }
                        }
                        "fixed_income" -> {
                            val amountStr = match.groupValues[patternConfig.amountGroup].replace(".", "").replace(",", "")
                            val parsedVal = amountStr.toDoubleOrNull()
                            if (parsedVal != null) {
                                amount = parsedVal
                                isCredit = true
                                matchedAmount = true
                                break
                            }
                        }
                        "raw_amount" -> {
                            val amountStr = match.groupValues[patternConfig.amountGroup].replace(".", "").replace(",", "")
                            val parsedVal = amountStr.toDoubleOrNull()
                            if (parsedVal != null) {
                                amount = parsedVal
                                val isExpense = globalExpenseKeywords.any { content.contains(it, ignoreCase = true) }
                                isCredit = !isExpense
                                matchedAmount = true
                                break
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi Regex amountPatterns: ${e.message}", e)
            }
        }

        if (!matchedAmount || amount == 0.0) return null

        // 2. Phân tích Số tài khoản (accountNumber)
        var accountNumber = "DEFAULT_ACC"
        for (patternStr in config.accountPatterns) {
            try {
                val regex = patternStr.toRegex(RegexOption.IGNORE_CASE)
                val match = regex.find(content)
                if (match != null) {
                    accountNumber = match.groupValues[1].trim()
                    break
                }
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi Regex accountPatterns: ${e.message}", e)
            }
        }

        // 3. Phân tích Nội dung giao dịch (content description)
        var parsedContent = "Giao dịch qua thông báo ngân hàng"
        for (patternStr in config.contentPatterns) {
            try {
                val regex = patternStr.toRegex(RegexOption.IGNORE_CASE)
                val match = regex.find(content)
                if (match != null) {
                    parsedContent = match.groupValues[1].trim()
                    break
                }
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi Regex contentPatterns: ${e.message}", e)
            }
        }

        // Loại bỏ phần số dư trong nội dung nếu có
        for (keyword in globalBalanceKeywords) {
            val index = parsedContent.lowercase().indexOf(keyword.lowercase())
            if (index != -1) {
                parsedContent = parsedContent.substring(0, index).trim()
                break
            }
        }
        parsedContent = parsedContent.trim().removeSuffix(".")

        // 4. Phân tích Đối tác (counterparty) — quét trên TOÀN BỘ content gốc
        var counterparty = "Chưa rõ đối tác"

        // Cách 1: Quét transferPatterns trên toàn bộ content gốc
        for (patternStr in config.transferPatterns) {
            try {
                val regex = patternStr.toRegex(RegexOption.IGNORE_CASE)
                val match = regex.find(content) // <-- quét trên content gốc, không phải parsedContent
                if (match != null) {
                    val potentialName = match.groupValues[1].trim()
                    if (potentialName.isNotBlank() && !globalExcludeKeywords.any { potentialName.lowercase().contains(it.lowercase()) }) {
                        counterparty = potentialName
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi Regex transferPatterns: ${e.message}", e)
            }
        }

        // Cách 2: Quét namePattern trên toàn bộ content gốc
        if (counterparty == "Chưa rõ đối tác" && globalNamePattern.isNotBlank()) {
            try {
                val regex = globalNamePattern.toRegex()
                val matches = regex.findAll(content).map { it.value.trim() }.toList()
                for (match in matches) {
                    val lowerMatch = match.lowercase()
                    val isExcluded = globalExcludeKeywords.any { keyword ->
                        val lk = keyword.lowercase()
                        lowerMatch == lk || lowerMatch.startsWith("$lk ") ||
                                lowerMatch.endsWith(" $lk") || lowerMatch.contains(" $lk ")
                    }
                    if (!isExcluded && match.split(" ").size >= 2) {
                        counterparty = match
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi Regex namePattern: ${e.message}", e)
            }
        }

        // Cách 3: Lấy vài từ đầu từ parsedContent
        if (counterparty == "Chưa rõ đối tác") {
            val words = parsedContent.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (words.size >= 2) {
                val candidate = words.take(3).joinToString(" ")
                if (!globalExcludeKeywords.any { candidate.lowercase().contains(it.lowercase()) }) {
                    counterparty = candidate
                }
            }
        }
        if (counterparty.isBlank()) counterparty = "Giao dịch nội địa"

        // 5. Phân tích Số dư hiện tại (balance)
        var parsedBalance: Double? = null
        for (patternStr in config.balancePatterns) {
            try {
                val regex = patternStr.toRegex(RegexOption.IGNORE_CASE)
                val match = regex.find(content)
                if (match != null) {
                    val balanceStr = match.groupValues[1].replace(".", "").replace(",", "")
                    parsedBalance = balanceStr.toDoubleOrNull()
                    if (parsedBalance != null) break
                }
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi Regex balancePatterns: ${e.message}", e)
            }
        }

        return ParsedData(
            bankName = bankName,
            accountNumber = accountNumber,
            amount = amount,
            isCredit = isCredit,
            counterparty = counterparty,
            content = parsedContent,
            timestamp = timestamp,
            balance = parsedBalance
        )
    }

    // ============================================================================
    // PHẦN 6: HÀM TIỆN ÍCH (backward compatibility)
    // ============================================================================

    /**
     * Hàm parse cũ (backward compatible) — gọi nội bộ parseMultiTier Tầng 1 (Regex only).
     * Dùng cho các phần code cũ chưa chuyển sang kiến trúc mới.
     */
    fun parse(bankName: String, content: String, timestamp: Long): ParsedData? {
        // Chuẩn hóa Unicode để khắc phục lỗi không khớp do chữ tổ hợp (Decomposed) và dựng sẵn (Precomposed)
        val normalizedContent = java.text.Normalizer.normalize(content, java.text.Normalizer.Form.NFC)
        val bankConfig = banks.find { it.name.equals(bankName, ignoreCase = true) }
        val parserConfig = bankConfig?.parserConfig ?: globalParserConfig ?: return null
        return parseWithRegex(bankName, normalizedContent, timestamp, parserConfig)
    }

    /**
     * @deprecated Sử dụng isBankNotification() thay thế.
     */
    fun isBankPackageOrKeywords(packageName: String, title: String, text: String): Boolean {
        return isBankNotification(packageName, title, text)
    }
}
