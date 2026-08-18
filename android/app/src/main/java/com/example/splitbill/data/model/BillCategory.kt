package com.example.splitbill.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Danh mục hóa đơn — 16 danh mục phổ biến với icon Rounded bo tròn hiện đại + màu HSL curated rực rỡ.
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
    icon = Icons.Rounded.Restaurant,
    bgColor = Color(0xFFFFF3E0),
    iconColor = Color(0xFFFF6D00)
  ),
  TRANSPORT(
    key = "TRANSPORT",
    displayName = "Di chuyển",
    icon = Icons.Rounded.DirectionsBus,
    bgColor = Color(0xFFE3F2FD),
    iconColor = Color(0xFF2979FF)
  ),
  SHOPPING(
    key = "SHOPPING",
    displayName = "Mua sắm",
    icon = Icons.Rounded.ShoppingCart,
    bgColor = Color(0xFFFCE4EC),
    iconColor = Color(0xFFF50057)
  ),
  BILLS(
    key = "BILLS",
    displayName = "Hóa đơn",
    icon = Icons.Rounded.Receipt,
    bgColor = Color(0xFFE8F5E9),
    iconColor = Color(0xFF4CAF50)
  ),
  ENTERTAINMENT(
    key = "ENTERTAINMENT",
    displayName = "Giải trí",
    icon = Icons.Rounded.Movie,
    bgColor = Color(0xFFF3E5F5),
    iconColor = Color(0xFFAA00FF)
  ),
  HEALTH(
    key = "HEALTH",
    displayName = "Sức khỏe",
    icon = Icons.Rounded.LocalHospital,
    bgColor = Color(0xFFFFEBEE),
    iconColor = Color(0xFFFF1744)
  ),
  EDUCATION(
    key = "EDUCATION",
    displayName = "Học tập",
    icon = Icons.Rounded.School,
    bgColor = Color(0xFFE0F7FA),
    iconColor = Color(0xFF00BFA5)
  ),
  COFFEE(
    key = "COFFEE",
    displayName = "Cà phê/Trà",
    icon = Icons.Rounded.LocalCafe,
    bgColor = Color(0xFFEFEBE9),
    iconColor = Color(0xFF8D6E63)
  ),
  BEAUTY(
    key = "BEAUTY",
    displayName = "Làm đẹp",
    icon = Icons.Rounded.Spa,
    bgColor = Color(0xFFF8BBD0),
    iconColor = Color(0xFFE91E63)
  ),
  PET(
    key = "PET",
    displayName = "Thú cưng",
    icon = Icons.Rounded.Pets,
    bgColor = Color(0xFFFFF8E1),
    iconColor = Color(0xFFFFAB00)
  ),
  SPORTS(
    key = "SPORTS",
    displayName = "Thể thao",
    icon = Icons.Rounded.FitnessCenter,
    bgColor = Color(0xFFE8EAF6),
    iconColor = Color(0xFF3D5AFE)
  ),
  FAMILY(
    key = "FAMILY",
    displayName = "Gia đình",
    icon = Icons.Rounded.FamilyRestroom,
    bgColor = Color(0xFFDCEDC8),
    iconColor = Color(0xFF689F38)
  ),
  TRAVEL(
    key = "TRAVEL",
    displayName = "Du lịch",
    icon = Icons.Rounded.Flight,
    bgColor = Color(0xFFE1F5FE),
    iconColor = Color(0xFF00B0FF)
  ),
  CLOTHING(
    key = "CLOTHING",
    displayName = "Quần áo",
    icon = Icons.Rounded.Checkroom,
    bgColor = Color(0xFFEDE7F6),
    iconColor = Color(0xFF651FFF)
  ),
  HOME(
    key = "HOME",
    displayName = "Nhà cửa",
    icon = Icons.Rounded.Home,
    bgColor = Color(0xFFE0F2F1),
    iconColor = Color(0xFF00BFA5)
  ),
  GENERAL(
    key = "GENERAL",
    displayName = "Khác",
    icon = Icons.Rounded.Category,
    bgColor = Color(0xFFECEFF1),
    iconColor = Color(0xFF78909C)
  );

  companion object {
    /** Tra danh mục từ key (trả về GENERAL nếu key không khớp) */
    fun fromKey(key: String): BillCategory =
      entries.find { it.key == key } ?: GENERAL

    /** 4 danh mục phổ biến nhất hiện ở hàng chọn nhanh */
    val quickPick = listOf(FOOD, TRANSPORT, SHOPPING, COFFEE)
  }
}
