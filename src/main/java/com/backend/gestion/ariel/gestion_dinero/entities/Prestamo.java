package com.backend.gestion.ariel.gestion_dinero.entities;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "prestamos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"usuario", "pagos"})
public class Prestamo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    @JsonBackReference("usuario-prestamos")
    private Usuario usuario;

    @NotBlank(message = "La referencia o nombre del deudor es obligatoria")
    @Size(max = 150, message = "La referencia no puede superar los 150 caracteres")
    @Column(name = "referencia", nullable = false, length = 150)
    private String referencia;

    @NotNull(message = "El capital inicial es obligatorio")
    @DecimalMin(value = "0.01", message = "El capital inicial debe ser mayor a 0")
    @Column(name = "capital_inicial", precision = 12, scale = 2, nullable = false)
    private BigDecimal capitalInicial;

    @NotNull(message = "El capital actual es obligatorio")
    @DecimalMin(value = "0.00", message = "El capital actual no puede ser negativo")
    @Column(name = "capital_actual", precision = 12, scale = 2, nullable = false)
    private BigDecimal capitalActual;

    @NotNull(message = "La tasa de interés es obligatoria")
    @DecimalMin(value = "0.00", message = "La tasa de interés no puede ser negativa")
    @Column(name = "tasa_interes", precision = 5, scale = 2, nullable = false)
    private BigDecimal tasaInteres;

    @Column(name = "interes_mensual", precision = 12, scale = 2)
    private BigDecimal interesMensual;

    @Min(value = 1, message = "El día de cobro debe ser como mínimo 1")
    @Max(value = 31, message = "El día de cobro debe ser como máximo 31")
    @Column(name = "dia_cobro")
    private Integer diaCobro;

    @NotNull(message = "La fecha de inicio es obligatoria")
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Builder.Default
    @Column(name = "estado", length = 30, nullable = false)
    private String estado = "ACTIVO"; 

    @Column(name = "ultimo_mes_pagado", length = 20)
    private String ultimoMesPagado;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "prestamo", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("prestamo-pagos")
    private List<Pago> pagos = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
        if (this.estado == null || this.estado.isBlank()) {
            this.estado = "ACTIVO";
        }
        if (this.capitalActual == null && this.capitalInicial != null) {
            this.capitalActual = this.capitalInicial;
        }
        calcularInteresMensualSiEsPosible();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
        calcularInteresMensualSiEsPosible();
    }

    private void calcularInteresMensualSiEsPosible() {
        if (this.interesMensual == null && this.capitalActual != null && this.tasaInteres != null) {
            this.interesMensual = this.capitalActual
                    .multiply(this.tasaInteres)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }
    }

    // Métodos helper de conveniencia para la relación bidireccional
    public void addPago(Pago pago) {
        if (this.pagos == null) {
            this.pagos = new ArrayList<>();
        }
        this.pagos.add(pago);
        pago.setPrestamo(this);
    }

    public void removePago(Pago pago) {
        if (this.pagos != null) {
            this.pagos.remove(pago);
            pago.setPrestamo(null);
        }
    }
}
