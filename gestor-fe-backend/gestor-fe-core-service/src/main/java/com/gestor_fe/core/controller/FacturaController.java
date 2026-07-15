package com.gestor_fe.core.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gestor_fe.core.service.FacturaService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/factura")
public class FacturaController {
	
	private final FacturaService service;
	
	public FacturaController(FacturaService service) {
		this.service = service;
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
	
}
