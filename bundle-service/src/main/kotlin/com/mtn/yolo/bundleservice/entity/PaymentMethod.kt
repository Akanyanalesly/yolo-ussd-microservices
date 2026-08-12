package com.mtn.yolo.bundleservice.entity

/**
 * Payment methods a bundle can be purchased with.
 * Not every bundle offers all three — e.g. Monthly and Hourly bundles under
 * YOLO Internet, and every merchant under Redeem Loyalty Points, only offer a subset.
 */
enum class PaymentMethod {
    AIRTIME,
    MOMO,
    IHEREREZE
}
