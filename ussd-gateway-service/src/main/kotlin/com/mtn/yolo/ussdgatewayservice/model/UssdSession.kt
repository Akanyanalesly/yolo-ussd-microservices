package com.mtn.yolo.ussdgatewayservice.model

import java.time.Instant

data class UssdSession(
    val id: String,
    val msisdn: String,
    val inputs: MutableList<String> = mutableListOf(),
    val createdAt: Instant = Instant.now(),
    var lastAccessedAt: Instant = Instant.now(),
    var status: String = "ACTIVE",
    var language: UssdLanguage = UssdLanguage.ENGLISH
)

enum class UssdLanguage {
    ENGLISH,
    KINYARWANDA
}
