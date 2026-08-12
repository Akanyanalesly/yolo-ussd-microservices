package com.mtn.yolo.paymentservice.dto

import com.mtn.yolo.paymentservice.entity.PaymentMethod
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class PaymentRequest(
    @field:NotBlank
    val ussdSessionId: String,

    @field:NotBlank
    val phoneNumber: String,

    val bundleId: Long? = null,

    @field:NotBlank
    val bundleLabel: String,

    @field:Min(0)
    val amountFrw: Int,

    val method: PaymentMethod
)
