package com.backend.gestion.ariel.gestion_dinero.services;

import java.util.List;
import java.util.Optional;

import com.backend.gestion.ariel.gestion_dinero.entities.Prestamo;

public interface PrestamoService {
    List<Prestamo> listarTodos();
    List<Prestamo> listarPorUsuario(Long usuarioId);
    List<Prestamo> listarPorUsuarioYEstado(Long usuarioId, String estado);
    Optional<Prestamo> buscarPorId(Long id);
    Prestamo crearPrestamo(Long usuarioId, Prestamo prestamo);
    Prestamo actualizar(Long id, Prestamo prestamo);
    void eliminar(Long id);
}

