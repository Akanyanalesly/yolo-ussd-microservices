package com.mtn.yolo.ussdgatewayservice.client

import com.mtn.yolo.ussdgatewayservice.dto.PaymentRequestDto
import com.mtn.yolo.ussdgatewayservice.dto.PaymentResponseDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class PaymentServiceClient(
    private val restTemplate: RestTemplate,
    @Value("\${services.payment-service.url:http://localhost:8082}") private val baseUrl: String
) {

    fun initiatePayment(request: PaymentRequestDto): PaymentResponseDto? {
        val url = "$baseUrl/api/payments"
        return restTemplate.postForObject(url, request, PaymentResponseDto::class.java)
    }
}
