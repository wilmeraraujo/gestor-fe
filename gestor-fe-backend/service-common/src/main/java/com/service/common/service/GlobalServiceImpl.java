package com.service.common.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public class GlobalServiceImpl <E, R extends JpaRepository<E, Long>> implements GlobalService<E>{

	@Autowired
	protected R repository;

	@Override
	public Iterable<E> findAll() {
		return repository.findAll();
	}

	@Override
	public Page<E> findAll(Pageable pageable) {
		return repository.findAll(pageable);
	}

	@Override
	public Optional<E> findById(Long id) {
		return repository.findById(id); 
	}

	@Override
	public E save(E entity) {
		return repository.save(entity);
	}

	@Override
	public void deleteById(Long id) {
		repository.deleteById(id);
	}

}
