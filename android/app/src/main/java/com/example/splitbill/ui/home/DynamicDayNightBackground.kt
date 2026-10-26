package com.example.splitbill.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.unit.dp
import java.time.LocalTime
import kotlin.math.sin

/**
 * 4 Phase chu kỳ ngày - đêm:
 * - DAWN: 05:00 - 08:00 (Bình minh ấm áp)
 * - DAY: 08:00 - 16:30 (Ban ngày trời xanh nắng vàng)
 * - SUNSET: 16:30 - 19:00 (Hoàng hôn rực rỡ lãng mạn)
 * - NIGHT: 19:00 - 05:00 (Màn đêm huyền ảo, trăng sao lấp lánh)
 */
enum class TimeOfDay(
  val label: String,
  val greetingPrefix: String,
  val emoji: String,
  val quote: String
) {
  DAWN(
    label = "Bình minh",
    greetingPrefix = "Chào buổi sáng,",
    emoji = "🌅",
    quote = "Bình minh rạng rỡ,\nchúc bạn một ngày tràn đầy năng lượng!"
  ),
  DAY(
    label = "Ban ngày",
    greetingPrefix = "Xin chào,",
    emoji = "☀️",
    quote = "Cùng nhau chia sẻ,\ncùng nhau tận hưởng!"
  ),
  SUNSET(
    label = "Hoàng hôn",
    greetingPrefix = "Buổi chiều an lành,",
    emoji = "🌇",
    quote = "Gác lại âu lo,\ncân đối chi tiêu hôm nay cùng bạn bè!"
  ),
  NIGHT(
    label = "Ban đêm",
    greetingPrefix = "Buổi tối an lành,",
    emoji = "🌙",
    quote = "Nghỉ ngơi thật tốt,\nmọi chi tiêu đã được SplitBill ghi nhận."
  )
}

/**
 * Tính toán TimeOfDay dựa vào giờ thực tế của thiết bị
 */
fun resolveCurrentTimeOfDay(now: LocalTime = LocalTime.now()): TimeOfDay {
  val hour = now.hour
  val minute = now.minute
  val totalMinutes = hour * 60 + minute

  return when {
    totalMinutes in (5 * 60)..(8 * 60 - 1) -> TimeOfDay.DAWN
    totalMinutes in (8 * 60)..(16 * 60 + 30 - 1) -> TimeOfDay.DAY
    totalMinutes in (16 * 60 + 30)..(19 * 60 - 1) -> TimeOfDay.SUNSET
    else -> TimeOfDay.NIGHT
  }
}

/**
 * Component Canvas vẽ phong cảnh 2D Day/Night Cycle sống động:
 * - Bầu trời chuyển sắc linh hoạt theo thời gian
 * - Mặt trời / mặt trăng lơ lửng chuyển động nhẹ (floating + breathing glow)
 * - Bầu trời sao nhấp nháy vào ban đêm
 * - Mây trôi bồng bềnh nhẹ nhàng ban ngày
 * - 3 tầng núi đồi 2D (Parallax depth layers) với hàng cây thông cách điệu
 */
