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

import com.gestor_fe.admin.service.model.entity.ConfiguracionSistema;
import com.gestor_fe.admin.service.services.ConfiguracionSistemaService;
import com.service.common.controller.GlobalController;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/admin/configuracion-sistema")
public class ConfiguracionSistemaController extends GlobalController<ConfiguracionSistema, ConfiguracionSistemaService> {

    private final ConfiguracionSistemaService service;

    public ConfiguracionSistemaController(ConfiguracionSistemaService service) {
        this.service = service;
    }

    @GetMapping("/buscar/{desc}")
    public ResponseEntity<?> filter(@PathVariable String desc) {
        return ResponseEntity.ok(service.findByDescripcion(desc));
    }

    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<?> obtenerPorCodigo(@PathVariable String codigo) {
        Optional<ConfiguracionSistema> config = service.findByCodigo(codigo);
        if (config.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(config.get());
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
        Optional<ConfiguracionSistema> x = service.findById(id);

        if (x.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ConfiguracionSistema xDb = x.get();
        xDb.setDeletedAt(LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.save(xDb));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> edit(@Validated @RequestBody ConfiguracionSistema x,
            BindingResult result,
            @PathVariable(name = "id") Long id) {

        if (result.hasErrors()) {
            return this.validar(result);
        }

        Optional<ConfiguracionSistema> objeto = service.findById(id);
        if (objeto.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ConfiguracionSistema xDb = objeto.get();
        xDb.setCodigo(x.getCodigo());
        xDb.setDescripcion(x.getDescripcion());
        xDb.setValor(x.getValor());
        xDb.setCategoria(x.getCategoria());
        xDb.setUpdatedAt(LocalDateTime.now());
        xDb.setDeletedAt(x.getDeletedAt());

        return ResponseEntity.status(HttpStatus.CREATED).body(service.save(xDb));
    }
}