package com.mtn.yolo.bundleservice.config

import com.mtn.yolo.bundleservice.entity.Bundle
import com.mtn.yolo.bundleservice.entity.PaymentMethod
import com.mtn.yolo.bundleservice.entity.PaymentMethod.*
import com.mtn.yolo.bundleservice.repository.BundleRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

/**
 * Seeds the database with the bundle catalog mapped from the real *154# menu screenshots,
 * so bundle-service has real data to serve from the moment it starts, instead of an empty table.
 * This mirrors what was previously hardcoded directly in ussd-gateway-service.
 */
@Component
class DataSeeder(private val repo: BundleRepository) : CommandLineRunner {

    private val all3 = listOf(AIRTIME, MOMO, IHEREREZE)
    private val airtimeMomo = listOf(AIRTIME, MOMO)
    private val momoOnly = listOf(MOMO)

    override fun run(vararg args: String?) {
        if (repo.count() > 0) return // don't reseed on every restart

        val bundles = mutableListOf<Bundle>()

        // ---- Gwamon' ----
        bundles += Bundle(category = "gwamon", optionNumber = "1", label = "1500Frw= 8GB+800Mins/7Days", priceFrw = 1500, paymentMethods = all3)
        bundles += Bundle(category = "gwamon", optionNumber = "2", label = "1000Frw= 7GB/7Days", priceFrw = 1000, paymentMethods = all3)
        bundles += Bundle(category = "gwamon", optionNumber = "3", label = "500Frw= 800Mins/7Days", priceFrw = 500, paymentMethods = all3)
        bundles += Bundle(category = "gwamon", optionNumber = "4", label = "Gwamon' Weekend", priceFrw = 0, paymentMethods = all3, restrictedToWeekend = true)

        // ---- YOLO Voice ----
        bundles += Bundle(category = "yolo-voice", optionNumber = "1", label = "200Frw=250Mins/24hrs", priceFrw = 200, paymentMethods = all3)
        bundles += Bundle(category = "yolo-voice", optionNumber = "2", label = "500Frw=800Mins/7Days", priceFrw = 500, paymentMethods = all3)
        bundles += Bundle(category = "yolo-voice", optionNumber = "3", label = "1000Frw=(120 Mins+1GB) per day /7days", priceFrw = 1000, paymentMethods = all3)
        bundles += Bundle(category = "yolo-voice", optionNumber = "4", label = "2000Frw=4000Mins/30", priceFrw = 2000, paymentMethods = airtimeMomo)

        // ---- YOLO Internet: Daily ----
        bundles += Bundle(category = "yolo-internet", subcategory = "daily", optionNumber = "1", label = "500Frw = 1.5GB", priceFrw = 500, paymentMethods = all3)
        bundles += Bundle(category = "yolo-internet", subcategory = "daily", optionNumber = "2", label = "1000Frw = 2.2GB", priceFrw = 1000, paymentMethods = all3)

        // ---- YOLO Internet: Weekly ----
        bundles += Bundle(category = "yolo-internet", subcategory = "weekly", optionNumber = "1", label = "500Frw=1GB/7days", priceFrw = 500, paymentMethods = all3)
        bundles += Bundle(category = "yolo-internet", subcategory = "weekly", optionNumber = "2", label = "1000Frw=(120 Mins+1GB) per day /7days", priceFrw = 1000, paymentMethods = all3)
        bundles += Bundle(category = "yolo-internet", subcategory = "weekly", optionNumber = "3", label = "2000Frw=3GB/7days", priceFrw = 2000, paymentMethods = all3)
        bundles += Bundle(category = "yolo-internet", subcategory = "weekly", optionNumber = "4", label = "5000Frw=10GB/7 Days", priceFrw = 5000, paymentMethods = all3)

        // ---- YOLO Internet: Monthly (no Iherereze) ----
        bundles += Bundle(category = "yolo-internet", subcategory = "monthly", optionNumber = "1", label = "2000Frw=2GB+30SMS", priceFrw = 2000, paymentMethods = airtimeMomo)
        bundles += Bundle(category = "yolo-internet", subcategory = "monthly", optionNumber = "2", label = "5000Frw=7GB/30 Days", priceFrw = 5000, paymentMethods = airtimeMomo)
        bundles += Bundle(category = "yolo-internet", subcategory = "monthly", optionNumber = "3", label = "10000Frw= 30GB/30 Days", priceFrw = 10000, paymentMethods = airtimeMomo)

        // ---- YOLO Internet: DesaDe ----
        bundles += Bundle(category = "yolo-internet", subcategory = "desade", optionNumber = "1", label = "200Frw=200Mins/48hrs", priceFrw = 200, paymentMethods = all3, infoLine = "DesaDe is used for MTN to MTN calls only")
        bundles += Bundle(category = "yolo-internet", subcategory = "desade", optionNumber = "2", label = "200Frw=100Mins+100MBs/48hrs", priceFrw = 200, paymentMethods = all3, infoLine = "DesaDe is used for MTN to MTN calls only")

        // ---- YOLO Internet: Hourly (no Iherereze) ----
        bundles += Bundle(category = "yolo-internet", subcategory = "hourly", optionNumber = "1", label = "100Frw=200MB(1hr)", priceFrw = 100, paymentMethods = airtimeMomo)
        bundles += Bundle(category = "yolo-internet", subcategory = "hourly", optionNumber = "2", label = "200Frw=500MB(2hr)", priceFrw = 200, paymentMethods = airtimeMomo)
        bundles += Bundle(category = "yolo-internet", subcategory = "hourly", optionNumber = "3", label = "500Frw=1GB(2hrs)", priceFrw = 500, paymentMethods = airtimeMomo)
        bundles += Bundle(category = "yolo-internet", subcategory = "hourly", optionNumber = "4", label = "1000Frw=3GB(2hrs)", priceFrw = 1000, paymentMethods = airtimeMomo)
        bundles += Bundle(category = "yolo-internet", subcategory = "hourly", optionNumber = "5", label = "1500Frw=5GB(3hrs)", priceFrw = 1500, paymentMethods = airtimeMomo)

        // ---- Social Media Bundles: WhatsApp (no Iherereze) ----
        bundles += Bundle(category = "social-media", subcategory = "whatsapp", optionNumber = "1", label = "50Frw=80MB(WhatsApp)88", priceFrw = 50, paymentMethods = airtimeMomo)
        bundles += Bundle(category = "social-media", subcategory = "whatsapp", optionNumber = "2", label = "100Frw=250MB(WhatsApp)", priceFrw = 100, paymentMethods = airtimeMomo)
        bundles += Bundle(category = "social-media", subcategory = "whatsapp", optionNumber = "3", label = "200Frw=510MB(WhatsApp)", priceFrw = 200, paymentMethods = airtimeMomo)

        // ---- Social Media Bundles: Facebook and Instagram (no Iherereze) ----
        bundles += Bundle(category = "social-media", subcategory = "facebook-instagram", optionNumber = "1", label = "100Frw=400MB(Facebook+Instagram)", priceFrw = 100, paymentMethods = airtimeMomo)
        bundles += Bundle(category = "social-media", subcategory = "facebook-instagram", optionNumber = "2", label = "200Frw=810MB(Facebook+Instagram)", priceFrw = 200, paymentMethods = airtimeMomo)

        // ---- Root-level DesaDe (48hrs) ----
        bundles += Bundle(category = "desade", optionNumber = "1", label = "200Frw=200Mins/48hrs", priceFrw = 200, paymentMethods = all3, infoLine = "DesaDe is used for MTN to MTN calls only")
        bundles += Bundle(category = "desade", optionNumber = "2", label = "200Frw=100Mins+100MBs/48hrs", priceFrw = 200, paymentMethods = all3, infoLine = "DesaDe is used for MTN to MTN calls only")

        // ---- FoLeva (no Iherereze) ----
        bundles += Bundle(category = "foleva", optionNumber = "1", label = "5000Frw=10GB+1000Mins", priceFrw = 5000, paymentMethods = airtimeMomo, infoLine = "FoLeva Bundles - valid until the last MB")
        bundles += Bundle(category = "foleva", optionNumber = "2", label = "10000Frw=25GB+2500Mins", priceFrw = 10000, paymentMethods = airtimeMomo, infoLine = "FoLeva Bundles - valid until the last MB")
        bundles += Bundle(category = "foleva", optionNumber = "3", label = "20000Frw=75GB+3000 Mins", priceFrw = 20000, paymentMethods = airtimeMomo, infoLine = "FoLeva Bundles - valid until the last MB")

        // ---- Loyalty redemption merchants (MoMo only, custom option numbering preserved) ----
        bundles += Bundle(category = "loyalty-redeem", subcategory = "entertainment", optionNumber = "1", label = "Fazenda Sengha", priceFrw = 0, paymentMethods = momoOnly)
        bundles += Bundle(category = "loyalty-redeem", subcategory = "restaurants", optionNumber = "1", label = "Vuba Vuba", priceFrw = 0, paymentMethods = momoOnly)
        bundles += Bundle(category = "loyalty-redeem", subcategory = "fashion", optionNumber = "1", label = "Masion Munezero", priceFrw = 0, paymentMethods = momoOnly)
        bundles += Bundle(category = "loyalty-redeem", subcategory = "fashion", optionNumber = "2", label = "Uzuri K&Y", priceFrw = 0, paymentMethods = emptyList(), active = false)
        bundles += Bundle(category = "loyalty-redeem", subcategory = "fashion", optionNumber = "3", label = "Graceland", priceFrw = 0, paymentMethods = emptyList(), active = false)
        bundles += Bundle(category = "loyalty-redeem", subcategory = "fashion", optionNumber = "4", label = "250 City Shoes", priceFrw = 0, paymentMethods = emptyList(), active = false)
        bundles += Bundle(category = "loyalty-redeem", subcategory = "lifestyle", optionNumber = "1", label = "Uris Collections", priceFrw = 0, paymentMethods = emptyList(), active = false)
        bundles += Bundle(category = "loyalty-redeem", subcategory = "lifestyle", optionNumber = "2", label = "Kts Pictures", priceFrw = 0, paymentMethods = emptyList(), active = false)

        repo.saveAll(bundles)
        println("DataSeeder: inserted ${bundles.size} bundles into the database")
    }
}
