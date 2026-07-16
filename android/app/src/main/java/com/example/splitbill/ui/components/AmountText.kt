package com.example.splitbill.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
  isDebt: Boolean? = null, // null means neutral (e.g., total bill amount)
  currency: String = "VND"
) {
  val animatedAmount by androidx.compose.animation.core.animateFloatAsState(
    targetValue = amount.toFloat(),
    animationSpec = tween(durationMillis = 800, easing = androidx.compose.animation.core.FastOutSlowInEasing),
    label = "amount_counter"
  )

  val formattedAmount = if (currency == "VND") {
    NumberFormat.getCurrencyInstance(Locale("vi", "VN")).format(animatedAmount.toDouble())
  } else {
    try {
      val format = NumberFormat.getCurrencyInstance(Locale.US)
      format.currency = java.util.Currency.getInstance(currency)
      format.format(animatedAmount.toDouble())
    } catch (e: Exception) {
      "${animatedAmount.toInt()} $currency"
    }
  }
  
  val textColor = when (isDebt) {
    true -> LocalSplitBillCustomColors.current.negativeAmount
    false -> LocalSplitBillCustomColors.current.positiveAmount
    null -> style.color // use default color from style
  }

  val finalColor = if (textColor != Color.Unspecified) textColor else style.color

  Text(
    text = formattedAmount,
    style = style,
    color = finalColor,
    modifier = modifier
  )
}
