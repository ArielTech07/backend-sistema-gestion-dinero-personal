package com.backend.gestion.ariel.gestion_dinero.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.gestion.ariel.gestion_dinero.entities.Pago;
import com.backend.gestion.ariel.gestion_dinero.services.PagoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class PagoController {

    private final PagoService pagoService;

    @GetMapping
    public ResponseEntity<List<Pago>> listarTodos() {
        return ResponseEntity.ok(pagoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pago> buscarPorId(@PathVariable Long id) {
        return pagoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/prestamo/{prestamoId}")
    public ResponseEntity<List<Pago>> listarPorPrestamo(@PathVariable Long prestamoId) {
        return ResponseEntity.ok(pagoService.listarPorPrestamo(prestamoId));
    }

    @PostMapping("/prestamo/{prestamoId}")
    public ResponseEntity<Pago> registrarPago(
            @PathVariable Long prestamoId,
            @Valid @RequestBody Pago pago) {
        Pago nuevoPago = pagoService.registrarPago(prestamoId, pago);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoPago);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarPago(@PathVariable Long id) {
        pagoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

