package com.service.common.controller;

import java.util.Optional;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

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
	
	@GetMapping("/{id}")
	public ResponseEntity<?> listById(@PathVariable(name = "id") Long id){
		Optional<E> o = service.findById(id);
		
		if(o.isEmpty()) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok().body(o.get());		
	}
	
	@PostMapping
	public ResponseEntity<?> create(@Validated @RequestBody E entity, BindingResult result) {

	  if (result.hasErrors()) {
	        return this.validar(result);
	    }

	    try {
	        E entityDb = service.save(entity);
	        return ResponseEntity.status(HttpStatus.CREATED).body(entityDb);
	    } catch (DuplicateKeyException e) {
	        // Capturar la excepción de clave duplicada y retornar un mensaje claro
	        return ResponseEntity.status(HttpStatus.CONFLICT).body("Error: El código ya existe.");
	    } catch (Exception e) {
	        // Manejo de cualquier otro error
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Se genero un error en la creacion de la entidad.");
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
	
	protected ResponseEntity<?> validar(BindingResult result){
		Map<String, Object> errores = new HashMap<>();
		result.getFieldErrors().forEach(e -> {
			errores.put(e.getField(), "El campo " + e.getField() + " " + e.getDefaultMessage());
		});
		
		return ResponseEntity.badRequest().body(errores);
	}
}
