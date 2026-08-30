package com.example.myapplication.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.myapplication.R
import com.example.myapplication.ui.theme.AppColors

@Composable
fun SpaceSheetHeader(title: String, subtitle: String? = null, onClose: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 22.sp
            )
            if (subtitle != null) Text(
                subtitle,
                color = AppColors.TextMuted,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp
            )
        }
        Image(
            painter = painterResource(R.drawable.ui_close_control_v2),
            contentDescription = stringResource(R.string.close),
            modifier = Modifier.size(48.dp).clickable(onClick = onClose),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun SpaceTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconRes: Int? = null,
    accent: Color = AppColors.Primary,
    iconSheetIndex: Int? = null
) {
    Surface(
        modifier = modifier.height(48.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) accent.copy(alpha = .14f) else Color.White.copy(alpha = .035f),
        border = if (selected) BorderStroke(1.dp, accent.copy(alpha = .46f)) else null,
        shadowElevation = 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            if (iconSheetIndex != null) {
                GeneratedSheetIcon(R.drawable.shop_ui_minimal_sheet_v1, iconSheetIndex, 19.dp, columns = 4, rows = 4)
                Spacer(Modifier.width(5.dp))
            } else if (iconRes != null) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(iconRes),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(5.dp))
            }
            Text(
                text,
                modifier = Modifier.padding(horizontal = 5.dp),
                color = if (selected) accent else Color.White.copy(alpha = 0.65f),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 11.sp
            )
        }
        }
    }
}

@Composable
fun SpaceDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
    actions: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    backgroundRes: Int? = null
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier.widthIn(max = 640.dp).fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = AppColors.CardBackground,
            border = null,
            shadowElevation = 18.dp
        ) {
            Box {
                if (backgroundRes != null) {
                    Image(
                        painter = painterResource(backgroundRes),
                        contentDescription = null,
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        Modifier
                            .matchParentSize()
                            .background(Color(0xFF030916).copy(alpha = .34f))
                    )
                }
                Column(
                    modifier = Modifier
                        .then(
                            if (backgroundRes == null) {
                                Modifier.background(Brush.verticalGradient(listOf(Color(0xFF142641), Color(0xFF080F1E))))
                            } else {
                                Modifier.background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF12213A).copy(alpha = .30f), Color(0xFF030817).copy(alpha = .64f))
                                    )
                                )
                            }
                        )
                        .padding(20.dp)
                ) {
                    SpaceSheetHeader(title = title, onClose = onDismiss)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color.White.copy(alpha = 0.10f))
                    content()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                        content = actions
                    )
                }
            }
        }
    }
}

@Composable
fun ComboIndicator(combo: Int, modifier: Modifier = Modifier) {
    val scale by animateFloatAsState(
        targetValue = 1f + (combo.toFloat() / 100f).coerceAtMost(0.08f),
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "combo_scale"
    )

    Column(
        modifier = modifier.scale(scale),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val message = when {
            combo >= 50 -> R.string.combo_cosmic_legend
            combo >= 40 -> R.string.combo_unstoppable
            combo >= 25 -> R.string.combo_overdrive
            combo >= 15 -> R.string.combo_perfect_chain
            combo >= 10 -> listOf(R.string.combo_impossible, R.string.combo_master, R.string.combo_signal_locked)[combo % 3]
            combo >= 5 -> listOf(R.string.combo_great, R.string.combo_clean_hit, R.string.combo_keep_going)[combo % 3]
            else -> listOf(R.string.combo, R.string.combo_start, R.string.combo_good)[combo % 3]
        }
        val accent = when {
            combo >= 50 -> AppColors.Danger
            combo >= 25 -> AppColors.Warning
            combo >= 15 -> Color(0xFFB987FF)
            combo >= 8 -> Color(0xFF55D9FF)
            else -> AppColors.Primary
        }
        Text(
            text = stringResource(message),
            color = accent,
            fontSize = if (combo >= 15) 17.sp else 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "x$combo",
            color = Color.White,
            fontSize = if (combo >= 25) 30.sp else 26.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PlanetUnlockBanner(
    planetIndex: Int,
    planetName: String,
    incomeBonusPercent: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.widthIn(max = 440.dp).fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF102A3D).copy(alpha = .97f),
        border = BorderStroke(1.dp, AppColors.Warning.copy(alpha = .72f)),
        shadowElevation = 14.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(painterResource(R.drawable.ui_new_badge_v2), null, Modifier.size(38.dp), contentScale = ContentScale.Fit)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(R.string.planet_unlocked_title, planetIndex),
                    color = AppColors.Warning,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(planetName, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black)
                Text(
                    stringResource(R.string.planet_unlocked_bonus, incomeBonusPercent),
                    color = AppColors.Secondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}
