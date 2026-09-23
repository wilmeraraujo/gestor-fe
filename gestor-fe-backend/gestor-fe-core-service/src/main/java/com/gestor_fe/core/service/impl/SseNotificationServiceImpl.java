package com.gestor_fe.core.service.impl;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.gestor_fe.core.service.SseNotificationService;

@Service
public class SseNotificationServiceImpl implements SseNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SseNotificationServiceImpl.class);

    // Mantiene las conexiones activas por usuario (Timeout 30 minutos)
    private final Map<String, SseEmitter> emisores = new ConcurrentHashMap<>();

    @Override
    public SseEmitter crearConexion(String usuario) {
        // Timeout de 30 minutos (1.800.000 ms)
        SseEmitter emitter = new SseEmitter(1800000L);

        this.emisores.put(usuario, emitter);

        emitter.onCompletion(() -> this.emisores.remove(usuario));
        emitter.onTimeout(() -> this.emisores.remove(usuario));
        emitter.onError((e) -> this.emisores.remove(usuario));

        LOGGER.info("📡 Nueva conexión SSE establecida para el usuario: {}", usuario);
        return emitter;
    }

    @Override
    public void notificarFinCargue(String usuario, Long cargueId, boolean existeError, int facturasProcesadas) {
        SseEmitter emitter = this.emisores.get(usuario);

        if (emitter != null) {
            try {
                Map<String, Object> payload = Map.of(
                    "cargueId", cargueId,
                    "exiteError", existeError,
                    "numeroRegistro", facturasProcesadas,
                    "timestamp", System.currentTimeMillis()
                );

                emitter.send(SseEmitter.event()
                    .name("FIN_CARGUE")
                    .data(payload));

                LOGGER.info("⚡ Notificación SSE emitida a [{}] para el cargue ID: {}", usuario, cargueId);
            } catch (IOException e) {
                LOGGER.error("❌ Error enviando evento SSE al usuario [{}]: {}", usuario, e.getMessage());
                this.emisores.remove(usuario);
            }
        }
    }
}