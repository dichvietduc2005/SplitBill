package com.example.splitbill.ui.components

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.splitbill.data.api.ProfileResponse
import com.example.splitbill.theme.Dimens
import java.text.NumberFormat
import java.util.Locale

// ─── VietQR URL helpers ────────────────────────────────────────────────────

/** QR image URL dùng để hiển thị ảnh quét trong app */
fun buildVietQrImageUrl(
  bankCode: String,
  accountNumber: String,
  accountName: String,
  amount: Long,
  description: String
): String {
  val encodedDesc = Uri.encode(description)
  val encodedName = Uri.encode(accountName)
  return "https://img.vietqr.io/image/$bankCode-$accountNumber-compact2.png" +
    "?amount=$amount&addInfo=$encodedDesc&accountName=$encodedName"
}

/**
 * Deep link chuẩn NAPAS/VietQR — khi mở trên Android sẽ hiện chooser
 * các app ngân hàng đang cài có hỗ trợ VietQR để người dùng chọn.
 * Sau khi chọn, app ngân hàng tự điền sẵn: số tài khoản, số tiền, nội dung.
 *
 * Tài liệu: https://dl.vietqr.io/pay (NAPAS chuẩn)
 */
fun buildVietQrDeepLink(
  bankCode: String,
  accountNumber: String,
  amount: Long,
  description: String
): String {
  val bankId = bankCode.lowercase()
  val encodedDesc = Uri.encode(description)
  return "https://dl.vietqr.io/pay" +
    "?app=$bankId" +
    "&ba=$accountNumber@$bankId" +
    "&am=$amount" +
    "&tn=$encodedDesc"
}

/** Mở deep link — nếu không có app nào xử lý thì show toast hướng dẫn */
fun openVietQrDeepLink(context: Context, deepLink: String, bankName: String) {
  try {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLink)).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
  } catch (e: ActivityNotFoundException) {
    Toast.makeText(
      context,
      "Không tìm thấy app $bankName. Vui lòng cài đặt hoặc dùng QR code bên trên.",
      Toast.LENGTH_LONG
    ).show()
  }
}

/** Lấy tên hiển thị của ngân hàng từ bank code */
fun getBankDisplayName(bankCode: String): String = bankDisplayNames[bankCode.uppercase()] ?: bankCode

private val bankDisplayNames = mapOf(
  "MB"     to "MB Bank",
  "VCB"    to "Vietcombank",
  "TCB"    to "Techcombank",
  "ACB"    to "ACB",
  "VPB"    to "VPBank",
  "BIDV"   to "BIDV",
  "VTB"    to "VietinBank",
  "HDB"    to "HDBank",
  "VIB"    to "VIB",
  "OCB"    to "OCB",
  "TPB"    to "TPBank",
  "STB"    to "Sacombank",
  "EIB"    to "Eximbank",
  "SHB"    to "SHBank",
  "NCB"    to "NCB",
  "MSB"    to "MSB",
  "BAB"    to "BacABank",
  "PGB"    to "PGBank",
  "CAKE"   to "CAKE",
  "TIMO"   to "Timo",
  "MOMO"   to "MoMo",
  "ZALOPAY" to "ZaloPay",
  "VNPAY"  to "VNPay"
)

