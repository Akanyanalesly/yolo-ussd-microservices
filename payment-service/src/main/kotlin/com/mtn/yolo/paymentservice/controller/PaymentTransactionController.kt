package com.mtn.yolo.paymentservice.controller

import com.mtn.yolo.paymentservice.dto.PaymentRequest
import com.mtn.yolo.paymentservice.entity.PaymentStatus
import com.mtn.yolo.paymentservice.entity.PaymentTransaction
import com.mtn.yolo.paymentservice.service.PaymentTransactionService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/payments")
class PaymentTransactionController(private val service: PaymentTransactionService) {

    // CREATE — initiates a payment attempt (Airtime / MoMo / Iherereze) and records the outcome
    @PostMapping
    fun create(@Valid @RequestBody request: PaymentRequest): ResponseEntity<PaymentTransaction> {
        val created = service.create(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    // READ - all, or filtered by phoneNumber / ussdSessionId
    @GetMapping
    fun findAll(
        @RequestParam(required = false) phoneNumber: String?,
        @RequestParam(required = false) ussdSessionId: String?
    ): ResponseEntity<List<PaymentTransaction>> {
        val result = when {
            phoneNumber != null -> service.findByPhoneNumber(phoneNumber)
            ussdSessionId != null -> service.findByUssdSessionId(ussdSessionId)
            else -> service.findAll()
        }
        return ResponseEntity.ok(result)
    }

    // READ - one
    @GetMapping("/{id}")
    fun findById(@PathVariable id: Long): ResponseEntity<PaymentTransaction> =
        ResponseEntity.ok(service.findById(id))

    // UPDATE - status (e.g. once a real MoMo callback/webhook exists to resolve PROCESSING -> SUCCESS/FAILED)
    @PutMapping("/{id}/status")
    fun updateStatus(
        @PathVariable id: Long,
        @RequestParam status: PaymentStatus,
        @RequestParam(required = false) message: String?
    ): ResponseEntity<PaymentTransaction> =
        ResponseEntity.ok(service.updateStatus(id, status, message))

    // DELETE
    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        service.delete(id)
        return ResponseEntity.noContent().build()
    }

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(ex: NoSuchElementException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to (ex.message ?: "Not found")))
}
