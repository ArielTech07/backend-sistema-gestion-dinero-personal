package com.backend.gestion.ariel.gestion_dinero.config;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.backend.gestion.ariel.gestion_dinero.entities.Usuario;
import com.backend.gestion.ariel.gestion_dinero.repositories.UsuarioRepository;

/**
 * Inicializador de configuración básica:
 */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Value("${app.admin.email:#{null}}")
    private String adminEmail;

    @Value("${app.admin.password:#{null}}")
    private String adminPassword;

    @Bean
    public CommandLineRunner initData(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Si no se configuraron variables de inicio de sesión, no se crea ningún
            // usuario por defecto
            if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
                log.info("No se han configurado las credenciales de Administrador.");
                return;
            }

            if (usuarioRepository.count() == 0) {
                usuarioRepository.save(
                        Usuario.builder()
                                .nombre("Administrador")
                                .email(adminEmail.trim())
                                .passwordHash(passwordEncoder.encode(adminPassword))
                                .monedaDefault("S/")
                                .tasaDefault(new BigDecimal("10.00"))
                                .build());
                log.info("Usuario inicial creado con éxito para el email configurado.");
            } else {
                // Si el usuario existe pero la contraseña está en texto plano, la actualiza con
                // hash BCrypt
                usuarioRepository.findByEmail(adminEmail.trim()).ifPresent(user -> {
                    if (user.getPasswordHash() != null && !user.getPasswordHash().startsWith("$2a$")) {
                        user.setPasswordHash(passwordEncoder.encode(adminPassword));
                        usuarioRepository.save(user);
                        log.info("Hash BCrypt actualizado para el usuario inicial.");
                    }
                });
            }
        };
    }
}
