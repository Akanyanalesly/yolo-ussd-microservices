package com.mtn.yolo.bundleservice.dto

import com.mtn.yolo.bundleservice.entity.PaymentMethod
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class BundleRequest(
    @field:NotBlank(message = "category is required")
    val category: String,

    val subcategory: String? = null,

    @field:NotBlank(message = "optionNumber is required")
    val optionNumber: String,

    @field:NotBlank(message = "label is required")
    val label: String,

    @field:Min(value = 0, message = "priceFrw must not be negative")
    val priceFrw: Int,

    val dataAmount: String? = null,
    val minutes: String? = null,
    val validityPeriod: String? = null,
    val infoLine: String? = null,
    val restrictedToWeekend: Boolean = false,
    val active: Boolean = true,
    val paymentMethods: List<PaymentMethod> = emptyList()
)
