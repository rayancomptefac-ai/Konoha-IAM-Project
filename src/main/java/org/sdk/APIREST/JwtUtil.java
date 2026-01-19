package org.sdk.APIREST;

import java.util.Date;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

public class JwtUtil {
    
    private static final String SECRET = "UN_JOUR_JE_SERAIS_HOKAGE";

    public static String genererToken(String nom, String id) {
        return JWT.create()
            .withSubject(id)
            .withClaim("Username", nom)
            .withExpiresAt(new Date(System.currentTimeMillis() + 3_600_000))
            .sign(Algorithm.HMAC256(SECRET));
    }

    public static String validerlebadge(String Badge) {
        try {
            return JWT.require(Algorithm.HMAC256(SECRET))
                .build()
                .verify(Badge)
                .getSubject();
                
        } catch (Exception e) {
            return null;
        }
    }
}