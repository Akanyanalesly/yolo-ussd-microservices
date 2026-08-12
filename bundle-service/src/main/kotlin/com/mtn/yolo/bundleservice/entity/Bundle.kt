package com.mtn.yolo.bundleservice.entity

import jakarta.persistence.*

/**
 * A single purchasable bundle/offer shown somewhere in the Yolo USSD menu tree.
 *
 * category    - top-level root menu branch, e.g. "gwamon", "yolo-voice", "yolo-internet",
 *               "social-media", "desade", "foleva", "loyalty-redeem"
 * subcategory - the intermediate menu level where one exists, e.g. duration
 *               ("daily","weekly","monthly","desade","hourly") for yolo-internet,
 *               platform ("whatsapp","facebook-instagram") for social-media,
 *               or loyalty category ("entertainment","restaurants","fashion","lifestyle")
 *               null when the branch has no intermediate menu (e.g. gwamon, desade, foleva)
 * optionNumber - the digit the subscriber types to pick this bundle. Usually sequential
 *               from 1, but NOT always — e.g. "Fazenda Sengha" only offers option "2" (MoMo)
 *               with no "1" shown, so this field is explicit rather than assumed.
 */
@Entity
@Table(name = "bundles")
data class Bundle(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val category: String,

    @Column(nullable = true)
    val subcategory: String? = null,

    @Column(nullable = false)
    val optionNumber: String,

    @Column(nullable = false, length = 255)
    val label: String,

    @Column(nullable = false)
    val priceFrw: Int,

    @Column(nullable = true)
    val dataAmount: String? = null,

    @Column(nullable = true)
    val minutes: String? = null,

    @Column(nullable = true)
    val validityPeriod: String? = null,

    /** Optional descriptive line shown above the numbered options, e.g. DesaDe's
     *  "DesaDe is used for MTN to MTN calls only" */
    @Column(nullable = true, length = 255)
    val infoLine: String? = null,

    @Column(nullable = false)
    val restrictedToWeekend: Boolean = false,

    /** Whether this bundle is currently purchasable (mirrors the "not yet available"
     *  merchants seen under Fashion/Lifestyle, which only show "Go back"). */
    @Column(nullable = false)
    val active: Boolean = true,

    @ElementCollection(targetClass = PaymentMethod::class, fetch = FetchType.EAGER)
    @CollectionTable(name = "bundle_payment_methods", joinColumns = [JoinColumn(name = "bundle_id")])
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    val paymentMethods: List<PaymentMethod> = emptyList()
)
