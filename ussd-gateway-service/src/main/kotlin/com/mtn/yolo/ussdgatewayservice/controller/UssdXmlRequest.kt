package com.mtn.yolo.ussdgatewayservice.controller

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

/** XML representation accepted by POST /ussd when Content-Type is application/xml. */
@JacksonXmlRootElement(localName = "ussdRequest")
data class UssdXmlRequest(
    val requestId: String = "",
    val sessionId: String? = null,
    val phoneNumber: String = "",
    val text: String? = null,
    val serviceCode: String? = null
)
