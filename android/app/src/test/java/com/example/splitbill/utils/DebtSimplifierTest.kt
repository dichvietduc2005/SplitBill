package com.example.splitbill.utils

import com.example.splitbill.data.api.BillResponse
import com.example.splitbill.data.api.BillSplitResponse
import com.example.splitbill.data.api.SettlementResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DebtSimplifierTest {

  private val memberMap = mapOf(
    "user_a" to "Alice",
    "user_b" to "Bob",
    "user_c" to "Charlie"
  )

  @Test
  fun testEmptyBillsAndSettlements_returnsEmpty() {
    val debts = DebtSimplifier.simplify(
      bills = emptyList(),
      settlements = emptyList(),
      memberMap = memberMap
    )
    assertTrue(debts.isEmpty())
  }

  @Test
  fun testSingleBillEqualSplitThreeUsers() {
    // Alice pays 300k, split equally (100k each) between Alice, Bob, Charlie
    val bill = BillResponse(
      id = "bill-1",
      groupId = "group-1",
      description = "Dinner",
      totalAmount = 300000.0,
      paidByUserId = "user_a",
      paidByUsername = "Alice",
      currency = "VND",
      exchangeRate = 1.0,
      splits = listOf(
        BillSplitResponse(userId = "user_a", username = "Alice", amountOwed = 100000.0),
        BillSplitResponse(userId = "user_b", username = "Bob", amountOwed = 100000.0),
        BillSplitResponse(userId = "user_c", username = "Charlie", amountOwed = 100000.0)
      ),
      createdAt = "2026-10-01T00:00:00Z"
    )

    val debts = DebtSimplifier.simplify(
      bills = listOf(bill),
      settlements = emptyList(),
      memberMap = memberMap
    )

    assertEquals(2, debts.size)
    // Both Bob and Charlie should owe Alice 100,000 VND
    for (debt in debts) {
      assertEquals("user_a", debt.toUserId)
      assertEquals("Alice", debt.toUsername)
      assertEquals(100000.0, debt.amount, 0.01)
      assertTrue(debt.fromUserId == "user_b" || debt.fromUserId == "user_c")
    }
  }

  @Test
  fun testCircularDebtCancellation_reducesToZero() {
    // Alice pays 100k for Bob
    val bill1 = BillResponse(
      id = "bill-1",
      groupId = "group-1",
      description = "A for B",
      totalAmount = 100000.0,
      paidByUserId = "user_a",
      paidByUsername = "Alice",
      currency = "VND",
      exchangeRate = 1.0,
      splits = listOf(BillSplitResponse(userId = "user_b", username = "Bob", amountOwed = 100000.0)),
      createdAt = "2026-10-01T00:00:00Z"
    )

    // Bob pays 100k for Charlie
    val bill2 = BillResponse(
      id = "bill-2",
      groupId = "group-1",
      description = "B for C",
      totalAmount = 100000.0,
      paidByUserId = "user_b",
      paidByUsername = "Bob",
      currency = "VND",
      exchangeRate = 1.0,
      splits = listOf(BillSplitResponse(userId = "user_c", username = "Charlie", amountOwed = 100000.0)),
      createdAt = "2026-10-01T00:00:00Z"
    )

    // Charlie pays 100k for Alice
    val bill3 = BillResponse(
      id = "bill-3",
      groupId = "group-1",
      description = "C for A",
      totalAmount = 100000.0,
      paidByUserId = "user_c",
      paidByUsername = "Charlie",
      currency = "VND",
      exchangeRate = 1.0,
      splits = listOf(BillSplitResponse(userId = "user_a", username = "Alice", amountOwed = 100000.0)),
      createdAt = "2026-10-01T00:00:00Z"
    )

    val debts = DebtSimplifier.simplify(
      bills = listOf(bill1, bill2, bill3),
      settlements = emptyList(),
      memberMap = memberMap
    )

    // Circular loop cancelled out completely -> 0 transactions needed
    assertTrue("Debts should be empty after circular cancellation", debts.isEmpty())
  }

  @Test
  fun testSettlementReducesDebt() {
    // Alice pays 200k for Bob
    val bill = BillResponse(
      id = "bill-1",
      groupId = "group-1",
      description = "A for B",
      totalAmount = 200000.0,
      paidByUserId = "user_a",
      paidByUsername = "Alice",
      currency = "VND",
      exchangeRate = 1.0,
      splits = listOf(BillSplitResponse(userId = "user_b", username = "Bob", amountOwed = 200000.0)),
      createdAt = "2026-10-01T00:00:00Z"
    )

    // Bob settles 150k to Alice
    val settlement = SettlementResponse(
      id = "set-1",
      groupId = "group-1",
      fromUserId = "user_b",
      fromUsername = "Bob",
      toUserId = "user_a",
      toUsername = "Alice",
      amount = 150000.0,
      note = "Transfer via VietQR",
      createdAt = "2026-10-02T00:00:00Z"
    )

    val debts = DebtSimplifier.simplify(
      bills = listOf(bill),
      settlements = listOf(settlement),
      memberMap = memberMap
    )

    assertEquals(1, debts.size)
    val debt = debts.first()
    assertEquals("user_b", debt.fromUserId)
    assertEquals("user_a", debt.toUserId)
    assertEquals(50000.0, debt.amount, 0.01)
  }

  @Test
  fun testForeignCurrencyConvertedWithExchangeRate() {
    // Alice pays 10 USD with exchangeRate 25,000 VND/USD for Bob
    val bill = BillResponse(
      id = "bill-usd",
      groupId = "group-1",
      description = "Lunch in USD",
      totalAmount = 10.0,
      paidByUserId = "user_a",
      paidByUsername = "Alice",
      currency = "USD",
      exchangeRate = 25000.0,
      splits = listOf(BillSplitResponse(userId = "user_b", username = "Bob", amountOwed = 10.0)),
      createdAt = "2026-10-01T00:00:00Z"
    )

    val debts = DebtSimplifier.simplify(
      bills = listOf(bill),
      settlements = emptyList(),
      memberMap = memberMap
    )

    assertEquals(1, debts.size)
    val debt = debts.first()
    assertEquals("user_b", debt.fromUserId)
    assertEquals("user_a", debt.toUserId)
    assertEquals(250000.0, debt.amount, 0.01)
  }

  @Test
  fun testTransitiveGreedySimplification() {
    // Bob owes Alice 50k
    val bill1 = BillResponse(
      id = "bill-1",
      groupId = "group-1",
      description = "Alice pays for Bob",
      totalAmount = 50000.0,
      paidByUserId = "user_a",
      paidByUsername = "Alice",
      currency = "VND",
      exchangeRate = 1.0,
      splits = listOf(BillSplitResponse(userId = "user_b", username = "Bob", amountOwed = 50000.0)),
      createdAt = "2026-10-01T00:00:00Z"
    )

    // Charlie owes Bob 50k
    val bill2 = BillResponse(
      id = "bill-2",
      groupId = "group-1",
      description = "Bob pays for Charlie",
      totalAmount = 50000.0,
      paidByUserId = "user_b",
      paidByUsername = "Bob",
      currency = "VND",
      exchangeRate = 1.0,
      splits = listOf(BillSplitResponse(userId = "user_c", username = "Charlie", amountOwed = 50000.0)),
      createdAt = "2026-10-01T00:00:00Z"
    )

    val debts = DebtSimplifier.simplify(
      bills = listOf(bill1, bill2),
      settlements = emptyList(),
      memberMap = memberMap
    )

    // Bob has net balance 0. Charlie pays Alice directly!
    assertEquals(1, debts.size)
    val debt = debts.first()
    assertEquals("user_c", debt.fromUserId)
    assertEquals("Charlie", debt.fromUsername)
    assertEquals("user_a", debt.toUserId)
    assertEquals("Alice", debt.toUsername)
    assertEquals(50000.0, debt.amount, 0.01)
  }
}
