package com.T2V.simple_expense_tracker.domain.config

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Quản lý tệp cấu hình banks_config.json động.
 * Sao chép từ assets sang Internal Storage khi lần đầu khởi chạy,
 * sau đó đọc/ghi tại Internal Storage để hỗ trợ cập nhật Regex tự động.
 */
@Singleton
class ConfigManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ConfigManager"
        private const val CONFIG_FILE_NAME = "banks_config.json"
    }

    private val configFile: File = File(context.filesDir, CONFIG_FILE_NAME)

    init {
        if (!configFile.exists()) {
            copyFromAssets()
        }
    }

    /**
     * Sao chép tệp cấu hình mặc định từ assets sang Internal Storage.
     */
    private fun copyFromAssets() {
        try {
            context.assets.open(CONFIG_FILE_NAME).use { input ->
                configFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Log.d(TAG, "Đã sao chép $CONFIG_FILE_NAME từ assets sang Internal Storage.")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi sao chép $CONFIG_FILE_NAME từ assets: ${e.message}", e)
        }
    }

    /**
     * Đọc cấu hình JSON từ Internal Storage.
     * @return JSONObject chứa toàn bộ cấu hình, hoặc JSONObject rỗng nếu có lỗi.
     */
    fun loadConfig(): JSONObject {
        return try {
            val jsonString = configFile.readText()
            JSONObject(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi đọc $CONFIG_FILE_NAME từ Internal Storage: ${e.message}", e)
            JSONObject()
        }
    }

    /**
     * Ghi cấu hình JSON xuống Internal Storage (định dạng đẹp với indent = 2).
     * @param config JSONObject cần lưu.
     */
    fun saveConfig(config: JSONObject) {
        try {
            configFile.writeText(config.toString(2))
            Log.d(TAG, "Đã lưu cấu hình vào $CONFIG_FILE_NAME tại Internal Storage.")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi ghi $CONFIG_FILE_NAME: ${e.message}", e)
        }
    }

    /**
     * Thêm hoặc cập nhật cấu hình phân tích (parserConfig) cho một ngân hàng cụ thể.
     * Nếu ngân hàng chưa tồn tại trong danh sách, thêm mới.
     * Nếu đã tồn tại, cập nhật parserConfig.
     *
     * @param bankName Tên ngân hàng (ví dụ: "Vietcombank").
     * @param color Mã màu HEX đại diện (ví dụ: "#4CAF50").
     * @param packageKeyword Từ khóa nhận diện package (ví dụ: "vietcombank").
     * @param parserConfig JSONObject chứa các regex pattern phân tích thông báo.
     */
    fun addBankParserConfig(bankName: String, color: String, packageKeyword: String, parserConfig: JSONObject) {
        try {
            val config = loadConfig()
            val banksArray = config.optJSONArray("banks") ?: return

            var found = false
            for (i in 0 until banksArray.length()) {
                val bankObj = banksArray.getJSONObject(i)
                if (bankObj.optString("name").equals(bankName, ignoreCase = true)) {
                    bankObj.put("parserConfig", parserConfig)
                    found = true
                    break
                }
            }

            if (!found) {
                val newBank = JSONObject()
                newBank.put("name", bankName)
                newBank.put("color", color)
                newBank.put("packageKeywords", org.json.JSONArray().put(packageKeyword))
                newBank.put("titleKeywords", org.json.JSONArray().put(bankName.lowercase()))
                newBank.put("parserConfig", parserConfig)
                banksArray.put(newBank)
            }

            saveConfig(config)
            Log.d(TAG, "Đã cập nhật parserConfig cho ngân hàng: $bankName")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi cập nhật parserConfig cho $bankName: ${e.message}", e)
        }
    }
}
