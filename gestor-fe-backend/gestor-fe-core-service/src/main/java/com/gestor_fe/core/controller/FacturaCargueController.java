package com.gestor_fe.core.controller;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.gestor_fe.core.client.AdminFeignClient;
import com.gestor_fe.core.dto.ConfiguracionFaseExtensionDto;
import com.gestor_fe.core.dto.ConfiguracionSistemaDto;
import com.gestor_fe.core.entity.Cargue;
import com.gestor_fe.core.service.CargueService;
import com.gestor_fe.core.service.SseNotificationService;

@RestController
@RequestMapping("/api/v1/cargue")
@CrossOrigin("*")
public class FacturaCargueController {

    private static final Logger LOGGER = LoggerFactory.getLogger(FacturaCargueController.class);

    @Autowired
    private CargueService cargueService;
    
    @Autowired
    private SseNotificationService sseNotificationService;

    @Autowired
    private AdminFeignClient adminFeignClient;

    @Value("${ruta.storage}")
    private String rutaStorage;

    private static final Long FASE_RADICACION_ID = 1L;
    private static final String CODIGO_TAMANO_MAX_ZIP = "01";

    @PostMapping("/procesar-zip")
    public ResponseEntity<?> procesarZipFacturas(
            @RequestParam("file") MultipartFile multipartFile,
            @RequestParam("usuario") String usuario
    ) {
        // 🛑 VALIDACIÓN 1: Archivo presente
        if (multipartFile == null || multipartFile.isEmpty()) {
            return ResponseEntity.badRequest().body("Debe adjuntar un archivo válido.");
        }

        // 🛑 VALIDACIÓN 2: Extensión .zip
        String extensionArchivo = StringUtils.getFilenameExtension(multipartFile.getOriginalFilename());
        if (extensionArchivo == null || !"zip".equalsIgnoreCase(extensionArchivo)) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .body("Formato inválido. Únicamente se permiten archivos con extensión .zip");
        }

        // 🛑 VALIDACIÓN 3: Límite de tamaño dinámico desde la BD
        long maxMb = 100; // Valor por defecto de seguridad
        try {
            ConfiguracionSistemaDto configTamano = adminFeignClient.obtenerConfiguracionPorCodigo(CODIGO_TAMANO_MAX_ZIP);
            if (configTamano != null && configTamano.getValor() != null) {
                maxMb = Long.parseLong(configTamano.getValor().trim());
            }
        } catch (Exception e) {
            LOGGER.warn("⚠️ No se pudo obtener [{}] vía Feign. Se usará el límite de respaldo: {} MB", CODIGO_TAMANO_MAX_ZIP, maxMb);
        }

        long maxBytesPermitidos = maxMb * 1024;
        if (multipartFile.getSize() > maxBytesPermitidos) {
            double pesoMB = (double) multipartFile.getSize() / (1024);
            String errorTamano = String.format("El archivo pesa %.2f MB y supera el tamaño máximo permitido de %d MB.", pesoMB, maxMb);
            LOGGER.error("❌ RECHAZADO: {}", errorTamano);
            
            // Retorna HTTP 413 (Payload Too Large) SIN crear ningún registro de cargue
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(errorTamano);
        }

        // 🚀 SI PASÓ TODAS LAS VALIDACIONES: Guardar archivo y registrar Cargue
        try {
            String originalFileName = multipartFile.getOriginalFilename();
            File carpetaDestino = new File(rutaStorage);
            if (!carpetaDestino.exists()) {
                carpetaDestino.mkdirs();
            }
            
            File zipToImport = new File(carpetaDestino, originalFileName);
            multipartFile.transferTo(zipToImport);

            Cargue cargue = new Cargue();
            cargue.setNombreArchivo(originalFileName);
            cargue.setUsuario(usuario);
            cargue.setCreatedAt(LocalDateTime.now());
            cargue.setExiteError(false);
            cargue.setNumeroRegistro(0);

            Cargue savedCargue = cargueService.save(cargue);

            // Iniciar Batch de procesamiento del contenido interno del ZIP
            cargueService.runBatchJobAsynchronously(zipToImport, savedCargue);

            return ResponseEntity.status(HttpStatus.CREATED).body(savedCargue);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error escribiendo el archivo en el servidor: " + e.getMessage());
        }
    }
    
    @GetMapping("/paginable/activos")
    public ResponseEntity<?> listAll(
            @RequestParam(value = "usuario", required = false, defaultValue = "") String usuario,
            @RequestParam(value = "roles", required = false) List<String> roles,
            Pageable pageable
    ) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "id"));

        if (roles == null || roles.isEmpty()) {
            return ResponseEntity.ok(cargueService.findByDeletedAtIsNull(sortedPageable));
        }

        return ResponseEntity.ok(cargueService.findCarguesSegunRol(usuario, roles, sortedPageable));
    }
    
    @GetMapping(value = "/sse/subscribir/{usuario}", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribirNotificaciones(@PathVariable("usuario") String usuario) {
        return sseNotificationService.crearConexion(usuario);
    }
    
    @PutMapping("/deleted-at/{id}")
    public ResponseEntity<?> eliminarLogico(@PathVariable("id") Long id) {
        try {
            Cargue cargueEliminado = cargueService.eliminarLogico(id);
            return ResponseEntity.ok(cargueEliminado);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al realizar el borrado lógico del cargue: " + e.getMessage());
        }
    }
}