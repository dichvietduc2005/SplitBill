package com.example.splitbill.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    val size: Float,
    var rotation: Float,
    val rotationSpeed: Float
)

@Composable
fun ConfettiOverlay(modifier: Modifier = Modifier) {
    val particles = remember {
        val colors = listOf(
            Color(0xFFE53935), Color(0xFFD81B60), Color(0xFF8E24AA),
            Color(0xFF5E35B1), Color(0xFF3949AB), Color(0xFF1E88E5),
            Color(0xFF039BE5), Color(0xFF00ACC1), Color(0xFF00897B),
            Color(0xFF43A047), Color(0xFF7CB342), Color(0xFFC0CA33),
            Color(0xFFFDD835), Color(0xFFFFB300), Color(0xFFFB8C00),
            Color(0xFFF4511E)
        )
        List(150) {
            Particle(
                x = 0.5f, // percentage, will be scaled to canvas size
                y = 0.5f,
                vx = Random.nextFloat() * 4f - 2f,
                vy = Random.nextFloat() * -4f - 1f, // Mostly upwards
                color = colors.random(),
                size = Random.nextFloat() * 15f + 10f,
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = Random.nextFloat() * 20f - 10f
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "confetti")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(16, easing = LinearEasing)
        ),
        label = "time"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        // Trigger recomposition on each frame
        time.hashCode() 
        
        val width = size.width
        val height = size.height

        val gravity = 0.15f
        val friction = 0.98f

        particles.forEach { p ->
            // Physics
            p.vy += gravity
            p.vx *= friction
            p.vy *= friction
            
            // Need to apply velocity as percentage of screen height for consistency? 
            // Or just treat vx/vy as raw pixel movements relative to a standard resolution.
            // Let's treat them as percentages (divided by 100)
            p.x += p.vx / 100f
            p.y += p.vy / 100f
            p.rotation += p.rotationSpeed

            // Only draw if on screen
            if (p.x in -0.1f..1.1f && p.y in -0.1f..1.1f) {
                rotate(degrees = p.rotation, pivot = Offset(p.x * width, p.y * height)) {
                    drawRect(
                        color = p.color,
                        topLeft = Offset(p.x * width - p.size/2, p.y * height - p.size/2),
                        size = Size(p.size, p.size)
                    )
                }
            }
        }
    }
}
