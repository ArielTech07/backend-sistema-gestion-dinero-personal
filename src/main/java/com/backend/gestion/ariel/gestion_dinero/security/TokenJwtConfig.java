package com.backend.gestion.ariel.gestion_dinero.security;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;

import io.jsonwebtoken.security.Keys;

/**
 * Constantes y configuración de JWT:
 * Define la clave de firma HMAC-SHA256 y los prefijos estándar de autenticación.
 * 
 * En producción toma automáticamente la variable de entorno JWT_SECRET si está definida,
 * o utiliza la clave de respaldo local para desarrollo.
 */
public class TokenJwtConfig {
    private static final String DEFAULT_SECRET = "ClaveSecretaSuperSeguraParaGestionDineroAriel2026_JWT_Key!";

    private static String getSecret() {
        String envSecret = System.getenv("JWT_SECRET");
        if (envSecret != null && envSecret.trim().length() >= 32) {
            return envSecret.trim();
        }
        return DEFAULT_SECRET;
    }

    // Clave secreta segura de 256 bits para firmar y validar tokens JWT
    public static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(
            getSecret().getBytes(StandardCharsets.UTF_8));

    public static final String PREFIX_TOKEN = "Bearer ";
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String CONTENT_TYPE = "application/json";

    // Tiempo de expiración del token: 8 horas (en milisegundos)
    public static final long EXPIRATION_TIME = 8 * 60 * 60 * 1000L;
}
