package com.gestor_fe.core.service.impl;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;

import com.gestor_fe.core.service.ExportarExcelService;

@Service
public class ExportarExcelServiceImpl<E> implements ExportarExcelService<E> {

	@Override
    public byte[] exportToExcel(List<E> data) throws IOException {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("No data to export");
        }

        try (SXSSFWorkbook workbook = new SXSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Data");

            // Crear encabezado
            Row headerRow = sheet.createRow(0);
            Field[] fields = data.get(0).getClass().getDeclaredFields();
            for (int i = 1; i < fields.length - 1; i++) {
                fields[i].setAccessible(true);
                headerRow.createCell(i).setCellValue(fields[i].getName());
            }

            // Rellenar datos
            int rowNum = 1;
            for (E item : data) {
                Row row = sheet.createRow(rowNum++);
                for (int i = 1; i < fields.length - 1; i++) {
                    Object value = fields[i].get(item);
                    row.createCell(i).setCellValue(value != null ? value.toString() : "");
                }
            }

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                workbook.write(outputStream);
                workbook.dispose();
                return outputStream.toByteArray();
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error accessing data fields", e);
        }
    }

}
