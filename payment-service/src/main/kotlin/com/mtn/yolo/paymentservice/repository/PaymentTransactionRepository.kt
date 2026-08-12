package com.mtn.yolo.paymentservice.repository

import com.mtn.yolo.paymentservice.entity.PaymentTransaction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PaymentTransactionRepository : JpaRepository<PaymentTransaction, Long> {
    fun findByPhoneNumber(phoneNumber: String): List<PaymentTransaction>
    fun findByUssdSessionId(ussdSessionId: String): List<PaymentTransaction>
}
