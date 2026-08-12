package com.mtn.yolo.ussdgatewayservice.dto

data class PaymentRequestDto(
    val ussdSessionId: String,
    val phoneNumber: String,
    val bundleId: Long?,
    val bundleLabel: String,
    val amountFrw: Int,
    val method: String
)

data class PaymentResponseDto(
    val id: Long,
    val status: String,
    val resultMessage: String?
)
