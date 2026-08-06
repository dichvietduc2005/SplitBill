package com.example.splitbill.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Danh mục hóa đơn — 16 danh mục phổ biến với icon Material + màu HSL curated.
 * Backend chỉ lưu key (String), toàn bộ metadata hiển thị nằm trên client.
 */
enum class BillCategory(
  val key: String,
  val displayName: String,
  val icon: ImageVector,
  val bgColor: Color,
  val iconColor: Color
) {
  FOOD(
    key = "FOOD",
    displayName = "Ăn uống",
    icon = Icons.Default.Restaurant,
    bgColor = Color(0xFFFFF3E0),
    iconColor = Color(0xFFE65100)
  ),
  TRANSPORT(
    key = "TRANSPORT",
    displayName = "Di chuyển",
    icon = Icons.Default.DirectionsBus,
    bgColor = Color(0xFFE3F2FD),
    iconColor = Color(0xFF0D47A1)
  ),
  SHOPPING(
    key = "SHOPPING",
    displayName = "Mua sắm",
    icon = Icons.Default.ShoppingCart,
    bgColor = Color(0xFFFCE4EC),
    iconColor = Color(0xFFAD1457)
  ),
  BILLS(
    key = "BILLS",
    displayName = "Hóa đơn",
    icon = Icons.Default.Receipt,
    bgColor = Color(0xFFE8F5E9),
    iconColor = Color(0xFF2E7D32)
  ),
  ENTERTAINMENT(
    key = "ENTERTAINMENT",
    displayName = "Giải trí",
    icon = Icons.Default.Movie,
    bgColor = Color(0xFFF3E5F5),
    iconColor = Color(0xFF6A1B9A)
  ),
  HEALTH(
    key = "HEALTH",
    displayName = "Sức khỏe",
    icon = Icons.Default.LocalHospital,
    bgColor = Color(0xFFFFEBEE),
    iconColor = Color(0xFFC62828)
  ),
  EDUCATION(
    key = "EDUCATION",
    displayName = "Học tập",
    icon = Icons.Default.School,
    bgColor = Color(0xFFE0F7FA),
    iconColor = Color(0xFF00695C)
  ),
  COFFEE(
    key = "COFFEE",
    displayName = "Cà phê/Trà",
    icon = Icons.Default.LocalCafe,
    bgColor = Color(0xFFEFEBE9),
    iconColor = Color(0xFF4E342E)
  ),
  BEAUTY(
    key = "BEAUTY",
    displayName = "Làm đẹp",
    icon = Icons.Default.Spa,
    bgColor = Color(0xFFF8BBD0),
    iconColor = Color(0xFFC2185B)
  ),
  PET(
    key = "PET",
    displayName = "Thú cưng",
    icon = Icons.Default.Pets,
    bgColor = Color(0xFFFFF8E1),
    iconColor = Color(0xFFF57F17)
  ),
  SPORTS(
    key = "SPORTS",
    displayName = "Thể thao",
    icon = Icons.Default.FitnessCenter,
    bgColor = Color(0xFFE8EAF6),
    iconColor = Color(0xFF283593)
  ),
  FAMILY(
    key = "FAMILY",
    displayName = "Gia đình",
    icon = Icons.Default.FamilyRestroom,
    bgColor = Color(0xFFDCEDC8),
    iconColor = Color(0xFF33691E)
  ),
  TRAVEL(
    key = "TRAVEL",
    displayName = "Du lịch",
    icon = Icons.Default.Flight,
    bgColor = Color(0xFFE1F5FE),
    iconColor = Color(0xFF01579B)
  ),
  CLOTHING(
    key = "CLOTHING",
    displayName = "Quần áo",
    icon = Icons.Default.Checkroom,
    bgColor = Color(0xFFEDE7F6),
    iconColor = Color(0xFF4527A0)
  ),
  HOME(
    key = "HOME",
    displayName = "Nhà cửa",
    icon = Icons.Default.Home,
    bgColor = Color(0xFFE0F2F1),
    iconColor = Color(0xFF004D40)
  ),
  GENERAL(
    key = "GENERAL",
    displayName = "Khác",
    icon = Icons.Default.Category,
    bgColor = Color(0xFFECEFF1),
    iconColor = Color(0xFF455A64)
  );

  companion object {
    /** Tra danh mục từ key (trả về GENERAL nếu key không khớp) */
    fun fromKey(key: String): BillCategory =
      entries.find { it.key == key } ?: GENERAL

    /** 4 danh mục phổ biến nhất hiện ở hàng chọn nhanh */
    val quickPick = listOf(FOOD, TRANSPORT, SHOPPING, COFFEE)
  }
}
