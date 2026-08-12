package com.mtn.yolo.ussdgatewayservice.entity

import com.mtn.yolo.ussdgatewayservice.model.UssdLanguage
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "ussd_session")
class UssdSessionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "session_id", nullable = false, length = 100)
    var sessionId: String = "",

    @Column(nullable = false, length = 15)
    var msisdn: String = "",

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ussd_session_inputs", joinColumns = [JoinColumn(name = "ussd_session_record_id")])
    @Column(name = "input_value", nullable = false, length = 50)
    var inputs: MutableList<String> = mutableListOf(),

    @Column(nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(nullable = false)
    var lastAccessedAt: Instant = Instant.now(),

    @Column(nullable = false, length = 10)
    var status: String = "ACTIVE",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var language: UssdLanguage = UssdLanguage.ENGLISH
)
