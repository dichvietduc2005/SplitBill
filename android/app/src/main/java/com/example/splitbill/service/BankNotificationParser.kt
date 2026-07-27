package com.example.splitbill.service

import android.util.Log

object BankNotificationParser {
  data class ParsedTransfer(
    val amount: Long,
    val senderName: String?,
    val content: String?
  )

  fun parse(text: String): ParsedTransfer? {
    try {
      // MB Bank: "TK: 123456789 | GD: +500,000 VND luc 12:30. ND: NGUYEN VAN A tra tien com"
      // VCB: "Vietcombank TK 1234567890 +500,000 VND ND: NGUYEN VAN A ck"
      // Techcombank: "TK ...1234: +500,000 VND. ND: NGUYEN VAN A chuyen khoan"
      
      // Regex for amount: looks for "+100,000", "+ 100.000", "+100000" or just numbers followed by VND/đ/dong
      // Only capture positive (incoming) transactions. Typically incoming transactions have '+' sign or start with "Nhận" / "Có"
      val cleanText = text.replace(",", "").replace(".", "").replace("'", "")
      
      // Look for credit amounts: "+500000", "nhan 500000", "co 500000", "phat sinh co 500000"
      val amountRegex = Regex("""(?:\+|\bnhan\b|\bco\b|\bcong\b|\b\+?\s*)\s*(\d+)\s*(?:VND|đ|dong|vnd)""", RegexOption.IGNORE_CASE)
      val match = amountRegex.find(cleanText) ?: return null
      val amountString = match.groupValues[1]
      val amount = amountString.toLongOrNull() ?: return null
      
      // Extract sender/content using some patterns
      // Sender: "tu NGUYEN VAN A" or "ND: NGUYEN VAN A" or "tu: NGUYEN VAN A"
      val senderRegex = Regex("""(?:tu|from)\s+([A-Z\s]+?)(?:\.|,|\s+ND|\s+Noi|$)""", RegexOption.IGNORE_CASE)
      val senderName = senderRegex.find(text)?.groupValues?.get(1)?.trim()

      // Content: "ND: xxx" or "Noi dung: xxx"
      val contentRegex = Regex("""(?:ND|Noi dung|Content)[:\s]+(.+)""", RegexOption.IGNORE_CASE)
      val content = contentRegex.find(text)?.groupValues?.get(1)?.trim()

      return ParsedTransfer(amount, senderName, content)
    } catch (e: Exception) {
      Log.e("BankParser", "Error parsing notification: ${e.message}", e)
      return null
    }
  }
}
