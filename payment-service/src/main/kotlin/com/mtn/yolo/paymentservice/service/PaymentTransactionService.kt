package com.mtn.yolo.paymentservice.service

import com.mtn.yolo.paymentservice.dto.PaymentRequest
import com.mtn.yolo.paymentservice.entity.PaymentMethod
import com.mtn.yolo.paymentservice.entity.PaymentStatus
import com.mtn.yolo.paymentservice.entity.PaymentTransaction
import com.mtn.yolo.paymentservice.repository.PaymentTransactionRepository
import org.springframework.stereotype.Service

@Service
class PaymentTransactionService(private val repo: PaymentTransactionRepository) {

    // ---------- CREATE (initiates a payment attempt and records the outcome) ----------
    fun create(request: PaymentRequest): PaymentTransaction {
        val (status, message) = resolveOutcome(request.method)

        val transaction = PaymentTransaction(
            ussdSessionId = request.ussdSessionId,
            phoneNumber = request.phoneNumber,
            bundleId = request.bundleId,
            bundleLabel = request.bundleLabel,
            amountFrw = request.amountFrw,
            method = request.method,
            status = status,
            resultMessage = message
        )
        return repo.save(transaction)
    }

    /**
     * STUB — mirrors the fixed outcomes captured from the real *154# flow, since there's
     * no real balance-check / MoMo-integration / loan-status API to call yet.
     * TODO: replace with real checks — airtime balance lookup, actual MoMo API call,
     * and real Iherereze loan-status lookup — once available from MTN.
     */
    private fun resolveOutcome(method: PaymentMethod): Pair<PaymentStatus, String> = when (method) {
        PaymentMethod.AIRTIME -> PaymentStatus.INSUFFICIENT_FUNDS to
            "Your airtime is insufficient. Please reload or dial *151# to borrow airtime"
        PaymentMethod.MOMO -> PaymentStatus.PROCESSING to
            "Y'ello We are processing your request. You will get an approval notification shortly"
        PaymentMethod.IHEREREZE -> PaymentStatus.NOT_ALLOWED to
            "You are not allowed to borrow airtime at the moment. dial *151# to check loan balance"
    }

    // ---------- READ ----------
    fun findAll(): List<PaymentTransaction> = repo.findAll()

    fun findById(id: Long): PaymentTransaction =
        repo.findById(id).orElseThrow { NoSuchElementException("Payment transaction with id $id not found") }

    fun findByPhoneNumber(phoneNumber: String): List<PaymentTransaction> = repo.findByPhoneNumber(phoneNumber)

    fun findByUssdSessionId(ussdSessionId: String): List<PaymentTransaction> = repo.findByUssdSessionId(ussdSessionId)

    // ---------- UPDATE (e.g. to mark a MoMo transaction resolved once a real callback exists) ----------
    fun updateStatus(id: Long, newStatus: PaymentStatus, message: String?): PaymentTransaction {
        val existing = findById(id)
        val updated = existing.copy(status = newStatus, resultMessage = message ?: existing.resultMessage)
        return repo.save(updated)
    }

    // ---------- DELETE ----------
    fun delete(id: Long) {
        if (!repo.existsById(id)) {
            throw NoSuchElementException("Payment transaction with id $id not found")
        }
        repo.deleteById(id)
    }
}
