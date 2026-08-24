package com.example.myapplication.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.myapplication.R

enum class CosmicButtonStyle { Primary, Secondary, Reward, Danger }

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    style: CosmicButtonStyle = CosmicButtonStyle.Primary,
    content: @Composable RowScope.() -> Unit
) {
    val frame = if (!enabled) R.drawable.ui_button_locked_v4 else when (style) {
        CosmicButtonStyle.Primary -> R.drawable.ui_button_primary_v4
        CosmicButtonStyle.Secondary -> R.drawable.ui_button_secondary_v4
        CosmicButtonStyle.Reward -> R.drawable.ui_button_reward_v4
        CosmicButtonStyle.Danger -> R.drawable.ui_button_danger_v4
    }
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 96.dp, minHeight = 42.dp)
            .alpha(if (enabled) 1f else .72f)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(frame),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            Row(
                modifier = Modifier.padding(contentPadding),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}
