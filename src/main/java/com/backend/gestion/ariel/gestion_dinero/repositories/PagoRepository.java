package com.backend.gestion.ariel.gestion_dinero.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.backend.gestion.ariel.gestion_dinero.entities.Pago;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    List<Pago> findByPrestamoId(Long prestamoId);
    List<Pago> findByPrestamoIdOrderByFechaDesc(Long prestamoId);
}