@Composable
fun DynamicDayNightBackground(
  timeOfDay: TimeOfDay,
  modifier: Modifier = Modifier
) {
  // 1. Chuyển động lơ lửng nhẹ nhàng của thiên thể (Sun / Moon)
  val infiniteTransition = rememberInfiniteTransition(label = "celestial_cycle")
  
  val floatOffsetAnim by infiniteTransition.animateFloat(
    initialValue = -5f,
    targetValue = 5f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "float_offset"
  )

  // 2. Độ tỏa hào quang xung quanh mặt trời / mặt trăng
  val glowPulseAnim by infiniteTransition.animateFloat(
    initialValue = 0.88f,
    targetValue = 1.14f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2800, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "glow_pulse"
  )

  // 3. Mây trôi ngang nhẹ nhàng
  val cloudDriftAnim by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 28000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "cloud_drift"
  )

  // 4. Nhịp nhấp nháy của các vì sao đêm
  val starTwinkleAnim by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = (2 * Math.PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 3200, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "star_twinkle"
  )

  Canvas(modifier = modifier.fillMaxSize()) {
    val w = size.width
    val h = size.height

    // ─── A. BẦU TRỜI (SKY GRADIENT) ──────────────────────────────────────────
    val skyColors = when (timeOfDay) {
      TimeOfDay.DAWN -> listOf(
        Color(0xFF23254B),
        Color(0xFF5A3E66),
        Color(0xFF9E5779),
        Color(0xFFFF9A76),
        Color(0xFFFFC38B)
      )
      TimeOfDay.DAY -> listOf(
        Color(0xFF104F96),
        Color(0xFF1976D2),
        Color(0xFF42A5F5),
        Color(0xFF81D4FA),
        Color(0xFFB3E5FC)
      )
      TimeOfDay.SUNSET -> listOf(
        Color(0xFF240E3E),
        Color(0xFF581845),
        Color(0xFF900C3F),
        Color(0xFFC70039),
        Color(0xFFFF5733),
        Color(0xFFFFC300)
      )
      TimeOfDay.NIGHT -> listOf(
        Color(0xFF020712),
        Color(0xFF051326),
        Color(0xFF0A2239),
        Color(0xFF12344D)
      )
    }

    drawRect(
      brush = Brush.verticalGradient(
        colors = skyColors,
        startY = 0f,
        endY = h * 0.95f
      ),
      size = size
    )

    // ─── B. SAO ĐÊM (STARS) NẾU LÀ BAN ĐÊM ──────────────────────────────────
    if (timeOfDay == TimeOfDay.NIGHT) {
      for (i in 0..32) {
        val starX = ((i * 79 + 31) % 100) / 100f * w
        val starY = ((i * 53 + 17) % 55) / 100f * (h * 0.70f)
        val alphaOffset = (sin(starTwinkleAnim + i * 0.95f) * 0.42f + 0.58f).coerceIn(0.15f, 1f)
        val starSize = if (i % 5 == 0) 2.2.dp.toPx() else 1.3.dp.toPx()

        drawCircle(
          color = Color.White.copy(alpha = alphaOffset),
          radius = starSize,
          center = Offset(starX, starY)
        )
      }
    }

    // ─── C. MÂY TRÔI (CLOUDS) KHI TRỜI SÁNG / HOÀNG HÔN ─────────────────────
    if (timeOfDay != TimeOfDay.NIGHT) {
      val cloudColor = when (timeOfDay) {
        TimeOfDay.DAWN -> Color(0xFFFFD1BA).copy(alpha = 0.28f)
        TimeOfDay.DAY -> Color.White.copy(alpha = 0.35f)
        TimeOfDay.SUNSET -> Color(0xFFFF9E79).copy(alpha = 0.30f)
        TimeOfDay.NIGHT -> Color.Transparent
      }

      // Đám mây 1
      val c1X = ((cloudDriftAnim * w * 1.3f) % (w * 1.5f)) - (w * 0.25f)
      drawCloud(
        offsetX = c1X,
        offsetY = h * 0.22f,
        width = 110.dp.toPx(),
        height = 32.dp.toPx(),
        color = cloudColor
      )

      // Đám mây 2
      val c2X = (((cloudDriftAnim + 0.52f) * w * 1.1f) % (w * 1.5f)) - (w * 0.3f)
      drawCloud(
        offsetX = c2X,
        offsetY = h * 0.36f,
        width = 140.dp.toPx(),
        height = 38.dp.toPx(),
        color = cloudColor.copy(alpha = cloudColor.alpha * 0.8f)
      )
    }

    // ─── D. MẶT TRỜI / MẶT TRĂNG (CELESTIAL BODY) ──────────────────────────
    val floatPx = floatOffsetAnim.dp.toPx()

    when (timeOfDay) {
      TimeOfDay.DAWN -> {
        // Mặt trời mới nhú mọc phía góc trái chân trời
        val sunCenter = Offset(w * 0.22f, h * 0.46f + floatPx)
        val sunRadius = 24.dp.toPx()

        // Hào quang tỏa ra
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(
              Color(0xFFFFB07C).copy(alpha = 0.65f * glowPulseAnim),
              Color(0xFFFF9A76).copy(alpha = 0.30f * glowPulseAnim),
              Color.Transparent
            ),
            center = sunCenter,
            radius = sunRadius * 3.2f * glowPulseAnim
          ),
          center = sunCenter,
          radius = sunRadius * 3.2f * glowPulseAnim
        )

        // Quả cầu mặt trời
        drawCircle(
          color = Color(0xFFFFEAA7),
          radius = sunRadius,
          center = sunCenter
        )
      }

      TimeOfDay.DAY -> {
        // Mặt trời ban ngày tỏa sáng rực rỡ ở góc phải trên cao
        val sunCenter = Offset(w * 0.72f, h * 0.22f + floatPx)
        val sunRadius = 26.dp.toPx()

        // Quầng sáng tỏa rộng
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(
              Color(0xFFFFF9C4).copy(alpha = 0.75f * glowPulseAnim),
              Color(0xFFFFEE58).copy(alpha = 0.35f * glowPulseAnim),
              Color(0xFFFFD54F).copy(alpha = 0.12f * glowPulseAnim),
              Color.Transparent
            ),
            center = sunCenter,
            radius = sunRadius * 3.6f * glowPulseAnim
          ),
          center = sunCenter,
          radius = sunRadius * 3.6f * glowPulseAnim
        )

        // Vành ánh sáng
        drawCircle(
          color = Color(0xFFFFF59D).copy(alpha = 0.5f),
          radius = sunRadius * 1.25f,
          center = sunCenter
        )

        // Tâm mặt trời
        drawCircle(
          color = Color(0xFFFFFFFF),
          radius = sunRadius,
          center = sunCenter
        )
      }

      TimeOfDay.SUNSET -> {
        // Mặt trời hoàng hôn đỏ rực đang lặn dần xuống dãy núi
        val sunCenter = Offset(w * 0.76f, h * 0.50f + floatPx)
        val sunRadius = 25.dp.toPx()

        // Quầng rực rỡ màu cam/đỏ
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(
              Color(0xFFFF7675).copy(alpha = 0.70f * glowPulseAnim),
              Color(0xFFD63031).copy(alpha = 0.32f * glowPulseAnim),
              Color.Transparent
            ),
            center = sunCenter,
            radius = sunRadius * 3.4f * glowPulseAnim
          ),
          center = sunCenter,
          radius = sunRadius * 3.4f * glowPulseAnim
        )

        // Đĩa mặt trời
        drawCircle(
          color = Color(0xFFFFBE76),
          radius = sunRadius,
          center = sunCenter
        )
      }

      TimeOfDay.NIGHT -> {
        // Mặt trăng lưỡi liềm phát sáng dịu êm góc phải
        val moonCenter = Offset(w * 0.74f, h * 0.24f + floatPx)
        val moonRadius = 21.dp.toPx()

        // Ánh trăng xanh mờ dịu
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(
              Color(0xFF90CAF9).copy(alpha = 0.40f * glowPulseAnim),
              Color(0xFF1E88E5).copy(alpha = 0.12f * glowPulseAnim),
              Color.Transparent
            ),
            center = moonCenter,
            radius = moonRadius * 2.8f * glowPulseAnim
          ),
          center = moonCenter,
          radius = moonRadius * 2.8f * glowPulseAnim
        )

        // Vẽ trăng lưỡi liềm bằng phép trừ đường Path (Difference)
        val fullMoonPath = Path().apply {
          addOval(Rect(center = moonCenter, radius = moonRadius))
        }
        val moonCutoutPath = Path().apply {
          addOval(
            Rect(
              center = Offset(moonCenter.x - moonRadius * 0.45f, moonCenter.y - moonRadius * 0.25f),
              radius = moonRadius * 0.88f
            )
          )
        }
        val crescentMoonPath = Path().apply {
          op(fullMoonPath, moonCutoutPath, PathOperation.Difference)
        }

        drawPath(
          path = crescentMoonPath,
          color = Color(0xFFF0F4F8)
        )
      }
    }

    // ─── E. DÃY NÚI XA (DISTANT MOUNTAINS - LAYER 1) ────────────────────────
    val distantMountainColor = when (timeOfDay) {
      TimeOfDay.DAWN -> Color(0xFF4C3660).copy(alpha = 0.75f)
      TimeOfDay.DAY -> Color(0xFF285E7B).copy(alpha = 0.65f)
      TimeOfDay.SUNSET -> Color(0xFF481C43).copy(alpha = 0.78f)
      TimeOfDay.NIGHT -> Color(0xFF091C2C).copy(alpha = 0.80f)
    }

    val backMountainPath = Path().apply {
      moveTo(0f, h * 0.62f)
      lineTo(w * 0.18f, h * 0.49f)
      lineTo(w * 0.34f, h * 0.58f)
      lineTo(w * 0.55f, h * 0.45f)
      lineTo(w * 0.72f, h * 0.56f)
      lineTo(w * 0.88f, h * 0.48f)
      lineTo(w, h * 0.57f)
      lineTo(w, h)
      lineTo(0f, h)
      close()
    }
    drawPath(path = backMountainPath, color = distantMountainColor)

    // ─── F. ĐỒI TRUNG CẢNH & HÀNG CÂY THÔNG (MID RIDGE - LAYER 2) ───────────
    val midRidgeColor = when (timeOfDay) {
      TimeOfDay.DAWN -> Color(0xFF332042).copy(alpha = 0.90f)
      TimeOfDay.DAY -> Color(0xFF1B4E63).copy(alpha = 0.88f)
      TimeOfDay.SUNSET -> Color(0xFF2D0F2E).copy(alpha = 0.90f)
      TimeOfDay.NIGHT -> Color(0xFF04111E).copy(alpha = 0.90f)
    }

    val midHillsPath = Path().apply {
      moveTo(0f, h * 0.68f)
      cubicTo(w * 0.20f, h * 0.62f, w * 0.35f, h * 0.72f, w * 0.50f, h * 0.65f)
      cubicTo(w * 0.68f, h * 0.58f, w * 0.84f, h * 0.69f, w, h * 0.64f)
      lineTo(w, h)
      lineTo(0f, h)
      close()
    }
    drawPath(path = midHillsPath, color = midRidgeColor)

    // Điểm xuyết vài cây thông 2D trên triền đồi
    val pinePositions = listOf(
      Offset(w * 0.12f, h * 0.65f),
      Offset(w * 0.16f, h * 0.64f),
      Offset(w * 0.46f, h * 0.66f),
      Offset(w * 0.51f, h * 0.65f),
      Offset(w * 0.80f, h * 0.65f),
      Offset(w * 0.85f, h * 0.63f)
    )
    for (pos in pinePositions) {
      drawPineTree(pos.x, pos.y, 14.dp.toPx(), 20.dp.toPx(), midRidgeColor)
    }

    // ─── G. ĐỒI TIỀN CẢNH (FOREGROUND SLOPE - LAYER 3) ──────────────────────
    val foregroundColor = when (timeOfDay) {
      TimeOfDay.DAWN -> Color(0xFF1D1126)
      TimeOfDay.DAY -> Color(0xFF0D2E3B)
      TimeOfDay.SUNSET -> Color(0xFF18061C)
      TimeOfDay.NIGHT -> Color(0xFF01060B)
    }

    val foregroundPath = Path().apply {
      moveTo(0f, h * 0.74f)
      cubicTo(w * 0.32f, h * 0.71f, w * 0.60f, h * 0.83f, w, h * 0.76f)
      lineTo(w, h)
      lineTo(0f, h)
      close()
    }
    drawPath(path = foregroundPath, color = foregroundColor)

    // ─── H. LỚP VIGNETTE CHUYỂN TIẾP CHO TEXT TRÊN HEADER & SHEET BÊN DƯỚI ─
    // Gradient tối trên cùng để icon status bar và avatar nổi bật rõ ràng
    drawRect(
      brush = Brush.verticalGradient(
        0.0f to Color.Black.copy(alpha = 0.45f),
        0.35f to Color.Transparent
      ),
      size = size
    )

    // Gradient dưới cùng hòa vào nền bo tròn của Warm Floating Sheet
    drawRect(
      brush = Brush.verticalGradient(
        0.0f to Color.Transparent,
        0.75f to foregroundColor.copy(alpha = 0.50f),
        1.0f to foregroundColor.copy(alpha = 0.95f)
      ),
      size = size
    )
  }
}

