import re
import os

files_to_update = [
    "app/src/main/java/com/T2V/simple_expense_tracker/ui/dashboard/DashboardScreen.kt",
    "app/src/main/java/com/T2V/simple_expense_tracker/ui/ledger/LedgerScreen.kt",
    "app/src/main/java/com/T2V/simple_expense_tracker/ui/settings/SettingsScreen.kt",
    "app/src/main/java/com/T2V/simple_expense_tracker/ui/notification/ManualParseScreen.kt" # wait let's check if it uses R.string
]

def snake_to_camel(snake_str):
    components = snake_str.split('_')
    return components[0] + ''.join(x.title() for x in components[1:])

for file_path in files_to_update:
    if not os.path.exists(file_path): continue
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Also find if we need to add import LocalAppStrings
    needs_import = False

    def replacer(match):
        global needs_import
        needs_import = True
        key = match.group(1)
        camel_key = snake_to_camel(key)
        return f"LocalAppStrings.current.{camel_key}"

    # stringResource(id = R.string.xxx) or stringResource(R.string.xxx)
    new_content = re.sub(r'stringResource\s*\(\s*(?:id\s*=\s*)?R\.string\.([a-zA-Z0-9_]+)\s*\)', replacer, content)
    
    if needs_import and 'import com.T2V.simple_expense_tracker.ui.theme.LocalAppStrings' not in new_content:
        new_content = new_content.replace('import androidx.compose.ui.Modifier', 'import androidx.compose.ui.Modifier\nimport com.T2V.simple_expense_tracker.ui.theme.LocalAppStrings')
        
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(new_content)
