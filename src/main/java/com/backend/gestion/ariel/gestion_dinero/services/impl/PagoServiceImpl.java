package com.backend.gestion.ariel.gestion_dinero.services.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backend.gestion.ariel.gestion_dinero.entities.Pago;
import com.backend.gestion.ariel.gestion_dinero.entities.Prestamo;
import com.backend.gestion.ariel.gestion_dinero.repositories.PagoRepository;
import com.backend.gestion.ariel.gestion_dinero.repositories.PrestamoRepository;
import com.backend.gestion.ariel.gestion_dinero.services.GoogleCalendarService;
import com.backend.gestion.ariel.gestion_dinero.services.PagoService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PagoServiceImpl implements PagoService {

    private final PagoRepository pagoRepository;
    private final PrestamoRepository prestamoRepository;
    private final GoogleCalendarService googleCalendarService;

    @Override
    @Transactional(readOnly = true)
    public List<Pago> listarTodos() {
        return pagoRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pago> listarPorPrestamo(Long prestamoId) {
        return pagoRepository.findByPrestamoIdOrderByFechaDesc(prestamoId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Pago> buscarPorId(Long id) {
        return pagoRepository.findById(id);
    }

    @Override
    @Transactional
    public Pago registrarPago(Long prestamoId, Pago pago) {
        Prestamo prestamo = prestamoRepository.findById(prestamoId)
                .orElseThrow(() -> new IllegalArgumentException("Préstamo no encontrado con ID: " + prestamoId));

        pago.setPrestamo(prestamo);

        String tipoPago = pago.getTipo() != null ? pago.getTipo().toUpperCase() : "";

        // Si el pago es de tipo LIQUIDACION o TOTAL, salda completamente el préstamo
        if ("LIQUIDACION".equals(tipoPago) || "TOTAL".equals(tipoPago)) {
            prestamo.setCapitalActual(BigDecimal.ZERO);
            prestamo.setInteresMensual(BigDecimal.ZERO);
            prestamo.setEstado("LIQUIDADO");
            eliminarEventoCalendarSiExiste(prestamo);
        } else if ("CAPITAL".equals(tipoPago) || "ABONO_CAPITAL".equals(tipoPago)) {
            BigDecimal capitalActual = prestamo.getCapitalActual();

            if (pago.getMonto().compareTo(capitalActual) > 0) {
                throw new IllegalArgumentException(
                        "El monto a amortizar (" + pago.getMonto() + ") supera el capital actual (" + capitalActual + ")");
            }

            BigDecimal nuevoCapital = capitalActual.subtract(pago.getMonto());
            prestamo.setCapitalActual(nuevoCapital);

            // Si el préstamo se salda completamente con el abono
            if (nuevoCapital.compareTo(BigDecimal.ZERO) <= 0) {
                prestamo.setCapitalActual(BigDecimal.ZERO);
                prestamo.setInteresMensual(BigDecimal.ZERO);
                prestamo.setEstado("LIQUIDADO");
                eliminarEventoCalendarSiExiste(prestamo);
            } else {
                // Recalcular interés mensual según el nuevo capital pendiente
                BigDecimal nuevoInteres = nuevoCapital
                        .multiply(prestamo.getTasaInteres())
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                prestamo.setInteresMensual(nuevoInteres);
                // Actualizar montos en la descripción del evento de Calendar
                try {
                    googleCalendarService.actualizarEvento(prestamo);
                } catch (Exception e) {
                    System.err.println("No se pudo actualizar evento de Calendar tras abono a capital: " + e.getMessage());
                }
            }
        }

        // Si se especificó el mes cobrado (interés o cuota mensual), actualizar en el préstamo
        if (pago.getMesCobrado() != null && !pago.getMesCobrado().isBlank()) {
            prestamo.setUltimoMesPagado(pago.getMesCobrado());
        }

        prestamoRepository.save(prestamo);
        return pagoRepository.save(pago);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        if (!pagoRepository.existsById(id)) {
            throw new IllegalArgumentException("Pago no encontrado con ID: " + id);
        }
        pagoRepository.deleteById(id);
    }

    private void eliminarEventoCalendarSiExiste(Prestamo prestamo) {
        if (prestamo.getGoogleCalendarEventId() != null && !prestamo.getGoogleCalendarEventId().isBlank()) {
            try {
                googleCalendarService.eliminarEvento(prestamo.getGoogleCalendarEventId());
                prestamo.setGoogleCalendarEventId(null);
            } catch (Exception e) {
                System.err.println("No se pudo eliminar evento de Calendar al liquidar préstamo: " + e.getMessage());
            }
        }
    }
}

