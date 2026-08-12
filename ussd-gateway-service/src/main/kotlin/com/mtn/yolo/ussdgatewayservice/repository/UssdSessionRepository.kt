package com.mtn.yolo.ussdgatewayservice.repository

import com.mtn.yolo.ussdgatewayservice.entity.UssdSessionEntity
import org.springframework.data.jpa.repository.JpaRepository

interface UssdSessionRepository : JpaRepository<UssdSessionEntity, Long> {
    fun findByMsisdnAndStatus(msisdn: String, status: String): List<UssdSessionEntity>
    fun findFirstBySessionIdAndStatusOrderByCreatedAtDesc(sessionId: String, status: String): UssdSessionEntity?
    fun findFirstByMsisdnAndStatusOrderByLastAccessedAtDesc(msisdn: String, status: String): UssdSessionEntity?
}
