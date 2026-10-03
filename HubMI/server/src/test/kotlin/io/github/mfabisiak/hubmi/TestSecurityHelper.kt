package io.github.mfabisiak.hubmi

import com.auth0.jwk.Jwk
import com.auth0.jwk.JwkProvider
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.github.mfabisiak.hubmi.api.Role
import org.koin.dsl.module
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.Base64
import java.util.Date

object TestSecurityHelper {
    private val keyPair =
        KeyPairGenerator
            .getInstance("RSA")
            .apply { initialize(2048) }
            .generateKeyPair()

    val publicKey: RSAPublicKey = keyPair.public as RSAPublicKey
    val privateKey: RSAPrivateKey = keyPair.private as RSAPrivateKey

    const val KEY_ID = "test-key-id"
    const val ISSUER = "http://localhost:8081/realms/hubmi"

    private val nBytes: String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(publicKey.modulus.toByteArray())
    private val eBytes: String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(publicKey.publicExponent.toByteArray())

    val testJwkProvider =
        JwkProvider { _ ->
            Jwk.fromValues(
                mapOf(
                    "kty" to "RSA",
                    "alg" to "RS256",
                    "use" to "sig",
                    "kid" to KEY_ID,
                    "n" to nBytes,
                    "e" to eBytes,
                ),
            )
        }

    val testSecurityModule =
        module {
            single<JwkProvider> { testJwkProvider }
        }

    fun generateToken(
        userId: String = "test-user-id",
        username: String = "test-user",
        email: String = "test@hubmi.local",
        roles: Set<Role> = setOf(Role.USER),
    ): String =
        JWT
            .create()
            .withKeyId(KEY_ID)
            .withIssuer(ISSUER)
            .withSubject(userId)
            .withClaim("preferred_username", username)
            .withClaim("email", email)
            .withClaim("realm_access", mapOf("roles" to roles.map { it.keycloakName }))
            .withExpiresAt(Date(System.currentTimeMillis() + 3600_000))
            .sign(Algorithm.RSA256(publicKey, privateKey))
}
