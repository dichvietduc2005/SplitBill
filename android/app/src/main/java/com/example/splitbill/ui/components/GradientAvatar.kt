package com.example.splitbill.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue

@Composable
fun GradientAvatar(
  name: String,
  modifier: Modifier = Modifier,
  size: Dp = 40.dp
) {
  val colors = rememberGradientForName(name)
  
  Surface(
    shape = CircleShape,
    color = Color.Transparent,
    modifier = modifier.size(size)
  ) {
    Box(
      contentAlignment = Alignment.Center, 
      modifier = Modifier
        .fillMaxSize()
        .background(Brush.linearGradient(colors))
    ) {
      Text(
        text = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = Color.White
      )
    }
  }
}

// Generate consistent gradient based on username hash
fun rememberGradientForName(name: String): List<Color> {
  val hash = name.hashCode().absoluteValue
  val palettes = listOf(
    listOf(Color(0xFF00B4D8), Color(0xFF7C3AED)), // Ocean
    listOf(Color(0xFFF97316), Color(0xFFEC4899)), // Sunset
    listOf(Color(0xFF6366F1), Color(0xFF06B6D4)), // Aurora
    listOf(Color(0xFF10B981), Color(0xFF3B82F6)), // Forest
    listOf(Color(0xFFF43F5E), Color(0xFFA855F7)), // Rose
    listOf(Color(0xFFEAB308), Color(0xFFF97316))  // Mango
  )
  return palettes[hash % palettes.size]
}
