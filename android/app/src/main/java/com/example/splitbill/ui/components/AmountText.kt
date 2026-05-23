package com.example.splitbill.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.example.splitbill.theme.LocalSplitBillCustomColors
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AmountText(
  amount: Double,
  modifier: Modifier = Modifier,
  style: TextStyle = LocalTextStyle.current,
  isDebt: Boolean? = null // null means neutral (e.g., total bill amount)
) {
  val formattedAmount = NumberFormat.getCurrencyInstance(Locale("vi", "VN")).format(amount)
  
  val textColor = when (isDebt) {
    true -> LocalSplitBillCustomColors.current.negativeAmount
    false -> LocalSplitBillCustomColors.current.positiveAmount
    null -> style.color // use default color from style
  }

  val finalColor = if (textColor != Color.Unspecified) textColor else style.color

  AnimatedContent(
    targetState = formattedAmount,
    transitionSpec = {
      fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
    },
    label = "amount_animation",
    modifier = modifier
  ) { targetText ->
    Text(
      text = targetText,
      style = style,
      color = finalColor
    )
  }
}
