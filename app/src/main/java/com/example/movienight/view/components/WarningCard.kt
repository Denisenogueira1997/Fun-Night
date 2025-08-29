package com.example.movienight.view.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class WarningCardStyle(
    val label: String, val color: Color, val fontSize: TextUnit = 15.sp
)

fun WarningCard(rating: String?): WarningCardStyle {
    return when (rating?.trim()?.lowercase()) {
        "l", "livre" -> WarningCardStyle("Livre", Color(0xFF4CAF50))
        "10" -> WarningCardStyle("10 anos", Color(0xFF03A9F4))
        "12" -> WarningCardStyle("12 anos", Color(0xFFFFC107))
        "14" -> WarningCardStyle("14 anos", Color(0xFFFF9800))
        "16" -> WarningCardStyle("16 anos", Color(0xFFF44336))
        "18" -> WarningCardStyle("18 anos", Color(0xFF000000))
        else -> WarningCardStyle("Classificação indicativa não disponível", Color.Gray)
    }
}

@Composable
fun WarningCard(rating: String?, modifier: Modifier = Modifier) {
    val style = WarningCard(rating)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = style.color),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = style.label,
                color = if (style.color == Color.Black) Color.White else Color.White,
                fontSize = 18.sp
            )
        }
    }
}
