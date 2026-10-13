package com.gestor_fe.core.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.gestor_fe.core.entity.Documento;
import com.gestor_fe.core.entity.Prestador;

public interface PrestadorService {

    // --- Operaciones del Prestador (CRUD / Maestro / Administración) ---
    Prestador crearOActualizarPrestador(Prestador prestador);
    Optional<Prestador> obtenerPorNit(String nit);
    Optional<Prestador> obtenerPorId(Long id);
    Page<Prestador> listarPrestadores(Pageable pageable);
    Page<Prestador> findByDeletedAtIsNull(Pageable pageable);
    Page<Prestador> buscarPaginado(Map<String, String> filtros, Pageable pageable);
    List<Prestador> findByDescripcion(String desc);
    Prestador crear(Prestador prestador);
    Prestador editar(Long id, Prestador prestador);
    Prestador toggleEstado(Long id);
    Prestador softDelete(Long id);
    void eliminar(Long id);

    // --- Operaciones de Soportes / Documentos del Prestador ---
    Documento cargarSoporte(String nitPrestador, String codigoTipo, String codigoExtension, MultipartFile archivo);
    Page<Documento> listarSoportes(Long prestadorId, Pageable pageable);
    void eliminarSoporte(Long documentoId);
}