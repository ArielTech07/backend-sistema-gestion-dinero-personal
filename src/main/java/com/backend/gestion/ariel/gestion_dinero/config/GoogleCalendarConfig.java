package com.backend.gestion.ariel.gestion_dinero.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class GoogleCalendarConfig {

    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarConfig.class);

    private final ResourceLoader resourceLoader;

    @Value("${google.calendar.credentials-path:classpath:google-calendar-credentials.json}")
    private String credentialsPath;

    @Bean
    public Calendar googleCalendarService() {
        try {
            InputStream credentialsStream = null;

            // 1. Prioridad: Revisar si existe variable de entorno con credenciales en Base64 (ideal para Render)
            String base64Credentials = System.getenv("GOOGLE_CALENDAR_CREDENTIALS_BASE64");
            if (base64Credentials != null && !base64Credentials.isBlank()) {
                byte[] decodedBytes = Base64.getDecoder().decode(base64Credentials.trim());
                credentialsStream = new ByteArrayInputStream(decodedBytes);
                log.info("Cargando credenciales de Google Calendar desde variable de entorno Base64.");
            } else {
                // 2. Si no, cargar desde el archivo configurado (localmente)
                Resource resource = resourceLoader.getResource(credentialsPath);
                if (resource.exists()) {
                    credentialsStream = resource.getInputStream();
                    log.info("Cargando credenciales de Google Calendar desde recurso: {}", credentialsPath);
                } else {
                    log.warn("Archivo de credenciales de Google Calendar no encontrado en: {}. El servicio de Calendar funcionará en modo inactivo o fallback.", credentialsPath);
                    return null;
                }
            }

            GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream)
                    .createScoped(Collections.singletonList(CalendarScopes.CALENDAR));

            NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            GsonFactory jsonFactory = GsonFactory.getDefaultInstance();

            return new Calendar.Builder(httpTransport, jsonFactory, new HttpCredentialsAdapter(credentials))
                    .setApplicationName("Gestion Dinero Backend")
                    .build();

        } catch (IOException | GeneralSecurityException e) {
            log.error("Error al inicializar el cliente de Google Calendar: {}", e.getMessage());
            return null;
        }
    }
}
