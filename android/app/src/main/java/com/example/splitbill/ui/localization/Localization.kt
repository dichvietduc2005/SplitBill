package com.example.splitbill.ui.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

enum class AppLanguage(val code: String, val displayName: String, val flag: String) {
  VIETNAMESE("vi", "Tiếng Việt", "🇻🇳"),
  ENGLISH("en", "English", "🇺🇸")
}

class Translator(private val language: AppLanguage) {
  fun translate(text: String): String {
    return when (language) {
      AppLanguage.ENGLISH -> englishTranslations[text] ?: text
      AppLanguage.VIETNAMESE -> text // default is Vietnamese
    }
  }

  companion object {
    val englishTranslations = mapOf(
      "Nhóm của tôi" to "My Groups",
      "Tạo Nhóm" to "Create Group",
      "Chưa có nhóm nào" to "No groups yet",
      "Hãy tạo nhóm mới để bắt đầu chia tiền nhé!" to "Create a new group to start splitting bills!",
      "Tạo nhóm mới" to "Create New Group",
      "Đặt tên cho nhóm của bạn:" to "Enter a name for your group:",
      "Tên nhóm (VD: Du lịch Đà Lạt)" to "Group name (e.g., Trip to Da Lat)",
      "Hủy" to "Cancel",
      "thành viên" to "members",
      "SplitBill" to "SplitBill",
      "Chia tiền dễ dàng, tình cảm bền lâu" to "Easy splitting, lasting friendships",
      "Tên đăng nhập" to "Username",
      "Mật khẩu" to "Password",
      "Đăng Nhập" to "Log In",
      "Đăng Ký" to "Register",
      "Chưa có tài khoản? Đăng ký ngay" to "No account? Sign up now",
      "Đã có tài khoản? Đăng nhập" to "Have an account? Log in",
      "Cài đặt" to "Settings",
      "Cỡ chữ" to "Font Size",
      "Ngôn ngữ" to "Language",
      "Giao diện" to "Theme",
      "Sáng" to "Light",
      "Tối" to "Dark",
      "Hệ thống" to "System",
      "Đăng xuất" to "Log Out",
      "Thông tin ngân hàng" to "Payment Account",
      "Thiết lập VietQR" to "VietQR Settings",
      "Bạn bè sẽ quét mã QR này khi muốn chuyển khoản trả nợ cho bạn." to "Friends will scan this QR to pay you back.",
      "Chọn Ngân Hàng" to "Select Bank",
      "Số tài khoản" to "Account Number",
      "Tên chủ tài khoản" to "Account Holder Name",
      "Nhập số tài khoản" to "Enter account number",
      "Lưu thông tin" to "Save Info",
      "Đã lưu thông tin ngân hàng thành công!" to "Bank info saved successfully!",
      "Tổng kết nợ" to "Debt Summary",
      "Tổng số giao dịch cần thực hiện:" to "Total transactions to make:",
      "giao dịch" to "transactions",
      "Thiết lập VietQR để bạn bè quét mã trả tiền bạn →" to "Setup VietQR so friends can scan and pay you →",
      "Tất cả đã huề!" to "All settled up!",
      "Không có ai nợ ai cả. Nhóm đã chia tiền rất công bằng!" to "No active debts. Everyone is fully settled!",
      "Danh sách cần thanh toán" to "Payment List",
      "trả cho" to "pays to",
      "Mã nhận tiền VietQR" to "Receive money QR",
      "Thanh toán VietQR" to "Pay via VietQR",
      "Làm mới" to "Refresh",
      "Xác nhận đăng xuất" to "Confirm Log Out",
      "Bạn có chắc chắn muốn đăng xuất?" to "Are you sure you want to log out?",
      "Cỡ chữ: Rất nhỏ" to "Font Size: Extra Small",
      "Cỡ chữ: Nhỏ" to "Font Size: Small",
      "Cỡ chữ: Bình thường" to "Font Size: Normal",
      "Cỡ chữ: Lớn" to "Font Size: Large",
      "Cỡ chữ: Rất lớn" to "Font Size: Extra Large",
      "Vui lòng nhập đầy đủ thông tin" to "Please enter all required information",
      "Đăng nhập thất bại" to "Login failed",
      "Đăng ký thất bại" to "Registration failed",
      "Tìm kiếm ngân hàng..." to "Search banks...",
      "Chọn ngân hàng" to "Select bank",
      "Tên chủ tài khoản" to "Account holder name",
      "Lưu thông tin" to "Save information",
      "Mã QR nhận tiền" to "Receive QR Code",
      "Chuyển khoản nợ" to "Transfer Debt",
      "Thành viên" to "Members",
      "Chi tiết nhóm" to "Group Details",
      "Hóa đơn" to "Bills",
      "Thêm Hóa Đơn" to "Add Bill",
      "Người trả tiền" to "Paid by",
      "Chia đều cho" to "Split equally with",
      "Mô tả hóa đơn" to "Bill description",
      "Số tiền" to "Amount",
      "Nhập số tiền" to "Enter amount",
      "Nhập mô tả (VD: Tiền ăn tối)" to "Enter description (e.g., Dinner)",
      "Thêm hóa đơn thành công!" to "Bill added successfully!",
      "Tạo hóa đơn" to "Create Bill",
      "Xem chi tiết nợ" to "View Debt Details",
      "Thêm hóa đơn" to "Add Bill",
      "Đang tải thông tin..." to "Loading details...",
      "Tài khoản thanh toán" to "Payment Account",
      "Cài đặt chung" to "General Settings",
      "Tùy chỉnh khác" to "Custom Settings",
      "Cấu hình hóa đơn mặc định" to "Default bill settings",
      "Thông báo đẩy" to "Push notifications",
      "Bật thông báo khi có hóa đơn mới" to "Notify when new bills are added",
      "Bảo mật sinh trắc học" to "Biometric security",
      "Sử dụng vân tay/khuôn mặt" to "Use fingerprint/face ID"
    )
  }
}

val LocalAppLanguage = compositionLocalOf { AppLanguage.VIETNAMESE }

@Composable
fun String.localized(): String {
  val lang = LocalAppLanguage.current
  return Translator(lang).translate(this)
}
