package com.example.myapplication.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import kotlin.math.abs

/** Restrained icon treatment for shop controls; game artwork remains untouched. */
@Composable
fun MinimalShopIcon(seed: Int, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val unit = size.minDimension
        drawRoundRect(color.copy(.10f), cornerRadius = CornerRadius(unit * .22f))
        drawRoundRect(color.copy(.65f), style = Stroke(unit * .045f), cornerRadius = CornerRadius(unit * .22f))
        when (abs(seed) % 3) {
            0 -> {
                drawCircle(color, unit * .18f, center)
                drawCircle(Color(0xFF09121E), unit * .075f, center)
            }
            1 -> {
                drawLine(color, Offset(unit * .25f, unit * .68f), Offset(unit * .5f, unit * .25f), unit * .1f, StrokeCap.Round)
                drawLine(color, Offset(unit * .5f, unit * .25f), Offset(unit * .75f, unit * .68f), unit * .1f, StrokeCap.Round)
            }
            else -> {
                drawRect(color, Offset(unit * .28f, unit * .3f), Size(unit * .44f, unit * .4f))
                drawLine(Color(0xFF09121E), Offset(unit * .38f, unit * .5f), Offset(unit * .62f, unit * .5f), unit * .055f)
            }
        }
    }
}

@Composable
fun GeneratedSheetIcon(
    drawable: Int,
    index: Int,
    size: Dp,
    modifier: Modifier = Modifier,
    columns: Int = 3,
    rows: Int = 3
) {
    val resources = LocalResources.current
    val bitmap = remember(drawable) { ImageBitmap.imageResource(resources, drawable) }
    Canvas(modifier.size(size)) {
        val safeIndex = index.coerceIn(0, columns * rows - 1)
        val cellWidth = bitmap.width / columns
        val cellHeight = bitmap.height / rows
        drawImage(
            image = bitmap,
            srcOffset = IntOffset((safeIndex % columns) * cellWidth, (safeIndex / columns) * cellHeight),
            srcSize = IntSize(cellWidth, cellHeight),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(this.size.width.toInt(), this.size.height.toInt()),
            filterQuality = FilterQuality.High
        )
    }
}

@Composable
fun GeneratedSheetPanel(
    drawable: Int,
    index: Int,
    modifier: Modifier = Modifier,
    columns: Int = 3,
    rows: Int = 3
) {
    val resources = LocalResources.current
    val bitmap = remember(drawable) { ImageBitmap.imageResource(resources, drawable) }
    Canvas(modifier) {
        val safeIndex = index.coerceIn(0, columns * rows - 1)
        val cellWidth = bitmap.width / columns
        val cellHeight = bitmap.height / rows
        drawImage(
            image = bitmap,
            srcOffset = IntOffset((safeIndex % columns) * cellWidth, (safeIndex / columns) * cellHeight),
            srcSize = IntSize(cellWidth, cellHeight),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
            filterQuality = FilterQuality.High
        )
    }
}
