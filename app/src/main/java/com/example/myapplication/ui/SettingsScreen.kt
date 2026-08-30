package com.example.myapplication.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.myapplication.ui.components.Button
import com.example.myapplication.ui.components.CosmicButtonStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.myapplication.ui.theme.SpaceDesign
import com.example.myapplication.ui.components.SpaceDialog

private data class LanguageOption(val tag: String?, val label: Int)

@Composable
fun SettingsScreen(
    selectedLanguage: String?,
    onLanguageSelected: (String?) -> Unit,
    soundEnabled: Boolean,
    onSoundEnabledChanged: (Boolean) -> Unit,
    reducedMotion: Boolean,
    onReducedMotionChanged: (Boolean) -> Unit,
    onAchievements: () -> Unit,
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
    ) {
        Image(
            painter = painterResource(R.drawable.bg_settings_space_v1),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            AppColors.BackgroundStart.copy(alpha = .44f),
                            Color(0xFF020817).copy(alpha = .62f),
                            AppColors.BackgroundStart.copy(alpha = .50f)
                        )
                    )
                )
        )
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
                shape = RoundedCornerShape(SpaceDesign.SheetRadius),
                color = AppColors.CardBackground.copy(alpha = .96f),
                border = null
            ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    Modifier.size(48.dp).clickable(onClick = onBack),
                    RoundedCornerShape(13.dp),
                    color = AppColors.Surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Primary.copy(alpha = .25f))
                ) {
                    Box(contentAlignment = Alignment.Center) { Text("‹", color = AppColors.Primary, fontSize = 30.sp) }
                }
                Spacer(Modifier.width(10.dp))
                Image(painterResource(R.drawable.ic_nav_settings_minimal), null, Modifier.size(34.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.settings),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            } }

            Spacer(Modifier.height(18.dp))
            SettingsSectionTitle(stringResource(R.string.settings_preferences_section), "01")
            Spacer(Modifier.height(8.dp))

            SettingsImageButton(
                label = stringResource(R.string.language),
                value = stringResource(languageLabel),
                icon = R.drawable.icon_settings_language_v2,
                onClick = { showLanguageDialog = true }
            )

            Spacer(Modifier.height(10.dp))
            SettingsToggleRow(
                title = stringResource(R.string.sound),
                description = stringResource(R.string.sound_description),
                icon = R.drawable.icon_settings_sound_v2,
                checked = soundEnabled,
                onCheckedChange = onSoundEnabledChanged
            )

            Spacer(Modifier.height(10.dp))
            SettingsToggleRow(
                title = stringResource(R.string.reduced_motion),
                description = stringResource(R.string.reduced_motion_description),
                icon = R.drawable.icon_settings_reduced_motion_v2,
                checked = reducedMotion,
                onCheckedChange = onReducedMotionChanged
            )

            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onAchievements,
                modifier = Modifier.fillMaxWidth().height(68.dp),
                style = CosmicButtonStyle.Primary,
                generatedArtwork = true,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.ic_achievement_medal),
                        contentDescription = null,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.achievements),
                        modifier = Modifier.weight(1f),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text("›", color = AppColors.Primary, fontSize = 22.sp)
                }
            }

            Spacer(Modifier.height(18.dp))
            SettingsSectionTitle(stringResource(R.string.settings_about_section), "02", AppColors.Secondary)
            Spacer(Modifier.height(8.dp))

            SettingsCard {
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

            Button(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth().height(68.dp),
                style = CosmicButtonStyle.Danger,
                generatedArtwork = true,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.icon_settings_reset_v2),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
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
            modifier = Modifier.widthIn(max = 420.dp),
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
                            CosmicSelectionIndicator(selected)
                            Text(stringResource(option.label), color = Color.White, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            },
            actions = {}
        )
    }

    if (showResetDialog) {
        SpaceDialog(
            title = stringResource(R.string.reset_game_confirm_title),
            onDismiss = { showResetDialog = false },
            modifier = Modifier.widthIn(max = 420.dp),
            content = {
                Text(stringResource(R.string.reset_game_confirm_message), color = AppColors.TextMuted, lineHeight = 18.sp)
            },
            actions = {
                Button(
                    modifier = Modifier.weight(1f).height(SpaceDesign.MinTouchSize),
                    onClick = { showResetDialog = false },
                    style = CosmicButtonStyle.Secondary,
                    compact = true,
                    generatedArtwork = true
                ) {
                    Text(stringResource(R.string.cancel), fontSize = 11.sp, maxLines = 1)
                }
                Button(
                    onClick = {
                        showResetDialog = false
                        onResetGame()
                    },
                    modifier = Modifier.weight(1f).height(SpaceDesign.MinTouchSize),
                    style = CosmicButtonStyle.Danger,
                    compact = true,
                    generatedArtwork = true,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                ) {
                    Text(stringResource(R.string.reset_game_confirm), fontSize = 11.sp, maxLines = 1)
                }
            }
        )
    }

}

@Composable
private fun SettingsToggleRow(
    title: String,
    description: String,
    icon: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) },
        shape = RoundedCornerShape(SpaceDesign.CardRadius),
        color = AppColors.Surface.copy(alpha = .92f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (checked) AppColors.Primary.copy(alpha = .42f) else AppColors.Outline
        )
    ) {
        Row(Modifier.padding(horizontal = 15.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(42.dp)
            )
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold)
                Text(description, color = AppColors.TextMuted, fontSize = 10.sp, lineHeight = 13.sp)
            }
            CosmicSwitch(checked)
        }
    }
}

@Composable
private fun CosmicSwitch(checked: Boolean) {
    Box(
        modifier = Modifier
            .size(width = 52.dp, height = 30.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(if (checked) AppColors.Primary.copy(alpha = .34f) else Color(0xFF172438))
            .border(
                1.dp,
                if (checked) AppColors.Primary.copy(alpha = .72f) else AppColors.Outline,
                RoundedCornerShape(15.dp)
            )
            .padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            Modifier
                .size(22.dp)
                .background(
                    if (checked) AppColors.Primary else AppColors.TextMuted,
                    androidx.compose.foundation.shape.CircleShape
                )
        )
    }
}

@Composable
private fun CosmicSelectionIndicator(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .border(
                1.dp,
                if (selected) AppColors.Primary else AppColors.Outline,
                androidx.compose.foundation.shape.CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                Modifier
                    .size(12.dp)
                    .background(AppColors.Primary, androidx.compose.foundation.shape.CircleShape)
            )
        }
    }
}

@Composable
private fun SettingsImageButton(label: String, value: String, icon: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(74.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(AppColors.Primary.copy(alpha = .16f), AppColors.Surface.copy(alpha = .96f))
                )
            )
            .border(1.dp, AppColors.Primary.copy(alpha = .34f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(painterResource(icon), null, Modifier.size(46.dp))
            Spacer(Modifier.width(14.dp))
            Text(label, modifier = Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.Bold)
            Text(value, color = AppColors.Primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            Text("›", color = AppColors.Primary, fontSize = 22.sp)
        }
    }
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(SpaceDesign.CardRadius),
        color = AppColors.Surface.copy(alpha = .92f),
        border = null,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingsRow(label: String, value: String, badge: String? = null, showChevron: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (badge != null) {
            SettingsBadge(badge, AppColors.Primary)
            Spacer(Modifier.width(11.dp))
        }
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
