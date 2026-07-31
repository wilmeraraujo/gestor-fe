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

import com.gestor_fe.admin.service.model.entity.TipoIdentificacion;
import com.gestor_fe.admin.service.services.TipoIdentificacionService;
import com.service.common.controller.GlobalController;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/admin/tipo-identificacion")
public class TipoIdentificacionController extends GlobalController<TipoIdentificacion, TipoIdentificacionService> {

	private final TipoIdentificacionService service;
	
	public TipoIdentificacionController(TipoIdentificacionService service) {
		this.service = service;
	}
	
	@GetMapping("/buscar/{desc}")
	public ResponseEntity<?> filter(@PathVariable String desc){
		return ResponseEntity.ok(service.findByDescripcion(desc));
	}
	
	@GetMapping("/paginable/activos")
	public ResponseEntity<?> listAll(Pageable pageable) {

	    Pageable sortedPageable = PageRequest.of(
	            pageable.getPageNumber(),
	            pageable.getPageSize(),
	            Sort.by(Sort.Direction.DESC, "id"));

	    return ResponseEntity.ok()
	            .body(service.findByDeletedAtIsNull(sortedPageable));
	}
	
	@PutMapping("/deleted-at/{id}")
	public ResponseEntity<?> addDeletedAt(@PathVariable Long id){
		Optional<TipoIdentificacion> ti = service.findById(id);
		
		if (ti.isEmpty()) {
			return ResponseEntity.notFound().build();
		}
		
		TipoIdentificacion tiDb = ti.get();
		tiDb.setDeletedAt(LocalDateTime.now());
		return ResponseEntity.status(HttpStatus.CREATED).body(service.save(tiDb));
	}
	
	@PutMapping("/{id}")
	public ResponseEntity<?> edit(@Validated @RequestBody TipoIdentificacion ti,
			BindingResult result,
			@PathVariable(name = "id") Long id) {
		
		if (result.hasErrors()) {
			return this.validar(result);
		}

		Optional<TipoIdentificacion> objeto = service.findById(id);
		if (objeto.isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		TipoIdentificacion tiDb = objeto.get();
		tiDb.setCodigo(ti.getCodigo());
		tiDb.setDescripcion(ti.getDescripcion());
		tiDb.setDeletedAt(ti.getDeletedAt());

		return ResponseEntity.status(HttpStatus.CREATED).body(service.save(tiDb));
	}
	
}
