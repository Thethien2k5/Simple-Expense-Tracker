package com.T2V.simple_expense_tracker.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.T2V.simple_expense_tracker.domain.repository.AppLanguage

enum class AppTheme(
    val colorScheme: ColorScheme,
    val isDark: Boolean
) {
    EMERALD(
        colorScheme = darkColorScheme(
            primary = DarkBluePrimary,
            onPrimary = DarkBlueOnPrimary,
            primaryContainer = DarkBluePrimaryContainer,
            onPrimaryContainer = DarkBlueOnPrimaryContainer,
            secondary = DarkBlueSecondary,
            onSecondary = DarkBlueOnSecondary,
            secondaryContainer = DarkBlueSecondaryContainer,
            onSecondaryContainer = DarkBlueOnSecondaryContainer,
            tertiary = DarkBlueTertiary,
            onTertiary = DarkBlueOnTertiary,
            tertiaryContainer = DarkBlueTertiaryContainer,
            onTertiaryContainer = DarkBlueOnTertiaryContainer,
            background = DarkBlueBackground,
            onBackground = DarkBlueOnBackground,
            surface = DarkBlueSurface,
            onSurface = DarkBlueOnSurface,
            surfaceVariant = DarkBlueSurfaceVariant,
            onSurfaceVariant = DarkBlueOnSurfaceVariant,
            surfaceTint = DarkBlueSurfaceTint,
            error = Error,
            onError = OnError,
            errorContainer = ErrorContainer,
            onErrorContainer = OnErrorContainer,
            outline = Outline,
            outlineVariant = OutlineVariant
        ),
        isDark = true
    ),
    OCEAN(
        colorScheme = lightColorScheme(
            primary = OceanPrimary,
            onPrimary = OceanOnPrimary,
            primaryContainer = OceanPrimaryContainer,
            onPrimaryContainer = OceanOnPrimaryContainer,
            secondary = OceanSecondary,
            onSecondary = OceanOnSecondary,
            secondaryContainer = OceanSecondaryContainer,
            onSecondaryContainer = OceanOnSecondaryContainer,
            tertiary = OceanTertiary,
            onTertiary = OceanOnTertiary,
            tertiaryContainer = OceanTertiaryContainer,
            onTertiaryContainer = OceanOnTertiaryContainer,
            background = OceanLightBackground,
            onBackground = OceanLightOnBackground,
            surface = OceanLightSurface,
            onSurface = OceanLightOnSurface,
            surfaceVariant = OceanSurfaceVariant,
            onSurfaceVariant = OceanOnSurfaceVariant,
            surfaceTint = OceanSurfaceTint,
            error = Error,
            onError = OnError,
            errorContainer = ErrorContainer,
            onErrorContainer = OnErrorContainer,
            outline = Outline,
            outlineVariant = OutlineVariant
        ),
        isDark = false
    ),
    FOREST(
        colorScheme = lightColorScheme(
            primary = ForestPrimary,
            onPrimary = ForestOnPrimary,
            primaryContainer = ForestPrimaryContainer,
            onPrimaryContainer = ForestOnPrimaryContainer,
            secondary = ForestSecondary,
            onSecondary = ForestOnSecondary,
            secondaryContainer = ForestSecondaryContainer,
            onSecondaryContainer = ForestOnSecondaryContainer,
            tertiary = ForestTertiary,
            onTertiary = ForestOnTertiary,
            tertiaryContainer = ForestTertiaryContainer,
            onTertiaryContainer = ForestOnTertiaryContainer,
            background = ForestLightBackground,
            onBackground = ForestLightOnBackground,
            surface = ForestLightSurface,
            onSurface = ForestLightOnSurface,
            surfaceVariant = ForestSurfaceVariant,
            onSurfaceVariant = ForestOnSurfaceVariant,
            surfaceTint = ForestSurfaceTint,
            error = Error,
            onError = OnError,
            errorContainer = ErrorContainer,
            onErrorContainer = OnErrorContainer,
            outline = Outline,
            outlineVariant = OutlineVariant
        ),
        isDark = false
    ),
    SUNSET(
        colorScheme = darkColorScheme(
            primary = SunsetPrimary,
            onPrimary = SunsetOnPrimary,
            primaryContainer = SunsetPrimaryContainer,
            onPrimaryContainer = SunsetOnPrimaryContainer,
            secondary = SunsetSecondary,
            onSecondary = SunsetOnSecondary,
            secondaryContainer = SunsetSecondaryContainer,
            onSecondaryContainer = SunsetOnSecondaryContainer,
            tertiary = SunsetTertiary,
            onTertiary = SunsetOnTertiary,
            tertiaryContainer = SunsetTertiaryContainer,
            onTertiaryContainer = SunsetOnTertiaryContainer,
            background = SunsetBackground,
            onBackground = SunsetOnBackground,
            surface = SunsetSurface,
            onSurface = SunsetOnSurface,
            surfaceVariant = SunsetSurfaceVariant,
            onSurfaceVariant = SunsetOnSurfaceVariant,
            surfaceTint = SunsetSurfaceTint,
            error = Error,
            onError = OnError,
            errorContainer = ErrorContainer,
            onErrorContainer = OnErrorContainer,
            outline = Outline,
            outlineVariant = OutlineVariant
        ),
        isDark = true
    ),
    CANDY(
        colorScheme = lightColorScheme(
            primary = CandyPrimary,
            onPrimary = CandyOnPrimary,
            primaryContainer = CandyPrimaryContainer,
            onPrimaryContainer = CandyOnPrimaryContainer,
            secondary = CandySecondary,
            onSecondary = CandyOnSecondary,
            secondaryContainer = CandySecondaryContainer,
            onSecondaryContainer = CandyOnSecondaryContainer,
            tertiary = CandyTertiary,
            onTertiary = CandyOnTertiary,
            tertiaryContainer = CandyTertiaryContainer,
            onTertiaryContainer = CandyOnTertiaryContainer,
            background = CandyLightBackground,
            onBackground = CandyLightOnBackground,
            surface = CandyLightSurface,
            onSurface = CandyLightOnSurface,
            surfaceVariant = CandySurfaceVariant,
            onSurfaceVariant = CandyOnSurfaceVariant,
            surfaceTint = CandySurfaceTint,
            error = Error,
            onError = OnError,
            errorContainer = ErrorContainer,
            onErrorContainer = OnErrorContainer,
            outline = Outline,
            outlineVariant = OutlineVariant
        ),
        isDark = false
    ),
    LUXURY(
        colorScheme = darkColorScheme(
            primary = LuxuryPrimary,
            onPrimary = LuxuryOnPrimary,
            primaryContainer = LuxuryPrimaryContainer,
            onPrimaryContainer = LuxuryOnPrimaryContainer,
            secondary = LuxurySecondary,
            onSecondary = LuxuryOnSecondary,
            secondaryContainer = LuxurySecondaryContainer,
            onSecondaryContainer = LuxuryOnSecondaryContainer,
            tertiary = LuxuryTertiary,
            onTertiary = LuxuryOnTertiary,
            tertiaryContainer = LuxuryTertiaryContainer,
            onTertiaryContainer = LuxuryOnTertiaryContainer,
            background = LuxuryBackground,
            onBackground = LuxuryOnBackground,
            surface = LuxurySurface,
            onSurface = LuxuryOnSurface,
            surfaceVariant = LuxurySurfaceVariant,
            onSurfaceVariant = LuxuryOnSurfaceVariant,
            surfaceTint = LuxurySurfaceTint,
            error = Error,
            onError = OnError,
            errorContainer = ErrorContainer,
            onErrorContainer = OnErrorContainer,
            outline = Outline,
            outlineVariant = OutlineVariant
        ),
        isDark = true
    ),
    MINIMAL(
        colorScheme = lightColorScheme(
            primary = MinimalPrimary,
            onPrimary = MinimalOnPrimary,
            primaryContainer = MinimalPrimaryContainer,
            onPrimaryContainer = MinimalOnPrimaryContainer,
            secondary = MinimalSecondary,
            onSecondary = MinimalOnSecondary,
            secondaryContainer = MinimalSecondaryContainer,
            onSecondaryContainer = MinimalOnSecondaryContainer,
            tertiary = MinimalTertiary,
            onTertiary = MinimalOnTertiary,
            tertiaryContainer = MinimalTertiaryContainer,
            onTertiaryContainer = MinimalOnTertiaryContainer,
            background = MinimalLightBackground,
            onBackground = MinimalLightOnBackground,
            surface = MinimalLightSurface,
            onSurface = MinimalLightOnSurface,
            surfaceVariant = MinimalSurfaceVariant,
            onSurfaceVariant = MinimalOnSurfaceVariant,
            surfaceTint = MinimalSurfaceTint,
            error = Error,
            onError = OnError,
            errorContainer = ErrorContainer,
            onErrorContainer = OnErrorContainer,
            outline = Outline,
            outlineVariant = OutlineVariant
        ),
        isDark = false
    );

    fun getLocalizedName(language: AppLanguage): String {
        return when (this) {
            EMERALD -> when (language) {
                AppLanguage.VIETNAMESE -> "Ngọc lục bảo"
                AppLanguage.CHINESE -> "翡翠"
                AppLanguage.RUSSIAN -> "Изумруд"
                AppLanguage.JAPANESE -> "エメラルド"
                else -> "Emerald"
            }
            OCEAN -> when (language) {
                AppLanguage.VIETNAMESE -> "Đại dương"
                AppLanguage.CHINESE -> "海洋"
                AppLanguage.RUSSIAN -> "Океан"
                AppLanguage.JAPANESE -> "オーシャン"
                else -> "Ocean"
            }
            FOREST -> when (language) {
                AppLanguage.VIETNAMESE -> "Rừng rậm"
                AppLanguage.CHINESE -> "森林"
                AppLanguage.RUSSIAN -> "Лес"
                AppLanguage.JAPANESE -> "フォレスト"
                else -> "Forest"
            }
            SUNSET -> when (language) {
                AppLanguage.VIETNAMESE -> "Hoàng hôn"
                AppLanguage.CHINESE -> "日落"
                AppLanguage.RUSSIAN -> "Закат"
                AppLanguage.JAPANESE -> "サンセット"
                else -> "Sunset"
            }
            CANDY -> when (language) {
                AppLanguage.VIETNAMESE -> "Kẹo ngọt"
                AppLanguage.CHINESE -> "糖果"
                AppLanguage.RUSSIAN -> "Конфета"
                AppLanguage.JAPANESE -> "キャンディ"
                else -> "Candy"
            }
            LUXURY -> when (language) {
                AppLanguage.VIETNAMESE -> "Sang trọng"
                AppLanguage.CHINESE -> "奢华"
                AppLanguage.RUSSIAN -> "Роскошь"
                AppLanguage.JAPANESE -> "ラグジュアリー"
                else -> "Luxury"
            }
            MINIMAL -> when (language) {
                AppLanguage.VIETNAMESE -> "Tối giản"
                AppLanguage.CHINESE -> "极简"
                AppLanguage.RUSSIAN -> "Минимализм"
                AppLanguage.JAPANESE -> "ミニマル"
                else -> "Minimal"
            }
        }
    }
}
