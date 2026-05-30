package com.T2V.simple_expense_tracker.domain.parser

import android.content.Context
import android.util.Log
import com.google.mlkit.nl.entityextraction.*
import com.T2V.simple_expense_tracker.domain.config.ConfigManager
import com.T2V.simple_expense_tracker.domain.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

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

    // ML Kit Entity Extractor (khởi tạo lazy)
    private var entityExtractor: EntityExtractor? = null

    init {
        loadConfiguration()
        initMlKit()
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
    // PHẦN 2: KHỞI TẠO ML KIT
    // ============================================================================

    private fun initMlKit() {
        try {
            val options = EntityExtractorOptions.Builder(EntityExtractorOptions.ENGLISH)
                .build()
            entityExtractor = EntityExtraction.getClient(options)
            // Tải model xuống nếu chưa có
            entityExtractor?.downloadModelIfNeeded()
                ?.addOnSuccessListener { Log.d(TAG, "ML Kit Entity Extraction model đã sẵn sàng.") }
                ?.addOnFailureListener { Log.w(TAG, "Không thể tải ML Kit model: ${it.message}") }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khởi tạo ML Kit Entity Extraction: ${e.message}", e)
        }
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

        // === TẦNG 1: Regex từ JSON ===
        var regexResult: ParsedData? = null
        val bankConfig = banks.find { it.name.equals(bankName, ignoreCase = true) }
        
        // 1. Thử dùng parserConfig riêng của ngân hàng trước
        if (bankConfig?.parserConfig != null) {
            regexResult = parseWithRegex(bankName, content, timestamp, bankConfig.parserConfig)
        }
        
        // 2. Nếu thất bại hoặc không có config riêng, thử dùng globalParserConfig làm phương án dự phòng
        if (regexResult == null && globalParserConfig != null) {
            regexResult = parseWithRegex(bankName, content, timestamp, globalParserConfig!!)
        }

        if (regexResult != null) {
            Log.d(TAG, "Tầng 1 (Regex): Phân tích thành công cho $bankName")
            return NotificationParseOutput(ParseResult.SUCCESS, regexResult, content, bankName)
        }
        Log.d(TAG, "Tầng 1 (Regex): Thất bại cho $bankName, chuyển sang Tầng 2 (ML Kit).")

        // === TẦNG 2: ML Kit Entity Extraction ===
        val mlKitResult = parseWithMlKit(bankName, content, timestamp)
        if (mlKitResult != null) {
            Log.d(TAG, "Tầng 2 (ML Kit): Phân tích thành công cho $bankName")
            // Tự động cập nhật JSON cho lần sau (nếu có thể sinh Regex)
            tryAutoUpdateConfig(bankName, content, mlKitResult)
            return NotificationParseOutput(ParseResult.SUCCESS, mlKitResult, content, bankName)
        }
        Log.d(TAG, "Tầng 2 (ML Kit): Thất bại cho $bankName, yêu cầu xử lý thủ công.")

        // === TẦNG 3: Yêu cầu xử lý thủ công ===
        return NotificationParseOutput(ParseResult.NEEDS_MANUAL, null, content, bankName)
    }

    // ============================================================================
    // PHẦN 5.1: TẦNG 1 - PHÂN TÍCH BẰNG REGEX
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
    // PHẦN 5.2: TẦNG 2 - ML KIT ENTITY EXTRACTION
    // ============================================================================

    /**
     * Phân tích thông báo bằng ML Kit Entity Extraction.
     * Trích xuất thực thể TYPE_MONEY để lấy số tiền.
     */
    private suspend fun parseWithMlKit(bankName: String, content: String, timestamp: Long): ParsedData? {
        val extractor = entityExtractor ?: return null

        return try {
            val annotations = extractEntities(extractor, content)
            if (annotations.isEmpty()) return null

            // Tìm thực thể tiền tệ (TYPE_MONEY)
            var amount = 0.0
            var isCredit = true // Mặc định nhận tiền

            for (annotation in annotations) {
                for (entity in annotation.entities) {
                    if (entity is MoneyEntity) {
                        val matchedText = annotation.annotatedText
                        val parsedAmt = parseAmountFromText(matchedText)
                        if (parsedAmt != null) {
                            amount = parsedAmt
                        } else {
                            val intPart = entity.integerPart.toDouble()
                            val fracPart = entity.fractionalPart.toDouble() / 100.0
                            amount = intPart + fracPart
                        }
                        break
                    }
                }
                if (amount > 0.0) break
            }

            if (amount == 0.0) return null

            // Xác định chiều giao dịch (isCredit)
            // 1. Kiểm tra ký tự ngay trước chuỗi số tiền hoặc phần đầu chuỗi số tiền để lấy dấu cộng/trừ
            var signDetected: Boolean? = null
            for (annotation in annotations) {
                for (entity in annotation.entities) {
                    if (entity is MoneyEntity) {
                        val matchedText = annotation.annotatedText
                        if (matchedText.startsWith("+")) {
                            signDetected = true
                        } else if (matchedText.startsWith("-")) {
                            signDetected = false
                        } else {
                            val amountIndex = content.indexOf(matchedText)
                            if (amountIndex > 0) {
                                val prefix = content.substring(0, amountIndex).trim()
                                if (prefix.endsWith("+")) {
                                    signDetected = true
                                } else if (prefix.endsWith("-")) {
                                    signDetected = false
                                }
                            }
                        }
                        if (signDetected != null) break
                    }
                }
                if (signDetected != null) break
            }

            if (signDetected != null) {
                isCredit = signDetected
            } else {
                // 2. Fallback sang quét từ khóa chi tiêu nếu không tìm thấy dấu +/- rõ ràng
                val lowerContent = content.lowercase()
                val isExpense = globalExpenseKeywords.any { keyword ->
                    val k = keyword.lowercase()
                    lowerContent == k || lowerContent.startsWith("$k ") || lowerContent.endsWith(" $k") || lowerContent.contains(" $k ")
                }
                isCredit = !isExpense
            }

            var smartCounterparty = "Chưa rõ đối tác (Phân tích thông minh)"
            val extractedName = extractNameHeuristics(content)
            if (extractedName != null) {
                smartCounterparty = extractedName
            }

            ParsedData(
                bankName = bankName,
                accountNumber = "DEFAULT_ACC",
                amount = amount,
                isCredit = isCredit,
                counterparty = smartCounterparty,
                content = content.take(100),
                timestamp = timestamp,
                balance = null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi ML Kit Entity Extraction: ${e.message}", e)
            null
        }
    }

    /**
     * Helper: Trích xuất entities từ ML Kit bằng coroutine suspension.
     */
    private suspend fun extractEntities(extractor: EntityExtractor, text: String): List<EntityAnnotation> {
        return suspendCancellableCoroutine { continuation ->
            val params = EntityExtractionParams.Builder(text).build()
            extractor.annotate(params)
                .addOnSuccessListener { annotations ->
                    continuation.resume(annotations)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "ML Kit annotate thất bại: ${e.message}", e)
                    continuation.resume(emptyList())
                }
        }
    }

    /**
     * Thuật toán phân tích Heuristic để tìm Tên Người Việt Nam trong nội dung tự do.
     * Tìm các chuỗi Title Case (Nguyễn Thị Trân Hồng Trúc) hoặc ALL CAPS (TRAN HONG TRUC).
     */
    private fun extractNameHeuristics(content: String): String? {
        try {
            val titleCasePattern = Regex("(?:\\p{Lu}\\p{Ll}*\\s+){1,4}\\p{Lu}\\p{Ll}*")
            val titleMatches = titleCasePattern.findAll(content).map { it.value.trim() }.toList()

            for (match in titleMatches) {
                val words = match.split(Regex("\\s+"))
                if (words.size in 2..5) {
                    if (!globalExcludeKeywords.any { match.equals(it, ignoreCase = true) }) {
                        return match
                    }
                }
            }

            val allCapsPattern = Regex("(?:\\p{Lu}+\\s+){1,4}\\p{Lu}+")
            val capsMatches = allCapsPattern.findAll(content).map { it.value.trim() }.toList()
            
            for (match in capsMatches) {
                val words = match.split(Regex("\\s+"))
                if (words.size in 2..5) {
                    if (!globalExcludeKeywords.any { match.equals(it, ignoreCase = true) } && !match.contains("VND") && !match.contains(" SD ")) {
                        return match
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi phân tích Heuristic tên người: ${e.message}", e)
        }
        return null
    }

    // ============================================================================
    // PHẦN 5.3: TỰ ĐỘNG CẬP NHẬT JSON
    // ============================================================================

    /**
     * Cố gắng tự động cập nhật cấu hình JSON khi ML Kit phân tích thành công.
     * Giúp hệ thống "tự học" cho các lần sau.
     */
    private fun tryAutoUpdateConfig(bankName: String, content: String, parsedData: ParsedData) {
        try {
            // Tìm xem ngân hàng này đã có parserConfig chưa
            val bankConfig = banks.find { it.name.equals(bankName, ignoreCase = true) }
            if (bankConfig?.parserConfig != null) {
                // Đã có config riêng nhưng Regex thất bại -> Không tự động ghi đè
                Log.d(TAG, "Ngân hàng $bankName đã có parserConfig. Bỏ qua auto-update.")
                return
            }

            // Sinh Regex đơn giản từ kết quả ML Kit
            val amountStr = java.text.DecimalFormat("#,###").format(parsedData.amount)
                .replace(",", ".")  // chuẩn VN format
            val escapedAmount = Regex.escape(amountStr)
            val sign = if (parsedData.isCredit) "\\+" else "\\-"

            val newParserConfig = JSONObject().apply {
                val amtPatterns = JSONArray()
                amtPatterns.put(JSONObject().apply {
                    put("pattern", "${sign}\\s*${escapedAmount}\\s*(?:VND|đ|d)")
                    put("type", if (parsedData.isCredit) "fixed_income" else "fixed_expense")
                    put("amountGroup", 1)
                })
                put("amountPatterns", amtPatterns)
                put("accountPatterns", JSONArray())
                put("contentPatterns", JSONArray())
                put("balancePatterns", JSONArray())
                put("transferPatterns", JSONArray())
            }

            val pkgKeyword = bankConfig?.packageKeywords?.firstOrNull() ?: bankName.lowercase()
            val color = bankConfig?.color ?: "#757575"
            configManager.addBankParserConfig(bankName, color, pkgKeyword, newParserConfig)
            Log.d(TAG, "Đã tự động cập nhật parserConfig cho $bankName vào JSON.")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi tự động cập nhật JSON cho $bankName: ${e.message}", e)
        }
    }

    /**
     * Tự học Regex từ giao dịch nhập thủ công của người dùng.
     */
    fun tryLearnRegexFromManual(
        bankName: String,
        content: String,
        amount: Double,
        isCredit: Boolean,
        accountNumber: String
    ) {
        val parsedData = ParsedData(
            bankName = bankName,
            accountNumber = accountNumber,
            amount = amount,
            isCredit = isCredit,
            counterparty = "Nhập thủ công",
            content = content,
            timestamp = System.currentTimeMillis()
        )
        tryAutoUpdateConfig(bankName, content, parsedData)
    }

    private fun parseAmountFromText(text: String): Double? {
        val cleanText = text.replace(Regex("[^0-9.,]"), "")
        if (cleanText.isEmpty()) return null

        return try {
            if (cleanText.contains(".") && cleanText.contains(",")) {
                val lastDotIdx = cleanText.lastIndexOf('.')
                val lastCommaIdx = cleanText.lastIndexOf(',')
                if (lastDotIdx > lastCommaIdx) {
                    cleanText.replace(",", "").toDoubleOrNull()
                } else {
                    cleanText.replace(".", "").replace(",", ".").toDoubleOrNull()
                }
            } else if (cleanText.contains(",") && !cleanText.contains(".")) {
                val parts = cleanText.split(",")
                if (parts.size == 2 && parts[1].length == 3) {
                    cleanText.replace(",", "").toDoubleOrNull()
                } else if (parts.size > 2) {
                    cleanText.replace(",", "").toDoubleOrNull()
                } else {
                    cleanText.replace(",", ".").toDoubleOrNull()
                }
            } else if (cleanText.contains(".") && !cleanText.contains(",")) {
                val parts = cleanText.split(".")
                if (parts.size == 2 && parts[1].length == 3) {
                    cleanText.replace(".", "").toDoubleOrNull()
                } else if (parts.size > 2) {
                    cleanText.replace(".", "").toDoubleOrNull()
                } else {
                    cleanText.toDoubleOrNull()
                }
            } else {
                cleanText.toDoubleOrNull()
            }
        } catch (e: Exception) {
            null
        }
    }

    // ============================================================================
    // PHẦN 6: HÀM TIỆN ÍCH (backward compatibility)
    // ============================================================================

    /**
     * Hàm parse cũ (backward compatible) — gọi nội bộ parseMultiTier Tầng 1 (Regex only).
     * Dùng cho các phần code cũ chưa chuyển sang kiến trúc mới.
     */
    fun parse(bankName: String, content: String, timestamp: Long): ParsedData? {
        val bankConfig = banks.find { it.name.equals(bankName, ignoreCase = true) }
        val parserConfig = bankConfig?.parserConfig ?: globalParserConfig ?: return null
        return parseWithRegex(bankName, content, timestamp, parserConfig)
    }

    /**
     * @deprecated Sử dụng isBankNotification() thay thế.
     */
    fun isBankPackageOrKeywords(packageName: String, title: String, text: String): Boolean {
        return isBankNotification(packageName, title, text)
    }
}
