package com.gestor_fe.core.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SseNotificationService {

    SseEmitter crearConexion(String usuario);

    void notificarFinCargue(String usuario, Long cargueId, boolean existeError, int facturasProcesadas);
}