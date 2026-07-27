package com.example.splitbill.utils

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.splitbill.data.api.BillResponse
import com.example.splitbill.data.api.GroupStatsResponse
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale

object ExportManager {

  private val currencyFormatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))

  fun generateCsv(
    context: Context,
    groupName: String,
    bills: List<BillResponse>
  ): File {
    val fileName = "SplitBill_${groupName.replace(" ", "_")}_${System.currentTimeMillis()}.csv"
    val file = File(context.cacheDir, fileName)

    file.printWriter(Charsets.UTF_8).use { out ->
      out.println("BÁO CÁO CHI TIÊU NHÓM: $groupName")
      out.println("Ngày xuất: ${java.time.LocalDate.now()}")
      out.println()
      out.println("Ngày,Mô tả,Tổng tiền (VND),Người trả,Trạng thái thanh toán")

      bills.forEach { bill ->
        val date = bill.createdAt.take(10)
        val desc = "\"${bill.description.replace("\"", "\"\"")}\""
        val amount = bill.totalAmount.toLong()
        val payer = "\"${bill.paidByUsername}\""
        val status = if (bill.isPaid) "Đã thanh toán" else "Chưa thanh toán"

        out.println("$date,$desc,$amount,$payer,$status")
      }
    }
    return file
  }

  fun generatePdf(
    context: Context,
    groupName: String,
    stats: GroupStatsResponse?,
    bills: List<BillResponse>
  ): File {
    val fileName = "SplitBill_${groupName.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
    val file = File(context.cacheDir, fileName)

    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 page
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas

    val paint = Paint().apply {
      isAntiAlias = true
      textSize = 14f
    }

    var y = 50f

    // Header Title
    paint.apply {
      color = Color.rgb(79, 70, 229)
      textSize = 22f
      isFakeBoldText = true
    }
    canvas.drawText("BÁO CÁO CHI TIÊU NHÓM: $groupName", 40f, y, paint)
    y += 30f

    paint.apply {
      color = Color.DKGRAY
      textSize = 11f
      isFakeBoldText = false
    }
    canvas.drawText("Xuất lúc: ${java.time.LocalDateTime.now().toString().take(19).replace("T", " ")}", 40f, y, paint)
    y += 30f

    // Summary Box
    if (stats != null) {
      paint.color = Color.LTGRAY
      paint.style = Paint.Style.STROKE
      paint.strokeWidth = 1f
      canvas.drawRect(40f, y, 555f, y + 60f, paint)

      paint.style = Paint.Style.FILL
      paint.color = Color.BLACK
      paint.textSize = 12f
      paint.isFakeBoldText = true

      val totalText = "Tổng chi tiêu nhóm: ${currencyFormatter.format(stats.totalSpent.toLong())} VND"
      canvas.drawText(totalText, 55f, y + 25f, paint)

      val mySpentText = "Tổng phần của bạn: ${currencyFormatter.format(stats.userOwed.toLong())} VND"
      canvas.drawText(mySpentText, 55f, y + 45f, paint)

      y += 80f
    }

    // Table Headers
    paint.apply {
      color = Color.rgb(243, 244, 246)
      style = Paint.Style.FILL
    }
    canvas.drawRect(40f, y, 555f, y + 25f, paint)

    paint.apply {
      color = Color.BLACK
      textSize = 11f
      isFakeBoldText = true
    }
    canvas.drawText("Ngày", 50f, y + 17f, paint)
    canvas.drawText("Mô tả", 130f, y + 17f, paint)
    canvas.drawText("Người trả", 330f, y + 17f, paint)
    canvas.drawText("Số tiền (VND)", 450f, y + 17f, paint)
    y += 30f

    // Table Content
    paint.isFakeBoldText = false
    paint.textSize = 10f

    bills.take(30).forEach { bill ->
      if (y > 780f) return@forEach // Single page limit safeguard

      val date = bill.createdAt.take(10)
      val desc = if (bill.description.length > 25) bill.description.take(22) + "..." else bill.description
      val payer = if (bill.paidByUsername.length > 15) bill.paidByUsername.take(12) + "..." else bill.paidByUsername
      val amountStr = currencyFormatter.format(bill.totalAmount.toLong())

      canvas.drawText(date, 50f, y, paint)
      canvas.drawText(desc, 130f, y, paint)
      canvas.drawText(payer, 330f, y, paint)
      canvas.drawText(amountStr, 450f, y, paint)

      y += 22f
    }

    pdfDocument.finishPage(page)
    FileOutputStream(file).use { out ->
      pdfDocument.writeTo(out)
    }
    pdfDocument.close()

    return file
  }

  fun shareFile(context: Context, file: File, mimeType: String) {
    val uri: Uri = FileProvider.getUriForFile(
      context,
      "${context.packageName}.fileprovider",
      file
    )

    val intent = Intent(Intent.ACTION_SEND).apply {
      type = mimeType
      putExtra(Intent.EXTRA_STREAM, uri)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Chia sẻ báo cáo"))
  }
}
