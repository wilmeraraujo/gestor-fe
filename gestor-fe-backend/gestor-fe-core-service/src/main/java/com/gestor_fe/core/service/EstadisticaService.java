package com.gestor_fe.core.service;

import java.time.LocalDate;
import java.util.List;
import com.gestor_fe.core.dto.EstadisticasDto.ResumenResponseDto;

public interface EstadisticaService {

    ResumenResponseDto obtenerResumenEstadisticas(
            String nit,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            String usuario,
            List<String> roles
    );
}
