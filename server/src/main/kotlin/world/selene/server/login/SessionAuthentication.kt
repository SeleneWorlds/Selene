package world.selene.server.login

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import javax.crypto.KeyGenerator

class SessionAuthentication {

    val issuer = "selene.server"
    val audience = "selene.client"
    private val secretKey = KeyGenerator.getInstance("HmacSHA256").let {
        it.init(256)
        it.generateKey()
    }
    val algorithm: Algorithm = Algorithm.HMAC256(secretKey.encoded)

    fun createToken(sub: String): String {
        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject(sub)
            .sign(algorithm)
    }

}