package com.pineapple.downloader.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * زوايا مقصودة غير نمطية: بطاقة الفيديو 12dp بلا elevation وحدود رفيعة —
 * عكس قالب "Card بزوايا 16 وحشو 16 في كل مكان".
 */
val PineappleShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(24.dp),
)
