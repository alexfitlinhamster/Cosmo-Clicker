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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
    backgroundMusicEnabled: Boolean,
    onBackgroundMusicChanged: (Boolean) -> Unit,
    onResetGame: () -> Unit,
    onBack: () -> Unit
) {
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showGameGuideDialog by remember { mutableStateOf(false) }
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
            showGameGuideDialog -> showGameGuideDialog = false
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
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 24.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.settings_header),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(112.dp).clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Text("‹", color = AppColors.Primary, fontSize = 40.sp)
                }
                Spacer(Modifier.width(8.dp))
                Image(
                    painter = painterResource(id = R.drawable.icon_game),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.settings),
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(
                            R.string.app_version_caption,
                            stringResource(R.string.app_name),
                            BuildConfig.VERSION_NAME,
                            BuildConfig.VERSION_CODE
                        ),
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            SettingsCard {
                SettingsRow(stringResource(R.string.application), stringResource(R.string.app_name))
            }

            Spacer(Modifier.height(12.dp))

            SettingsCard(
                modifier = Modifier.clickable { showLanguageDialog = true }
            ) {
                SettingsRow(stringResource(R.string.language), stringResource(languageLabel))
            }

            Spacer(Modifier.height(12.dp))

            SettingsCard(
                modifier = Modifier.clickable { showGameGuideDialog = true }
            ) {
                SettingsRow(stringResource(R.string.how_to_play), stringResource(R.string.open_guide))
            }

            Spacer(Modifier.height(12.dp))

            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.background_music), color = Color.White)
                        Text(
                            stringResource(R.string.background_music_desc),
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }
                    Switch(checked = backgroundMusicEnabled, onCheckedChange = onBackgroundMusicChanged)
                }
            }

            Spacer(Modifier.height(12.dp))

            SettingsCard(modifier = Modifier.clickable { showResetDialog = true }) {
                Column(Modifier.padding(vertical = 8.dp)) {
                    Text(stringResource(R.string.reset_game), color = AppColors.Danger, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.reset_game_description), color = Color.LightGray, fontSize = 11.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            SettingsCard {
                SettingsRow(stringResource(R.string.developer), "Alexei Fitlin")
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
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.telegram), color = Color.White)
                    }
                    Text("@AlexFitlin", color = AppColors.Primary, fontWeight = FontWeight.SemiBold)
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
                        shape = RoundedCornerShape(14.dp),
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

    if (showGameGuideDialog) {
        val guideSections = listOf(
            R.string.guide_basics_title to R.string.guide_basics_body,
            R.string.guide_drones_title to R.string.guide_drones_body,
            R.string.guide_cases_title to R.string.guide_cases_body,
            R.string.guide_events_title to R.string.guide_events_body,
            R.string.guide_progress_title to R.string.guide_progress_body,
            R.string.guide_performance_title to R.string.guide_performance_body
        )
        SpaceDialog(
            title = stringResource(R.string.how_to_play),
            onDismiss = { showGameGuideDialog = false },
            content = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(guideSections, key = { it.first }) { (title, body) ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.05f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text(stringResource(title), color = AppColors.Primary, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(5.dp))
                                Text(stringResource(body), color = Color.White.copy(alpha = 0.82f), lineHeight = 19.sp)
                            }
                        }
                    }
                }
            },
            actions = {
                Button(onClick = { showGameGuideDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
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
                }) { Text(stringResource(R.string.reset_game_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF11223A),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.09f)),
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingsRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White)
        Text(value, color = Color.LightGray)
    }
}
