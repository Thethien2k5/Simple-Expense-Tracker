package com.T2V.simple_expense_tracker.domain.parser

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

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
    val timestamp: Long,
    val balance: Double? = null
)

data class BankConfig(
    val name: String,
    val color: String,
    val packageKeywords: List<String>,
    val titleKeywords: List<String>
)

data class AmountPattern(
    val pattern: String,
    val type: String,
    val signGroup: Int,
    val amountGroup: Int
)

@Singleton
class NotificationParser @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Loaded configs
    private val banks = mutableListOf<BankConfig>()
    private val amountPatterns = mutableListOf<AmountPattern>()
    private val expenseKeywords = mutableListOf<String>()
    private val accountPatterns = mutableListOf<String>()
    private val contentPatterns = mutableListOf<String>()
    private val balanceKeywords = mutableListOf<String>()
    private val excludeKeywords = mutableListOf<String>()
    private val transferPatterns = mutableListOf<String>()
    private val balancePatterns = mutableListOf<String>()
    private var namePattern: String = ""

    init {
        loadConfiguration()
    }

    private fun loadConfiguration() {
        try {
            val jsonString = context.assets.open("banks_config.json").bufferedReader().use { it.readText() }
            val root = JSONObject(jsonString)

            // 1. Parse banks
            val banksArray = root.optJSONArray("banks")
            if (banksArray != null) {
                for (i in 0 until banksArray.length()) {
                    val bankObj = banksArray.getJSONObject(i)
                    val name = bankObj.optString("name", "")
                    val color = bankObj.optString("color", "#757575")
                    
                    val pkgKeywords = mutableListOf<String>()
                    val pkgArray = bankObj.optJSONArray("packageKeywords")
                    if (pkgArray != null) {
                        for (j in 0 until pkgArray.length()) {
                            pkgKeywords.add(pkgArray.getString(j))
                        }
                    }
                    
                    val titleKeywords = mutableListOf<String>()
                    val titleArray = bankObj.optJSONArray("titleKeywords")
                    if (titleArray != null) {
                        for (j in 0 until titleArray.length()) {
                            titleKeywords.add(titleArray.getString(j))
                        }
                    }
                    
                    banks.add(BankConfig(name, color, pkgKeywords, titleKeywords))
                }
            }

            // 2. Parse parserConfig
            val parserConfig = root.optJSONObject("parserConfig")
            if (parserConfig != null) {
                // amountPatterns
                val amtArray = parserConfig.optJSONArray("amountPatterns")
                if (amtArray != null) {
                    for (i in 0 until amtArray.length()) {
                        val amtObj = amtArray.getJSONObject(i)
                        val pattern = amtObj.optString("pattern", "")
                        val type = amtObj.optString("type", "")
                        val signGroup = amtObj.optInt("signGroup", 1)
                        val amountGroup = amtObj.optInt("amountGroup", 2)
                        if (pattern.isNotEmpty() && type.isNotEmpty()) {
                            amountPatterns.add(AmountPattern(pattern, type, signGroup, amountGroup))
                        }
                    }
                }

                // expenseKeywords
                val expArray = parserConfig.optJSONArray("expenseKeywords")
                if (expArray != null) {
                    for (i in 0 until expArray.length()) {
                        expenseKeywords.add(expArray.getString(i))
                    }
                }

                // accountPatterns
                val accArray = parserConfig.optJSONArray("accountPatterns")
                if (accArray != null) {
                    for (i in 0 until accArray.length()) {
                        accountPatterns.add(accArray.getString(i))
                    }
                }

                // contentPatterns
                val contentArray = parserConfig.optJSONArray("contentPatterns")
                if (contentArray != null) {
                    for (i in 0 until contentArray.length()) {
                        contentPatterns.add(contentArray.getString(i))
                    }
                }

                // balanceKeywords
                val balArray = parserConfig.optJSONArray("balanceKeywords")
                if (balArray != null) {
                    for (i in 0 until balArray.length()) {
                        balanceKeywords.add(balArray.getString(i))
                    }
                }

                // excludeKeywords
                val exclArray = parserConfig.optJSONArray("excludeKeywords")
                if (exclArray != null) {
                    for (i in 0 until exclArray.length()) {
                        excludeKeywords.add(exclArray.getString(i))
                    }
                }

                // transferPatterns
                val transArray = parserConfig.optJSONArray("transferPatterns")
                if (transArray != null) {
                    for (i in 0 until transArray.length()) {
                        transferPatterns.add(transArray.getString(i))
                    }
                }

                // namePattern
                namePattern = parserConfig.optString("namePattern", "")

                // balancePatterns
                val balPatArray = parserConfig.optJSONArray("balancePatterns")
                if (balPatArray != null) {
                    for (i in 0 until balPatArray.length()) {
                        balancePatterns.add(balPatArray.getString(i))
                    }
                }
            }
            Log.d("NotificationParser", "Đã tải cấu hình thành công từ banks_config.json. Đã đọc được ${banks.size} ngân hàng.")
        } catch (e: Exception) {
            Log.e("NotificationParser", "Lỗi tải cấu hình từ banks_config.json. Sử dụng cấu hình mặc định (fallback).", e)
            loadFallbackConfiguration()
        }
    }

    private fun loadFallbackConfiguration() {
        // Safe defaults
        banks.clear()
        banks.add(BankConfig("Vietcombank", "#4CAF50", listOf("vietcombank"), listOf("vietcombank", "vcb")))
        banks.add(BankConfig("Techcombank", "#E53935", listOf("tcb"), listOf("techcombank", "tcb")))
        banks.add(BankConfig("MB Bank", "#1E88E5", listOf("mbmobile"), listOf("mb bank", "mbbank")))
        banks.add(BankConfig("TPBank", "#8E24AA", listOf("tpbank"), listOf("tpbank", "tpb")))
        banks.add(BankConfig("BIDV", "#0D47A1", listOf("bidv"), listOf("bidv")))
        banks.add(BankConfig("VPBank", "#2E7D32", listOf("vpb"), listOf("vpbank", "vp bank")))
        banks.add(BankConfig("Cake by VPBank", "#EC407A", listOf("cake"), listOf("cake")))
        banks.add(BankConfig("Sacombank", "#0288D1", listOf("sacombank"), listOf("sacombank")))
        banks.add(BankConfig("ACB", "#0091EA", listOf("acb"), listOf("acb")))
        banks.add(BankConfig("MSB", "#7E57C2", listOf("msb"), listOf("msb")))
        banks.add(BankConfig("TNEX", "#00CBD6", listOf("tnex", "msb.tnex"), listOf("tnex")))

        amountPatterns.clear()
        amountPatterns.add(AmountPattern("(?:(?:GD|giao dịch)\\s*)?([\\+\\-])\\s*([0-9.,]+)\\s*(?:VND|đ|d|vnd|Vnd)", "sign_amount", 1, 2))
        amountPatterns.add(AmountPattern("(?:trừ|tru)\\s*([0-9.,]+)\\s*(?:VND|đ|d|vnd)", "fixed_expense", 0, 1))
        amountPatterns.add(AmountPattern("(?:tăng|tang|cộng|cong|nhận|nhan)\\s*([0-9.,]+)", "fixed_income", 0, 1))
        amountPatterns.add(AmountPattern("(?:giảm|giam|trừ|tru)\\s*([0-9.,]+)", "fixed_expense", 0, 1))
        amountPatterns.add(AmountPattern("([0-9.,]+)\\s*(?:VND|đ|d|vnd)", "raw_amount", 0, 1))

        expenseKeywords.clear()
        expenseKeywords.addAll(listOf("trừ", "tru", "giảm", "giam", "thanh toan", "chuyển khoản đi", "-"))

        accountPatterns.clear()
        accountPatterns.add("(?:TK|tài khoản(?: thanh toán)?|tai khoan(?: thanh toan)?|so tk|account)\\s*[:\\-]?\\s*([0-9xX]+)")
        accountPatterns.add("\\b([0-9]{8,15})\\b")

        contentPatterns.clear()
        contentPatterns.add("(?:ND|nội dung(?: giao dịch| gd)?|noi dung(?: giao dich| gd)?|nd|ref|ly do|lý do)\\s*[:\\-]?\\s*(.+)")

        balanceKeywords.clear()
        balanceKeywords.addAll(listOf("số dư", "so du", "sd:", "balance"))

        excludeKeywords.clear()
        excludeKeywords.addAll(listOf(
            "chuyen", "chuyển", "tien", "tiền", "thanh toan", "thanh toán", 
            "an trua", "an sang", "mua", "nap", "nạp", "tra no", "trả nợ", 
            "luong", "lương", "thang", "tháng", "gd", "ck", "banking", "ib", "mb",
            "chuyen khoan", "chuyển khoản", "nop", "nộp", "rut", "rút", "mat", "mặt"
        ))

        transferPatterns.clear()
        transferPatterns.add("(?:từ|tu|bởi|by|tới|toi|gửi|gui|nhận|nhan|khach hang|kh)\\s+([A-Z0-9\\s]{3,30}?)(?:\\s+chuyen|\\s+ck|\\s+nap|\\s+thanh|\\s+toi|\\s+tai|$)")

        namePattern = "\\b([A-Z]{2,10}(?:\\s+[A-Z]{1,10}){1,3})\\b"

        balancePatterns.clear()
        balancePatterns.add("(?:SD|số dư|so du|balance)\\s*[:\\-]?\\s*([0-9.,]{4,15})")
        balancePatterns.add("(?:SD|số dư|so du|balance)[^0-9]*?là\\s*([0-9.,]{4,15})")
        balancePatterns.add("(?:SD|số dư|so du|balance)[^0-9]*?[:\\-]\\s*([0-9.,]{4,15})")
    }

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
     * Lấy màu sắc đặc trưng của từng ngân hàng để đồng bộ trực quan trên giao diện Dashboard.
     */
    fun getBankColor(bankName: String): String {
        val bank = banks.find { it.name.equals(bankName, ignoreCase = true) }
        return bank?.color ?: "#757575"
    }

    /**
     * Nhận dạng xem thông báo có phải từ ứng dụng ngân hàng hoặc chứa biến động số dư chuyển khoản hay không.
     */
    fun isBankPackageOrKeywords(packageName: String, title: String, text: String): Boolean {
        val lowerPkg = packageName.lowercase()
        val lowerTitle = title.lowercase()
        val lowerText = text.lowercase()

        // 1. Kiểm tra xem có thuộc danh sách package/title của ngân hàng nào đã định nghĩa không
        var isBank = false
        for (bank in banks) {
            val matchesPkg = bank.packageKeywords.any { lowerPkg.contains(it.lowercase()) }
            val matchesTitle = bank.titleKeywords.any { lowerTitle.contains(it.lowercase()) }
            if (matchesPkg || matchesTitle) {
                isBank = true
                break
            }
        }

        // 2. Kiểm tra từ khóa giao dịch tài chính thông dụng
        val hasFluctuationKeywords = balanceKeywords.any { lowerText.contains(it.lowercase()) } ||
                lowerText.contains("biến động") || lowerText.contains("bien dong") ||
                lowerText.contains("tài khoản") || lowerText.contains("tai khoan") ||
                lowerText.contains("giao dịch") || lowerText.contains("giao dich") ||
                lowerText.contains("vnd") || lowerText.contains("chuyển khoản") ||
                lowerText.contains("chuyen khoan")

        val hasSignKeywords = lowerText.contains("+") || lowerText.contains("-") ||
                lowerText.contains("cộng") || lowerText.contains("cong") ||
                lowerText.contains("trừ") || lowerText.contains("tru") ||
                lowerText.contains("tăng") || lowerText.contains("tang") ||
                lowerText.contains("giảm") || lowerText.contains("giam") ||
                lowerText.contains("nhận") || lowerText.contains("nhan")

        val hasKeywords = hasFluctuationKeywords && hasSignKeywords

        return isBank || hasKeywords
    }

    fun parse(bankName: String, content: String, timestamp: Long): ParsedData? {
        if (content.isBlank()) return null

        var amount = 0.0
        var isCredit = true
        var matchedAmount = false

        // 1. Phân tích Số tiền (amount) & Chiều giao dịch (isCredit)
        for (patternConfig in amountPatterns) {
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
                                // Mặc định là chi tiêu (-) nếu chứa các từ khóa thanh toán/trừ
                                val isExpense = expenseKeywords.any { content.contains(it, ignoreCase = true) }
                                isCredit = !isExpense
                                matchedAmount = true
                                break
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("NotificationParser", "Lỗi thực thi Regex của amountPatterns: ${e.message}", e)
            }
        }

        if (!matchedAmount || amount == 0.0) return null

        // 2. Phân tích Số tài khoản (accountNumber)
        var accountNumber = "DEFAULT_ACC"
        for (patternStr in accountPatterns) {
            try {
                val regex = patternStr.toRegex(RegexOption.IGNORE_CASE)
                val match = regex.find(content)
                if (match != null) {
                    accountNumber = match.groupValues[1]
                    break
                }
            } catch (e: Exception) {
                Log.e("NotificationParser", "Lỗi thực thi Regex của accountPatterns: ${e.message}", e)
            }
        }

        // 3. Phân tích Nội dung giao dịch (content)
        var parsedContent = "Giao dịch qua thông báo ngân hàng"
        for (patternStr in contentPatterns) {
            try {
                val regex = patternStr.toRegex(RegexOption.IGNORE_CASE)
                val match = regex.find(content)
                if (match != null) {
                    parsedContent = match.groupValues[1].trim()
                    break
                }
            } catch (e: Exception) {
                Log.e("NotificationParser", "Lỗi thực thi Regex của contentPatterns: ${e.message}", e)
            }
        }

        // Loại bỏ thông tin số dư tiếp theo trong nội dung nếu có
        for (keyword in balanceKeywords) {
            val index = parsedContent.lowercase().indexOf(keyword.lowercase())
            if (index != -1) {
                parsedContent = parsedContent.substring(0, index).trim()
                break
            }
        }
        parsedContent = parsedContent.trim().removeSuffix(".")

        // 4. Phân tích Đối tác (counterparty)
        var counterparty = "Chưa rõ đối tác"
        
        // Cách 1: Quét các từ chỉ định người gửi/nhận
        for (patternStr in transferPatterns) {
            try {
                val regex = patternStr.toRegex(RegexOption.IGNORE_CASE)
                val match = regex.find(parsedContent)
                if (match != null) {
                    val potentialName = match.groupValues[1].trim()
                    if (potentialName.isNotBlank() && !excludeKeywords.any { potentialName.lowercase().contains(it.lowercase()) }) {
                        counterparty = potentialName
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e("NotificationParser", "Lỗi thực thi Regex của transferPatterns: ${e.message}", e)
            }
        }
        
        // Cách 2: Quét chuỗi gồm 2 đến 4 từ viết hoa liên tiếp trong nội dung
        if (counterparty == "Chưa rõ đối tác" && namePattern.isNotBlank()) {
            try {
                val regex = namePattern.toRegex()
                val matches = regex.findAll(parsedContent).map { it.value.trim() }.toList()
                for (match in matches) {
                    val lowerMatch = match.lowercase()
                    val isExcluded = excludeKeywords.any { keyword -> 
                        val lowerKeyword = keyword.lowercase()
                        lowerMatch == lowerKeyword || 
                        lowerMatch.startsWith(lowerKeyword + " ") || 
                        lowerMatch.endsWith(" " + lowerKeyword) ||
                        lowerMatch.contains(" " + lowerKeyword + " ")
                    }
                    if (!isExcluded && match.split(" ").size >= 2) {
                        counterparty = match
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e("NotificationParser", "Lỗi thực thi Regex của namePattern: ${e.message}", e)
            }
        }
        
        // Cách 3: Nếu vẫn chưa ra, lấy 2-3 từ đầu tiên của nội dung chuyển tiền làm tên tạm
        if (counterparty == "Chưa rõ đối tác") {
            val words = parsedContent.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (words.size >= 2) {
                val candidate = words.take(3).joinToString(" ")
                if (!excludeKeywords.any { candidate.lowercase().contains(it.lowercase()) }) {
                    counterparty = candidate
                }
            }
        }

        if (counterparty.isBlank()) {
            counterparty = "Giao dịch nội địa"
        }

        // 5. Phân tích Số dư hiện tại (balance)
        var parsedBalance: Double? = null
        for (patternStr in balancePatterns) {
            try {
                val regex = patternStr.toRegex(RegexOption.IGNORE_CASE)
                val match = regex.find(content)
                if (match != null) {
                    val balanceStr = match.groupValues[1].replace(".", "").replace(",", "")
                    val parsedBal = balanceStr.toDoubleOrNull()
                    if (parsedBal != null) {
                        parsedBalance = parsedBal
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e("NotificationParser", "Lỗi thực thi Regex của balancePatterns: ${e.message}", e)
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
}
