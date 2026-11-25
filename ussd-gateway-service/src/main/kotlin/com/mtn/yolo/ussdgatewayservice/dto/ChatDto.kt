package com.mtn.yolo.ussdgatewayservice.dto

data class MenuNavigateRequest(
    val sessionId: String,
    val phoneNumber: String? = null,
    val input: String
)

data class ChatButton(
    val label: String,
    val value: String
)

data class MenuNavigateResponse(
    val sessionId: String,
    val message: String,
    val buttons: List<ChatButton> = emptyList(),
    val isEnd: Boolean = false,
    val inputType: String = "BUTTONS"
)

data class AskRequest(
    val sessionId: String,
    val input: String
)

data class AskResponse(
    val sessionId: String,
    val message: String
)
