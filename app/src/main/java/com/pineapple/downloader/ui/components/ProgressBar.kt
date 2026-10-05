package com.pineapple.downloader.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.pineapple.downloader.model.formatEta
import com.pineapple.downloader.model.formatSpeed
import com.pineapple.downloader.ui.theme.PineappleGreen
import com.pineapple.downloader.ui.theme.PineappleGreenLight
import com.pineapple.downloader.ui.theme.PineappleOutline

/**
 * شريط تقدم مخصص: تدرج أخضر (#1B8A3E → #4CAF50) + نسبة وسرعة ووقت متبقٍ بخط واضح.
 */
@Composable
fun PineappleProgressBar(
    percent: Int,
    speedBytesPerSec: Double,
    etaSeconds: Int,
    modifier: Modifier = Modifier,
) {
    val animated by animateFloatAsState(
        targetValue = (percent.coerceIn(0, 100) / 100f),
        label = "progress",
    )
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "$percent٪",
                style = MaterialTheme.typography.labelMedium,
                color = PineappleGreen,
            )
            Text(
                "${formatSpeed(speedBytesPerSec)}  •  ${formatEta(etaSeconds)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(PineappleOutline.copy(alpha = 0.5f)),
        ) {
            LinearProgressIndicator(
                progress = { animated },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = PineappleGreen,
                trackColor = PineappleOutline.copy(alpha = 0.5f),
                strokeCap = StrokeCap.Round,
            )
            // طبقة التدرج فوق المؤشر
            Box(
                modifier = Modifier
                    .fillMaxWidth(animated)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Brush.horizontalGradient(listOf(PineappleGreen, PineappleGreenLight))),
            )
        }
    }
}
