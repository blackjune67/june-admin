package com.dh.admin.infrastructure.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.access-token-expiration}") private val accessTokenExpiration: Long,
    @Value("\${jwt.refresh-token-expiration}") private val refreshTokenExpiration: Long
) {

    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    fun generateAccessToken(userId: Long, email: String, roles: List<String>): String =
        generateToken(userId, email, roles, accessTokenExpiration)

    fun generateRefreshToken(userId: Long, email: String, roles: List<String>): String =
        generateToken(userId, email, roles, refreshTokenExpiration)

    fun getRefreshTokenExpiration(): Long = refreshTokenExpiration

    fun getUserId(token: String): Long = parseClaims(token).subject.toLong()

    fun getEmail(token: String): String = parseClaims(token)["email"] as String

    @Suppress("UNCHECKED_CAST")
    fun getRoles(token: String): List<String> =
        parseClaims(token)["roles"] as? List<String> ?: emptyList()

    fun validateToken(token: String): Boolean {
        return try {
            parseClaims(token)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun isExpired(token: String): Boolean {
        return try {
            parseClaims(token).expiration.before(Date())
        } catch (e: ExpiredJwtException) {
            true
        }
    }

    private fun generateToken(userId: Long, email: String, roles: List<String>, expiration: Long): String {
        val now = Date()
        val expiryDate = Date(now.time + expiration)

        return Jwts.builder()
            .subject(userId.toString())
            .claim("email", email)
            .claim("roles", roles)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(key)
            .compact()
    }

    private fun parseClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}
