package com.mtn.yolo.paymentservice.entity

enum class PaymentStatus {
    PENDING,
    PROCESSING,      // MoMo: fired off, real outcome arrives async via MTN's own flow + SMS
    SUCCESS,
    INSUFFICIENT_FUNDS,   // Airtime outcome seen in the screenshots
    NOT_ALLOWED,           // Iherereze outcome seen in the screenshots (existing unpaid loan)
    FAILED
}
