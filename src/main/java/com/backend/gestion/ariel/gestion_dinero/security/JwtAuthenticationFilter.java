package com.backend.gestion.ariel.gestion_dinero.security;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.backend.gestion.ariel.gestion_dinero.entities.Usuario;
import com.backend.gestion.ariel.gestion_dinero.repositories.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtro de autenticación para el endpoint de Login (/api/usuarios/login):
 * Valida email y contraseña contra la base de datos y genera el token JWT.
 */
public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthenticationFilter(AuthenticationManager authenticationManager, UsuarioRepository usuarioRepository) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;

        //ruta exacta de la api para el back
        setFilterProcessesUrl("/api/usuarios/login");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        String email = "";
        String password = "";

        try {
            LoginRequest loginRequest = new ObjectMapper().readValue(request.getInputStream(), LoginRequest.class);
            email = loginRequest.getEmail();
            password = loginRequest.getPassword();
        } catch (IOException e) {
            e.printStackTrace();
        }

        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(email, password);
        return authenticationManager.authenticate(authenticationToken);
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
            Authentication authResult) throws IOException, ServletException {

        org.springframework.security.core.userdetails.User userDetails =
                (org.springframework.security.core.userdetails.User) authResult.getPrincipal();

        String email = userDetails.getUsername();
        List<String> roles = authResult.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        // Creamos los claims del token con la lista de nombres de roles
        Claims claims = Jwts.claims()
                .add("authorities", new ObjectMapper().writeValueAsString(roles))
                .add("username", email)
                .build();

        // Generamos el token firmado con JJWT
        String token = Jwts.builder()
                .subject(email)
                .claims(claims)
                .expiration(new Date(System.currentTimeMillis() + TokenJwtConfig.EXPIRATION_TIME))
                .issuedAt(new Date())
                .signWith(TokenJwtConfig.SECRET_KEY)
                .compact();

        // Añadimos el token al header de respuesta
        response.addHeader(TokenJwtConfig.HEADER_AUTHORIZATION, TokenJwtConfig.PREFIX_TOKEN + token);

        // Cargamos los datos del usuario en BD para devolverlos al frontend
        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);

        Map<String, Object> body = new HashMap<>();
        body.put("token", token);
        body.put("message", String.format("¡Hola %s, has iniciado sesión con éxito!", usuario != null ? usuario.getNombre() : email));
        
        if (usuario != null) {
            body.put("id", usuario.getId());
            body.put("nombre", usuario.getNombre());
            body.put("email", usuario.getEmail());
            body.put("monedaDefault", usuario.getMonedaDefault());
            body.put("tasaDefault", usuario.getTasaDefault());
        }

        response.getWriter().write(new ObjectMapper().writeValueAsString(body));
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(TokenJwtConfig.CONTENT_TYPE);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException failed) throws IOException, ServletException {

        Map<String, String> body = new HashMap<>();
        body.put("message", "Error de autenticación: correo o contraseña incorrectos");
        body.put("error", failed.getMessage());

        response.getWriter().write(new ObjectMapper().writeValueAsString(body));
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(TokenJwtConfig.CONTENT_TYPE);
    }

    /**
     * DTO interno para capturar credenciales (acepta tanto 'email' como 'username')
     */
    private static class LoginRequest {
        private String email;
        private String username;
        private String password;

        public String getEmail() {
            if (email != null && !email.isBlank()) {
                return email;
            }
            return username != null ? username : "";
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password != null ? password : "";
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
