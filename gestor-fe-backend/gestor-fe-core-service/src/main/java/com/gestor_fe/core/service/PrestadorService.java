package com.gestor_fe.core.service;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.gestor_fe.core.entity.Documento;
import com.gestor_fe.core.entity.Prestador;

public interface PrestadorService {

    // --- Operaciones del Prestador (CRUD / Maestro) ---
    Prestador crearOActualizarPrestador(Prestador prestador);
    Optional<Prestador> obtenerPorNit(String nit);
    Optional<Prestador> obtenerPorId(Long id);
    Page<Prestador> listarPrestadores(Pageable pageable);

    // --- Operaciones de Soportes / Documentos del Prestador ---
    Documento cargarSoporte(String nitPrestador, Long tipoId, Long extensionId, MultipartFile archivo);
    Page<Documento> listarSoportes(Long prestadorId, Pageable pageable);
    void eliminarSoporte(Long documentoId);
}