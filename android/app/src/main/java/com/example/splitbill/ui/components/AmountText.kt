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
import com.example.splitbill.ui.localization.AppLanguage
import com.example.splitbill.ui.localization.LocalAppLanguage
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AmountText(
  amount: Double,
  modifier: Modifier = Modifier,
  style: TextStyle = LocalTextStyle.current,
  isDebt: Boolean? = null, // null means neutral (e.g., total bill amount)
  currency: String = "VND",
  overrideColor: Color = Color.Unspecified
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
  
  val themeColor = when (isDebt) {
    true -> LocalSplitBillCustomColors.current.negativeAmount
    false -> LocalSplitBillCustomColors.current.positiveAmount
    null -> style.color // use default color from style
  }

  val finalColor = when {
    overrideColor != Color.Unspecified -> overrideColor
    themeColor != Color.Unspecified -> themeColor
    else -> style.color
  }

  Text(
    text = formattedAmount,
    style = style,
    color = finalColor,
    modifier = modifier
  )
}

/** Số tiền đầy đủ theo tiền đồng, ví dụ "1.292.477.696 ₫" */
fun formatVndFull(amount: Double): String =
  NumberFormat.getCurrencyInstance(Locale("vi", "VN")).format(amount)

/**
 * Số tiền rút gọn cho những chỗ hẹp (giữa biểu đồ tròn, nhãn cột, thẻ đôi):
 * 250.000 · 1,2 tr · 1,29 tỷ
 */
@Composable
fun formatCompactAmount(amount: Double): String {
  val english = LocalAppLanguage.current == AppLanguage.ENGLISH
  val value = kotlin.math.abs(amount)
  val sign = if (amount < 0) "-" else ""
  val locale = if (english) Locale.US else Locale.GERMANY
  val formatter = DecimalFormat("#,##0.##", DecimalFormatSymbols(locale))
  return when {
    value >= 1_000_000_000 -> sign + formatter.format(value / 1_000_000_000) + if (english) "B" else " tỷ"
    value >= 1_000_000 -> sign + formatter.format(value / 1_000_000) + if (english) "M" else " tr"
    else -> sign + NumberFormat.getIntegerInstance(locale).format(value.toLong())
  }
}
