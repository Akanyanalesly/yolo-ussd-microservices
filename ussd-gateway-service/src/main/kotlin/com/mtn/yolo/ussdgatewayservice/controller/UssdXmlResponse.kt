package com.mtn.yolo.ussdgatewayservice.controller

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

@JacksonXmlRootElement(localName = "response")
data class UssdXmlResponse(
    @JacksonXmlProperty(localName = "applicationResponse")
    val applicationResponse: String,
    
    @JacksonXmlProperty(localName = "freeflow")
    val freeflow: ResponseFreeflow
)

@JacksonXmlRootElement(localName = "freeflow")
data class ResponseFreeflow(
    @JacksonXmlProperty(localName = "freeflowState")
    val freeflowState: String = "FC",  // FC = Continue, FE = End
    
    @JacksonXmlProperty(localName = "freeflowCharging")
    val freeflowCharging: String = "N",  // Y/N for charging
    
    @JacksonXmlProperty(localName = "freeflowChargingAmount")
    val freeflowChargingAmount: String = "0"
)
