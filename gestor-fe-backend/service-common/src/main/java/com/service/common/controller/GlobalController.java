package com.service.common.controller;

import java.util.Optional;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.service.common.service.GlobalService;

public class GlobalController <E,S extends GlobalService<E>> {

	@Autowired
	protected S service;
	
	
	@GetMapping
	public ResponseEntity<?> listAll(){
		return ResponseEntity.ok().body(service.findAll());
	}
	
	@GetMapping("/paginable")
	public ResponseEntity<?> listAll(Pageable pageable){
		return ResponseEntity.ok().body(service.findAll(pageable));
	}
	
	@GetMapping("/paginable/buscar")
	public ResponseEntity<?> buscarPaginado(@RequestParam Map<String, String> params, Pageable pageable) {
		Pageable sortedPageable = PageRequest.of(
				pageable.getPageNumber(),
				pageable.getPageSize(),
				Sort.by(Sort.Direction.DESC, "id"));
		return ResponseEntity.ok().body(service.buscarPaginado(params, sortedPageable));
	}

	
	@GetMapping("/{id}")
	public ResponseEntity<?> listById(@PathVariable(name = "id") Long id){
		Optional<E> o = service.findById(id);
		
		if(o.isEmpty()) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok().body(o.get());		
	}
	
	@PostMapping
	public ResponseEntity<?> create(
			@Validated @RequestBody E entity,
			BindingResult result,
			jakarta.servlet.http.HttpServletRequest request) {

		if (result.hasErrors()) {
			return this.validar(result);
		}

		try {
			String username = extraerUsername(request, null);
			E entityDb = service.saveWithLog(entity, "CREAR", null, username);
			return ResponseEntity.status(HttpStatus.CREATED).body(entityDb);
		} catch (IllegalArgumentException e) {
			Map<String, String> error = new HashMap<>();
			error.put("error", e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
		} catch (org.springframework.dao.DataIntegrityViolationException e) {
			Map<String, String> error = new HashMap<>();
			error.put("error", "El código ya se encuentra registrado.");
			return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
		} catch (Exception e) {
			Map<String, String> error = new HashMap<>();
			error.put("error", e.getMessage() != null ? e.getMessage() : "Se generó un error en la creación de la entidad.");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
		}
	}
		
	@DeleteMapping("/{id}")
	public ResponseEntity<?> delete(@PathVariable Long id){
		
		Optional<E> entityDb = service.findById(id);
		
		if(entityDb.isEmpty()) {
			return ResponseEntity.notFound().build();
		}
		
		service.deleteById(id);
		return ResponseEntity.noContent().build();
	}

	@org.springframework.web.bind.annotation.PatchMapping("/{id}/toggle-estado")
	public ResponseEntity<?> toggleEstado(
			@PathVariable Long id,
			@RequestBody(required = false) Map<String, String> body,
			jakarta.servlet.http.HttpServletRequest request) {
		
		Optional<E> entityDb = service.findById(id);
		if (entityDb.isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		String observacion = null;
		String usernameFromBody = null;

		if (body != null) {
			observacion = body.get("observacion");
			usernameFromBody = body.get("username");
			if (usernameFromBody == null || usernameFromBody.trim().isEmpty()) {
				usernameFromBody = body.get("userName");
			}
			if (usernameFromBody == null || usernameFromBody.trim().isEmpty()) {
				usernameFromBody = body.get("usuario");
			}
		}

		String username = extraerUsername(request, usernameFromBody);

		try {
			E updated = service.toggleEstado(id, observacion, username);
			return ResponseEntity.ok(updated);
		} catch (Exception e) {
			Map<String, String> error = new HashMap<>();
			error.put("error", e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
		}
	}

	public String extraerUsername(jakarta.servlet.http.HttpServletRequest request, String usernameFromBody) {
		if (usernameFromBody != null && !usernameFromBody.trim().isEmpty()) {
			return usernameFromBody.trim();
		}

		if (request != null) {
			String username = request.getHeader("X-User");
			if (username == null || username.trim().isEmpty()) {
				username = request.getHeader("username");
			}
			if ((username == null || username.trim().isEmpty()) && request.getUserPrincipal() != null) {
				username = request.getUserPrincipal().getName();
			}
			if (username != null && !username.trim().isEmpty()) {
				return username.trim();
			}
		}

		return "SISTEMA";
	}
	
	@org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException e) {
		Map<String, String> error = new HashMap<>();
		error.put("error", e.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}

	@org.springframework.web.bind.annotation.ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
	public ResponseEntity<?> handleDataIntegrityViolationException(org.springframework.dao.DataIntegrityViolationException e) {
		Map<String, String> error = new HashMap<>();
		error.put("error", "Error de integridad: El código o registro ya existe en el sistema.");
		return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
	}
	
	protected ResponseEntity<?> validar(BindingResult result){
		Map<String, Object> errores = new HashMap<>();
		result.getFieldErrors().forEach(e -> {
			errores.put(e.getField(), "El campo " + e.getField() + " " + e.getDefaultMessage());
		});
		
		return ResponseEntity.badRequest().body(errores);
	}
}
