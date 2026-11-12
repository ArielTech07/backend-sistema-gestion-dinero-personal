package com.backend.gestion.ariel.gestion_dinero.services;

import java.util.List;
import java.util.Optional;

import com.backend.gestion.ariel.gestion_dinero.entities.Pago;

public interface PagoService {
    List<Pago> listarTodos();
    List<Pago> listarPorPrestamo(Long prestamoId);
    Optional<Pago> buscarPorId(Long id);
    Pago registrarPago(Long prestamoId, Pago pago);
    void eliminar(Long id);
}

