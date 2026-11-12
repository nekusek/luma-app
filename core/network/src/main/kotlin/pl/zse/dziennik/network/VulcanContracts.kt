package pl.zse.dziennik.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI
import java.security.MessageDigest
import java.time.Instant

fun createSchoolHttpClient() = HttpClient(OkHttp) {
    expectSuccess = true
    followRedirects = false
    install(HttpTimeout) { requestTimeoutMillis = 20_000; connectTimeoutMillis = 10_000 }
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
}


data class VulcanTenant(val municipality: String, val apiBase: URI, val authBase: URI) {
    init {
        require(listOf(apiBase, authBase).all { it.scheme == "https" && it.host != null && it.userInfo == null })
    }
}

sealed interface LoginMethod {
    data class OAuthPkce(val authorizeEndpoint: URI, val tokenEndpoint: URI, val clientId: String, val redirectUri: URI) : LoginMethod
    data class Adfs(val signInEndpoint: URI) : LoginMethod
    data object DeviceCertificate : LoginMethod
}


@Serializable
data class SessionTokens(val accessToken: String, val refreshToken: String?, val expiresAtEpochSeconds: Long)

data class SigningInput(val method: String, val uri: URI, val body: ByteArray, val timestamp: Instant, val nonce: String)


interface VulcanRequestSigner { suspend fun headers(input: SigningInput): Map<String, String> }

fun sha256(bytes: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(bytes)
