 package com.mtn.yolo.ussdgatewayservice.service

import com.mtn.yolo.ussdgatewayservice.entity.UssdSessionEntity
import com.mtn.yolo.ussdgatewayservice.model.UssdLanguage
import com.mtn.yolo.ussdgatewayservice.model.UssdSession
import com.mtn.yolo.ussdgatewayservice.repository.UssdSessionRepository
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

@Service
class UssdSessionService(
    private val sessionRepository: UssdSessionRepository
) {
    private val sessionTtlMinutes = 5L

    fun createSession(msisdn: String, requestedId: String): UssdSession {
        // Preserve session history while ensuring this subscriber has only one
        // active USSD conversation at a time.
        sessionRepository.findByMsisdnAndStatus(msisdn, "ACTIVE").forEach { session ->
            session.status = "ENDED"
            session.lastAccessedAt = Instant.now()
            sessionRepository.save(session)
        }

        val now = Instant.now()
        return sessionRepository.save(
            UssdSessionEntity(
                sessionId = requestedId,
                msisdn = msisdn,
                createdAt = now,
                lastAccessedAt = now
            )
        ).toModel()
    }

    fun isSessionIdInUse(id: String): Boolean {
        val session = findActiveSession(id) ?: return false
        if (isExpired(session)) {
            expire(session)
            return false
        }
        return true
    }

    fun getSession(id: String): UssdSession? {
        val session = findActiveSession(id) ?: return null
        if (isExpired(session)) {
            expire(session)
            return null
        }
        session.lastAccessedAt = Instant.now()
        return sessionRepository.save(session).toModel()
    }

    fun getUnfinishedSession(msisdn: String, currentSessionId: String): UssdSession? {
        val session = sessionRepository
            .findFirstByMsisdnAndStatusOrderByLastAccessedAtDesc(msisdn, "ENDED")
            ?: return null
        if (session.sessionId == currentSessionId) return null
        if (session.inputs.isEmpty()) return null
        if (session.inputs.firstOrNull() == "__resume__") return null
        return session.toModel()
    }

    fun restoreInputs(targetSessionId: String, sourceInputs: List<String>) {
        val session = findActiveSession(targetSessionId) ?: return
        session.inputs.clear()
        session.inputs.addAll(sourceInputs)
        session.lastAccessedAt = Instant.now()
        sessionRepository.save(session)
    }
    fun replaceInputs(id: String, inputs: List<String>) {
        val session = findActiveSession(id) ?: return
        session.inputs.clear()
        session.inputs.addAll(inputs)
        session.lastAccessedAt = Instant.now()
        sessionRepository.save(session)
    }

    fun endSession(id: String, natural: Boolean = false) {
        val session = findActiveSession(id) ?: return
        session.status = "ENDED"
        if (natural) session.inputs.clear()
        session.lastAccessedAt = Instant.now()
        sessionRepository.save(session)
    }

    fun toggleLanguage(id: String): UssdLanguage? {
        val session = findActiveSession(id) ?: return null
        if (isExpired(session)) {
            expire(session)
            return null
        }
        session.language = if (session.language == UssdLanguage.ENGLISH) {
            UssdLanguage.KINYARWANDA
        } else {
            UssdLanguage.ENGLISH
        }
        session.lastAccessedAt = Instant.now()
        return sessionRepository.save(session).language
    }

    fun setLanguage(id: String, language: UssdLanguage) {
        val session = findActiveSession(id) ?: return
        session.language = language
        session.lastAccessedAt = Instant.now()
        sessionRepository.save(session)
    }

    private fun isExpired(session: UssdSessionEntity): Boolean =
        Duration.between(session.lastAccessedAt, Instant.now()).toMinutes() > sessionTtlMinutes

    private fun expire(session: UssdSessionEntity) {
        session.status = "EXPIRED"
        sessionRepository.save(session)
    }

    private fun UssdSessionEntity.toModel() = UssdSession(
        id = sessionId,
        msisdn = msisdn,
        inputs = inputs.toMutableList(),
        createdAt = createdAt,
        lastAccessedAt = lastAccessedAt,
        status = status,
        language = language
    )

    private fun findActiveSession(sessionId: String): UssdSessionEntity? =
        sessionRepository.findFirstBySessionIdAndStatusOrderByCreatedAtDesc(sessionId, "ACTIVE")
}
