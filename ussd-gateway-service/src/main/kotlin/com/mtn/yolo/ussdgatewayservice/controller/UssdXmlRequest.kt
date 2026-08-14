package com.mtn.yolo.ussdgatewayservice.controller

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

/** XML representation accepted by POST /ussd when Content-Type is application/xml. */
@JacksonXmlRootElement(localName = "request")
data class UssdXmlRequest(
    @JacksonXmlProperty(localName = "msisdn")
    val msisdn: String = "",
    
    @JacksonXmlProperty(localName = "imsi")
    val imsi: String? = null,
    
    @JacksonXmlProperty(localName = "input")
    val input: String? = null,
    
    @JacksonXmlProperty(localName = "sessionid")
    val sessionid: String? = null,
    
    @JacksonXmlProperty(localName = "CellID")
    val cellId: String? = null,
    
    @JacksonXmlProperty(localName = "new_request")
    val newRequest: String? = null,
    
    @JacksonXmlProperty(localName = "parameters")
    val parameters: String? = null,
    
    @JacksonXmlProperty(localName = "freeflow")
    val freeflow: Freeflow? = null
)

@JacksonXmlRootElement(localName = "freeflow")
data class Freeflow(
    @JacksonXmlProperty(localName = "mode")
    val mode: String? = null
)
