package com.splitbill.data

import com.splitbill.models.SimplifiedDebt
import java.util.PriorityQueue
import kotlin.math.abs
import kotlin.math.min

/**
 * Thuật toán Tối giản nợ (Debt Simplification)
 *
 * Bài toán: Trong nhóm có N thành viên, sau nhiều hóa đơn phát sinh,
 * mỗi người có thể vừa nợ vừa được nợ. Mục tiêu: tối thiểu hóa số
 * giao dịch chuyển tiền cần thực hiện để giải quyết hết nợ.
 *
 * Thuật toán Greedy bằng Max-Heap:
 * 1. Tính "số dư ròng" (net balance) cho mỗi người:
 *    balance = tổng tiền đã trả hộ - tổng tiền nợ người khác
 *    - balance > 0: Người này ĐƯỢC nợ (creditor - chủ nợ)
 *    - balance < 0: Người này ĐANG nợ (debtor - con nợ)
 *
 * 2. Dùng 2 Max-Heap (PriorityQueue):
 *    - Heap chủ nợ: sắp xếp giảm dần theo balance (ai được nợ nhiều nhất lên đầu)
 *    - Heap con nợ: sắp xếp giảm dần theo |balance| (ai nợ nhiều nhất lên đầu)
 *
 * 3. Lặp: Lấy chủ nợ lớn nhất & con nợ lớn nhất, ghép cặp thanh toán:
 *    - Số tiền giao dịch = min(số chủ nợ được nợ, số con nợ đang nợ)
 *    - Trừ balance cả hai bên. Nếu ai còn dư thì đẩy lại heap.
 *    - Tiếp tục cho đến khi cả hai heap rỗng.
 *
 * Kết quả: Số giao dịch tối thiểu (tối đa là N-1 giao dịch).
 */
object DebtSimplifier {

    data class BalanceEntry(
        val userId: String,
        val username: String,
        val balance: Double  // Dương = được nợ, Âm = đang nợ
    )

    /**
     * Tính toán danh sách giao dịch tối giản từ tất cả hóa đơn trong nhóm.
     *
     * @param bills      Tất cả hóa đơn trong nhóm
     * @param allSplits  Tất cả chi tiết chia nợ tương ứng
     * @param memberMap  Map<userId, username> cho tra cứu tên
     * @return           Danh sách các giao dịch tối giản
     */
    fun simplify(
        bills: List<Bill>,
        allSplits: List<BillSplit>,
        memberMap: Map<String, String>
    ): List<SimplifiedDebt> {

        // Bước 1: Tính net balance cho mỗi người
        val balanceMap = mutableMapOf<String, Double>()

        // Với mỗi hóa đơn, người trả tiền (paidByUserId) ĐƯỢC cộng totalAmount
        for (bill in bills) {
            balanceMap[bill.paidByUserId] =
                (balanceMap[bill.paidByUserId] ?: 0.0) + bill.totalAmount.toDouble()
        }

        // Với mỗi split, người nợ (userId) BỊ trừ amountOwed
        for (split in allSplits) {
            balanceMap[split.userId] =
                (balanceMap[split.userId] ?: 0.0) - split.amountOwed.toDouble()
        }

        // Bước 2: Phân chia thành chủ nợ và con nợ
        // Max-Heap cho chủ nợ (balance dương, ai nhiều nhất lên đầu)
        val creditors = PriorityQueue<BalanceEntry>(compareByDescending { it.balance })
        // Max-Heap cho con nợ (balance âm, ai nợ nhiều nhất lên đầu, so sánh theo |balance|)
        val debtors = PriorityQueue<BalanceEntry>(compareByDescending { abs(it.balance) })

        for ((userId, balance) in balanceMap) {
            // Bỏ qua số dư quá nhỏ (lỗi làm tròn)
            if (abs(balance) < 0.01) continue

            val username = memberMap[userId] ?: "Unknown"
            if (balance > 0) {
                creditors.add(BalanceEntry(userId, username, balance))
            } else {
                debtors.add(BalanceEntry(userId, username, balance))
            }
        }

        // Bước 3: Ghép cặp Greedy để tạo giao dịch tối giản
        val result = mutableListOf<SimplifiedDebt>()

        while (creditors.isNotEmpty() && debtors.isNotEmpty()) {
            val creditor = creditors.poll()
            val debtor = debtors.poll()

            val amount = min(creditor.balance, abs(debtor.balance))

            // Tạo giao dịch: con nợ trả tiền cho chủ nợ
            result.add(
                SimplifiedDebt(
                    fromUserId = debtor.userId,
                    fromUsername = debtor.username,
                    toUserId = creditor.userId,
                    toUsername = creditor.username,
                    amount = Math.round(amount * 100.0) / 100.0 // Làm tròn 2 chữ số
                )
            )

            // Tính lại balance sau giao dịch
            val newCreditorBalance = creditor.balance - amount
            val newDebtorBalance = debtor.balance + amount

            // Nếu chủ nợ vẫn còn được nợ, đẩy lại heap
            if (newCreditorBalance > 0.01) {
                creditors.add(BalanceEntry(creditor.userId, creditor.username, newCreditorBalance))
            }

            // Nếu con nợ vẫn còn nợ, đẩy lại heap
            if (abs(newDebtorBalance) > 0.01) {
                debtors.add(BalanceEntry(debtor.userId, debtor.username, newDebtorBalance))
            }
        }

        return result
    }
}
