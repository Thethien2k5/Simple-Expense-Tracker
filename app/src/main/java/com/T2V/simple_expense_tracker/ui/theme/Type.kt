package com.T2V.simple_expense_tracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography tokens tùy chỉnh theo thiết kế.
 * Thiết kế gốc dùng font Geist — tại đây dùng FontFamily.Default (sans-serif)
 * vì Geist chưa được nhúng vào tài nguyên ứng dụng.
 */
val AppTypography = Typography(
    // displayBalance: 48px, 300 weight, letter-spacing 0.05em
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Light,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        letterSpacing = 2.4.sp
    ),
    // headlineLg: 32px, 600 weight, -0.02em
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.64).sp
    ),
    // headlineLgMobile: 24px, 600 weight
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    // bodyMd: 16px, 400 weight
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    // bodyMd (alias for smaller contexts)
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    // labelCaps: 12px, 600 weight, 0.1em letter-spacing
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.2.sp
    ),
    // dataMono: 14px, 500 weight, 0.02em
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.28.sp
    )
)