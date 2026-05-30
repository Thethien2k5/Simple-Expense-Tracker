import re

with open('app/src/main/java/com/T2V/simple_expense_tracker/ui/theme/AppTheme.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = re.sub(r'    val themeName: String,\n', '', content)
content = re.sub(r'\s*themeName = \"[^\"]+\",\n', '\n', content)

if 'import com.T2V.simple_expense_tracker.domain.repository.AppLanguage' not in content:
    content = content.replace('import androidx.compose.ui.graphics.Color\n', 'import androidx.compose.ui.graphics.Color\nimport com.T2V.simple_expense_tracker.domain.repository.AppLanguage\n')

func_code = '''
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
'''
content = content.replace('    )\n}', '    );\n' + func_code)

with open('app/src/main/java/com/T2V/simple_expense_tracker/ui/theme/AppTheme.kt', 'w', encoding='utf-8') as f:
    f.write(content)
