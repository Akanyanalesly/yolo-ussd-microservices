package com.mtn.yolo.ussdgatewayservice.dto

data class BundleDto(
    val id: Long,
    val category: String,
    val subcategory: String?,
    val optionNumber: String,
    val label: String,
    val priceFrw: Int,
    val infoLine: String?,
    val restrictedToWeekend: Boolean,
    val active: Boolean,
    val paymentMethods: List<String>
)
