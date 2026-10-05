package com.pineapple.downloader.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.pineapple.downloader.R
import com.pineapple.downloader.download.FfmpegPostProcessor
import com.pineapple.downloader.ui.components.PineappleTopBar
import com.pineapple.downloader.ui.theme.PineappleGreen
import com.pineapple.downloader.ui.theme.ThemeMode
import com.pineapple.downloader.util.SettingsStore
import kotlinx.coroutines.launch

/** شاشة الإعدادات: الثيم + مجلد الحفظ + إخلاء المسؤولية + حالة FFmpeg. */
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themeMode by remember { SettingsStore.themeMode(context) }
        .collectAsState(initial = ThemeMode.SYSTEM)

    fun setTheme(mode: ThemeMode) {
        scope.launch { SettingsStore.setThemeMode(context, mode) }
    }

    Column(Modifier.fillMaxSize()) {
        PineappleTopBar(onSettingsClick = onBack)
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text("الإعدادات", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(16.dp))

            Text("المظهر", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            ThemeOption("فاتح", "خلفية بيضاء بهوية الأناناس", themeMode == ThemeMode.LIGHT) {
                setTheme(ThemeMode.LIGHT)
            }
            ThemeOption("داكن", "خلفية #121212 مع نفس الأخضر والأصفر", themeMode == ThemeMode.DARK) {
                setTheme(ThemeMode.DARK)
            }
            ThemeOption("تلقائي", "يتبع نظام الهاتف", themeMode == ThemeMode.SYSTEM) {
                setTheme(ThemeMode.SYSTEM)
            }

            Spacer(Modifier.height(24.dp))
            Text("مجلد الحفظ", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_folder),
                    contentDescription = null,
                    tint = PineappleGreen,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "التنزيلات / Pineapple",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                "تُحفظ الملفات في مجلد التنزيلات العامة عبر MediaStore على أندرويد 10+.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )

            Spacer(Modifier.height(24.dp))
            Text("المحرك", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                if (FfmpegPostProcessor.isSupported) {
                    "FFmpeg متاح على هذا الجهاز — الدمج والتحويل إلى MP3 يعملان."
                } else {
                    "FFmpeg غير متاح على معمارية هذا الجهاز — سيُحمَّل MP4/M4A مباشرة بدون تحويل."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )

            Spacer(Modifier.height(24.dp))
            Text("إخلاء المسؤولية", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "حمّل فقط المحتوى الذي تملكه أو المسموح بتحميله. احترام شروط الاستخدام " +
                    "الخاصة بكل موقع وحقوق النشر مسؤوليتك وحدك.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ThemeOption(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = PineappleGreen),
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}
