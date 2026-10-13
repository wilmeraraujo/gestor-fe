package com.gestor_fe.core.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.gestor_fe.core.entity.Cargue;

@Repository
public interface CargueRepository extends JpaRepository<Cargue, Long> {
	
	Page<Cargue> findByDeletedAtIsNull(Pageable pageable);

	@Query("select x from Cargue x where deletedAt is null and upper(x.nitPrestador) like upper(concat('%', ?1, '%'))")
	List<Cargue> findByNitPrestador(String desc);

	/**
	 * Consulta para el Prestador:
	 * 1. Muestra TODOS los cargues EXITOSOS (exiteError = false).
	 * 2. Muestra ÚNICAMENTE EL ÚLTIMO cargue con error (exiteError = true), siempre y cuando
	 *    no exista un cargue exitoso posterior a él.
	 */
	@Query("SELECT c FROM Cargue c WHERE c.deletedAt IS NULL AND UPPER(TRIM(c.usuario)) = UPPER(TRIM(:usuario)) AND (" +
	       "  c.exiteError = false OR " +
	       "  (" +
	       "     c.exiteError = true " +
	       "     AND c.id = (" +
	       "        SELECT MAX(c2.id) FROM Cargue c2 WHERE c2.deletedAt IS NULL AND UPPER(TRIM(c2.usuario)) = UPPER(TRIM(:usuario)) AND c2.exiteError = true" +
	       "     )" +
	       "     AND c.id > COALESCE((" +
	       "        SELECT MAX(c3.id) FROM Cargue c3 WHERE c3.deletedAt IS NULL AND UPPER(TRIM(c3.usuario)) = UPPER(TRIM(:usuario)) AND c3.exiteError = false" +
	       "     ), 0)" +
	       "  )" +
	       ")")
	Page<Cargue> findCarguesVisiblesPrestador(@Param("usuario") String usuario, Pageable pageable);

}