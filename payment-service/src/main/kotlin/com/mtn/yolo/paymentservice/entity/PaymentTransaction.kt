package com.mtn.yolo.paymentservice.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "payment_transactions")
data class PaymentTransaction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val ussdSessionId: String,

    @Column(nullable = false)
    val phoneNumber: String,

    @Column(nullable = true)
    val bundleId: Long? = null,

    @Column(nullable = false)
    val bundleLabel: String,

    @Column(nullable = false)
    val amountFrw: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val method: PaymentMethod,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: PaymentStatus,

    @Column(nullable = true, length = 500)
    val resultMessage: String? = null,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)