/**
 * Hàm vẽ đám mây cách điệu 2D từ các hình ellipse/capsule
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCloud(
  offsetX: Float,
  offsetY: Float,
  width: Float,
  height: Float,
  color: Color
) {
  val path = Path().apply {
    addOval(Rect(offsetX, offsetY + height * 0.25f, offsetX + width, offsetY + height * 0.95f))
    addOval(Rect(offsetX + width * 0.2f, offsetY, offsetX + width * 0.65f, offsetY + height * 0.85f))
    addOval(Rect(offsetX + width * 0.45f, offsetY + height * 0.1f, offsetX + width * 0.85f, offsetY + height * 0.9f))
  }
  drawPath(path, color)
}

/**
 * Hàm vẽ cây thông 2D tam giác cách điệu
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPineTree(
  x: Float,
  y: Float,
  width: Float,
  height: Float,
  color: Color
) {
  val path = Path().apply {
    // Tầng ngọn
    moveTo(x, y - height)
    lineTo(x - width * 0.35f, y - height * 0.45f)
    lineTo(x - width * 0.2f, y - height * 0.45f)
    // Tầng giữa
    lineTo(x - width * 0.5f, y)
    lineTo(x + width * 0.5f, y)
    lineTo(x + width * 0.2f, y - height * 0.45f)
    lineTo(x + width * 0.35f, y - height * 0.45f)
    close()
  }
  drawPath(path, color)
}
