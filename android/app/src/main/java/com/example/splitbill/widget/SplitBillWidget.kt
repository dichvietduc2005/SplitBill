package com.example.splitbill.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.splitbill.MainActivity
import java.text.NumberFormat
import java.util.Locale

class SplitBillWidget : GlanceAppWidget() {

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val prefs = context.getSharedPreferences("splitbill_widget_prefs", Context.MODE_PRIVATE)
    val owedToOthers = prefs.getLong("owed_to_others", 0L)
    val othersOweMe = prefs.getLong("others_owe_me", 0L)

    val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))

    provideContent {
      WidgetContent(
        owedToOthersStr = "${formatter.format(owedToOthers)} đ",
        othersOweMeStr = "${formatter.format(othersOweMe)} đ",
        context = context
      )
    }
  }

  @Composable
  private fun WidgetContent(
    owedToOthersStr: String,
    othersOweMeStr: String,
    context: Context
  ) {
    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    Column(
      modifier = GlanceModifier
        .fillMaxSize()
        .cornerRadius(16.dp)
        .background(ColorProvider(Color(0xFFF1F5F9))) // Soft Off-White/Light Slate
        .padding(12.dp)
        .clickable(actionStartActivity(intent)),
      verticalAlignment = Alignment.CenterVertically,
      horizontalAlignment = Alignment.Start
    ) {
      Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "SplitBill 💸",
          style = TextStyle(
            color = ColorProvider(Color(0xFF059669)), // Fresh Emerald Green
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
          )
        )
      }

      Spacer(modifier = GlanceModifier.height(6.dp))

      Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
          Text(
            text = "Bạn nợ",
            style = TextStyle(color = ColorProvider(Color(0xFF64748B)), fontSize = 11.sp)
          )
          Text(
            text = owedToOthersStr,
            style = TextStyle(
              color = ColorProvider(Color(0xFFDC2626)), // Bright Red
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold
            )
          )
        }

        Column(modifier = GlanceModifier.defaultWeight()) {
          Text(
            text = "Được nợ",
            style = TextStyle(color = ColorProvider(Color(0xFF64748B)), fontSize = 11.sp)
          )
          Text(
            text = othersOweMeStr,
            style = TextStyle(
              color = ColorProvider(Color(0xFF059669)), // Fresh Emerald Green
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold
            )
          )
        }
      }
    }
  }
}
