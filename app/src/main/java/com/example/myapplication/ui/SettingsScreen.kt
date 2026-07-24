package com.example.myapplication.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
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

private data class LanguageOption(val tag: String?, val label: Int)

@Composable
fun SettingsScreen(
    selectedLanguage: String?,
    onLanguageSelected: (String?) -> Unit,
    onBack: () -> Unit
) {
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showGameGuideDialog by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val languages = listOf(
        LanguageOption(null, R.string.language_system),
        LanguageOption("en", R.string.language_english),
        LanguageOption("ru", R.string.language_russian),
        LanguageOption("es", R.string.language_spanish)
    )
    val languageLabel = languages.firstOrNull { it.tag == selectedLanguage }?.label
        ?: R.string.language_system

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
                SettingsRow(stringResource(R.string.developer), "Alexei Fitlin")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { uriHandler.openUri("https://t.me/Alexfit") }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.telegram), color = Color.White)
                    Text("@Alexfit", color = AppColors.Primary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.language)) },
            text = {
                Column {
                    languages.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showLanguageDialog = false
                                    onLanguageSelected(option.tag)
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedLanguage == option.tag,
                                onClick = null
                            )
                            Text(stringResource(option.label))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.back))
                }
            }
        )
    }

    if (showGameGuideDialog) {
        AlertDialog(
            onDismissRequest = { showGameGuideDialog = false },
            title = { Text(stringResource(R.string.how_to_play)) },
            text = {
                Text(
                    text = stringResource(R.string.game_guide_body),
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showGameGuideDialog = false }) {
                    Text(stringResource(R.string.close))
                }
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
        color = Color.White.copy(alpha = 0.08f)
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
