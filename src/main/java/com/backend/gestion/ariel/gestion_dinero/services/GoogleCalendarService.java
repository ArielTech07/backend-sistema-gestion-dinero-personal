package com.backend.gestion.ariel.gestion_dinero.services;

import com.backend.gestion.ariel.gestion_dinero.entities.Prestamo;

public interface GoogleCalendarService {
    String crearEventoCobro(Prestamo prestamo);
    void eliminarEvento(String eventId);
    void actualizarEvento(Prestamo prestamo);
}
