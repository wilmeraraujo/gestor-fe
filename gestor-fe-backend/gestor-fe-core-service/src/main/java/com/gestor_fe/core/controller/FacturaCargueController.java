package com.gestor_fe.core.controller;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.gestor_fe.core.entity.Cargue;
import com.gestor_fe.core.service.CargueService;

@RestController
@RequestMapping("/api/v1/cargue")
@CrossOrigin("*")
public class FacturaCargueController {

    @Autowired
    private CargueService cargueService;

    @Value("${ruta.storage}")
    private String rutaStorage;

    @PostMapping("/procesar-zip")
    public ResponseEntity<?> procesarZipFacturas(
            @RequestParam("file") MultipartFile multipartFile,
            @RequestParam("usuario") String usuario
    ) {
        // 1. Validaciones iniciales defensivas del archivo recibido
        if (multipartFile == null || multipartFile.isEmpty()) {
            return ResponseEntity.badRequest().body("Debe adjuntar un archivo válido.");
        }

        String extension = StringUtils.getFilenameExtension(multipartFile.getOriginalFilename());
        if (extension == null || !extension.equalsIgnoreCase("zip")) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .body("Formato inválido. Solo se permiten archivos comprimidos con extensión .zip");
        }

        try {
            // 2. Guardar físicamente el ZIP recibido en la ruta de almacenamiento configurada
            String originalFileName = multipartFile.getOriginalFilename();
            File carpetaDestino = new File(rutaStorage);
            if (!carpetaDestino.exists()) {
                carpetaDestino.mkdirs();
            }
            
            File zipToImport = new File(carpetaDestino, originalFileName);
            multipartFile.transferTo(zipToImport);

            // 3. Crear el registro maestro inicial en estado pendiente/procesando
            Cargue cargue = new Cargue();
            cargue.setNombreArchivo(originalFileName);
            cargue.setUsuario(usuario);
            cargue.setCreatedAt(LocalDateTime.now());
            cargue.setExiteError(false); // Inicialmente sin error confirmado
            cargue.setNumeroRegistro(0);  // El Listener actualizará esto al finalizar

            Cargue savedCargue = cargueService.save(cargue);

            // 4. Disparar el motor de Spring Batch en un hilo independiente
            cargueService.runBatchJobAsynchronously(zipToImport, savedCargue);

            // 5. Responder de inmediato al Frontend (Angular) en menos de 1 segundo
            return ResponseEntity.status(HttpStatus.CREATED).body(savedCargue);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error de I/O escribiendo el archivo en el servidor: " + e.getMessage());
        }
    }
    
    @GetMapping("/paginable/activos")
	public ResponseEntity<?> listAll(Pageable pageable) {

	    Pageable sortedPageable = PageRequest.of(
	            pageable.getPageNumber(),
	            pageable.getPageSize(),
	            Sort.by(Sort.Direction.DESC, "id"));

	    return ResponseEntity.ok()
	            .body(cargueService.findByDeletedAtIsNull(sortedPageable));
	}
}