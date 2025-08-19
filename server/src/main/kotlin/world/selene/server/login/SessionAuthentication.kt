package world.selene.server.login

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import world.selene.server.config.ServerConfig
import javax.crypto.KeyGenerator

class SessionAuthentication(private val config: ServerConfig) {

    data class TokenData(val userId: String)

    val issuer = "selene.server"
    val audience = "selene.client"
    private val secretKey = KeyGenerator.getInstance("HmacSHA256").let {
        it.init(256)
        it.generateKey()
    }
    val algorithm: Algorithm = Algorithm.HMAC256(secretKey.encoded)

    fun createToken(data: TokenData): String {
        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject(data.userId)
            .sign(algorithm)
    }

    fun parseToken(token: String): TokenData {
        try {
            val decoded = if (config.insecureMode)
                JWT.decode(token)
            else JWT.require(algorithm)
                .withIssuer(issuer)
                .withAudience(audience)
                .build()
                .verify(token)
            return TokenData(decoded.subject)
        } catch (e: Exception) {
            if (config.insecureMode) {
                return TokenData(token)
            }
            throw e
        }
    }

}