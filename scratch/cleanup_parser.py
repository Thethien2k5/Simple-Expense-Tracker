import re

with open("app/src/main/java/com/T2V/simple_expense_tracker/domain/parser/NotificationParser.kt", "r", encoding="utf-8") as f:
    text = f.read()

text = re.sub(r"import com\.google\.mlkit\.nl\.entityextraction\.\*\n", "", text)
text = re.sub(r"import kotlinx\.coroutines\.suspendCancellableCoroutine\n", "", text)
text = re.sub(r"import kotlin\.coroutines\.resume\n", "", text)

text = re.sub(r"// ML Kit Entity Extractor.*?private var entityExtractor: EntityExtractor\? = null\n", "", text, flags=re.DOTALL)
text = re.sub(r"\s*initMlKit\(\)", "", text)

new_parse_multi = """    suspend fun parseMultiTier(bankName: String, content: String, timestamp: Long): NotificationParseOutput {
        if (content.isBlank()) {
            return NotificationParseOutput(ParseResult.REJECTED)
        }

        var regexResult: ParsedData? = null
        val bankConfig = banks.find { it.name.equals(bankName, ignoreCase = true) }
        
        if (bankConfig?.parserConfig != null) {
            regexResult = parseWithRegex(bankName, content, timestamp, bankConfig.parserConfig)
        }
        
        if (regexResult == null && globalParserConfig != null) {
            regexResult = parseWithRegex(bankName, content, timestamp, globalParserConfig!!)
        }

        if (regexResult != null) {
            Log.d(TAG, "Phân tích thành công bằng Regex cho $bankName")
            return NotificationParseOutput(ParseResult.SUCCESS, regexResult, content, bankName)
        }

        Log.d(TAG, "Phân tích thất bại cho $bankName. Từ chối.")
        return NotificationParseOutput(ParseResult.REJECTED, null, content, bankName)
    }"""

old_parse_multi_pattern = r"suspend fun parseMultiTier.*?return NotificationParseOutput\(ParseResult\.NEEDS_MANUAL, null, content, bankName\)\n\s*\}"
text = re.sub(old_parse_multi_pattern, new_parse_multi, text, flags=re.DOTALL)

remove_pattern = r"// ============================================================================\n\s*// PHẦN 5\.2: TẦNG 2 - ML KIT ENTITY EXTRACTION.*?// ============================================================================\n\s*// PHẦN 6: HÀM TIỆN ÍCH"
text = re.sub(remove_pattern, "// ============================================================================\n    // PHẦN 6: HÀM TIỆN ÍCH", text, flags=re.DOTALL)

init_pattern = r"// ============================================================================\n\s*// PHẦN 2: KHỞI TẠO ML KIT.*?// ============================================================================\n\s*// PHẦN 3: LỌC THÔNG BÁO"
text = re.sub(init_pattern, "// ============================================================================\n    // PHẦN 3: LỌC THÔNG BÁO", text, flags=re.DOTALL)

text = text.replace("PHẦN 5.1: TẦNG 1 - PHÂN TÍCH BẰNG REGEX", "PHẦN 5.1: PHÂN TÍCH BẰNG REGEX")

with open("app/src/main/java/com/T2V/simple_expense_tracker/domain/parser/NotificationParser.kt", "w", encoding="utf-8") as f:
    f.write(text)
