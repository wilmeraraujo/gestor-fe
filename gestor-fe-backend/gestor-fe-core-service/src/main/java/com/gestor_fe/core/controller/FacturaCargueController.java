package com.gestor_fe.core.controller;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
import com.gestor_fe.core.entity.Cargue;
import com.gestor_fe.core.service.CargueService;
import com.gestor_fe.core.service.SseNotificationService;

@RestController
@RequestMapping("/api/v1/cargue")
@CrossOrigin("*")
public class FacturaCargueController {

    @Autowired
    private CargueService cargueService;
    
    @Autowired
    private SseNotificationService sseNotificationService;

    @Autowired
    private AdminFeignClient adminFeignClient;

    @Value("${ruta.storage}")
    private String rutaStorage;

    private static final Long FASE_RADICACION_ID = 1L;

    @PostMapping("/procesar-zip")
    public ResponseEntity<?> procesarZipFacturas(
            @RequestParam("file") MultipartFile multipartFile,
            @RequestParam("usuario") String usuario
    ) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            return ResponseEntity.badRequest().body("Debe adjuntar un archivo válido.");
        }

        // 1. Obtener la extensión del archivo cargado
        String extensionArchivo = StringUtils.getFilenameExtension(multipartFile.getOriginalFilename());
        if (extensionArchivo == null) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .body("El archivo no posee una extensión válida.");
        }

        // 2. 🔌 CONSULTA DINÁMICA VÍA FEIGN: Obtener las reglas para la Fase de Radicación
        List<ConfiguracionFaseExtensionDto> reglasRadicacion = adminFeignClient.obtenerConfiguracionesPorFase(FASE_RADICACION_ID);

        // Buscar si existe una regla activa para la extensión que el usuario subió (ej: 'zip')
        Optional<ConfiguracionFaseExtensionDto> reglaMatching = reglasRadicacion.stream()
                .filter(regla -> regla.getDescripcion() != null && regla.getDescripcion().toLowerCase().contains(extensionArchivo.toLowerCase())
                        || "zip".equalsIgnoreCase(extensionArchivo)) // Respaldo para el tipo de archivo ZIP
                .findFirst();

        if (reglaMatching.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .body("Formato no permitido para la fase de Radicación. Extensión '" + extensionArchivo + "' no configurada.");
        }

        ConfiguracionFaseExtensionDto regla = reglaMatching.get();

        // 3. ⚡ VALIDACIÓN DINÁMICA DE TAMAÑO MÁXIMO (MB -> Bytes)
        long maxBytesPermitidos = (long) regla.getTamanoMaximoMb() * 1024 * 1024;
        if (multipartFile.getSize() > maxBytesPermitidos) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(String.format("El archivo supera el tamaño máximo permitido de %d MB para esta fase.", regla.getTamanoMaximoMb()));
        }

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

            cargueService.runBatchJobAsynchronously(zipToImport, savedCargue);

            return ResponseEntity.status(HttpStatus.CREATED).body(savedCargue);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error de I/O escribiendo el archivo en el servidor: " + e.getMessage());
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