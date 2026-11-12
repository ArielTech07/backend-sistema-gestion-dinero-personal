package com.backend.gestion.ariel.gestion_dinero.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.backend.gestion.ariel.gestion_dinero.entities.Prestamo;

public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {

    // EntityGraph carga la lista de pagos en la misma consulta para evitar
    // problemas de LazyInitializationException y optimizar el rendimiento.
    @Override
    @EntityGraph(attributePaths = {"pagos"})
    List<Prestamo> findAll();

    @Override
    @EntityGraph(attributePaths = {"pagos"})
    Optional<Prestamo> findById(Long id);

    @EntityGraph(attributePaths = {"pagos"})
    List<Prestamo> findByUsuarioId(Long usuarioId);

    @EntityGraph(attributePaths = {"pagos"})
    List<Prestamo> findByUsuarioIdAndEstado(Long usuarioId, String estado);
}
