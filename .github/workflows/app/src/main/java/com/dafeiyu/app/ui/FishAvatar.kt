package com.dafeiyu.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp

private val FishBlue = Color(0xFF2E6BE6)
private val FishBelly = Color(0xFF8FB8FF)
private val FishEye = Color(0xFF102040)

/** 会上下浮动的鲸鱼，纯 Canvas 画的，不需要图片资源，也不需要悬浮窗权限。 */
@Composable
fun FishAvatar(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "fishFloat")
    val dy by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dy"
    )

    Canvas(modifier = modifier.size(44.dp).offset(y = dy.dp)) {
        val w = size.width
        val h = size.height

        drawPath(
            path = Path().apply {
                moveTo(w * 0.77f, h * 0.50f)
                lineTo(w * 0.99f, h * 0.24f)
                lineTo(w * 0.92f, h * 0.50f)
                lineTo(w * 0.99f, h * 0.76f)
                close()
            },
            color = FishBlue
        )
        drawOval(
            color = FishBlue,
            topLeft = Offset(w * 0.04f, h * 0.27f),
            size = Size(w * 0.74f, h * 0.46f)
        )
        drawOval(
            color = FishBelly,
            topLeft = Offset(w * 0.14f, h * 0.60f),
            size = Size(w * 0.56f, h * 0.12f)
        )
        drawCircle(
            color = Color.White,
            radius = w * 0.055f,
            center = Offset(w * 0.25f, h * 0.46f)
        )
        drawCircle(
            color = FishEye,
            radius = w * 0.026f,
            center = Offset(w * 0.26f, h * 0.46f)
        )
    }
}