// ─── Main Composable ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VietQrBottomSheet(
  creditorProfile: ProfileResponse?,
  debtorName: String,
  amount: Double,
  isCreditorMode: Boolean = false,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val context = LocalContext.current
  var showQr by remember { mutableStateOf(isCreditorMode) } // Mặc định hiện QR nếu là chủ nợ
  var copiedField by remember { mutableStateOf<String?>(null) }
  var showPayerBankSelection by remember { mutableStateOf(false) }

  val BIN_MAP = mapOf(
    "ICB" to "970415",
    "VCB" to "970436",
    "BIDV" to "970418",
    "TCB" to "970407",
    "ACB" to "970416",
    "VPB" to "970432",
    "TPB" to "970423",
    "MB" to "970422",
    "VIB" to "970441",
    "HDB" to "970437",
    "MSB" to "970426",
    "STB" to "970403" // Sacombank
  )

  val amountLong = amount.toLong()
  val formattedAmount = NumberFormat.getNumberInstance(Locale("vi", "VN")).format(amountLong)
  val description = "SplitBill tra no $debtorName"

  // Auto-clear copied state
  LaunchedEffect(copiedField) {
    if (copiedField != null) {
      kotlinx.coroutines.delay(2000)
      copiedField = null
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = Dimens.SpacingL)
        .padding(bottom = Dimens.SpacingXL),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {

      // ── Header ──────────────────────────────────────────────────────────
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(44.dp)
            .background(
              Brush.linearGradient(
                listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
              ),
              shape = MaterialTheme.shapes.medium
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            Icons.Default.AccountBalance,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(22.dp)
          )
        }
        Spacer(Modifier.width(Dimens.SpacingM))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            "Thanh toán VietQR",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
          )
          Text(
            "Chuyển khoản nhanh, an toàn",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "Đóng")
        }
      }

      Spacer(Modifier.height(Dimens.SpacingL))

      if (creditorProfile?.bankCode == null || creditorProfile.accountNumber == null) {
        // ── Chưa thiết lập ngân hàng ────────────────────────────────────
        NoBankInfoCard(creditorName = creditorProfile?.username ?: "Người nhận")
      } else {
        // Tự động sửa lỗi mã CTG cũ thành ICB (mã chuẩn VietQR cho VietinBank)
        val rawBankCode = creditorProfile.bankCode
        val bankCode = if (rawBankCode.uppercase() == "CTG") "ICB" else rawBankCode
        
        val accountNumber = creditorProfile.accountNumber
        val accountName = creditorProfile.accountName ?: creditorProfile.username
        val bankName = getBankDisplayName(bankCode)
        val deepLink = buildVietQrDeepLink(bankCode, accountNumber, amountLong, description)
        val qrImageUrl = buildVietQrImageUrl(bankCode, accountNumber, accountName, amountLong, description)

        // ── Số tiền nổi bật ─────────────────────────────────────────────
        AmountCard(formattedAmount = formattedAmount)

        Spacer(Modifier.height(Dimens.SpacingM))

        if (!isCreditorMode) {
          // ── [1] Thanh toán nhanh ──────────────────────────────────────────
          Card(
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
          ) {
            Column(modifier = Modifier.padding(Dimens.SpacingM)) {
              Text(
                "Thanh toán nhanh",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
              Spacer(Modifier.height(Dimens.SpacingS))

              // Bước 1
              Row(verticalAlignment = Alignment.Top) {
                Box(
                  modifier = Modifier
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Text("1", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimary)
                }
                Spacer(Modifier.width(Dimens.SpacingS))
                Text(
                  "Bấm nút bên dưới → App ngân hàng mở lên & mã QR tự lưu vào máy",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onPrimaryContainer
                )
              }
              Spacer(Modifier.height(6.dp))

              // Bước 2
              Row(verticalAlignment = Alignment.Top) {
                Box(
                  modifier = Modifier
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Text("2", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimary)
                }
                Spacer(Modifier.width(Dimens.SpacingS))
                Text(
                  "Trong app ngân hàng → chọn Quét QR (thường ở trang chủ)",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onPrimaryContainer
                )
              }
              Spacer(Modifier.height(6.dp))

              // Bước 3
              Row(verticalAlignment = Alignment.Top) {
                Box(
                  modifier = Modifier
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Text("3", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimary)
                }
                Spacer(Modifier.width(Dimens.SpacingS))
                Text(
                  "Bấm biểu tượng 🖼️ Thư viện → chọn ảnh QR vừa lưu → Thông tin tự điền!",
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.primary
                )
              }
            }
          }

          Spacer(Modifier.height(Dimens.SpacingM))

          // Nút mở app ngân hàng
          Button(
            onClick = { showPayerBankSelection = true },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary
            )
          ) {
            Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(Dimens.SpacingS))
            Text("Chọn ngân hàng & thanh toán", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
          }

          if (showPayerBankSelection) {
            com.example.splitbill.ui.profile.BankSelectionBottomSheet(
              onDismiss = { showPayerBankSelection = false },
              onBankSelected = { payerAppId ->
                // 1. Lưu QR vào thư viện
                saveImageToGallery(context, qrImageUrl, showToast = false)

                // 2. Copy số tài khoản vào clipboard
                val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Account Number", accountNumber)
                clipboardManager.setPrimaryClip(clip)

                // Package names của các ngân hàng tại Việt Nam
                val packageNames = mapOf(
                  "vcb" to "com.VCB",
                  "tcb" to "vn.com.techcombank.bb.app",
                  "mb" to "com.mbmobile",
                  "bidv" to "com.bidv.smartbanking",
                  "icb" to "com.vietinbank.ipay",
                  "vpb" to "com.vpbank.neo",
                  "acb" to "com.acb.mb.online",
                  "tpb" to "com.tpb.mb.gprs",
                  "msb" to "com.msb.digital",
                  "stb" to "com.sacombank.mbanking",
                  "vib" to "com.vib.vibmobile",
                  "vib-2" to "com.vib.vibmobile",
                  "hdb" to "com.hdb.mobile",
                  "ocb" to "com.ocb.mobilebanking",
                  "shb" to "com.shb.smartbanking",
                  "lpb" to "com.lpb.mobilebanking",
                  "cake" to "com.vpbank.cake",
                  "vba" to "com.vnpay.vba"
                )

                val appIdLower = payerAppId.lowercase()
                val packageName = packageNames[appIdLower]

                var launched = false
                if (packageName != null) {
                  try {
                    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                    if (intent != null) {
                      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                      context.startActivity(intent)
                      launched = true
                    }
                  } catch (e: Exception) { }
                }

                if (launched) {
                  Toast.makeText(
                    context,
                    "✅ Đã lưu QR & mở app. Chọn Quét QR → Thư viện ảnh → chọn ảnh QR!",
                    Toast.LENGTH_LONG
                  ).show()
                } else {
                  Toast.makeText(
                    context,
                    "Không tìm thấy app. Mã QR đã lưu vào Thư viện ảnh, hãy mở app ngân hàng thủ công.",
                    Toast.LENGTH_LONG
                  ).show()
                }
              }
            )
          }

          Spacer(Modifier.height(Dimens.SpacingM))

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

          Spacer(Modifier.height(Dimens.SpacingM))
        }

        // ── [2] QR Code toggle ──────────────────────────────────────────
        QrToggleSection(
          showQr = showQr,
          qrImageUrl = qrImageUrl,
          onToggle = { showQr = !showQr }
        )

        Spacer(Modifier.height(Dimens.SpacingM))

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Spacer(Modifier.height(Dimens.SpacingM))

        // ── [3] Thông tin copy ──────────────────────────────────────────
        Text(
          "Thông tin chuyển khoản",
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(Dimens.SpacingS))

        Card(
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(Dimens.SpacingM)) {
            CopyRow(
              label = "Ngân hàng",
              value = bankName,
              icon = Icons.Default.AccountBalance,
              copied = copiedField == "bank",
              onCopy = {
                copyToClipboard(context, "Ngân hàng", bankCode)
                copiedField = "bank"
              }
            )
            HorizontalDivider(
              modifier = Modifier.padding(vertical = Dimens.SpacingXS),
              color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
            CopyRow(
              label = "Số tài khoản",
              value = accountNumber,
              icon = Icons.Default.CreditCard,
              copied = copiedField == "account",
              onCopy = {
                copyToClipboard(context, "Số tài khoản", accountNumber)
                copiedField = "account"
              }
            )
            HorizontalDivider(
              modifier = Modifier.padding(vertical = Dimens.SpacingXS),
              color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
            CopyRow(
              label = "Chủ tài khoản",
              value = accountName,
              icon = Icons.Default.Person,
              copied = false,
              onCopy = null
            )
            HorizontalDivider(
              modifier = Modifier.padding(vertical = Dimens.SpacingXS),
              color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
            CopyRow(
              label = "Số tiền",
              value = "$formattedAmount đ",
              icon = Icons.Default.Payments,
              copied = copiedField == "amount",
              onCopy = {
                copyToClipboard(context, "Số tiền", amountLong.toString())
                copiedField = "amount"
              }
            )
            HorizontalDivider(
              modifier = Modifier.padding(vertical = Dimens.SpacingXS),
              color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
            CopyRow(
              label = "Nội dung CK",
              value = description,
              icon = Icons.Default.Message,
              copied = copiedField == "desc",
              onCopy = {
                copyToClipboard(context, "Nội dung", description)
                copiedField = "desc"
              }
            )
          }
        }

        // Thông báo đã copy
        AnimatedVisibility(visible = copiedField != null) {
          Row(
            modifier = Modifier.padding(top = Dimens.SpacingS),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              Icons.Default.CheckCircle,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
              "Đã sao chép!",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary
            )
          }
        }
      }

      Spacer(Modifier.height(Dimens.SpacingM))
    }
  }
}

