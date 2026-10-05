package com.pineapple.downloader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.pineapple.downloader.R
import com.pineapple.downloader.model.DownloadKind
import com.pineapple.downloader.model.FormatOption
import com.pineapple.downloader.ui.theme.PineappleGreen
import com.pineapple.downloader.ui.theme.PineappleInk
import com.pineapple.downloader.ui.theme.PineappleOutline
import com.pineapple.downloader.ui.theme.PineappleSurface
import com.pineapple.downloader.ui.theme.PineappleWhite
import com.pineapple.downloader.ui.theme.PineappleYellow

/**
 * اختيار النوع: "فيديو" أصفر عند التحديد، "صوت" أخضر عند التحديد.
 * Segmented control مخصص — ليس FilterChip الافتراضي.
 */
@Composable
fun KindSegmentedControl(
    selected: DownloadKind,
    onSelect: (DownloadKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        KindSegment(
            label = "فيديو",
            icon = R.drawable.ic_video,
            isSelected = selected == DownloadKind.VIDEO,
            selectedColor = PineappleYellow,
            selectedContent = PineappleInk,
            onClick = { onSelect(DownloadKind.VIDEO) },
            modifier = Modifier.weight(1f),
        )
        KindSegment(
            label = "صوت فقط",
            icon = R.drawable.ic_audio,
            isSelected = selected == DownloadKind.AUDIO,
            selectedColor = PineappleGreen,
            selectedContent = PineappleWhite,
            onClick = { onSelect(DownloadKind.AUDIO) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun KindSegment(
    label: String,
    icon: Int,
    isSelected: Boolean,
    selectedColor: androidx.compose.ui.graphics.Color,
    selectedContent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) selectedColor else PineappleSurface)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = PineappleGreen.copy(alpha = 0.12)),
                onClick = onClick,
            )
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = if (isSelected) selectedContent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) selectedContent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

/**
 * قائمة منسدلة أنيقة للجودة مع حجم تقريبي ملوّن حسب الجودة
 * (أخضر للصغيرة، أصفر للمتوسطة، برتقالي للكبيرة).
 */
@Composable
fun QualityDropdown(
    options: List<FormatOption>,
    selected: FormatOption?,
    onSelect: (FormatOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(PineappleSurface)
                .clickable(
                    interactionSource = interaction,
                    indication = ripple(color = PineappleGreen.copy(alpha = 0.12)),
                    onClick = { expanded = true },
                )
                .padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    selected?.let { "${it.resolution} • ${it.ext}" } ?: "اختر الجودة",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                if (selected?.displaySize != null) {
                    QualitySizeBadge(selected.filesize)
                }
                Icon(
                    painter = painterResource(R.drawable.ic_chevron),
                    contentDescription = "فتح",
                    tint = PineappleGreen,
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${opt.resolution} • ${opt.ext}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            if (opt.displaySize != null) QualitySizeBadge(opt.filesize)
                        }
                    },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun QualitySizeBadge(filesize: Long) {
    val color = when {
        filesize <= 0 -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        filesize < 20 * 1024 * 1024 -> PineappleGreen
        filesize < 200 * 1024 * 1024 -> PineappleYellow
        else -> com.pineapple.downloader.ui.theme.PineappleOrangeDark
    }
    Text(
        com.pineapple.downloader.model.formatBytes(filesize).takeIf { filesize > 0 } ?: "—",
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier.padding(start = 8.dp),
    )
}
