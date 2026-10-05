package com.pineapple.downloader.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.pineapple.downloader.R
import com.pineapple.downloader.ui.theme.PineappleGreen
import com.pineapple.downloader.ui.theme.PineappleInk

/**
 * شريط علوي مخصص (لا TopAppBar افتراضي):
 * شعار الأناناس يمينًا (RTL) + "Pineapple" بالأسود و"Downloader" بالأخضر،
 * أيقونة إعدادات Lucide يسارًا.
 */
@Composable
fun PineappleTopBar(onSettingsClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = androidx.compose.runtime.remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Image(
            painter = painterResource(R.drawable.pineapple_logo),
            contentDescription = "شعار الأناناس",
            modifier = Modifier.size(32.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = PineappleInk, fontWeight = FontWeight.Bold)) {
                    append("Pineapple")
                }
                append(" ")
                withStyle(SpanStyle(color = PineappleGreen, fontWeight = FontWeight.Bold)) {
                    append("Downloader")
                }
            },
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.weight(1f))
        Icon(
            painter = painterResource(R.drawable.ic_settings),
            contentDescription = "الإعدادات",
            tint = PineappleGreen,
            modifier = Modifier
                .size(40.dp)
                .clickable(
                    interactionSource = interaction,
                    indication = ripple(color = PineappleGreen.copy(alpha = 0.12)),
                ) { onSettingsClick() }
                .padding(8.dp),
        )
    }
}
