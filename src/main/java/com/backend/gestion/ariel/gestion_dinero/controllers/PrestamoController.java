package com.backend.gestion.ariel.gestion_dinero.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.backend.gestion.ariel.gestion_dinero.entities.Prestamo;
import com.backend.gestion.ariel.gestion_dinero.services.PrestamoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/prestamos")
@RequiredArgsConstructor
public class PrestamoController {

    private final PrestamoService prestamoService;

    @GetMapping
    public ResponseEntity<List<Prestamo>> listarTodos() {
        return ResponseEntity.ok(prestamoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Prestamo> buscarPorId(@PathVariable Long id) {
        return prestamoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<Prestamo>> listarPorUsuario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(prestamoService.listarPorUsuario(usuarioId));
    }

    @GetMapping("/usuario/{usuarioId}/filtro")
    public ResponseEntity<List<Prestamo>> listarPorUsuarioYEstado(
            @PathVariable Long usuarioId,
            @RequestParam(required = false) String estado) {
        if (estado != null && !estado.isBlank()) {
            return ResponseEntity.ok(prestamoService.listarPorUsuarioYEstado(usuarioId, estado));
        }
        return ResponseEntity.ok(prestamoService.listarPorUsuario(usuarioId));
    }

    @PostMapping("/usuario/{usuarioId}")
    public ResponseEntity<Prestamo> crearPrestamo(
            @PathVariable Long usuarioId,
            @Valid @RequestBody Prestamo prestamo) {
        Prestamo nuevoPrestamo = prestamoService.crearPrestamo(usuarioId, prestamo);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoPrestamo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Prestamo> actualizarPrestamo(
            @PathVariable Long id,
            @Valid @RequestBody Prestamo prestamo) {
        Prestamo prestamoActualizado = prestamoService.actualizar(id, prestamo);
        return ResponseEntity.ok(prestamoActualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarPrestamo(@PathVariable Long id) {
        prestamoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

