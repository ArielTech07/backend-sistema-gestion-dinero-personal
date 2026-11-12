package com.backend.gestion.ariel.gestion_dinero.security;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Mixin para que Jackson pueda deserializar los GrantedAuthority
 * almacenados dentro de los claims del token JWT.
 */
public abstract class SimpleGrantedAuthorityJsonCreator {

    @JsonCreator
    public SimpleGrantedAuthorityJsonCreator(@JsonProperty("authority") String role) {
    }
}

