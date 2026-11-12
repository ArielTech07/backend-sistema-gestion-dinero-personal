package com.backend.gestion.ariel.gestion_dinero.services.impl;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.backend.gestion.ariel.gestion_dinero.entities.Prestamo;
import com.backend.gestion.ariel.gestion_dinero.services.GoogleCalendarService;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;

@Service
public class GoogleCalendarServiceImpl implements GoogleCalendarService {

    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarServiceImpl.class);

    @Autowired(required = false)
    private Calendar calendarService;

    @Value("${google.calendar.calendar-id:primary}")
    private String calendarId;

    @Value("${google.calendar.timezone:America/Lima}")
    private String timeZone;

    @Override
    public String crearEventoCobro(Prestamo prestamo) {
        if (calendarService == null) {
            log.warn("Google Calendar no está configurado o inicializado. Omitiendo creación de evento.");
            return null;
        }

        try {
            // 1. Determinar fecha base del cobro
            LocalDate fechaCobro;
            if (prestamo.getDiaCobro() != null) {
                LocalDate hoy = LocalDate.now();
                int dia = Math.min(prestamo.getDiaCobro(), hoy.lengthOfMonth());
                fechaCobro = LocalDate.of(hoy.getYear(), hoy.getMonth(), dia);
                // Si el día ya pasó este mes, programar para el siguiente mes
                if (fechaCobro.isBefore(hoy)) {
                    LocalDate proximoMes = hoy.plusMonths(1);
                    int diaAjustado = Math.min(prestamo.getDiaCobro(), proximoMes.lengthOfMonth());
                    fechaCobro = LocalDate.of(proximoMes.getYear(), proximoMes.getMonth(), diaAjustado);
                }
            } else if (prestamo.getFechaInicio() != null) {
                fechaCobro = prestamo.getFechaInicio().plusMonths(1);
            } else {
                fechaCobro = LocalDate.now();
            }

            // 2. Construir el evento de Google Calendar
            Event event = new Event();
            event.setSummary("💰 Cobro de préstamo: " + prestamo.getReferencia());

            StringBuilder descripcion = new StringBuilder();
            descripcion.append("Recordatorio de cobro de préstamo:\n");
            descripcion.append("- Referencia: ").append(prestamo.getReferencia()).append("\n");
            descripcion.append("- Capital actual: ").append(
                    prestamo.getCapitalActual() != null ? prestamo.getCapitalActual() : prestamo.getCapitalInicial())
                    .append("\n");
            if (prestamo.getInteresMensual() != null) {
                descripcion.append("- Interés mensual: ").append(prestamo.getInteresMensual()).append("\n");
            }
            if (prestamo.getTasaInteres() != null) {
                descripcion.append("- Tasa: ").append(prestamo.getTasaInteres()).append("%\n");
            }
            event.setDescription(descripcion.toString());

            // Horario del evento: 8:20 AM a 9:00 AM (para notificación puntual en iPhone)
            ZoneId zone = ZoneId.of(timeZone != null && !timeZone.isBlank() ? timeZone : "America/Lima");
            ZonedDateTime inicio = fechaCobro.atTime(8, 20).atZone(zone);
            ZonedDateTime fin = fechaCobro.atTime(9, 0).atZone(zone);

            EventDateTime start = new EventDateTime()
                    .setDateTime(new DateTime(Date.from(inicio.toInstant())))
                    .setTimeZone(zone.getId());
            event.setStart(start);

            EventDateTime end = new EventDateTime()
                    .setDateTime(new DateTime(Date.from(fin.toInstant())))
                    .setTimeZone(zone.getId());
            event.setEnd(end);

            // Regla de recurrencia mensual (si tiene diaCobro)
            if (prestamo.getDiaCobro() != null) {
                event.setRecurrence(
                        Collections.singletonList("RRULE:FREQ=MONTHLY;BYMONTHDAY=" + prestamo.getDiaCobro()));
            }

            // Recordatorios: notificación al momento exacto (8:20 AM) y 1 día antes
            Event.Reminders reminders = new Event.Reminders();
            reminders.setUseDefault(false);
            EventReminder reminderMismoDia = new EventReminder().setMethod("popup").setMinutes(0); // Suena a las 8:20 AM exactas
            EventReminder reminderUnDiaAntes = new EventReminder().setMethod("popup").setMinutes(24 * 60); // 1 día antes por prevención
            reminders.setOverrides(Arrays.asList(reminderMismoDia, reminderUnDiaAntes));
            event.setReminders(reminders);

            // 3. Insertar evento en Google Calendar
            Event createdEvent = calendarService.events().insert(calendarId, event).execute();
            log.info("Evento de cobro creado en Google Calendar con ID: {}", createdEvent.getId());
            return createdEvent.getId();

        } catch (Exception e) {
            log.error("Error al crear evento en Google Calendar: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void eliminarEvento(String eventId) {
        if (calendarService == null || eventId == null || eventId.isBlank()) {
            return;
        }

        try {
            calendarService.events().delete(calendarId, eventId).execute();
            log.info("Evento de Google Calendar eliminado con éxito: {}", eventId);
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 404) {
                log.warn("El evento {} ya no existía en Google Calendar (404).", eventId);
            } else {
                log.error("Error al eliminar evento {} en Google Calendar: {}", eventId, e.getMessage());
            }
        } catch (IOException e) {
            log.error("Error de conexión al eliminar evento de Google Calendar: {}", e.getMessage());
        }
    }

    @Override
    public void actualizarEvento(Prestamo prestamo) {
        if (calendarService == null || prestamo == null) {
            return;
        }

        String eventId = prestamo.getGoogleCalendarEventId();
        if (eventId == null || eventId.isBlank()) {
            // Si no tenía evento asignado, intentar crearlo
            String nuevoEventId = crearEventoCobro(prestamo);
            prestamo.setGoogleCalendarEventId(nuevoEventId);
            return;
        }

        try {
            Event event = calendarService.events().get(calendarId, eventId).execute();
            if (event != null) {
                event.setSummary("Cobro de préstamo: " + prestamo.getReferencia());

                StringBuilder descripcion = new StringBuilder();
                descripcion.append("Recordatorio de cobro de préstamo:\n");
                descripcion.append("- Referencia: ").append(prestamo.getReferencia()).append("\n");
                descripcion.append("- Capital actual: ").append(prestamo.getCapitalActual()).append("\n");
                if (prestamo.getInteresMensual() != null) {
                    descripcion.append("- Interés mensual: ").append(prestamo.getInteresMensual()).append("\n");
                }
                if (prestamo.getTasaInteres() != null) {
                    descripcion.append("- Tasa: ").append(prestamo.getTasaInteres()).append("%\n");
                }
                event.setDescription(descripcion.toString());

                if (prestamo.getDiaCobro() != null) {
                    event.setRecurrence(
                            Collections.singletonList("RRULE:FREQ=MONTHLY;BYMONTHDAY=" + prestamo.getDiaCobro()));
                }

                calendarService.events().update(calendarId, eventId, event).execute();
                log.info("Evento de Google Calendar actualizado con éxito: {}", eventId);
            }
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 404) {
                log.warn("El evento {} no se encontró para actualizar. Creando uno nuevo.", eventId);
                String nuevoEventId = crearEventoCobro(prestamo);
                prestamo.setGoogleCalendarEventId(nuevoEventId);
            } else {
                log.error("Error al actualizar evento en Google Calendar: {}", e.getMessage());
            }
        } catch (IOException e) {
            log.error("Error de conexión al actualizar evento de Google Calendar: {}", e.getMessage());
        }
    }
}
