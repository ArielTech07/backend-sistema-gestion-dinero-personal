package com.backend.gestion.ariel.gestion_dinero.services.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

import org.hibernate.jdbc.Expectations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backend.gestion.ariel.gestion_dinero.entities.Prestamo;
import com.backend.gestion.ariel.gestion_dinero.entities.Usuario;
import com.backend.gestion.ariel.gestion_dinero.repositories.PrestamoRepository;
import com.backend.gestion.ariel.gestion_dinero.repositories.UsuarioRepository;
import com.backend.gestion.ariel.gestion_dinero.services.GoogleCalendarService;
import com.backend.gestion.ariel.gestion_dinero.services.PrestamoService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PrestamoServiceImpl implements PrestamoService {

    private final PrestamoRepository prestamoRepository;
    private final UsuarioRepository usuarioRepository;
    private final GoogleCalendarService googleCalendarService;

    @Override
    @Transactional(readOnly = true)
    public List<Prestamo> listarTodos() {
        return prestamoRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Prestamo> listarPorUsuario(Long usuarioId) {
        return prestamoRepository.findByUsuarioId(usuarioId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Prestamo> listarPorUsuarioYEstado(Long usuarioId, String estado) {
        return prestamoRepository.findByUsuarioIdAndEstado(usuarioId, estado);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Prestamo> buscarPorId(Long id) {
        return prestamoRepository.findById(id);
    }

    @Override
    @Transactional
    public Prestamo crearPrestamo(Long usuarioId, Prestamo prestamo) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + usuarioId));

        prestamo.setUsuario(usuario);

        // Si no se definió capital actual, parte del capital inicial
        if (prestamo.getCapitalActual() == null) {
            prestamo.setCapitalActual(prestamo.getCapitalInicial());
        }

        // Si no se asignó tasa, tomar la tasa default configurada en el usuario
        if (prestamo.getTasaInteres() == null && usuario.getTasaDefault() != null) {
            prestamo.setTasaInteres(usuario.getTasaDefault());
        }

        
        if (prestamo.getCapitalActual() != null && prestamo.getTasaInteres() != null) {
            BigDecimal interes = prestamo.getCapitalActual()
                    .multiply(prestamo.getTasaInteres())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            prestamo.setInteresMensual(interes);
        }

        if (prestamo.getEstado() == null || prestamo.getEstado().isBlank()) {
            prestamo.setEstado("ACTIVO");
        }

        Prestamo saved = prestamoRepository.save(prestamo);

        try {
            String eventId = googleCalendarService.crearEventoCobro(saved);
            saved.setGoogleCalendarEventId(eventId);
            prestamoRepository.save(saved);
        } catch (Exception e) {
            System.err.println("No se pudo crear evento en Calendar " + e.getMessage());
        }

        return saved;
    }

    @Override
    @Transactional
    public Prestamo actualizar(Long id, Prestamo datosActualizados) {
        Prestamo prestamo = prestamoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Préstamo no encontrado con ID: " + id));

        prestamo.setReferencia(datosActualizados.getReferencia());
        prestamo.setDiaCobro(datosActualizados.getDiaCobro());
        prestamo.setFechaInicio(datosActualizados.getFechaInicio());

        if (datosActualizados.getEstado() != null && !datosActualizados.getEstado().isBlank()) {
            prestamo.setEstado(datosActualizados.getEstado());
            // Si el préstamo pasa a PAGADO, LIQUIDADO o CANCELADO, eliminar evento de Google Calendar
            if ("PAGADO".equalsIgnoreCase(datosActualizados.getEstado())
                    || "LIQUIDADO".equalsIgnoreCase(datosActualizados.getEstado())
                    || "CANCELADO".equalsIgnoreCase(datosActualizados.getEstado())) {
                if (prestamo.getGoogleCalendarEventId() != null) {
                    try {
                        googleCalendarService.eliminarEvento(prestamo.getGoogleCalendarEventId());
                        prestamo.setGoogleCalendarEventId(null);
                    } catch (Exception e) {
                        System.err.println("No se pudo eliminar evento al cambiar estado: " + e.getMessage());
                    }
                }
            }
        }

        if (datosActualizados.getTasaInteres() != null) {
            prestamo.setTasaInteres(datosActualizados.getTasaInteres());
            // Recalcular interés mensual con la nueva tasa
            if (prestamo.getCapitalActual() != null) {
                BigDecimal interes = prestamo.getCapitalActual()
                        .multiply(prestamo.getTasaInteres())
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                prestamo.setInteresMensual(interes);
            }
        }

        Prestamo actualizado = prestamoRepository.save(prestamo);

        // Si sigue activo, sincronizar cambios en el evento de Calendar
        if (!"PAGADO".equalsIgnoreCase(actualizado.getEstado())
                && !"LIQUIDADO".equalsIgnoreCase(actualizado.getEstado())
                && !"CANCELADO".equalsIgnoreCase(actualizado.getEstado())) {
            try {
                googleCalendarService.actualizarEvento(actualizado);
            } catch (Exception e) {
                System.err.println("No se pudo actualizar evento en Calendar: " + e.getMessage());
            }
        }

        return actualizado;
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        if (!prestamoRepository.existsById(id)) {
            throw new IllegalArgumentException("Préstamo no encontrado con ID: " + id);
        }
        Prestamo prestamo=prestamoRepository.findById(id).orElseThrow();
        
        if (prestamo.getGoogleCalendarEventId()!=null) {
            
            try{
                googleCalendarService.eliminarEvento(prestamo.getGoogleCalendarEventId());
            }catch(Exception e){
                System.err.println("No se pudo eliminar evento de Calendar "+e.getMessage());
            }
        }
        prestamoRepository.deleteById(id);
    }
}
