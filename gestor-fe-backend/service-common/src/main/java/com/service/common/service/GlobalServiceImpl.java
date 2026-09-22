package com.service.common.service;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class GlobalServiceImpl <E, R extends JpaRepository<E, Long>> implements GlobalService<E>{

	@Autowired
	protected R repository;

	@PersistenceContext
	protected EntityManager entityManager;

	@Autowired(required = false)
	protected LogAccionService logAccionService;

	private Class<E> entityClass;

	@Override
	public Iterable<E> findAll() {
		return repository.findAll();
	}

	@Override
	public Page<E> findAll(Pageable pageable) {
		return repository.findAll(pageable);
	}

	@SuppressWarnings("unchecked")
	protected Class<E> getEntityClass() {
		if (this.entityClass == null) {
			Type type = getClass().getGenericSuperclass();
			while (type != null && !(type instanceof ParameterizedType)) {
				if (type instanceof Class) {
					type = ((Class<?>) type).getGenericSuperclass();
				} else {
					break;
				}
			}
			if (type instanceof ParameterizedType) {
				this.entityClass = (Class<E>) ((ParameterizedType) type).getActualTypeArguments()[0];
			}
		}
		return this.entityClass;
	}

	@Override
	@Transactional(readOnly = true)
	public Page<E> buscarPaginado(Map<String, String> filtros, Pageable pageable) {
		Class<E> clazz = getEntityClass();
		if (clazz == null) {
			return repository.findAll(pageable);
		}

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();

		CriteriaQuery<E> query = cb.createQuery(clazz);
		Root<E> root = query.from(clazz);

		List<Predicate> predicates = construirPredicados(cb, root, filtros);
		query.where(predicates.toArray(new Predicate[0]));

		if (pageable.getSort().isSorted()) {
			List<Order> orders = new ArrayList<>();
			pageable.getSort().forEach(sortOrder -> {
				try {
					if (sortOrder.isAscending()) {
						orders.add(cb.asc(root.get(sortOrder.getProperty())));
					} else {
						orders.add(cb.desc(root.get(sortOrder.getProperty())));
					}
				} catch (Exception e) {
					// Fallback silencioso si la propiedad no existe
				}
			});
			if (!orders.isEmpty()) {
				query.orderBy(orders);
			}
		} else {
			try {
				query.orderBy(cb.desc(root.get("id")));
			} catch (Exception e) {
				// Fallback
			}
		}

		TypedQuery<E> typedQuery = entityManager.createQuery(query);
		typedQuery.setFirstResult((int) pageable.getOffset());
		typedQuery.setMaxResults(pageable.getPageSize());

		List<E> content = typedQuery.getResultList();

		CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
		Root<E> countRoot = countQuery.from(clazz);

		List<Predicate> countPredicates = construirPredicados(cb, countRoot, filtros);
		countQuery.select(cb.count(countRoot)).where(countPredicates.toArray(new Predicate[0]));

		Long total = entityManager.createQuery(countQuery).getSingleResult();

		return new PageImpl<>(content, pageable, total);
	}

	protected List<Predicate> construirPredicados(CriteriaBuilder cb, Root<E> root, Map<String, String> filtros) {
		List<Predicate> predicates = new ArrayList<>();

		// 1. Filtrado opcional por estado o deletedAt
		try {
			if (filtros != null && filtros.containsKey("estado")) {
				String estadoFiltro = filtros.get("estado");
				if ("ACTIVO".equalsIgnoreCase(estadoFiltro)) {
					predicates.add(cb.isNull(root.get("deletedAt")));
				} else if ("INACTIVO".equalsIgnoreCase(estadoFiltro)) {
					predicates.add(cb.isNotNull(root.get("deletedAt")));
				}
			}
		} catch (Exception e) {
			// Ignora si el campo deletedAt no existe en la entidad
		}

		if (filtros == null || filtros.isEmpty()) {
			return predicates;
		}

		filtros.forEach((key, val) -> {
			if (val != null && !val.trim().isEmpty()) {
				String k = key.trim();
				String v = val.trim();

				if (k.equalsIgnoreCase("page") || k.equalsIgnoreCase("size") || k.equalsIgnoreCase("sort") || k.equalsIgnoreCase("estado") || k.equalsIgnoreCase("incluirInactivos")) {
					return;
				}

				try {
					if (k.equalsIgnoreCase("id")) {
						try {
							Long idVal = Long.parseLong(v);
							predicates.add(cb.equal(root.get("id"), idVal));
						} catch (NumberFormatException ex) {
							// Ignorar si el ID no es numérico
						}
					} else {
						// Para cualquier otro atributo de texto (codigo, descripcion, etc.)
						predicates.add(cb.like(cb.upper(root.get(k).as(String.class)), "%" + v.toUpperCase() + "%"));
					}
				} catch (Exception e) {
					// Ignora atributos que no pertenezcan al modelo de la entidad
				}
			}
		});

		return predicates;
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

	@Override
	@Transactional
	public E toggleEstado(Long id, String observacion, String username) {
		Optional<E> optionalEntity = repository.findById(id);
		if (optionalEntity.isEmpty()) {
			throw new RuntimeException("Registro no encontrado con ID: " + id);
		}

		E entity = optionalEntity.get();
		String accion = "ACTUALIZAR";

		try {
			java.lang.reflect.Method getDeletedAt = entity.getClass().getMethod("getDeletedAt");
			java.lang.reflect.Method setDeletedAt = entity.getClass().getMethod("setDeletedAt", java.time.LocalDateTime.class);

			java.time.LocalDateTime currentDeletedAt = (java.time.LocalDateTime) getDeletedAt.invoke(entity);
			if (currentDeletedAt == null) {
				setDeletedAt.invoke(entity, java.time.LocalDateTime.now());
				accion = "INACTIVAR";
			} else {
				setDeletedAt.invoke(entity, (java.time.LocalDateTime) null);
				accion = "ACTIVAR";
			}
		} catch (Exception e) {
			throw new RuntimeException("La entidad no soporta activación/inactivación lógica: " + e.getMessage());
		}

		E savedEntity = repository.save(entity);

		if (logAccionService != null) {
			String modulo = getEntityClass() != null ? getEntityClass().getSimpleName() : "ADMIN";
			logAccionService.registrarLog(modulo, id, accion, observacion, username);
		}

		return savedEntity;
	}

}

