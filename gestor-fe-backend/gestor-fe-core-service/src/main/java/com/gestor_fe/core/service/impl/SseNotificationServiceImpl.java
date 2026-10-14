package com.gestor_fe.core.service.impl;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.gestor_fe.core.service.SseNotificationService;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
public class SseNotificationServiceImpl implements SseNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SseNotificationServiceImpl.class);

    // Mantiene las conexiones activas por usuario (Timeout 30 minutos)
    private final Map<String, SseEmitter> emisores = new ConcurrentHashMap<>();

    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "sse-heartbeat-thread");
        t.setDaemon(true);
        return t;
    });

    @PostConstruct
    public void initHeartbeat() {
        // Enviar ping cada 15 segundos para mantener vivo el túnel HTTP en Gateway y Proxies
        heartbeatExecutor.scheduleAtFixedRate(() -> {
            if (emisores.isEmpty()) {
                return;
            }
            emisores.forEach((usuario, emitter) -> {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (Exception e) {
                    LOGGER.debug("Cliente desconectado detectado durante heartbeat: {}", usuario);
                    emisores.remove(usuario);
                }
            });
        }, 15, 15, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void destroy() {
        heartbeatExecutor.shutdownNow();
    }

    @Override
    public SseEmitter crearConexion(String usuario) {
        // Timeout de 30 minutos (1.800.000 ms)
        SseEmitter emitter = new SseEmitter(1800000L);

        // Si ya existía un emisor previo para el usuario, cerrarlo limpiamente
        SseEmitter emisorPrevio = this.emisores.put(usuario, emitter);
        if (emisorPrevio != null) {
            try {
                emisorPrevio.complete();
            } catch (Exception ignored) {}
        }

        emitter.onCompletion(() -> {
            LOGGER.info("🔒 Conexión SSE completada para el usuario: {}", usuario);
            this.emisores.remove(usuario);
        });
        emitter.onTimeout(() -> {
            LOGGER.warn("⏱️ Timeout alcanzado en conexión SSE para el usuario: {}", usuario);
            emitter.complete();
            this.emisores.remove(usuario);
        });
        emitter.onError((e) -> {
            LOGGER.error("❌ Error en conexión SSE para el usuario [{}]: {}", usuario, e.getMessage());
            this.emisores.remove(usuario);
        });

        // 🔥 CRÍTICO: Enviar handshake inicial para forzar el commit de cabeceras HTTP (200 OK + text/event-stream)
        try {
            emitter.send(SseEmitter.event()
                .name("INIT")
                .data("CONECTADO")
                .comment("Conexión SSE establecida exitosamente"));
            LOGGER.info("📡 Conexión SSE inicializada y confirmada para el usuario: {}", usuario);
        } catch (IOException e) {
            LOGGER.error("❌ Error enviando handshake inicial SSE a [{}]: {}", usuario, e.getMessage());
            emitter.completeWithError(e);
            this.emisores.remove(usuario);
        }

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