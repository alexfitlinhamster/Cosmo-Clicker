package com.example.myapplication.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import com.example.myapplication.ui.components.Button
import com.example.myapplication.ui.components.CosmicButtonStyle
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.BuildConfig
import com.example.myapplication.R
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.ui.components.SpaceDialog

private data class LanguageOption(val tag: String?, val label: Int)

@Composable
fun SettingsScreen(
    selectedLanguage: String?,
    onLanguageSelected: (String?) -> Unit,
    onResetGame: () -> Unit,
    onBack: () -> Unit
) {
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val languages = listOf(
        LanguageOption(null, R.string.language_system),
        LanguageOption("en", R.string.language_english),
        LanguageOption("ru", R.string.language_russian),
        LanguageOption("es", R.string.language_spanish)
    )
    val languageLabel = languages.firstOrNull { it.tag == selectedLanguage }?.label
        ?: R.string.language_system

    BackHandler {
        when {
            showLanguageDialog -> showLanguageDialog = false
            showResetDialog -> showResetDialog = false
            else -> onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(AppColors.BackgroundStart, AppColors.BackgroundMid, AppColors.BackgroundStart)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 24.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = AppColors.CardBackground.copy(alpha = .96f),
                border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Primary.copy(alpha = .28f))
            ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    Modifier.size(42.dp).clickable(onClick = onBack),
                    RoundedCornerShape(13.dp),
                    color = AppColors.Surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Primary.copy(alpha = .25f))
                ) {
                    Box(contentAlignment = Alignment.Center) { Text("‹", color = AppColors.Primary, fontSize = 30.sp) }
                }
                Spacer(Modifier.width(10.dp))
                Image(painterResource(R.drawable.ic_nav_settings_minimal), null, Modifier.size(34.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = stringResource(R.string.settings),
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(
                            R.string.app_version_caption,
                            stringResource(R.string.app_name),
                            BuildConfig.VERSION_NAME,
                            BuildConfig.VERSION_CODE
                        ),
                        color = AppColors.TextMuted,
                        fontSize = 10.sp
                    )
                }
            } }

            Spacer(Modifier.height(18.dp))
            SettingsSectionTitle(stringResource(R.string.settings_preferences_section), "01")
            Spacer(Modifier.height(8.dp))

            SettingsCard(
                modifier = Modifier.clickable { showLanguageDialog = true },
                accent = AppColors.Primary
            ) {
                SettingsRow(stringResource(R.string.language), stringResource(languageLabel), "文", true)
            }

            Spacer(Modifier.height(18.dp))
            SettingsSectionTitle(stringResource(R.string.settings_about_section), "02", AppColors.Secondary)
            Spacer(Modifier.height(8.dp))

            SettingsCard(accent = AppColors.Secondary) {
                SettingsRow(
                    stringResource(R.string.version),
                    "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    "V"
                )
                SettingsRow(stringResource(R.string.developer), "Alexei Fitlin", "A")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { uriHandler.openUri("https://t.me/AlexFitlin") }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.ic_telegram),
                            contentDescription = stringResource(R.string.telegram),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.telegram), color = Color.White)
                    }
                    Text("@AlexFitlin  ›", color = AppColors.Primary, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(18.dp))
            SettingsSectionTitle(stringResource(R.string.settings_danger_section), "03", AppColors.Danger)
            Spacer(Modifier.height(8.dp))

            SettingsCard(modifier = Modifier.clickable { showResetDialog = true }, accent = AppColors.Danger) {
                Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    SettingsBadge("!", AppColors.Danger)
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.reset_game), color = AppColors.Danger, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.reset_game_description), color = AppColors.TextMuted, fontSize = 10.sp, lineHeight = 14.sp)
                    }
                    Text("›", color = AppColors.Danger, fontSize = 22.sp)
                }
            }

        }
    }

    if (showLanguageDialog) {
        SpaceDialog(
            title = stringResource(R.string.language),
            onDismiss = { showLanguageDialog = false },
            content = {
                languages.forEach { option ->
                    val selected = selectedLanguage == option.tag
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                            showLanguageDialog = false
                            onLanguageSelected(option.tag)
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) AppColors.Primary.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.04f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) AppColors.Primary else Color.White.copy(alpha = 0.08f))
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selected, onClick = null)
                            Text(stringResource(option.label), color = Color.White, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            },
            actions = {}
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.reset_game_confirm_title)) },
            text = { Text(stringResource(R.string.reset_game_confirm_message)) },
            confirmButton = {
                Button(onClick = {
                    showResetDialog = false
                    onResetGame()
                }, style = CosmicButtonStyle.Danger) { Text(stringResource(R.string.reset_game_confirm)) }
            },
            dismissButton = {
                Button(onClick = { showResetDialog = false }, style = CosmicButtonStyle.Secondary) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    accent: Color = AppColors.Primary,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = AppColors.Surface.copy(alpha = .86f),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = .24f)),
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingsRow(label: String, value: String, badge: String, showChevron: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsBadge(badge, AppColors.Primary)
        Spacer(Modifier.width(11.dp))
        Text(label, modifier = Modifier.weight(1f), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text(value, color = AppColors.TextMuted, fontSize = 11.sp)
        if (showChevron) {
            Spacer(Modifier.width(7.dp))
            Text("›", color = AppColors.Primary, fontSize = 20.sp)
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String, number: String, color: Color = AppColors.Primary) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(number, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Text(title.uppercase(), color = AppColors.TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
    }
}

@Composable
private fun SettingsBadge(text: String, color: Color) {
    Surface(
        modifier = Modifier.size(32.dp),
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = .11f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = .24f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
