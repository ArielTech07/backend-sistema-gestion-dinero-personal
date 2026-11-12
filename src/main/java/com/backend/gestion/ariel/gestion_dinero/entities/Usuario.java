package com.backend.gestion.ariel.gestion_dinero.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "prestamos")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Debe proporcionar un email válido")
    @Size(max = 150, message = "El email no puede superar los 150 caracteres")
    @Column(name = "email", nullable = false, length = 150, unique = true)
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // Seguridad: no expone la contraseña en las respuestas JSON
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Builder.Default
    @Column(name = "moneda_default", length = 5)
    private String monedaDefault = "S/";

    @Builder.Default
    @Column(name = "tasa_default", precision = 5, scale = 2)
    private BigDecimal tasaDefault = new BigDecimal("10.00");

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore // Evita serialización circular o LazyInitializationException al listar usuarios
    private List<Prestamo> prestamos = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.monedaDefault == null) {
            this.monedaDefault = "S/";
        }
        if (this.tasaDefault == null) {
            this.tasaDefault = new BigDecimal("10.00");
        }
    }

    // Métodos helper de conveniencia para la relación bidireccional
    public void addPrestamo(Prestamo prestamo) {
        if (this.prestamos == null) {
            this.prestamos = new ArrayList<>();
        }
        this.prestamos.add(prestamo);
        prestamo.setUsuario(this);
    }

    public void removePrestamo(Prestamo prestamo) {
        if (this.prestamos != null) {
            this.prestamos.remove(prestamo);
            prestamo.setUsuario(null);
        }
    }
}
