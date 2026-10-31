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
      "Sử dụng vân tay/khuôn mặt" to "Use fingerprint/face ID",
      "Cài Đặt" to "Settings",
      "Tùy chỉnh trải nghiệm của bạn" to "Customize your experience",
      "Tài khoản & Thiết bị" to "Account & Device",
      "Đồng bộ đám mây" to "Cloud sync",
      "Chế độ hiển thị" to "Display mode",
      "Chuẩn" to "Standard",
      "Nhỏ" to "Small",
      "Lớn" to "Large",
      "Xem trước: Bữa tối nhóm hôm qua chia đều 145.000đ cho mỗi thành viên." to
        "Preview: Last night's group dinner split evenly at 145,000₫ per member.",
      "Ngôn ngữ hiển thị" to "Display language",
      "Áp dụng toàn bộ ứng dụng" to "Applies to the entire app",
      "Thông báo" to "Notifications",
      "Nhắc nợ lịch sự & xác nhận thanh toán tức thời" to
        "Gentle reminders & instant payment confirmation",
      "Bảo mật và đối soát" to "Security & reconciliation",
      "Khóa bằng vân tay hoặc khuôn mặt" to "Lock with fingerprint or face",
      "Tự động nhận diện thanh toán" to "Automatic payment detection",
      "Khớp tiền hoàn từ biến động số dư ngân hàng" to
        "Match incoming transfers from your bank balance updates",
      "Cần quyền Notification Listener" to "Notification Listener permission required",
      "Bảo mật thiết bị Android" to "Android device security",
      "Thiết lập quyền" to "Grant permission",
      "Đã bật đối soát giao dịch tự động" to "Automatic reconciliation is on",
      "Ứng dụng đang lắng nghe thông báo hoàn tiền để tự động cập nhật khoản thu chung." to
        "The app listens for transfer notifications to update shared expenses automatically.",
      "Thanh toán & Chuyển khoản" to "Payments & Transfer",
      "Đã kết nối" to "Connected",
      "Chưa kết nối" to "Not connected",
      "Tạo mã QR kèm đúng số tiền nợ cho bạn bè quét trả" to
        "Create a QR with the exact amount owed for friends to scan",
      "Thông tin ứng dụng" to "App information",
      "Điều khoản sử dụng" to "Terms of use",
      "Chính sách quyền riêng tư" to "Privacy policy",
      "Phiên bản ứng dụng" to "App version",
      "Đóng" to "Close",
      "Đăng xuất tài khoản khỏi thiết bị này" to "Sign out of this device",
      "Tài chính minh bạch • Tình bạn bền lâu" to "Transparent finances • Lasting friendships",
      "Đăng xuất khỏi SplitBill?" to "Sign out of SplitBill?",
      "Dữ liệu sổ thu chi vẫn sẽ được lưu an toàn trên đám mây." to
        "Your ledger data stays safely backed up in the cloud.",
      "Chọn ngôn ngữ" to "Choose language",
      ("SplitBill cần quyền phát hiện thông báo ngân hàng khi bạn nhận tiền hoàn từ bạn bè. " +
        "Ứng dụng cam kết hoàn toàn không đọc tin nhắn SMS hay thông tin tài khoản riêng tư.") to
        ("SplitBill needs to detect bank notifications when you receive a refund from friends. " +
          "The app never reads your SMS messages or private account details."),
      ("SplitBill là công cụ hỗ trợ ghi lại và chia sẻ chi phí trong nhóm. Bạn chịu trách nhiệm về " +
        "tính chính xác của các khoản chi đã nhập cũng như các thỏa thuận thanh toán giữa các thành viên. " +
        "Vui lòng không sử dụng ứng dụng cho mục đích trái pháp luật.") to
        ("SplitBill helps you record and share group expenses. You are responsible for the " +
          "accuracy of the expenses you enter and for the payment agreements between members. " +
          "Please do not use the app for unlawful purposes."),
      ("SplitBill chỉ lưu những thông tin cần thiết để vận hành: tên, email, ảnh đại diện và thông tin " +
        "ngân hàng do bạn tự nhập. Ứng dụng không đọc tin nhắn SMS hay nội dung trò chuyện cá nhân. " +
        "Dữ liệu được lưu trữ an toàn trên đám mây và bạn có thể yêu cầu xóa bất cứ lúc nào.") to
        ("SplitBill stores only what it needs to run: your name, email, avatar and the bank details " +
          "you enter yourself. The app does not read SMS messages or private conversations. " +
          "Your data is stored securely in the cloud and you can request deletion at any time."),
      // Màn hình Cá nhân
      "Cá nhân" to "Profile",
      "Giúp bạn bè chuyển khoản hoàn trả dễ dàng hơn." to "Make it easy for friends to pay you back.",
      "Đã lưu" to "Saved",
      "Chưa lưu" to "Not saved",
      "Chủ tài khoản" to "Account holder",
      "Sao chép" to "Copy",
      "Đã chép!" to "Copied!",
      "Chỉnh sửa" to "Edit",
      "Thành viên từ" to "Member since",
      "Thiết lập ngay" to "Set up now",
      "Chưa thiết lập ngân hàng" to "No bank account yet",
      "Thêm tài khoản nhận để bạn bè chuyển khoản trả nợ." to
        "Add a receiving account so friends can transfer money back.",
      "Thanh toán bằng VietQR" to "Pay with VietQR",
      "Bạn bè có thể quét mã QR để chuyển khoản hoàn trả cho bạn." to
        "Friends can scan your QR code to transfer money back.",
      "Tự động gắn mã QR khi đòi nợ" to "Attach QR automatically when settling up",
      "Đang bật" to "On",
      "Chưa bật" to "Off",
      "Mỗi khi chia tiền trong nhóm, SplitBill sẽ tự động gắn thông tin VietQR ở trên để bạn bè thanh toán chỉ trong 1 chạm." to
        "Whenever you split a bill, SplitBill attaches your VietQR details above so friends can pay in one tap.",
      "Xem trước mã QR" to "Preview QR code",
      "Cài đặt kèm số tiền" to "Set an amount",
      "Cập nhật tài khoản nhận" to "Update receiving account",
      "Sử dụng để tạo VietQR nhận hoàn tiền tức thì" to
        "Used to create a VietQR for instant refunds",
      "Ngân hàng thụ hưởng" to "Beneficiary bank",
      "Nhập số tài khoản (giữ số 0 đầu)" to "Enter account number (keep leading zeros)",
      "Hỗ trợ giữ nguyên số 0 ở đầu tài khoản" to "Leading zeros are kept as entered",
      "Tên chủ tài khoản (In hoa không dấu)" to "Account holder (uppercase, no accents)",
      "Hủy" to "Cancel",
      "Lưu thông tin" to "Save details",
      "Mã VietQR cá nhân" to "Your VietQR code",
      "Sử dụng để bạn bè thanh toán chia tiền" to "Let friends pay you for shared bills",
      "Số tiền cần thu (đ)" to "Amount to collect (VND)",
      "Lưu mã QR về máy" to "Save QR to gallery",
      "Chưa có thông báo mới" to "No new notifications",
      "Thông báo về khoản chi mới, nhắc nợ và xác nhận thanh toán sẽ xuất hiện ở đây." to
        "Notifications about new expenses, reminders and payment confirmations will appear here.",
      // Màn hình Thống kê nhóm
      "Thống kê nhóm" to "Group stats",
      "Quay lại" to "Back",
      "Xuất báo cáo" to "Export report",
      "Tháng này" to "This month",
      "3 tháng gần đây" to "Last 3 months",
      "Tất cả thời gian" to "All time",
      "Tổng chi tiêu nhóm" to "Group spending",
      "Tăng %s%% so với kỳ trước" to "Up %s%% vs the previous period",
      "Tăng gấp %s lần so với kỳ trước" to "Up %sx vs the previous period",
      "Giảm %s%% so với kỳ trước" to "Down %s%% vs the previous period",
      "Không đổi so với kỳ trước" to "Unchanged vs the previous period",
      "Chưa đủ dữ liệu để so sánh" to "Not enough data to compare yet",
      "Bạn đã chi trả" to "You paid",
      "Phần của bạn" to "Your share",
      "Xu hướng chi tiêu" to "Spending trend",
      "6 tháng gần nhất" to "Last 6 months",
      "Chi tiêu theo danh mục" to "Spending by category",
      "Chưa có khoản chi nào trong kỳ này" to "No expenses in this period yet",
      "Tỷ lệ đóng góp chi tiêu" to "Contribution share",
      "Khoản chi gần đây" to "Recent expenses",
      "Phần bạn chi trả trong nhóm" to "What you paid inside the group",
      "Bạn đã trả" to "You paid",
      "Xuất báo cáo chi tiêu" to "Export expense report",
      "Không thể tải thống kê" to "Could not load the stats",
      "Thử lại" to "Try again"
    )
  }
}

val LocalAppLanguage = compositionLocalOf { AppLanguage.VIETNAMESE }

@Composable
fun String.localized(): String {
  val lang = LocalAppLanguage.current
  return Translator(lang).translate(this)
}
