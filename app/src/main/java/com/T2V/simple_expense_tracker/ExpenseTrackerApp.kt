package com.T2V.simple_expense_tracker

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Lớp Application gốc — bắt buộc để Hilt khởi tạo DI container.
 * Được khai báo trong AndroidManifest.xml qua android:name=".ExpenseTrackerApp".
 */
@HiltAndroidApp
class ExpenseTrackerApp : Application()
