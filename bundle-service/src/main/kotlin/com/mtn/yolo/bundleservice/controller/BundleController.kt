package com.mtn.yolo.bundleservice.controller

import com.mtn.yolo.bundleservice.dto.BundleRequest
import com.mtn.yolo.bundleservice.entity.Bundle
import com.mtn.yolo.bundleservice.service.BundleService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/bundles")
class BundleController(private val bundleService: BundleService) {

    // CREATE
    @PostMapping
    fun create(@Valid @RequestBody request: BundleRequest): ResponseEntity<Bundle> {
        val created = bundleService.create(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    // READ - all
    @GetMapping
    fun findAll(
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) subcategory: String?,
        @RequestParam(required = false, defaultValue = "false") purchasableOnly: Boolean
    ): ResponseEntity<List<Bundle>> {
        val result = when {
            category != null && purchasableOnly -> bundleService.findPurchasable(category, subcategory)
            category != null && subcategory != null -> bundleService.findByCategoryAndSubcategory(category, subcategory)
            category != null -> bundleService.findByCategory(category)
            else -> bundleService.findAll()
        }
        return ResponseEntity.ok(result)
    }

    // READ - one
    @GetMapping("/{id}")
    fun findById(@PathVariable id: Long): ResponseEntity<Bundle> =
        ResponseEntity.ok(bundleService.findById(id))

    // UPDATE
    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody request: BundleRequest): ResponseEntity<Bundle> =
        ResponseEntity.ok(bundleService.update(id, request))

    // DELETE
    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        bundleService.delete(id)
        return ResponseEntity.noContent().build()
    }

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(ex: NoSuchElementException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to (ex.message ?: "Not found")))
}
