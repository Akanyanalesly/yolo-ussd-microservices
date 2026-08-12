package com.mtn.yolo.ussdgatewayservice.client

import com.mtn.yolo.ussdgatewayservice.dto.BundleDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import org.springframework.http.HttpMethod
import org.springframework.core.ParameterizedTypeReference

@Component
class BundleServiceClient(
    private val restTemplate: RestTemplate,
    @Value("\${services.bundle-service.url:http://localhost:8081}") private val baseUrl: String
) {

    /** Bundles directly under a root category with no intermediate menu (e.g. gwamon, desade, foleva). */
    fun getByCategory(category: String): List<BundleDto> {
        val url = "$baseUrl/api/bundles?category=$category&purchasableOnly=true"
        return fetchList(url)
    }

    /** Returns every bundle in a root category, including time-restricted offers. */
    fun getAllByCategory(category: String): List<BundleDto> {
        val url = "$baseUrl/api/bundles?category=$category"
        return fetchList(url)
    }

    /** Bundles under a category + subcategory (e.g. yolo-internet + daily). */
    fun getByCategoryAndSubcategory(category: String, subcategory: String): List<BundleDto> {
        val url = "$baseUrl/api/bundles?category=$category&subcategory=$subcategory&purchasableOnly=true"
        return fetchList(url)
    }

    /**
     * Some USSD menus show partners that are not currently purchasable. The gateway
     * still needs to display those entries so it can return their prescribed message.
     */
    fun getAllByCategoryAndSubcategory(category: String, subcategory: String): List<BundleDto> {
        val url = "$baseUrl/api/bundles?category=$category&subcategory=$subcategory"
        return fetchList(url)
    }

    private fun fetchList(url: String): List<BundleDto> {
        val response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            null,
            object : ParameterizedTypeReference<List<BundleDto>>() {}
        )
        return response.body ?: emptyList()
    }
}