// ─── Sub-composables ────────────────────────────────────────────────────────

@Composable
private fun AmountCard(formattedAmount: String) {
  Card(
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.errorContainer
    ),
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.large
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = Dimens.SpacingM),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        "Số tiền cần chuyển",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
      )
      Text(
        "$formattedAmount đ",
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
        color = MaterialTheme.colorScheme.error
      )
    }
  }
}

@Composable
private fun QrToggleSection(
  showQr: Boolean,
  qrImageUrl: String,
  onToggle: () -> Unit
) {
  val context = LocalContext.current
  val arrowRotation by animateFloatAsState(targetValue = if (showQr) 180f else 0f, label = "arrow")

  OutlinedButton(
    onClick = onToggle,
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.medium,
    colors = ButtonDefaults.outlinedButtonColors(
      contentColor = MaterialTheme.colorScheme.primary
    )
  ) {
    Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
    Spacer(Modifier.width(Dimens.SpacingS))
    Text(
      if (showQr) "Ẩn mã QR" else "Hiện mã QR để quét",
      style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
      modifier = Modifier.weight(1f)
    )
    Icon(
      Icons.Default.KeyboardArrowDown,
      contentDescription = null,
      modifier = Modifier
        .size(20.dp)
        .rotate(arrowRotation)
    )
  }

  AnimatedVisibility(
    visible = showQr,
    enter = expandVertically() + fadeIn(),
    exit = shrinkVertically() + fadeOut()
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Spacer(Modifier.height(Dimens.SpacingM))
      Box(
        modifier = Modifier
          .size(260.dp)
          .clip(MaterialTheme.shapes.large)
          .background(Color.White)
          .border(
            width = 2.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
            shape = MaterialTheme.shapes.large
          ),
        contentAlignment = Alignment.Center
      ) {
        AsyncImage(
          model = qrImageUrl,
          contentDescription = "Mã VietQR để quét",
          contentScale = ContentScale.Fit,
          modifier = Modifier.size(240.dp)
        )
      }
      Spacer(Modifier.height(Dimens.SpacingS))
      Text(
        "Mở app ngân hàng → Quét QR → Chọn ảnh từ thư viện",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )
      Spacer(Modifier.height(Dimens.SpacingS))
      Button(
        onClick = { saveImageToGallery(context, qrImageUrl) },
        shape = MaterialTheme.shapes.medium
      ) {
        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(Dimens.SpacingXS))
        Text("Lưu mã QR về máy")
      }
    }
  }
}

