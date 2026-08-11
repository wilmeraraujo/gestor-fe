package com.gestor_fe.admin.service.controller;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gestor_fe.admin.service.model.entity.ConfiguracionFaseExtension;
import com.gestor_fe.admin.service.services.ConfiguracionFaseExtensionService;
import com.service.common.controller.GlobalController;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/admin/configuracion-fase")
public class ConfiguracionFaseExtensionController extends GlobalController<ConfiguracionFaseExtension, ConfiguracionFaseExtensionService> {

    private final ConfiguracionFaseExtensionService service;

    public ConfiguracionFaseExtensionController(ConfiguracionFaseExtensionService service) {
        this.service = service;
    }

    @GetMapping("/buscar/{desc}")
    public ResponseEntity<?> filter(@PathVariable String desc) {
        return ResponseEntity.ok(service.findByDescripcion(desc));
    }

    @GetMapping("/fase/{faseId}")
    public ResponseEntity<?> obtenerPorFase(@PathVariable Long faseId) {
        return ResponseEntity.ok(service.findByFaseId(faseId));
    }

    @GetMapping("/paginable/activos")
    public ResponseEntity<?> listAll(Pageable pageable) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "id"));

        return ResponseEntity.ok().body(service.findByDeletedAtIsNull(sortedPageable));
    }

    @PutMapping("/deleted-at/{id}")
    public ResponseEntity<?> addDeletedAt(@PathVariable Long id) {
        Optional<ConfiguracionFaseExtension> x = service.findById(id);

        if (x.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ConfiguracionFaseExtension xDb = x.get();
        xDb.setDeletedAt(LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.save(xDb));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> edit(@Validated @RequestBody ConfiguracionFaseExtension x,
            BindingResult result,
            @PathVariable(name = "id") Long id) {

        if (result.hasErrors()) {
            return this.validar(result);
        }

        Optional<ConfiguracionFaseExtension> objeto = service.findById(id);
        if (objeto.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ConfiguracionFaseExtension xDb = objeto.get();
        xDb.setCodigo(x.getCodigo());
        xDb.setDescripcion(x.getDescripcion());
        xDb.setFaseId(x.getFaseId());
        xDb.setExtensionId(x.getExtensionId());
        xDb.setTamanoMaximoMb(x.getTamanoMaximoMb());
        xDb.setObligatorio(x.getObligatorio());
        xDb.setPermiteMultiple(x.getPermiteMultiple());
        xDb.setUpdatedAt(LocalDateTime.now());
        xDb.setDeletedAt(x.getDeletedAt());

        return ResponseEntity.status(HttpStatus.CREATED).body(service.save(xDb));
    }
}