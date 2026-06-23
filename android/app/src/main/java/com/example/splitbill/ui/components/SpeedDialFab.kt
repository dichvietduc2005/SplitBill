package com.example.splitbill.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.splitbill.theme.Dimens
import com.example.splitbill.theme.Motion

data class SpeedDialItem(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
    val color: Color? = null,
    val onColor: Color? = null
)

@Composable
fun SpeedDialFab(
    items: List<SpeedDialItem>,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.Add,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    var expanded by remember { mutableStateOf(false) }


    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingM),
        modifier = modifier
    ) {
        // Render items when expanded
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(expandFrom = Alignment.Bottom, animationSpec = Motion.springBouncy()) + fadeIn(animationSpec = Motion.tweenMedium()),
            exit = shrinkVertically(shrinkTowards = Alignment.Bottom, animationSpec = Motion.springGentle()) + fadeOut(animationSpec = Motion.tweenFast())
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingM)
            ) {
                items.forEachIndexed { index, item ->
                    SpeedDialItemRow(item = item, delayMillis = (items.size - index) * 50) {
                        expanded = false
                        item.onClick()
                    }
                }
            }
        }

        // Main FAB
        val rotation by animateFloatAsState(
            targetValue = if (expanded) 45f else 0f,
            animationSpec = Motion.springBouncy(),
            label = "fab_rotation"
        )

        val gradient = androidx.compose.ui.graphics.Brush.horizontalGradient(
            listOf(com.example.splitbill.theme.GradientOceanStart, com.example.splitbill.theme.GradientOceanEnd)
        )
        val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
        
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(end = 4.dp)
                .shadow(
                    elevation = if (expanded) 2.dp else 8.dp,
                    shape = CircleShape,
                    spotColor = com.example.splitbill.theme.GradientOceanStart,
                    ambientColor = com.example.splitbill.theme.GradientOceanStart
                )
                .size(56.dp)
                .clip(CircleShape)
                .background(gradient)
                .clickable {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    expanded = !expanded 
                }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Menu",
                tint = Color.White,
                modifier = Modifier.rotate(rotation)
            )
        }
    }
}

@Composable
private fun SpeedDialItemRow(item: SpeedDialItem, delayMillis: Int, onClick: () -> Unit) {
    // Optional: Could add staggered entrance per item here if needed, 
    // but the column animation usually suffices for a few items.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.shadow(4.dp, RoundedCornerShape(8.dp))
        ) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = Dimens.SpacingM, vertical = Dimens.SpacingS)
            )
        }
        
        Spacer(modifier = Modifier.width(Dimens.SpacingM))

        // Haptic feedback for child item
        val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

        val itemGradient = if (item.color != null) {
            androidx.compose.ui.graphics.Brush.linearGradient(
                listOf(item.color, item.color.copy(alpha = 0.8f))
            )
        } else {
            androidx.compose.ui.graphics.Brush.linearGradient(
                listOf(
                    com.example.splitbill.theme.GradientOceanStart,
                    com.example.splitbill.theme.GradientOceanEnd
                )
            )
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = CircleShape,
                    spotColor = item.color ?: com.example.splitbill.theme.GradientOceanStart,
                    ambientColor = item.color ?: com.example.splitbill.theme.GradientOceanStart
                )
                .clip(CircleShape)
                .background(itemGradient)
                .clickable {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    onClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = item.onColor ?: Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp)) // Alignment offset
    }
}
