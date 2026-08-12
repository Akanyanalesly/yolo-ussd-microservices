package com.mtn.yolo.bundleservice.repository

import com.mtn.yolo.bundleservice.entity.Bundle
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BundleRepository : JpaRepository<Bundle, Long> {

    fun findByCategoryAndSubcategoryIsNull(category: String): List<Bundle>

    fun findByCategoryAndSubcategory(category: String, subcategory: String): List<Bundle>

    fun findByCategory(category: String): List<Bundle>
}
