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

import com.gestor_fe.admin.service.model.entity.Departamento;
import com.gestor_fe.admin.service.services.DepartamentoService;
import com.service.common.controller.GlobalController;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/admin/departamento")
public class DepartamentoController extends GlobalController<Departamento, DepartamentoService> {

    private final DepartamentoService service;

    public DepartamentoController(DepartamentoService service) {
        this.service = service;
    }

    @GetMapping("/buscar/{desc}")
    public ResponseEntity<?> filter(@PathVariable String desc) {
        return ResponseEntity.ok(service.findByDescripcion(desc));
    }

    @GetMapping("/paginable/activos")
    public ResponseEntity<?> listAll(Pageable pageable) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "id"));

        return ResponseEntity.ok(service.findByDeletedAtIsNull(sortedPageable));
    }

    @PutMapping("/deleted-at/{id}")
    public ResponseEntity<?> addDeletedAt(@PathVariable Long id) {
        Optional<Departamento> o = service.findById(id);
        if (o.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Departamento dDb = o.get();
        dDb.setDeletedAt(LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.save(dDb));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> edit(@Validated @RequestBody Departamento d,
                                  BindingResult result,
                                  @PathVariable Long id) {
        if (result.hasErrors()) {
            return this.validar(result);
        }

        Optional<Departamento> o = service.findById(id);
        if (o.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Departamento dDb = o.get();
        dDb.setCodigo(d.getCodigo());
        dDb.setDescripcion(d.getDescripcion());
        dDb.setDeletedAt(d.getDeletedAt());

        return ResponseEntity.status(HttpStatus.CREATED).body(service.save(dDb));
    }
}