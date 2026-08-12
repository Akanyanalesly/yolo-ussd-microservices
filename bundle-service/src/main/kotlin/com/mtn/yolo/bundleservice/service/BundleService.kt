package com.mtn.yolo.bundleservice.service

import com.mtn.yolo.bundleservice.dto.BundleRequest
import com.mtn.yolo.bundleservice.entity.Bundle
import com.mtn.yolo.bundleservice.repository.BundleRepository
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate

@Service
class BundleService(private val bundleRepository: BundleRepository) {

    // ---------- CREATE ----------
    fun create(request: BundleRequest): Bundle {
        val bundle = Bundle(
            category = request.category,
            subcategory = request.subcategory,
            optionNumber = request.optionNumber,
            label = request.label,
            priceFrw = request.priceFrw,
            dataAmount = request.dataAmount,
            minutes = request.minutes,
            validityPeriod = request.validityPeriod,
            infoLine = request.infoLine,
            restrictedToWeekend = request.restrictedToWeekend,
            active = request.active,
            paymentMethods = request.paymentMethods
        )
        return bundleRepository.save(bundle)
    }

    // ---------- READ ----------
    fun findAll(): List<Bundle> = bundleRepository.findAll()

    fun findById(id: Long): Bundle =
        bundleRepository.findById(id)
            .orElseThrow { NoSuchElementException("Bundle with id $id not found") }

    /** Bundles directly under a root category with no intermediate menu (e.g. gwamon, desade, foleva). */
    fun findByCategory(category: String): List<Bundle> =
        bundleRepository.findByCategoryAndSubcategoryIsNull(category)

    /** Bundles under a category + subcategory, e.g. yolo-internet + daily. */
    fun findByCategoryAndSubcategory(category: String, subcategory: String): List<Bundle> =
        bundleRepository.findByCategoryAndSubcategory(category, subcategory)

    /** Only bundles currently available for purchase (excludes "not yet available" merchants),
     *  and applies the Gwamon' Weekend day-of-week restriction. */
    fun findPurchasable(category: String, subcategory: String? = null): List<Bundle> {
        val bundles = if (subcategory != null) {
            bundleRepository.findByCategoryAndSubcategory(category, subcategory)
        } else {
            bundleRepository.findByCategoryAndSubcategoryIsNull(category)
        }
        val today = LocalDate.now().dayOfWeek
        val isWeekend = today == DayOfWeek.FRIDAY || today == DayOfWeek.SATURDAY || today == DayOfWeek.SUNDAY
        return bundles.filter { it.active && (!it.restrictedToWeekend || isWeekend) }
    }

    // ---------- UPDATE ----------
    fun update(id: Long, request: BundleRequest): Bundle {
        val existing = findById(id)
        val updated = existing.copy(
            category = request.category,
            subcategory = request.subcategory,
            optionNumber = request.optionNumber,
            label = request.label,
            priceFrw = request.priceFrw,
            dataAmount = request.dataAmount,
            minutes = request.minutes,
            validityPeriod = request.validityPeriod,
            infoLine = request.infoLine,
            restrictedToWeekend = request.restrictedToWeekend,
            active = request.active,
            paymentMethods = request.paymentMethods
        )
        return bundleRepository.save(updated)
    }

    // ---------- DELETE ----------
    fun delete(id: Long) {
        if (!bundleRepository.existsById(id)) {
            throw NoSuchElementException("Bundle with id $id not found")
        }
        bundleRepository.deleteById(id)
    }
}
