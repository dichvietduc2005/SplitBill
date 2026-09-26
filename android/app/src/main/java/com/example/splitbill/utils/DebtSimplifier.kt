package com.example.splitbill.utils

import com.example.splitbill.data.api.BillResponse
import com.example.splitbill.data.api.SettlementResponse
import com.example.splitbill.data.api.SimplifiedDebt
import java.util.PriorityQueue
import kotlin.math.abs
import kotlin.math.min

/**
 * Thuật toán Tối giản nợ (Debt Simplification) chạy trực tiếp trên Client Android.
 * Sử dụng Greedy 2 Max-Heaps để tối ưu hoá số giao dịch thanh toán tối thiểu (tối đa N-1 giao dịch).
 */
object DebtSimplifier {

    data class BalanceEntry(
        val userId: String,
        val username: String,
        val balance: Double  // Dương = được nợ, Âm = đang nợ
    )

    fun simplify(
        bills: List<BillResponse>,
        settlements: List<SettlementResponse>,
        memberMap: Map<String, String>
    ): List<SimplifiedDebt> {
        val balanceMap = mutableMapOf<String, Double>()

        // 1. Tính net balance từ các hóa đơn
        for (bill in bills) {
            val rate = if (bill.exchangeRate > 0) bill.exchangeRate else 1.0
            val totalInVnd = bill.totalAmount * rate

            // Người trả tiền được cộng số tiền
            balanceMap[bill.paidByUserId] = (balanceMap[bill.paidByUserId] ?: 0.0) + totalInVnd

            // Những người trong splits bị trừ số tiền phải trả
            for (split in bill.splits) {
                val splitInVnd = split.amountOwed * rate
                balanceMap[split.userId] = (balanceMap[split.userId] ?: 0.0) - splitInVnd
            }
        }

        // 2. Tính net balance từ các thanh toán nợ (settlements)
        for (settlement in settlements) {
            val amount = settlement.amount
            // Người trả (fromUser) giảm nợ => balance tăng lên (dương hơn)
            balanceMap[settlement.fromUserId] = (balanceMap[settlement.fromUserId] ?: 0.0) + amount
            // Người nhận (toUser) giảm được nợ => balance giảm đi (âm hơn)
            balanceMap[settlement.toUserId] = (balanceMap[settlement.toUserId] ?: 0.0) - amount
        }

        // 3. Phân loại chủ nợ (creditors) và con nợ (debtors)
        val creditors = PriorityQueue<BalanceEntry>(compareByDescending { it.balance })
        val debtors = PriorityQueue<BalanceEntry>(compareByDescending { abs(it.balance) })

        for ((userId, balance) in balanceMap) {
            if (abs(balance) < 0.01) continue

            val username = memberMap[userId] ?: "Unknown"
            if (balance > 0) {
                creditors.add(BalanceEntry(userId, username, balance))
            } else {
                debtors.add(BalanceEntry(userId, username, balance))
            }
        }

        // 4. Ghép cặp Greedy để tạo giao dịch tối giản
        val result = mutableListOf<SimplifiedDebt>()

        while (creditors.isNotEmpty() && debtors.isNotEmpty()) {
            val creditor = creditors.poll() ?: break
            val debtor = debtors.poll() ?: break

            val amount = min(creditor.balance, abs(debtor.balance))

            result.add(
                SimplifiedDebt(
                    fromUserId = debtor.userId,
                    fromUsername = debtor.username,
                    toUserId = creditor.userId,
                    toUsername = creditor.username,
                    amount = Math.round(amount * 100.0) / 100.0
                )
            )

            val newCreditorBalance = creditor.balance - amount
            val newDebtorBalance = debtor.balance + amount

            if (newCreditorBalance > 0.01) {
                creditors.add(BalanceEntry(creditor.userId, creditor.username, newCreditorBalance))
            }

            if (abs(newDebtorBalance) > 0.01) {
                debtors.add(BalanceEntry(debtor.userId, debtor.username, newDebtorBalance))
            }
        }

        return result
    }
}
