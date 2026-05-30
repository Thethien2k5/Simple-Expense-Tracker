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
        syncConfigWithAssets()
    }

    /**
     * Đồng bộ file cấu hình: nếu file ở Internal Storage chưa có, hoặc version 
     * trong assets lớn hơn version hiện tại, thì ghi đè file từ assets.
     */
    private fun syncConfigWithAssets() {
        if (!configFile.exists()) {
            copyFromAssets()
            return
        }
        try {
            val assetJsonString = context.assets.open(CONFIG_FILE_NAME).bufferedReader().use { it.readText() }
            val assetConfig = JSONObject(assetJsonString)
            val assetVersion = assetConfig.optInt("version", 0)

            val internalConfig = loadConfig()
            val internalVersion = internalConfig.optInt("version", 0)

            if (assetVersion > internalVersion) {
                Log.d(TAG, "Cập nhật $CONFIG_FILE_NAME từ assets (Version $assetVersion > $internalVersion)")
                copyFromAssets()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi đồng bộ $CONFIG_FILE_NAME: ${e.message}", e)
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

}
