package com.mtn.yolo.ussdgatewayservice.controller

import com.mtn.yolo.ussdgatewayservice.client.BundleServiceClient
import com.mtn.yolo.ussdgatewayservice.client.PaymentServiceClient
import com.mtn.yolo.ussdgatewayservice.dto.BundleDto
import com.mtn.yolo.ussdgatewayservice.dto.PaymentRequestDto
import com.mtn.yolo.ussdgatewayservice.model.UssdLanguage
import com.mtn.yolo.ussdgatewayservice.service.UssdSessionService
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.DayOfWeek
import java.time.LocalDate

@RestController
class UssdController(
    private val bundleServiceClient: BundleServiceClient,
    private val paymentServiceClient: PaymentServiceClient,
    private val ussdSessionService: UssdSessionService
) {

    @PostMapping("/ussd", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun handleUssd(
        @RequestParam requestId: String,
        @RequestParam(required = false, defaultValue = "") sessionId: String,
        @RequestParam phoneNumber: String,
        @RequestParam text: String,
        @RequestParam(required = false) serviceCode: String?
    ): ResponseEntity<String> {
        val normalizedRequestId = requestId.trim()
        val normalizedSessionId = sessionId.trim()
        val normalizedPhoneNumber = phoneNumber.trim()

        validateRequest(normalizedRequestId, normalizedSessionId, normalizedPhoneNumber)?.let { error ->
            return invalidRequestResponse(error)
        }

        val appSessionId: String
        val inputs: List<String>

        when (normalizedRequestId) {
            "1" -> {
                if (normalizedSessionId.isNotBlank()) {
                    if (ussdSessionService.isSessionIdInUse(normalizedSessionId)) {
                        return invalidRequestResponse("sessionId is already in use. Choose a unique sessionId.")
                    }
                }
                val session = ussdSessionService.createSession(normalizedPhoneNumber, normalizedSessionId)
                appSessionId = session.id

                val unfinished = ussdSessionService.getUnfinishedSession(normalizedPhoneNumber, appSessionId)
                if (unfinished != null) {
                    // A resumed conversation keeps the language the customer had selected.
                    ussdSessionService.setLanguage(appSessionId, unfinished.language)
                    ussdSessionService.replaceInputs(appSessionId, listOf("__resume__") + unfinished.inputs)
                    val headers = HttpHeaders()
                    headers.contentType = MediaType.TEXT_PLAIN
                    headers.add("X-USSD-Session-Id", appSessionId)
                    headers.add("X-USSD-Action", "CON")
                    return ResponseEntity.ok().headers(headers).body(
                        resumePrompt(unfinished.language).removePrefix("CON ")
                    )
                }
                inputs = emptyList()
            }
            "0" -> {
                val session = ussdSessionService.getSession(normalizedSessionId)
                if (session == null || session.msisdn != normalizedPhoneNumber) {
                    return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                        .body("END Your session has expired or is invalid. Please dial *154# to start a new session.")
                }
                appSessionId = session.id

                inputs = if (text.isBlank()) {
                    ussdSessionService.replaceInputs(appSessionId, emptyList())
                    emptyList()
                } else if (text.contains("*")) {
                    val parts = text.split("*")
                    ussdSessionService.replaceInputs(appSessionId, parts)
                    parts
                } else {
                    val path = session.inputs
                    path.add(text.trim())
                    ussdSessionService.replaceInputs(appSessionId, path)
                    path.toList()
                }
            }
            else -> {
                return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                    .body("END Invalid request. Please dial *154# to start again.")
            }
        }

        val response = buildResponse(appSessionId, normalizedPhoneNumber, inputs)
        if (response.startsWith("END")) {
            ussdSessionService.endSession(appSessionId, natural = true)
        }

        val headers = HttpHeaders()
        headers.contentType = MediaType.TEXT_PLAIN
        headers.add("X-USSD-Session-Id", appSessionId)
        val action = when {
            response.startsWith("CON") -> "CON"
            response.startsWith("END") -> "END"
            else -> ""
        }
        if (action.isNotEmpty()) headers.add("X-USSD-Action", action)
        return ResponseEntity.ok().headers(headers)
            .body(response.removePrefix("CON ").removePrefix("END "))
    }

    private fun validateRequest(requestId: String, sessionId: String, phoneNumber: String): String? = when {
        requestId !in setOf("0", "1") -> "requestId must be either 1 (new session) or 0 (continue session)."
        phoneNumber.isBlank() -> "phoneNumber is required."
        !phoneNumber.matches(Regex("(078|079)\\d{7}|(25078|25079)\\d{7}")) ->
            "phoneNumber must be a valid MTN Rwanda number: 10 digits starting with 078/079 or 12 digits starting with 25078/25079."
        sessionId.isBlank() -> "sessionId is required. Enter a unique sessionId."
        else -> null
    }

    private fun invalidRequestResponse(message: String): ResponseEntity<String> =
        ResponseEntity.badRequest()
            .contentType(MediaType.TEXT_PLAIN)
            .body("END Invalid request: $message")

    private fun buildResponse(sessionId: String, phoneNumber: String, inputs: List<String>): String {
        // Handle resume prompt — inputs[0] == "__resume__" means we're waiting for 1 or 2
        if (inputs.isNotEmpty() && inputs[0] == "__resume__") {
            val savedInputs = inputs.drop(1) // everything after the marker is the saved path
            if (savedInputs.isEmpty() || inputs.size == 1) {
                // Still on the prompt screen (first display)
                return resumePrompt(languageFor(sessionId))
            }
            return when (savedInputs.last()) {
                "1" -> {
                    // User chose resume — savedInputs minus the "1" choice = actual path
                    val restoredInputs = savedInputs.dropLast(1)
                    ussdSessionService.replaceInputs(sessionId, restoredInputs)
                    buildResponse(sessionId, phoneNumber, restoredInputs)
                }
                "2" -> {
                    ussdSessionService.replaceInputs(sessionId, emptyList())
                    val language = ussdSessionService.getSession(sessionId)?.language ?: UssdLanguage.ENGLISH
                    rootMenu(language)
                }
                else -> resumePrompt(languageFor(sessionId))
            }
        }


        if (inputs.size == 2 && inputs[0] == "n") {
            return when (inputs[1]) {
                "7" -> iherezeResponse(languageFor(sessionId))
                "8" -> yoloStarMenu(languageFor(sessionId))
                "9" -> {
                    ussdSessionService.replaceInputs(sessionId, emptyList())
                    ussdSessionService.toggleLanguage(sessionId)
                    buildResponse(sessionId, phoneNumber, emptyList())
                }
                // Any other number on page 2 — treat as root menu selection
                else -> {
                    val rootInputs = listOf(inputs[1])
                    ussdSessionService.replaceInputs(sessionId, rootInputs)
                    buildResponse(sessionId, phoneNumber, rootInputs)
                }
            }
        }

        if (inputs.size >= 2 && inputs[inputs.lastIndex - 1] == "n" && inputs.last() == "8") {
            return yoloStarMenu(languageFor(sessionId))
        }

        if (inputs.size >= 3 && inputs[inputs.lastIndex - 2] == "n" &&
            inputs[inputs.lastIndex - 1] == "8" && inputs.last() == "1"
        ) {
            return yoloStarMemberResponse(languageFor(sessionId))
        }

        if (inputs.size >= 3 && inputs[inputs.lastIndex - 2] == "n" &&
            inputs[inputs.lastIndex - 1] == "8" && inputs.last() == "2"
        ) {
            return yoloStarAccountResponse(languageFor(sessionId), phoneNumber)
        }

        if (inputs.size >= 3 && inputs[inputs.lastIndex - 2] == "n" &&
            inputs[inputs.lastIndex - 1] == "8" && inputs.last() == "3"
        ) {
            return smsConfirmation(languageFor(sessionId))
        }

        if (inputs == listOf("n", "8", "5")) {
            return otherInfoResponse(languageFor(sessionId))
        }

        if (inputs == listOf("n", "8", "0")) {
            ussdSessionService.replaceInputs(sessionId, listOf("n"))
            val language = ussdSessionService.getSession(sessionId)?.language ?: UssdLanguage.ENGLISH
            return rootMenuPage2(language)
        }



        // Unavailable Fashion and Lifestyle partners lead to a "0)Go back" screen.
        // Its back action returns to the partner list, rather than skipping all the
        // way to Redeem Loyalty Points.
        if (inputs.size == 6 && inputs.take(3) == listOf("n", "8", "4") &&
            ((inputs[3] == "3" && inputs[4] in setOf("2", "3", "4")) ||
                (inputs[3] == "4" && inputs[4] in setOf("1", "2"))) &&
            inputs[5] == "0"
        ) {
            val partnerMenuPath = inputs.dropLast(2)
            ussdSessionService.replaceInputs(sessionId, partnerMenuPath)
            return handleLoyaltyRedemption(partnerMenuPath, sessionId, phoneNumber)
        }

        if (inputs.isNotEmpty() && inputs.last() == "0" && inputs.size > 1) {
            val backTarget = inputs.dropLast(2)
            ussdSessionService.replaceInputs(sessionId, backTarget)
            return buildResponse(sessionId, phoneNumber, backTarget)
        }

        if (inputs.size >= 3 && inputs[0] == "n" && inputs[1] == "8" && inputs[2] == "4") {
            return handleLoyaltyRedemption(inputs, sessionId, phoneNumber)
        }

        val language = ussdSessionService.getSession(sessionId)?.language ?: UssdLanguage.ENGLISH
        if (inputs.isEmpty()) return rootMenu(language)

        return when (inputs[0]) {
            "0" -> handleGwamon(sessionId, phoneNumber, inputs)
            "1" -> handleYoloVoice(sessionId, phoneNumber, inputs)
            "2" -> handleYoloInternet(sessionId, phoneNumber, inputs)
            "3" -> handleSocialMedia(sessionId, phoneNumber, inputs)
            "4" -> handleDesaDe(sessionId, phoneNumber, inputs)
            "5" -> noActivePackResponse(language)
            "6" -> handleFoLeva(sessionId, phoneNumber, inputs)
            "n" -> rootMenuPage2(language)
            "p" -> rootMenu(language)
            "7" -> iherezeResponse(language)
            "8" -> if (inputs.size == 1) yoloStarMenu(language) else handleYoloStarFromRoot(sessionId, phoneNumber, inputs)
            "9" -> {
                ussdSessionService.replaceInputs(sessionId, emptyList())
                ussdSessionService.toggleLanguage(sessionId)
                buildResponse(sessionId, phoneNumber, emptyList())
            }
            else -> invalidOption(sessionId, phoneNumber, emptyList())
        }
    }

    private fun rootMenu(language: UssdLanguage): String = when (language) {
        UssdLanguage.ENGLISH ->
            "CON Dial *100# for great deals!\n\n" +
                    "0)Gwamon'\n1)YOLO Voice\n2)YOLO Internet\n" +
                    "3)Social Media Bundles(24hrs)\n4)DesaDe (48hrs)\n5)Balance check\n6)FoLeva\n7)Ihereze\n8)YOLO Star\n9)Hindura Ururimi(Language)"
        UssdLanguage.KINYARWANDA ->
            "CON Kanda *100# ubone Dilu nziza!\n\n" +
                    "0)Gwamon'\n1)YOLO Guhamagara\n2)YOLO Interneti\n" +
                    "3)Bundle za Social Media (24hrs)\n4)DesaDe (48hr)\n5)Ayo usigaranye\n6)FoLeva\n7)Ihereze\n8)YOLO Star\n9)Change Language(Ururimi)"
    }

    private fun rootMenuPage2(language: UssdLanguage): String = when (language) {
        UssdLanguage.ENGLISH -> "CON 7)Ihereze\n8)YOLO Star\n9)Hindura Ururimi(Language)"
        UssdLanguage.KINYARWANDA -> "CON 6)FoLeva\n7)Ihereze\n8)YOLO Star\n9)Change Language(Ururimi)"
    }

    private fun yoloStarMenu(language: UssdLanguage): String = when (language) {
        UssdLanguage.ENGLISH -> "CON Dial *100# for great deals!\n\n" +
                "1)Join YOLO Star\n2)My YOLO Star Account\n3)YOLO Star Partners\n" +
                "4)Redeem Loyalty Points\n5)Other info\n0)Go back"
        UssdLanguage.KINYARWANDA -> "CON Kanda *100# ubone Dilu nziza!\n\n" +
                "1)Kwiyandikisha muri YOLO Star\n2)Konti ya YOLO Star\n3)Abafatanyabikorwa\n" +
                "4)Guhabwa igabanyirizwa\n5)Andi makuru\n0)Gusubira Inyuma"
    }

    private fun handleLoyaltyRedemption(inputs: List<String>, sessionId: String = "", phoneNumber: String = ""): String {
        val language = languageFor(sessionId)
        if (inputs.size == 3) {
            return if (language == UssdLanguage.KINYARWANDA) {
                "CON Kanda *100# ubone Dilu nziza!\n\n1)Imyidagaduro\n2)Resitora\n3)Imideli\n4)Imibereho\n0)Gusubira Inyuma"
            } else {
                "CON Dial *100# for great deals!\n\n1)Entertainment\n2)Restaurants\n3)Fashion\n4)Lifestyle\n0)Go back"
            }
        }

        val subcategory = when (inputs[3]) {
            "1" -> "entertainment"
            "2" -> "restaurants"
            "3" -> "fashion"
            "4" -> "lifestyle"
            else -> return invalidOption(sessionId, phoneNumber, inputs.dropLast(1))
        }
        val bundles = if (subcategory in setOf("fashion", "lifestyle")) {
            bundleServiceClient.getAllByCategoryAndSubcategory("loyalty-redeem", subcategory)
        } else {
            bundleServiceClient.getByCategoryAndSubcategory("loyalty-redeem", subcategory)
        }

        return when (inputs.size) {
            4 -> if (subcategory in setOf("fashion", "lifestyle")) buildLoyaltyPartnerMenu(bundles, language) else buildBundleMenu(bundles, language)
            5 -> when {
                subcategory == "fashion" && inputs[4] in setOf("2", "3", "4") ->
                    backOnlyMenu(language)
                subcategory == "lifestyle" && inputs[4] in setOf("1", "2") ->
                    backOnlyMenu(language)
                else -> bundles.find { it.optionNumber == inputs[4] }?.let { buildPaymentMenu(it, language) }
                    ?: invalidOption(sessionId, phoneNumber, inputs.dropLast(1))
            }
            6 -> if (inputs[5] == "2") {
                if (language == UssdLanguage.KINYARWANDA) {
                    "END Y'ello, turimo gukurikirana ubusabe bwawe. Urahabwa ubutumwa bwo kubyemeza mu kanya."
                } else {
                    "END Y'ello We are processing your request. You will get an approval notification shortly"
                }
            } else {
                invalidOption(sessionId, phoneNumber, inputs.dropLast(1))
            }
            else -> invalidOption(sessionId, phoneNumber, inputs.dropLast(1))
        }
    }


    // ---------- YOLO Star (direct access via 8 from root) ----------

    private fun handleYoloStarFromRoot(sessionId: String, phoneNumber: String, inputs: List<String>): String {
        return when {
            inputs.size == 2 && inputs[1] == "1" -> yoloStarMemberResponse(languageFor(sessionId))
            inputs.size == 2 && inputs[1] == "2" -> yoloStarAccountResponse(languageFor(sessionId), phoneNumber)
            inputs.size == 2 && inputs[1] == "3" -> smsConfirmation(languageFor(sessionId))
            inputs.size == 2 && inputs[1] == "4" -> handleLoyaltyRedemption(listOf("n", "8") + inputs.drop(1), sessionId, phoneNumber)
            inputs.size == 2 && inputs[1] == "5" -> otherInfoResponse(languageFor(sessionId))
            inputs.size >= 3 && inputs[1] == "4" -> handleLoyaltyRedemption(listOf("n", "8") + inputs.drop(1), sessionId, phoneNumber)
            else -> invalidOption(sessionId, phoneNumber, inputs.dropLast(1))
        }
    }

    // ---------- Gwamon' ----------

    private fun handleGwamon(sessionId: String, phoneNumber: String, inputs: List<String>): String {
        val language = languageFor(sessionId)
        val bundles = bundleServiceClient.getAllByCategory("gwamon")
        return when (inputs.size) {
            1 -> buildGwamonMenu(bundles, language)
            2 -> {
                val selected = bundles.find { it.optionNumber == inputs[1] }
                    ?: return invalidOption(sessionId, phoneNumber, inputs.dropLast(1))
                if (selected.restrictedToWeekend && !isGwamonWeekendAvailable()) {
                    return gwamonWeekendResponse(language)
                }
                buildPaymentMenu(selected, language)
            }
            3 -> {
                val selected = bundles.find { it.optionNumber == inputs[1] }
                    ?: return invalidOption(sessionId, phoneNumber, inputs.dropLast(1))
                if (selected.restrictedToWeekend && !isGwamonWeekendAvailable()) {
                    return gwamonWeekendResponse(language)
                }
                handlePaymentSelection(sessionId, phoneNumber, selected, inputs[2], inputs.dropLast(1))
            }
            else -> invalidOption(sessionId, phoneNumber, inputs.dropLast(1))
        }
    }

    private fun isGwamonWeekendAvailable(): Boolean =
        LocalDate.now().dayOfWeek in setOf(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

    // ---------- YOLO Voice ----------

    private fun handleYoloVoice(sessionId: String, phoneNumber: String, inputs: List<String>): String {
        val language = languageFor(sessionId)
        val bundles = bundleServiceClient.getByCategory("yolo-voice")
        return when (inputs.size) {
            1 -> buildBundleMenu(bundles, language)
            2 -> bundles.find { it.optionNumber == inputs[1] }
                ?.let { buildPaymentMenu(it, language) }
                ?: invalidOption(sessionId, phoneNumber, inputs.dropLast(1))
            3 -> {
                val selected = bundles.find { it.optionNumber == inputs[1] }
                    ?: return invalidOption(sessionId, phoneNumber, inputs.dropLast(1))
                handlePaymentSelection(sessionId, phoneNumber, selected, inputs[2], inputs.dropLast(1))
            }
            else -> invalidOption(sessionId, phoneNumber, inputs.dropLast(1))
        }
    }

    // ---------- YOLO Internet ----------

    private fun handleYoloInternet(sessionId: String, phoneNumber: String, inputs: List<String>): String {
        val language = languageFor(sessionId)
        if (inputs.size == 1) {
            return if (language == UssdLanguage.KINYARWANDA) {
                "CON Kanda *100# ubone Dilu nziza!\n\n1)Umunsi(24hrs)\n2)Icyumweru\n3)Ukwezi\n4)DesaDe (48hr)\n5)Burisaha\n0)Gusubira Inyuma"
            } else {
                "CON Dial *100# for great deals!\n\n1)Daily(24hrs)\n2)Weekly(7Days)\n3)Monthly(30Days)\n4)DesaDe (48hrs)\n5)Hourly\n0)Go back"
            }
        }
        val subcategory = when (inputs[1]) {
            "1" -> "daily"
            "2" -> "weekly"
            "3" -> "monthly"
            "4" -> "desade"
            "5" -> "hourly"
            else -> return invalidOption(sessionId, phoneNumber, inputs.dropLast(1))
        }
        val bundles = bundleServiceClient.getByCategoryAndSubcategory("yolo-internet", subcategory)
        return when (inputs.size) {
            2 -> buildBundleMenu(bundles, language)
            3 -> bundles.find { it.optionNumber == inputs[2] }?.let { buildPaymentMenu(it, language) }
                ?: invalidOption(sessionId, phoneNumber, inputs.dropLast(1))
            4 -> {
                val selected = bundles.find { it.optionNumber == inputs[2] }
                    ?: return invalidOption(sessionId, phoneNumber, inputs.dropLast(1))
                handlePaymentSelection(sessionId, phoneNumber, selected, inputs[3], inputs.dropLast(1))
            }
            else -> invalidOption(sessionId, phoneNumber, inputs.dropLast(1))
        }
    }

    // ---------- Social Media Bundles ----------

    private fun handleSocialMedia(sessionId: String, phoneNumber: String, inputs: List<String>): String {
        val language = languageFor(sessionId)
        if (inputs.size == 1) return if (language == UssdLanguage.KINYARWANDA)
            "CON Kanda *100# ubone Dilu nziza!\n\n1)Whatsapp\n2)Facebook na Instagram\n0)Gusubira Inyuma"
        else "CON Social Media Bundles (24hrs)\n\n1)WhatsApp\n2)Facebook+Instagram\n0)Go back"
        val subcategory = when (inputs[1]) {
            "1" -> "whatsapp"
            "2" -> "facebook-instagram"
            else -> return invalidOption(sessionId, phoneNumber, inputs.dropLast(1))
        }
        val bundles = bundleServiceClient.getByCategoryAndSubcategory("social-media", subcategory)
        return when (inputs.size) {
            2 -> buildBundleMenu(bundles, language)
            3 -> bundles.find { it.optionNumber == inputs[2] }?.let { buildPaymentMenu(it, language) }
                ?: invalidOption(sessionId, phoneNumber, inputs.dropLast(1))
            4 -> {
                val selected = bundles.find { it.optionNumber == inputs[2] }
                    ?: return invalidOption(sessionId, phoneNumber, inputs.dropLast(1))
                handlePaymentSelection(sessionId, phoneNumber, selected, inputs[3], inputs.dropLast(1))
            }
            else -> invalidOption(sessionId, phoneNumber, inputs.dropLast(1))
        }
    }

    private fun handleDesaDe(sessionId: String, phoneNumber: String, inputs: List<String>): String {
        val bundles = bundleServiceClient.getByCategory("desade")
        return handleSimpleBundleFlow(sessionId, phoneNumber, inputs, bundles)
    }

    // ---------- FoLeva ----------

    private fun handleFoLeva(sessionId: String, phoneNumber: String, inputs: List<String>): String {
        val bundles = bundleServiceClient.getByCategory("foleva")
        return handleSimpleBundleFlow(sessionId, phoneNumber, inputs, bundles)
    }

    private fun handleSimpleBundleFlow(
        sessionId: String,
        phoneNumber: String,
        inputs: List<String>,
        bundles: List<BundleDto>
    ): String {
        val language = languageFor(sessionId)
        return when (inputs.size) {
            1 -> buildBundleMenu(bundles, language)
            2 -> bundles.find { it.optionNumber == inputs[1] }?.let { buildPaymentMenu(it, language) }
                ?: invalidOption(sessionId, phoneNumber, inputs.dropLast(1))
            3 -> {
                val selected = bundles.find { it.optionNumber == inputs[1] }
                    ?: return invalidOption(sessionId, phoneNumber, inputs.dropLast(1))
                handlePaymentSelection(sessionId, phoneNumber, selected, inputs[2], inputs.dropLast(1))
            }
            else -> invalidOption(sessionId, phoneNumber, inputs.dropLast(1))
        }
    }

    private fun resumePrompt(language: UssdLanguage): String =
        if (language == UssdLanguage.KINYARWANDA) {
            "CON Murakaza neza!\nUfite session utarangije.\n\n1)Komeza aho wari ugeze\n2)Tangira bundi bushya"
        } else {
            "CON Welcome back!\nYou have an unfinished session.\n\n1)Resume where you left off\n2)Start fresh"
        }

    private fun describeSession(inputs: List<String>): String {
        if (inputs.isEmpty()) return "browsing the main menu"
        return when (inputs[0]) {
            "0" -> when (inputs.size) {
                1 -> "browsing Gwamon' bundles"
                2 -> "selecting a Gwamon' bundle"
                else -> "paying for a Gwamon' bundle"
            }
            "1" -> when (inputs.size) {
                1 -> "browsing YOLO Voice bundles"
                2 -> "selecting a YOLO Voice bundle"
                else -> "paying for a YOLO Voice bundle"
            }
            "2" -> when {
                inputs.size == 1 -> "browsing YOLO Internet categories"
                inputs.size == 2 -> "browsing YOLO Internet ${subcategoryLabel(inputs[1])} bundles"
                inputs.size == 3 -> "selecting a YOLO Internet bundle"
                else -> "paying for a YOLO Internet bundle"
            }
            "3" -> when {
                inputs.size == 1 -> "browsing Social Media bundle categories"
                inputs.size == 2 -> "browsing ${socialMediaLabel(inputs[1])} bundles"
                inputs.size == 3 -> "selecting a Social Media bundle"
                else -> "paying for a Social Media bundle"
            }
            "4" -> when (inputs.size) {
                1 -> "browsing DesaDe bundles"
                2 -> "selecting a DesaDe bundle"
                else -> "paying for a DesaDe bundle"
            }
            "6" -> when (inputs.size) {
                1 -> "browsing FoLeva bundles"
                2 -> "selecting a FoLeva bundle"
                else -> "paying for a FoLeva bundle"
            }
            "8" -> when (inputs.size) {
                1 -> "browsing YOLO Star menu"
                else -> "in the YOLO Star section"
            }
            else -> "browsing the menu"
        }
    }

    private fun subcategoryLabel(input: String) = when (input) {
        "1" -> "Daily"
        "2" -> "Weekly"
        "3" -> "Monthly"
        "4" -> "DesaDe"
        "5" -> "Hourly"
        else -> ""
    }

    private fun socialMediaLabel(input: String) = when (input) {
        "1" -> "WhatsApp"
        "2" -> "Facebook+Instagram"
        else -> ""
    }

    // ---------- Invalid option helper ----------

    /** Pops the bad input from the session, then re-renders the parent menu with an error line. */
    private fun invalidOption(sessionId: String, phoneNumber: String, parent: List<String>): String {
        ussdSessionService.replaceInputs(sessionId, parent)
        val parentMenu = buildResponse(sessionId, phoneNumber, parent)
        val error = if (languageFor(sessionId) == UssdLanguage.KINYARWANDA) {
            "CON Ihitamo si ryo. Ongera ugerageze:\n\n"
        } else {
            "CON Invalid option. Please try again:\n\n"
        }
        return parentMenu.replaceFirst("CON ", error)
    }

    // ---------- Shared helpers ----------

    private fun languageFor(sessionId: String): UssdLanguage =
        ussdSessionService.getSession(sessionId)?.language ?: UssdLanguage.ENGLISH

    private fun yoloStarMemberResponse(language: UssdLanguage): String =
        if (language == UssdLanguage.KINYARWANDA) "END Usanzwe uri muri MTN YOLO STAR"
        else "END you are already in MTN YOLO STAR"

    private fun noActivePackResponse(language: UssdLanguage): String =
        if (language == UssdLanguage.KINYARWANDA) "END Pack yawe yarangiye, gura indi."
        else "END You have no active pack, please dial *154# to buy another one."

    private fun iherezeResponse(language: UssdLanguage): String =
        if (language == UssdLanguage.KINYARWANDA) "END Kanda *100# ubone Dilu nziza!\n\nihereze redirection"
        else "END Dial *100# for great deals!\n\nihereze redirection"

    private fun smsConfirmation(language: UssdLanguage): String =
        if (language == UssdLanguage.KINYARWANDA) "END Murakoze, urahabwa SMS mu kanya"
        else "END Thank you, you will receive an sms shortly"

    private fun otherInfoResponse(language: UssdLanguage): String =
        if (language == UssdLanguage.KINYARWANDA) "END Kanda *100# ubone Dilu nziza!\n\nKwamamaza andi makuru"
        else "END Dial *100# for great deals!\n\nchecking other news"

    private fun gwamonWeekendResponse(language: UssdLanguage): String =
        if (language == UssdLanguage.KINYARWANDA) "END Gwamon' Weekend iboneka kuva ku wa Gatanu kugeza ku Cyumweru"
        else "END Gwamon' Weekend is only available from Friday to Sunday"

    private fun yoloStarAccountResponse(language: UssdLanguage, phoneNumber: String): String =
        if (language == UssdLanguage.KINYARWANDA) {
            "END Kanda *100# ubone Dilu nziza!\n\nMurakaza neza: MTN RWANDACELL PLC\n\n" +
                    "Nomero: $phoneNumber\nIcyiciro cya YOLO STAR: Silver\nAmanota y'ubudahemuka: 500\nIcyiciro gikurikira: Gold"
        } else {
            "END Dial *100# for great deals!\n\nWelcome: MTN RWANDACELL PLC\n\n" +
                    "Number: $phoneNumber\nYOLO STAR Category: Silver\nLoyalty Points:500\nNext Category: Gold"
        }

    private fun translatedPaymentResult(message: String, language: UssdLanguage): String {
        if (language != UssdLanguage.KINYARWANDA) return message
        return when (message) {
            "Your airtime is insufficient. Please reload or dial *151# to borrow airtime" ->
                "Amafaranga ufite muri telefone ntahagije, kanda *151# wiguriye"
            "Y'ello We are processing your request. You will get an approval notification shortly" ->
                "Y'ello Ibyo musabye biri gukorwa. Murakira ubutumwa bwo kwemeza igikorwa mu kanya"
            "You are not allowed to borrow airtime at the moment. dial *151# to check loan balance" ->
                "Ntabwo wemerewe kugurana inite ubu. Kanda *151# urebe umwenda usigaranye"
            else -> message
        }
    }

    private fun backLabel(language: UssdLanguage): String =
        if (language == UssdLanguage.KINYARWANDA) "Gusubira Inyuma" else "Go back"

    private fun backOnlyMenu(language: UssdLanguage): String =
        "CON ${if (language == UssdLanguage.KINYARWANDA) "Kanda *100# ubone Dilu nziza!" else "Dial *100# for great deals!"}\n\n0)${backLabel(language)}"

    private fun translatedInfoLine(infoLine: String, language: UssdLanguage): String =
        if (language != UssdLanguage.KINYARWANDA) infoLine else when (infoLine) {
            "DesaDe is used for MTN to MTN calls only" -> "DesaDe ihamagara MTN kuri MTN gusa"
            "FoLeva Bundles - valid until the last MB" -> "Bundle za FoLeva zikora kugeza kuri MB ya nyuma"
            else -> infoLine
        }

    private fun translatedBundleLabel(bundle: BundleDto, language: UssdLanguage): String {
        if (language != UssdLanguage.KINYARWANDA) return bundle.label
        return when (bundle.category) {
            "gwamon" -> when (bundle.optionNumber) {
                "1" -> "1500Frw= 8GB+800Mins/Iminsi 7"
                "2" -> "1000Frw= 7GB/Iminsi 7"
                "3" -> "500Frw= 800Mins/Iminsi 7"
                else -> bundle.label
            }
            "yolo-voice" -> when (bundle.optionNumber) {
                "1" -> "200Frw=250Mins/24hrs"
                "2" -> "500Frw=800Mins/Iminsi 7"
                "3" -> "1000Frw=(120Mins+1GB) ku munsi / iminsi 7"
                else -> bundle.label
            }
            else -> bundle.label
        }
    }

    private fun buildBundleMenu(bundles: List<BundleDto>, language: UssdLanguage): String {
        if (bundles.isEmpty()) return if (language == UssdLanguage.KINYARWANDA) {
            "END Nta bundle ziboneka ubu"
        } else {
            "END No bundles currently available"
        }
        val infoLine = bundles.first().infoLine?.let { "${translatedInfoLine(it, language)}\n\n" } ?: ""
        val options = bundles.joinToString("\n") { "${it.optionNumber})${translatedBundleLabel(it, language)}" }
        return "CON $infoLine$options\n0)${backLabel(language)}"
    }

    private fun buildGwamonMenu(bundles: List<BundleDto>, language: UssdLanguage): String {
        val options = bundles.joinToString("\n") { "${it.optionNumber})${translatedBundleLabel(it, language)}" }
        val heading = if (language == UssdLanguage.KINYARWANDA) "Kanda *100# ubone Dilu nziza!" else "Dial *100# for great deals!"
        return "CON $heading\n\n$options\n0)${backLabel(language)}"
    }

    private fun buildLoyaltyPartnerMenu(bundles: List<BundleDto>, language: UssdLanguage): String {
        val options = bundles.joinToString("\n") { "${it.optionNumber})${it.label}" }
        return "CON Dial *100# for great deals!\n\n$options\n0)${backLabel(language)}"
    }

    private fun buildPaymentMenu(bundle: BundleDto, language: UssdLanguage): String {
        // The 2,000 Frw YOLO Voice bundle is payable only by Airtime or MoMo.
        // Keep this rule at the gateway as well as in the bundle catalog so an
        // older catalog record cannot expose Iherereze on the USSD menu.
        val methodLines = availablePaymentMethods(bundle).joinToString("\n") { method ->
            when (method) {
                "AIRTIME" -> if (language == UssdLanguage.KINYARWANDA) "1)Inite" else "1)Airtime"
                "MOMO" -> "2)MoMo"
                "IHEREREZE" -> "3)Iherereze"
                else -> ""
            }
        }
        val prompt = if (language == UssdLanguage.KINYARWANDA) {
            "CON Ishyura ${translatedBundleLabel(bundle, language)} ukoresheje:"
        } else {
            "CON Pay ${bundle.label} via:"
        }
        return "$prompt\n$methodLines\n0)${backLabel(language)}"
    }

    private fun availablePaymentMethods(bundle: BundleDto): List<String> = if (
            bundle.category == "yolo-voice" && bundle.optionNumber == "4"
        ) {
            bundle.paymentMethods.filter { it in setOf("AIRTIME", "MOMO") }
        } else {
            bundle.paymentMethods
        }

    private fun handlePaymentSelection(
        sessionId: String,
        phoneNumber: String,
        bundle: BundleDto,
        methodInput: String,
        parentInputs: List<String>
    ): String {
        val method = when (methodInput) {
            "1" -> "AIRTIME"
            "2" -> "MOMO"
            "3" -> "IHEREREZE"
            else -> return invalidOption(sessionId, phoneNumber, parentInputs)
        }
        if (method !in availablePaymentMethods(bundle)) return invalidOption(sessionId, phoneNumber, parentInputs)

        // Ihereze hands the customer off to MTN's loan flow rather than completing a bundle payment.
        if (method == "IHEREREZE" && languageFor(sessionId) == UssdLanguage.KINYARWANDA) {
            return "CON Kanda *100# ubone Dilu nziza!\n\nihereze redirection"
        }

        val result = paymentServiceClient.initiatePayment(
            PaymentRequestDto(
                ussdSessionId = sessionId,
                phoneNumber = phoneNumber,
                bundleId = bundle.id,
                bundleLabel = bundle.label,
                amountFrw = bundle.priceFrw,
                method = method
            )
        )
        val message = result?.resultMessage ?: "Unable to process your request right now"
        return "END ${translatedPaymentResult(message, languageFor(sessionId))}"
    }
}
