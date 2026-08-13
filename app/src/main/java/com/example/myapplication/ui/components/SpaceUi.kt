package com.example.myapplication.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 22.sp
            )
            if (subtitle != null) Text(
                subtitle,
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp
            )
        }
        Surface(
            modifier = Modifier.size(38.dp).clickable(onClick = onClose),
            shape = RoundedCornerShape(9.dp),
            color = Color.White.copy(alpha = 0.07f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) { Box(contentAlignment = Alignment.Center) { Text("×", color = Color.White, fontSize = 24.sp) } }
    }
}

@Composable
fun SpaceTab(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(42.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(9.dp),
        color = if (selected) AppColors.Primary.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.035f),
        border = BorderStroke(1.dp, if (selected) AppColors.Primary.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.08f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text,
                modifier = Modifier.padding(horizontal = 5.dp),
                color = if (selected) AppColors.Primary else Color.White.copy(alpha = 0.65f),
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

@Composable
fun SpaceDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
    actions: @Composable RowScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = AppColors.CardBackground,
            border = BorderStroke(1.dp, AppColors.Primary.copy(alpha = 0.35f)),
            shadowElevation = 18.dp
        ) {
            Column(
                modifier = Modifier
                    .background(Brush.verticalGradient(listOf(Color(0xFF142641), Color(0xFF080F1E))))
                    .padding(20.dp)
            ) {
                SpaceSheetHeader(title = title, onClose = onDismiss)
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color.White.copy(alpha = 0.08f))
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

@Composable
fun ComboIndicator(combo: Int, modifier: Modifier = Modifier) {
    val scale by animateFloatAsState(
        targetValue = 1f + (combo.toFloat() / 10f).coerceAtMost(0.35f),
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "combo_scale"
    )

    Column(
        modifier = modifier.scale(scale),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = when {
                combo >= 10 -> stringResource(R.string.combo_impossible)
                combo >= 7 -> stringResource(R.string.combo_master)
                combo >= 4 -> stringResource(R.string.combo_great)
                else -> stringResource(R.string.combo)
            },
            color = AppColors.Primary,
            fontSize = if (combo >= 7) 20.sp else 15.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = "x$combo",
            color = Color.White,
            fontSize = if (combo >= 10) 42.sp else 32.sp,
            fontWeight = FontWeight.ExtraBold
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
            Text("✦", color = AppColors.Warning, fontSize = 30.sp, fontWeight = FontWeight.Black)
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
