package com.mtn.yolo.ussdgatewayservice.service

import org.springframework.stereotype.Service

@Service
class FaqService {
    private val entries = listOf(
        setOf("iherereze", "ihe reze", "borrow airtime", "loan") to
            "Ihereze lets eligible customers borrow airtime. Dial *151# to check your loan balance or eligibility.",
        setOf("gwamon weekend", "gwamon' weekend", "weekend gwamon") to
            "Gwam'on Weekend is the Gwam'on offer available from Friday to Sunday.",
        setOf("gwamon", "gwamon'") to
            "Gwamon' offers value bundles, including special weekend offers that are available from Friday to Sunday.",
        setOf("what does yolo voice do", "what is yolo voice", "yolo voice") to
            "YOLO Voice lets you browse and buy voice bundles for calls.",
        setOf("what does yolo internet do", "what is yolo internet", "yolo internet") to
            "YOLO Internet lets you browse data bundles by duration, including daily, weekly, monthly, DesaDe, and hourly options.",
        setOf("social media bundle", "social media bundles", "whatsapp bundle", "facebook bundle", "instagram bundle") to
            "Social Media Bundles provide short-term bundles for WhatsApp or Facebook and Instagram.",
        setOf("what is desade", "what does desade do", "desade") to
            "DesaDe provides MTN-to-MTN calling bundles, including options that combine minutes and data. The bundles are valid for 48 hours.",
        setOf("what is foleva", "what does foleva do", "foleva") to
            "FoLeva provides combined data and calling bundles that remain valid until the included data is used up.",
        setOf("balance check", "check my balance", "what does balance check do") to
            "Balance check shows your active-pack information. If you have no active pack, the menu directs you to dial *154# to buy another one.",
        setOf("balance", "remaining", "active pack", "pack") to
            "Choose Balance check from the main menu to see your active-pack information.",
        setOf("payment", "pay", "momo", "airtime") to
            "Bundle payments may be made with Airtime, MoMo, or Ihereze where that option is available.",
        setOf("redeem loyalty points", "redeem loyalty point", "redeeming loyalty points", "redeeming loyalty point") to
            "Redeem Loyalty Points opens the YOLO Star partner categories where available loyalty rewards can be selected.",
        setOf("yolo star partner", "yolo star partners", "what are the partners") to
            "YOLO Star Partners lets you browse partner offers connected to the loyalty programme.",
        setOf("join yolo star", "what does join yolo star do") to
            "Join YOLO Star registers you for the YOLO Star programme. If you are already registered, the menu confirms that you are already in YOLO Star.",
        setOf("yolo star account", "my yolo star account", "account details", "loyalty point balance") to
            "My YOLO Star Account shows your membership category, loyalty points, and the next category you can reach.",
        setOf("other info", "other information", "what does other info do") to
            "Other info opens the additional YOLO Star information option.",
        setOf("yolo star", "loyalty", "points") to
            "YOLO Star lets you view membership details, loyalty points, partners, and redemption options.",
        setOf("voice", "call", "minutes") to
            "YOLO Voice bundles provide calling minutes. Open YOLO Voice in the menu to view the current options.",
        setOf("internet", "data", "mb", "gb") to
            "YOLO Internet has data bundles by duration. Open YOLO Internet to browse the available offers.",
        setOf("language", "kinyarwanda", "english") to
            "Select Change Language from the main menu to switch between English and Kinyarwanda."
    )

    fun findAnswer(question: String): String {
        val normalized = question.lowercase().trim()
        return entries.firstOrNull { (keywords, _) -> keywords.any(normalized::contains) }?.second
            ?: "I can help you navigate YOLO bundles, payments, Ihereze, balance checks, and YOLO Star. Please try asking about one of those."
    }
}
