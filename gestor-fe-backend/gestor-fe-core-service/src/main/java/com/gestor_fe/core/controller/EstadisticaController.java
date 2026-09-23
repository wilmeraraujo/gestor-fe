package com.gestor_fe.core.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gestor_fe.core.dto.EstadisticasDto.ResumenResponseDto;
import com.gestor_fe.core.service.EstadisticaService;

@RestController
@RequestMapping("/api/v1/estadisticas")
@CrossOrigin("*")
public class EstadisticaController {

    @Autowired
    private EstadisticaService estadisticaService;

    @GetMapping("/resumen")
    public ResponseEntity<ResumenResponseDto> obtenerResumen(
            @RequestParam(value = "nit", required = false) String nit,
            @RequestParam(value = "fechaInicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(value = "fechaFin", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(value = "usuario", required = false) String usuario,
            @RequestParam(value = "roles", required = false) List<String> roles
    ) {
        ResumenResponseDto resumen = estadisticaService.obtenerResumenEstadisticas(
                nit,
                fechaInicio,
                fechaFin,
                usuario,
                roles
        );
        return ResponseEntity.ok(resumen);
    }
}
