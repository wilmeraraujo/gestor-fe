package com.gestor_fe.core.service.impl;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.gestor_fe.core.entity.Documento;
import com.gestor_fe.core.entity.Prestador;
import com.gestor_fe.core.repository.DocumentoRepository;
import com.gestor_fe.core.repository.PrestadorRepository;
import com.gestor_fe.core.service.PrestadorService;

@Service
public class PrestadorServiceImpl implements PrestadorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrestadorServiceImpl.class);

    private final PrestadorRepository prestadorRepository;
    private final DocumentoRepository documentoRepository;
    private final String rutaStorageValidos;

    public PrestadorServiceImpl(
            PrestadorRepository prestadorRepository,
            DocumentoRepository documentoRepository,
            @Value("${app.storage.validos:E:/gestion-fe-validos}") String rutaStorageValidos) {
        this.prestadorRepository = prestadorRepository;
        this.documentoRepository = documentoRepository;
        this.rutaStorageValidos = rutaStorageValidos;
    }

    // =========================================================================
    // 👤 MAESTRO PRESTADOR
    // =========================================================================

    @Override
    @Transactional
    public Prestador crearOActualizarPrestador(Prestador prestador) {
        return prestadorRepository.save(prestador);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Prestador> obtenerPorNit(String nit) {
        return prestadorRepository.findByNitAndDeletedAtIsNull(nit);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Prestador> obtenerPorId(Long id) {
        return prestadorRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Prestador> listarPrestadores(Pageable pageable) {
        return prestadorRepository.findAll(pageable);
    }

    // =========================================================================
    // 📎 GESTIÓN DE SOPORTES (DOCUMENTOS DEL PRESTADOR)
    // =========================================================================

    @Override
    @Transactional
    public Documento cargarSoporte(String nitPrestador, Long tipoId, Long extensionId, MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo cargado se encuentra vacío.");
        }

        // 1. Obtener prestador activo por NIT
        Prestador prestador = prestadorRepository.findByNitAndDeletedAtIsNull(nitPrestador)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró un prestador activo con el NIT: " + nitPrestador));

        try {
            // 2. Si ya existe un documento activo del mismo tipo (ej. un RUT viejo), realizar Soft-Delete
            Optional<Documento> soporteExistente = documentoRepository
                    .findByPrestadorIdAndTipoIdAndDeletedAtIsNull(prestador.getId(), tipoId);

            soporteExistente.ifPresent(docOld -> {
                docOld.setDeletedAt(LocalDate.now());
                documentoRepository.save(docOld);
                LOGGER.info("ℹ️ Reemplazando soporte previo ID {} para el tipoId {}", docOld.getId(), tipoId);
            });

            // 3. Crear estructura física de directorios: E:\gestion-fe-validos\{NIT}\SOPORTES_PRESTADOR\
            String nitSaneado = sanearNombreCarpeta(nitPrestador);
            Path directorioSoportes = Paths.get(rutaStorageValidos, nitSaneado, "SOPORTES_PRESTADOR");

            if (!Files.exists(directorioSoportes)) {
                Files.createDirectories(directorioSoportes);
            }

            // 4. Copiar el archivo recibido a la ruta final con UUID único
            String nombreOriginal = StringUtils.cleanPath(
                    archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "soporte.pdf"
            );
            String nombreUnico = UUID.randomUUID() + "_" + nombreOriginal;
            Path destinoFinal = directorioSoportes.resolve(nombreUnico);

            Files.copy(archivo.getInputStream(), destinoFinal, StandardCopyOption.REPLACE_EXISTING);

            // 5. Instanciar la entidad Documento y mapear sus campos
            Documento documento = new Documento();
            documento.setNombreOriginal(nombreOriginal);
            documento.setRuta(destinoFinal.toString());
            documento.setTamano(archivo.getSize());
            documento.setEstadoId(1L);
            documento.setExtensionId(extensionId);
            documento.setTipoId(tipoId);

            // Vinculación bidireccional usando el método helper de la entidad Prestador
            prestador.addSoporte(documento);

            Documento soporteGuardado = documentoRepository.save(documento);
            LOGGER.info("✅ Soporte guardado exitosamente con ID {} para el Prestador NIT {}", soporteGuardado.getId(), nitPrestador);

            return soporteGuardado;

        } catch (Exception e) {
            LOGGER.error("❌ Error guardando el soporte para el prestador {}: {}", nitPrestador, e.getMessage());
            throw new RuntimeException("No se pudo procesar y almacenar el archivo de soporte.", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Documento> listarSoportes(Long prestadorId, Pageable pageable) {
        return documentoRepository.findByPrestadorIdAndDeletedAtIsNull(prestadorId, pageable);
    }

    @Override
    @Transactional
    public void eliminarSoporte(Long documentoId) {
        Documento doc = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el documento con ID: " + documentoId));

        // Soft Delete en base de datos
        doc.setDeletedAt(LocalDate.now());
        documentoRepository.save(doc);

        // Intento opcional de limpieza en disco
        try {
            File archivoFisico = new File(doc.getRuta());
            if (archivoFisico.exists()) {
                archivoFisico.delete();
            }
        } catch (Exception e) {
            LOGGER.warn("⚠️ No se pudo eliminar el archivo físico en ruta {}: {}", doc.getRuta(), e.getMessage());
        }
    }

    private String sanearNombreCarpeta(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "DESCONOCIDO";
        }
        return nombre.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }
}