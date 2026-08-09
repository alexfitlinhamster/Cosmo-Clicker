package com.example.myapplication.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.myapplication.ui.theme.AppColors

@Composable
fun SpaceSheetHeader(title: String, subtitle: String? = null, onClose: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(42.dp).background(AppColors.Primary.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center
        ) { Text("✦", color = AppColors.Primary, fontSize = 20.sp) }
        Spacer(Modifier.width(12.dp))
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
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.07f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) { Box(contentAlignment = Alignment.Center) { Text("×", color = Color.White, fontSize = 24.sp) } }
    }
}

@Composable
fun SpaceTab(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(42.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(13.dp),
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
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
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
        targetValue = 1f + (combo.toFloat() / 100f).coerceAtMost(0.4f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "combo_scale"
    )

    Column(
        modifier = modifier.scale(scale),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "COMBO",
            color = AppColors.Primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = "x$combo",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