private fun saveImageToGallery(context: Context, imageUrl: String, showToast: Boolean = true) {
  CoroutineScope(Dispatchers.IO).launch {
    try {
      val url = java.net.URL(imageUrl)
      val connection = url.openConnection() as java.net.HttpURLConnection
      connection.doInput = true
      connection.connect()
      val input = connection.inputStream
      val bitmap = android.graphics.BitmapFactory.decodeStream(input)
      
      val contentValues = android.content.ContentValues().apply {
        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "SplitBill_QR_${System.currentTimeMillis()}.png")
        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
        put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/SplitBill")
      }
      val resolver = context.contentResolver
      val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
      if (uri != null) {
        resolver.openOutputStream(uri)?.use { outputStream ->
          bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, outputStream)
        }
        if (showToast) {
          withContext(Dispatchers.Main) {
            Toast.makeText(context, "Đã lưu mã QR vào thư viện ảnh!", Toast.LENGTH_LONG).show()
          }
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
      if (showToast) {
        withContext(Dispatchers.Main) {
          Toast.makeText(context, "Lỗi khi lưu ảnh: ${e.message}", Toast.LENGTH_SHORT).show()
        }
      }
    }
  }
}

@Composable
private fun NoBankInfoCard(creditorName: String) {
  Card(
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.errorContainer
    ),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(Dimens.SpacingL).fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Icon(
        Icons.Default.AccountBalanceWallet,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.error,
        modifier = Modifier.size(48.dp)
      )
      Spacer(Modifier.height(Dimens.SpacingS))
      Text(
        "$creditorName chưa thiết lập thông tin ngân hàng",
        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onErrorContainer,
        textAlign = TextAlign.Center
      )
      Spacer(Modifier.height(Dimens.SpacingXS))
      Text(
        "Nhờ họ vào phần \"Thông tin thanh toán\" (biểu tượng ví) để thiết lập nhé!",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onErrorContainer,
        textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
private fun CopyRow(
  label: String,
  value: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  copied: Boolean,
  onCopy: (() -> Unit)?
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      icon,
      contentDescription = null,
      tint = if (copied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(18.dp)
    )
    Spacer(Modifier.width(Dimens.SpacingS))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Text(
        value,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        color = if (copied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
      )
    }
    if (onCopy != null) {
      IconButton(onClick = onCopy, modifier = Modifier.size(36.dp)) {
        Icon(
          if (copied) Icons.Default.CheckCircle else Icons.Default.ContentCopy,
          contentDescription = "Sao chép",
          tint = if (copied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(18.dp)
        )
      }
    }
  }
}

// ─── Helpers ────────────────────────────────────────────────────────────────

private fun copyToClipboard(context: Context, label: String, value: String) {
  val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
  cm.setPrimaryClip(ClipData.newPlainText(label, value))
  Toast.makeText(context, "Đã sao chép $label", Toast.LENGTH_SHORT).show()
}

object VietQrGenerator {
  fun generate(binCode: String, account: String, amount: String, info: String): String {
    val cleanInfo = removeAccents(info).take(50) // Chuẩn hóa không dấu và giới hạn độ dài
    
    val beneficiary = "0006${binCode}01${account.length.toString().padStart(2, '0')}$account"
    val merchantInfo = "0010A00000072701${beneficiary.length.toString().padStart(2, '0')}${beneficiary}0208QRIBFTTA"
    val additional = if (cleanInfo.isNotEmpty()) "08${cleanInfo.length.toString().padStart(2, '0')}$cleanInfo" else ""

    var payload = "00020101021238${merchantInfo.length.toString().padStart(2, '0')}$merchantInfo" +
      "5303704"
    if (amount.isNotEmpty()) {
      payload += "54${amount.length.toString().padStart(2, '0')}$amount"
    }
    payload += "5802VN"
    if (additional.isNotEmpty()) {
      payload += "62${additional.length.toString().padStart(2, '0')}$additional"
    }
    payload += "6304"
    return payload + crc16(payload)
  }

  private fun removeAccents(str: String): String {
    val normalized = java.text.Normalizer.normalize(str, java.text.Normalizer.Form.NFD)
    val regex = "\\p{InCombiningDiacriticalMarks}+".toRegex()
    var result = regex.replace(normalized, "")
    result = result.replace('đ', 'd').replace('Đ', 'D')
    return result.replace(Regex("[^a-zA-Z0-9 ]"), " ").trim()
  }

  private fun crc16(data: String): String {
    var crc = 0xFFFF
    for (i in 0 until data.length) {
      crc = crc xor (data[i].code shl 8)
      for (j in 0 until 8) {
        if ((crc and 0x8000) != 0) {
          crc = (crc shl 1) xor 0x1021
        } else {
          crc = (crc shl 1)
        }
        crc = crc and 0xFFFF
      }
    }
    return crc.toString(16).uppercase().padStart(4, '0')
  }
}
