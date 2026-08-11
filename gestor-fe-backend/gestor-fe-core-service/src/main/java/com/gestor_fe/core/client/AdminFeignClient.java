package com.gestor_fe.core.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.gestor_fe.core.dto.TipoDto;

@FeignClient(name = "admin-service", url = "${admin-service.url}")
public interface AdminFeignClient {

    @GetMapping("/api/v1/admin/tipo/buscar/{desc}")
    List<TipoDto> buscarPorDescripcion(@PathVariable("desc") String desc);

    @GetMapping("/api/v1/admin/tipo")
    List<TipoDto> listarTipos();
}