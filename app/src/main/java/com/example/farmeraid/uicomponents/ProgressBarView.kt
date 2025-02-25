package com.example.farmeraid.uicomponents

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.example.farmeraid.uicomponents.models.UiComponentModel
import java.lang.Float.max
import kotlin.math.min

@Composable
fun ProgressBarView(
    progressBarUiState : UiComponentModel.ProgressBarUiState,
    modifier : Modifier = Modifier,
) {
    var target by remember {
        mutableFloatStateOf(0f)
    }

    val progress by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .drawWithContent {
                if (target != min(progressBarUiState.progress, 1f)) {
                    target = min(progressBarUiState.progress, 1f)
                }
                with(drawContext.canvas.nativeCanvas) {
                    val checkPoint = saveLayer(null, null)

                    // Destination
                    drawContent()

                    // Source
                    drawRect(
                        color = progressBarUiState.progressColor,
                        size = Size(width = size.width * progress, height = size.height),
                        blendMode = BlendMode.SrcOut
                    )
                    drawRect(
                        color = progressBarUiState.expectedProgressColor,
                        topLeft = Offset(size.width * progress, 0f),
                        size = Size(width = max(size.width * progressBarUiState.expectedProgress, size.width * progress) - (size.width * progress), height = size.height),
                        blendMode = BlendMode.SrcOut
                    )
                    restoreToCount(checkPoint)
                }
            }
            .background(progressBarUiState.containerColor)
            .padding(2.5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = progressBarUiState.text,
            color = progressBarUiState.progressColor,
            fontSize = progressBarUiState.fontSize,
            fontWeight = progressBarUiState.fontWeight,
        )
    }
}