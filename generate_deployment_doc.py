# -*- coding: utf-8 -*-
"""
Script para generar el documento Word técnico de Arquitectura y Despliegue de Gestor FE.
Mutual Assolidaria 2 / HostDime Cloud
Incluye:
- Guía detallada de ejecución de los scripts .py (opción por opción).
- Configuración técnica de certificados SSL/TLS, HTTPS, Keycloak, Frontend y API Gateway.
- Certificados de firma digital DIAN (XAdES-BES).
"""

import os
import sys
from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_ALIGN_VERTICAL
from docx.oxml import OxmlElement, parse_xml
from docx.oxml.ns import nsdecls, qn

def set_cell_background(cell, fill_hex):
    """Establece el color de fondo de una celda."""
    tcPr = cell._element.get_or_add_tcPr()
    shd = parse_xml(f'<w:shd {nsdecls("w")} w:fill="{fill_hex}"/>')
    tcPr.append(shd)

def set_cell_margins(cell, top=140, bottom=140, left=180, right=180):
    """Establece el padding interior de una celda en dxa (1 pt = 20 dxa)."""
    tcPr = cell._element.get_or_add_tcPr()
    tcMar = parse_xml(
        f'<w:tcMar {nsdecls("w")}>'
        f'<w:top w:w="{top}" w:type="dxa"/>'
        f'<w:bottom w:w="{bottom}" w:type="dxa"/>'
        f'<w:left w:w="{left}" w:type="dxa"/>'
        f'<w:right w:w="{right}" w:type="dxa"/>'
        f'</w:tcMar>'
    )
    tcPr.append(tcMar)

def set_cell_borders(cell, top="CCCCCC", bottom="CCCCCC", left="CCCCCC", right="CCCCCC", sz="4"):
    """Establece bordes finos o personalizados para una celda."""
    tcPr = cell._element.get_or_add_tcPr()
    borders = parse_xml(
        f'<w:tcBorders {nsdecls("w")}>'
        f'<w:top w:val="single" w:sz="{sz}" w:space="0" w:color="{top}"/>'
        f'<w:bottom w:val="single" w:sz="{sz}" w:space="0" w:color="{bottom}"/>'
        f'<w:left w:val="single" w:sz="{sz}" w:space="0" w:color="{left}"/>'
        f'<w:right w:val="single" w:sz="{sz}" w:space="0" w:color="{right}"/>'
        f'</w:tcBorders>'
    )
    tcPr.append(borders)

def add_callout(doc, text, title="NOTA IMPORTANTE", border_color="005691", bg_color="F0F4F8"):
    """Crea una caja de aviso destacada tipo Callout."""
    table = doc.add_table(rows=1, cols=1)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False
    
    cell = table.cell(0, 0)
    cell.width = Inches(6.5)
    set_cell_background(cell, bg_color)
    set_cell_margins(cell, top=160, bottom=160, left=240, right=200)
    
    tcPr = cell._element.get_or_add_tcPr()
    borders = parse_xml(
        f'<w:tcBorders {nsdecls("w")}>'
        f'<w:top w:val="none"/>'
        f'<w:bottom w:val="none"/>'
        f'<w:left w:val="single" w:sz="30" w:space="0" w:color="{border_color}"/>'
        f'<w:right w:val="none"/>'
        f'</w:tcBorders>'
    )
    tcPr.append(borders)
    
    p = cell.paragraphs[0]
    p.paragraph_format.space_before = Pt(2)
    p.paragraph_format.space_after = Pt(3)
    p.paragraph_format.line_spacing = 1.15
    run_title = p.add_run(f"📌 {title}: ")
    run_title.bold = True
    run_title.font.name = "Calibri"
    run_title.font.size = Pt(10.5)
    run_title.font.color.rgb = RGBColor.from_string(border_color)
    
    run_text = p.add_run(text)
    run_text.font.name = "Calibri"
    run_text.font.size = Pt(10)
    run_text.font.color.rgb = RGBColor(40, 40, 40)
    
    p_spacer = doc.add_paragraph()
    p_spacer.paragraph_format.space_before = Pt(0)
    p_spacer.paragraph_format.space_after = Pt(4)

def add_code_block(doc, code_text):
    """Crea una caja de bloque de código con fuente monoespaciada y fondo gris."""
    table = doc.add_table(rows=1, cols=1)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False
    
    cell = table.cell(0, 0)
    cell.width = Inches(6.5)
    set_cell_background(cell, "F4F6F9")
    set_cell_margins(cell, top=140, bottom=140, left=180, right=180)
    set_cell_borders(cell, top="D1D5DB", bottom="D1D5DB", left="D1D5DB", right="D1D5DB", sz="4")
    
    p = cell.paragraphs[0]
    p.paragraph_format.space_before = Pt(0)
    p.paragraph_format.space_after = Pt(0)
    p.paragraph_format.line_spacing = 1.05
    run = p.add_run(code_text)
    run.font.name = "Consolas"
    run.font.size = Pt(9)
    run.font.color.rgb = RGBColor(30, 30, 30)
    
    p_spacer = doc.add_paragraph()
    p_spacer.paragraph_format.space_before = Pt(0)
    p_spacer.paragraph_format.space_after = Pt(4)

def add_header_styled(doc, text, level):
    h = doc.add_heading(text, level=level)
    h.paragraph_format.keep_with_next = True
    if level == 1:
        h.paragraph_format.space_before = Pt(18)
        h.paragraph_format.space_after = Pt(6)
        for run in h.runs:
            run.font.name = "Calibri"
            run.font.size = Pt(15.5)
            run.font.bold = True
            run.font.color.rgb = RGBColor(0, 51, 102) # Navy Blue
    elif level == 2:
        h.paragraph_format.space_before = Pt(13)
        h.paragraph_format.space_after = Pt(4)
        for run in h.runs:
            run.font.name = "Calibri"
            run.font.size = Pt(12.5)
            run.font.bold = True
            run.font.color.rgb = RGBColor(15, 76, 129) # Classic Blue
    elif level == 3:
        h.paragraph_format.space_before = Pt(9)
        h.paragraph_format.space_after = Pt(2)
        for run in h.runs:
            run.font.name = "Calibri"
            run.font.size = Pt(11)
            run.font.bold = True
            run.font.color.rgb = RGBColor(50, 50, 50)
    return h

def add_body_paragraph(doc, text, bold_prefix=None, space_after=4):
    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(0)
    p.paragraph_format.space_after = Pt(space_after)
    p.paragraph_format.line_spacing = 1.15
    if bold_prefix:
        r_pre = p.add_run(bold_prefix)
        r_pre.bold = True
        r_pre.font.name = "Calibri"
        r_pre.font.size = Pt(10)
        r_pre.font.color.rgb = RGBColor(20, 20, 20)
    r = p.add_run(text)
    r.font.name = "Calibri"
    r.font.size = Pt(10)
    r.font.color.rgb = RGBColor(40, 40, 40)
    return p

def add_bullet_point(doc, text, bold_prefix=None):
    p = doc.add_paragraph(style='List Bullet')
    p.paragraph_format.space_before = Pt(0)
    p.paragraph_format.space_after = Pt(3)
    p.paragraph_format.line_spacing = 1.15
    if bold_prefix:
        r_pre = p.add_run(bold_prefix)
        r_pre.bold = True
        r_pre.font.name = "Calibri"
        r_pre.font.size = Pt(9.5)
        r_pre.font.color.rgb = RGBColor(20, 20, 20)
    r = p.add_run(text)
    r.font.name = "Calibri"
    r.font.size = Pt(9.5)
    r.font.color.rgb = RGBColor(50, 50, 50)
    return p

def build_table(doc, headers, data, col_widths=None, header_bg="003366"):
    table = doc.add_table(rows=len(data) + 1, cols=len(headers))
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False

    # Header row
    hdr_cells = table.rows[0].cells
    for i, title in enumerate(headers):
        hdr_cells[i].text = title
        set_cell_background(hdr_cells[i], header_bg)
        set_cell_margins(hdr_cells[i], top=130, bottom=130, left=130, right=130)
        set_cell_borders(hdr_cells[i], top="002244", bottom="002244", left="002244", right="002244", sz="6")
        p = hdr_cells[i].paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.runs[0].font.name = "Calibri"
        p.runs[0].font.size = Pt(9)
        p.runs[0].font.bold = True
        p.runs[0].font.color.rgb = RGBColor(255, 255, 255)

    # Data rows
    for r_idx, row in enumerate(data):
        row_cells = table.rows[r_idx + 1].cells
        bg_color = "F8FAFC" if r_idx % 2 == 1 else "FFFFFF"
        for c_idx, val in enumerate(row):
            row_cells[c_idx].text = str(val)
            set_cell_background(row_cells[c_idx], bg_color)
            set_cell_margins(row_cells[c_idx], top=90, bottom=90, left=110, right=110)
            set_cell_borders(row_cells[c_idx], top="E2E8F0", bottom="E2E8F0", left="E2E8F0", right="E2E8F0", sz="4")
            p = row_cells[c_idx].paragraphs[0]
            p.paragraph_format.line_spacing = 1.1
            p.paragraph_format.space_before = Pt(0)
            p.paragraph_format.space_after = Pt(0)
            if p.runs:
                p.runs[0].font.name = "Calibri"
                p.runs[0].font.size = Pt(8.5)
                p.runs[0].font.color.rgb = RGBColor(40, 40, 40)

    # Set column widths if provided
    if col_widths:
        for row in table.rows:
            for i, w in enumerate(col_widths):
                row.cells[i].width = Inches(w)

    p_sp = doc.add_paragraph()
    p_sp.paragraph_format.space_before = Pt(0)
    p_sp.paragraph_format.space_after = Pt(5)

def main():
    doc = Document()

    # Configuración de márgenes estándar (1 pulgada = 2.54 cm)
    for section in doc.sections:
        section.top_margin = Inches(1)
        section.bottom_margin = Inches(1)
        section.left_margin = Inches(1)
        section.right_margin = Inches(1)
        
        # Header y Footer
        header = section.header
        hp = header.paragraphs[0]
        hp.alignment = WD_ALIGN_PARAGRAPH.RIGHT
        hrun = hp.add_run("GESTOR FE | Documento Técnico de Arquitectura y Despliegue")
        hrun.font.name = "Calibri"
        hrun.font.size = Pt(8.5)
        hrun.font.color.rgb = RGBColor(120, 120, 120)
        
        footer = section.footer
        fp = footer.paragraphs[0]
        fp.alignment = WD_ALIGN_PARAGRAPH.CENTER
        frun = fp.add_run("Mutual Assolidaria 2 • Entorno Cloud HostDime • Confidencial")
        frun.font.name = "Calibri"
        frun.font.size = Pt(8.5)
        frun.font.color.rgb = RGBColor(140, 140, 140)

    # =========================================================================
    # PORTADA PROFESIONAL
    # =========================================================================
    p_cov_sp = doc.add_paragraph()
    p_cov_sp.paragraph_format.space_before = Pt(28)

    p_badge = doc.add_paragraph()
    r_badge = p_badge.add_run("MANUAL TÉCNICO, ARQUITECTURA DE INFRAESTRUCTURA Y OPERACIONES")
    r_badge.font.name = "Calibri"
    r_badge.font.size = Pt(11)
    r_badge.font.bold = True
    r_badge.font.color.rgb = RGBColor(0, 102, 204)

    p_title = doc.add_paragraph()
    p_title.paragraph_format.space_before = Pt(6)
    p_title.paragraph_format.space_after = Pt(12)
    r_title = p_title.add_run("SISTEMA DE FACTURACIÓN ELECTRÓNICA\nGESTOR FE")
    r_title.font.name = "Calibri"
    r_title.font.size = Pt(26)
    r_title.font.bold = True
    r_title.font.color.rgb = RGBColor(0, 51, 102)

    p_sub = doc.add_paragraph()
    p_sub.paragraph_format.space_before = Pt(0)
    p_sub.paragraph_format.space_after = Pt(20)
    r_sub = p_sub.add_run(
        "Arquitectura de Despliegue, Guía Completa de Ejecución de Scripts (.py),\n"
        "Configuración Criptográfica SSL/TLS de Keycloak, Frontend y API Gateway,\n"
        "Requisitos de Entorno y Procedimientos de Producción"
    )
    r_sub.font.name = "Calibri"
    r_sub.font.size = Pt(12.5)
    r_sub.font.color.rgb = RGBColor(80, 80, 80)

    p_line = doc.add_paragraph()
    p_line.paragraph_format.space_after = Pt(24)
    r_line = p_line.add_run("―" * 50)
    r_line.font.color.rgb = RGBColor(0, 102, 204)
    r_line.font.bold = True

    meta_headers = ["Parámetro", "Detalle del Sistema"]
    meta_data = [
        ["Organización / Tenant", "MUTUAL ASSOLIDARIA 2 (HostDime Cloud)"],
        ["Sistema / Producto", "Gestor FE (Facturación Electrónica DIAN)"],
        ["Topología", "Multi-servidor (3 Máquinas Virtuales en Nube Privada 10.3.26.0/24)"],
        ["Versión de Arquitectura", "v2.0 (Spring Cloud + Angular 17 + Keycloak 26 + Nginx SSL)"],
        ["Seguridad Perimetral", "Túnel VPN OpenVPN PKI + Certificados SSL Let's Encrypt / ZeroSSL"],
        ["Fecha de Emisión", "Octubre 2026"],
        ["Clasificación", "Documento de Ingeniería y Operaciones (Uso Técnico Exclusivo)"]
    ]
    build_table(doc, meta_headers, meta_data, col_widths=[2.2, 4.3], header_bg="003366")

    doc.add_page_break()

    # =========================================================================
    # TABLA DE CONTENIDO RESUMIDA
    # =========================================================================
    add_header_styled(doc, "ÍNDICE GENERAL DEL DOCUMENTO", level=1)
    
    sections_index = [
        ["1. Resumen Ejecutivo y Alcance de la Plataforma", "Visión general, propósito del sistema Gestor FE y principios de seguridad."],
        ["2. Topología e Infraestructura de Servidores (VMs)", "Distribución de VM1 (BD), VM2 (Backend) y VM3 (Frontend/Auth), red y túnel VPN."],
        ["3. Requisitos Previos e Instalación en Máquina Local", "Matriz de herramientas mandatorias: Python, Docker, JDK 17, Maven, Node.js y OpenVPN."],
        ["4. Arquitectura de Software y Ecosistema de Microservicios", "Spring Cloud Eureka, Config Server, Gateway, Admin Server, Core y Admin Service."],
        ["5. Catálogo Técnico de Archivos y Scripts de Despliegue", "Análisis estructural de deploy_infra.py, deploy_back.py, deploy_front.py y configs."],
        ["6. Guía Detallada de Ejecución de los Scripts Python (.py)", "Instrucciones de ejecución paso a paso, opciones de menú, flags (--all) y mecánica interna."],
        ["7. Configuración de Certificados SSL/TLS, Keycloak, Frontend y Gateway", "Terminación SSL en Nginx, certificados de Keycloak (RS256/JWKS), CORS en Gateway y firma DIAN."],
        ["8. Guía Paso a Paso para el Despliegue en Producción", "Procedimiento cronológico estricto de compilación, empaquetado y arranque."],
        ["9. Matriz Unificada de Credenciales, Puertos, Variables y Rutas", "Inventario unificado de accesos SSH, conexiones JDBC, volúmenes de almacenamiento."],
        ["10. Diagnóstico, Verificación Post-Despliegue y Rollback", "Comandos de salud (Health Check), lectura de logs y contingencia ante fallos."]
    ]
    build_table(doc, ["Capítulo", "Descripción del Contenido"], sections_index, col_widths=[2.8, 3.7], header_bg="004080")

    # =========================================================================
    # CAPÍTULO 1: RESUMEN EJECUTIVO Y ALCANCE
    # =========================================================================
    add_header_styled(doc, "1. RESUMEN EJECUTIVO Y ALCANCE DE LA PLATAFORMA", level=1)
    
    add_body_paragraph(
        doc,
        "El presente documento técnico tiene como propósito describir exhaustivamente la arquitectura "
        "de despliegue y desarrollo del sistema Gestor Facturación Electrónica (Gestor FE) implementado para "
        "la organización MUTUAL ASSOLIDARIA 2 en la plataforma de nube privada HostDime Cloud."
    )
    add_body_paragraph(
        doc,
        "Gestor FE es una solución empresarial de alta criticidad encargada de procesar, validar, firmar, transmitir "
        "y custodiar documentos electrónicos (facturas, notas débito, notas crédito y documentos soporte) ante la "
        "Dirección de Impuestos y Aduanas Nacionales (DIAN) de Colombia. Debido a su volumen y naturaleza transaccional, "
        "el sistema fue desacoplado en una arquitectura moderna basada en microservicios contenerizados mediante Docker, "
        "con gestión de identidad centralizada (IAM) y una infraestructura segmentada en tres niveles computacionales independientes."
    )

    add_callout(
        doc,
        "Toda interacción con los entornos de backend, base de datos y servidores de producción debe realizarse a "
        "través del túnel seguro OpenVPN proporcionado por el proveedor de nube. Ningún puerto interno (PostgreSQL 5432, "
        "MongoDB 27017, Eureka 8761, Gateway 8090, microservicios 9095/9096) se encuentra expuesto directamente a la "
        "Internet pública, garantizando el aislamiento de datos corporativos.",
        title="PRINCIPIO DE SEGURIDAD PERIMETRAL"
    )

    # =========================================================================
    # CAPÍTULO 2: TOPOLOGÍA E INFRAESTRUCTURA DE SERVIDORES
    # =========================================================================
    add_header_styled(doc, "2. TOPOLOGÍA E INFRAESTRUCTURA DE SERVIDORES", level=1)
    
    add_body_paragraph(
        doc,
        "La arquitectura física/virtual de Gestor FE se encuentra distribuida en tres (3) Máquinas Virtuales (VM) "
        "con sistema operativo Linux, interconectadas en una red de área local privada bajo el segmento 10.3.26.0/24."
    )

    add_header_styled(doc, "2.1. Servidor de Bases de Datos: VM1 (10.3.26.2)", level=2)
    add_body_paragraph(
        doc,
        "Aloja el repositorio de persistencia transaccional relacional y no relacional. Los motores operan contenerizados "
        "en Docker con persistencia vinculada a volúmenes físicos en el sistema de archivos del host (/opt/databases/)."
    )
    add_bullet_point(doc, "Versión 16. Puerto expuesto 5432. Aloja las bases de datos gestor_fe_core_db (esquemas: admin, gestor, logs, public) y keycloak (tablas maestras de identidad y tokens).", bold_prefix="PostgreSQL: ")
    add_bullet_point(doc, "Versión 7.0. Puerto expuesto 27017. Almacenamiento no estructurado de logs transaccionales y auditoría.", bold_prefix="MongoDB: ")
    add_bullet_point(doc, "Usuario root en puerto 22, clave configurada MTL2026*-.", bold_prefix="Acceso Administrativo SSH: ")

    add_header_styled(doc, "2.2. Servidor Backend y Microservicios: VM2 (10.3.26.3)", level=2)
    add_body_paragraph(
        doc,
        "Constituye el núcleo de cómputo del sistema. En esta máquina residen los microservicios de infraestructura "
        "Spring Cloud y los microservicios de lógica de negocio, comunicados a través de una red bridge interna Docker "
        "denominada 'gestor-fe-network'."
    )
    add_bullet_point(doc, "Netflix Eureka Discovery Server (Puerto 8761), Spring Cloud Config Server (Puerto 8888), Spring Cloud Gateway (Puerto 8090) y Spring Boot Admin Server (Puerto 9000).", bold_prefix="Servicios Cloud: ")
    add_bullet_point(doc, "gestor-fe-admin-service (Puerto 9095) y gestor-fe-core-service (Puerto 9096).", bold_prefix="Servicios de Negocio: ")
    add_bullet_point(doc, "Almacenamiento persistente en /opt/storage/gestion-fe/ y /opt/storage/gestion-fe-validos/ para los documentos XML, PDF y anexos procesados.", bold_prefix="Volúmenes Físicos: ")

    add_header_styled(doc, "2.3. Servidor Frontend, Proxy Reverso y Autenticación: VM3 (10.3.26.4)", level=2)
    add_body_paragraph(
        doc,
        "Punto de contacto para los usuarios finales y los navegadores web. Maneja la capa de presentación SPA, el "
        "servidor de autorización OIDC/OAuth2 y el proxy reverso que canaliza las peticiones hacia la infraestructura."
    )
    add_bullet_point(doc, "Versión 26.0.0. Contenedor 'keycloak_app', puerto interno 8080. Gestiona autenticación, usuarios, roles, SSO y emisión de JWT para el realm 'gestor-fe'.", bold_prefix="Keycloak IAM: ")
    add_bullet_point(doc, "Aplicación Angular 17 servida en contenedor multi-stage sobre Nginx Alpine (Puertos 80 y 4200).", bold_prefix="Frontend Angular: ")
    add_bullet_point(doc, "Configuración en /opt/nginx/conf.d/default.conf encargada del enrutamiento SPA, reenvío de tráfico a Keycloak y al API Gateway.", bold_prefix="Nginx Reverse Proxy: ")

    vm_headers = ["Servidor", "IP Privada", "Rol Principal", "Servicios Contenerizados", "Puertos Expuestos"]
    vm_table_data = [
        ["VM1", "10.3.26.2", "Bases de Datos", "PostgreSQL 16, MongoDB 7.0", "5432, 27017, 22 (SSH)"],
        ["VM2", "10.3.26.3", "Backend & Cloud", "Eureka, Config, Gateway, Admin Server, Admin Service, Core Service", "8761, 8888, 8090, 9000, 9095, 9096, 22"],
        ["VM3", "10.3.26.4", "Frontend & IAM", "Angular 17 (Nginx), Keycloak 26", "80, 4200, 8080, 22"]
    ]
    build_table(doc, vm_headers, vm_table_data, col_widths=[0.8, 1.1, 1.3, 2.3, 1.0])

    # =========================================================================
    # CAPÍTULO 3: REQUISITOS PREVIOS E INSTALACIÓN EN MÁQUINA LOCAL
    # =========================================================================
    add_header_styled(doc, "3. REQUISITOS PREVIOS E INSTALACIÓN EN LA MÁQUINA LOCAL", level=1)
    
    add_body_paragraph(
        doc,
        "Para poder compilar los proyectos, generar los contenedores y ejecutar los scripts automatizados de despliegue, "
        "el equipo de desarrollo o el ingeniero de operaciones debe contar con el siguiente conjunto de herramientas "
        "instaladas y configuradas en su equipo local (Windows 10/11 o Linux):"
    )

    req_headers = ["Herramienta", "Versión Requerida", "Propósito en el Despliegue", "Comando de Verificación"]
    req_data = [
        ["OpenVPN Client", "2.7.4 o superior", "Conexión al túnel privado 10.3.26.0/24 de HostDime", "Icono OpenVPN conectado"],
        ["Python", "3.10 o superior", "Ejecutar scripts deploy_infra.py, deploy_back.py y deploy_front.py", "python --version"],
        ["Docker Desktop", "24.x o superior", "Construcción multi-stage de imágenes y empaquetado .tar", "docker --version"],
        ["Java JDK", "17 LTS (Eclipse Temurin)", "Compilación de microservicios Spring Boot 3", "javac -version"],
        ["Apache Maven", "3.8.x o 3.9.x", "Construcción del ciclo de vida JAR/WAR de Spring", "mvn -version"],
        ["Node.js & NPM", "Node 18/20 LTS, NPM 10+", "Compilación de dependencias Angular 17", "node -v && npm -v"],
        ["Git SCM", "2.40 o superior", "Control de versiones e integración con Spring Cloud Config", "git --version"]
    ]
    build_table(doc, req_headers, req_data, col_widths=[1.4, 1.3, 2.4, 1.4])

    add_header_styled(doc, "3.1. Detalle de Configuración de Cada Componente", level=2)
    
    add_bullet_point(
        doc,
        "Debe instalarse utilizando el ejecutable 'openvpn-MUTUAL-ASSOLIDARIA-2-UDP4-1194-ma2.user-1-install-2.7.4-I001-amd64.exe' "
        "ubicado en la raíz de despliegue. Tras la instalación, se deben importar los certificados 'MUTUAL-ASSOLIDARIA-2-UDP4-1194-ma2.user-1.p12' "
        "y 'MUTUAL-ASSOLIDARIA-2-UDP4-1194-ma2.user-1-tls.key'. Sin el túnel VPN activo, los scripts fallarán inmediatamente por timeout de conexión SSH.",
        bold_prefix="1. Conexión VPN: "
    )
    add_bullet_point(
        doc,
        "Los scripts de despliegue cuentan con un mecanismo inteligente de auto-instalación que descarga automáticamente "
        "las librerías necesarias mediante pip ('paramiko', 'rich', 'questionary', 'requests'). No obstante, se puede verificar "
        "manualmente ejecutando: pip install paramiko rich questionary requests python-docx.",
        bold_prefix="2. Entorno Python: "
    )
    add_bullet_point(
        doc,
        "Debe estar activo y configurado con Linux Containers. Es fundamental tener suficiente espacio en disco "
        "(mínimo 20 GB libres), ya que la generación de imágenes Docker y la exportación de archivos comprimidos .tar "
        "requieren almacenamiento temporal durante el empaquetado.",
        bold_prefix="3. Docker Desktop / Engine: "
    )
    add_bullet_point(
        doc,
        "La variable de entorno JAVA_HOME debe apuntar a la ruta del JDK 17 (por ejemplo, C:\\Program Files\\Eclipse Adoptium\\jdk-17). "
        "La compilación de Spring Boot 3 no es compatible con versiones obsoletas de Java como JDK 8 u 11.",
        bold_prefix="4. JDK 17 y Maven: "
    )

    # =========================================================================
    # CAPÍTULO 4: ARQUITECTURA DE SOFTWARE Y ECOSISTEMA DE MICROSERVICIOS
    # =========================================================================
    add_header_styled(doc, "4. ARQUITECTURA DE SOFTWARE Y ECOSISTEMA DE MICROSERVICIOS", level=1)
    
    add_body_paragraph(
        doc,
        "La solución Gestor FE implementa el patrón de arquitectura orientada a microservicios distribuidos, "
        "aprovechando el ecosistema Spring Cloud para la infraestructura de backend y un diseño de capa SPA desacoplada "
        "con Angular en el frontend."
    )

    add_header_styled(doc, "4.1. Módulos de Infraestructura Spring Cloud", level=2)
    add_bullet_point(
        doc,
        "Actúa como directorio telefónico de los microservicios. Cada instancia de servicio que arranca en la red "
        "se registra dinámicamente con un identificador de servicio (SPRING_APPLICATION_NAME). Permite el balanceo de "
        "cargas y el enrutamiento desacoplado de direcciones IP rígidas. URL: http://10.3.26.3:8761.",
        bold_prefix="Eureka Discovery Server: "
    )
    add_bullet_point(
        doc,
        "Centraliza los archivos de configuración YAML externos conectándose al repositorio Git "
        "(https://github.com/wilmeraraujo/gestor-fe.git, rama development, carpeta 'config-repo'). Permite modificar "
        "parámetros en caliente sin necesidad de recompilar el código fuente. URL: http://10.3.26.3:8888.",
        bold_prefix="Spring Cloud Config Server: "
    )
    add_bullet_point(
        doc,
        "Punto de entrada unificado para todas las solicitudes dirigidas a los microservicios. Implementa filtrado de "
        "cabeceras, resolución de microservicios mediante lb:// (Load Balancer con Eureka), StripPrefix=1 y "
        "política centralizada de CORS para aceptar llamadas de https://gestor-fe.assolidaria.org.co y el entorno local. "
        "Puerto: 8090.",
        bold_prefix="Spring Cloud Gateway: "
    )
    add_bullet_point(
        doc,
        "Panel gráfico de telemetría y monitoreo que recopila métricas en tiempo real de Spring Boot Actuator "
        "(uso de memoria JVM, hilos de ejecución, endpoints activos, estado de salud y reinicios de contexto). Puerto: 9000.",
        bold_prefix="Spring Boot Admin Server: "
    )

    add_header_styled(doc, "4.2. Módulos de Negocio Backend", level=2)
    add_bullet_point(
        doc,
        "Módulo de soporte transversal. Contiene las entidades JPA compartidas, DTOs, utilitarios de cifrado, "
        "manejadores globales de excepciones y constantes del negocio. Se compila como artefacto JAR e instala "
        "obligatoriamente en el repositorio Maven local (~/.m2) mediante 'mvn clean install'.",
        bold_prefix="service-common: "
    )
    add_bullet_point(
        doc,
        "Gestiona la configuración maestra del sistema, catálogos DIAN (tipos de documentos, municipios, responsabilidades "
        "fiscales, impuestos), parametrización de empresas emisoras, certificados digitales y claves técnicas. Puerto: 9095.",
        bold_prefix="gestor-fe-admin-service: "
    )
    add_bullet_point(
        doc,
        "Motor central de facturación electrónica. Procesa lotes de documentos (Spring Batch), genera la estructura XML UBL 2.1, "
        "aplica la firma digital XAdES-BES, calcula el código CUFE/CUDE, genera el código QR, transmite a la DIAN vía Web Service "
        "SOAP, interpreta las respuestas de validación y archiva los documentos válidos y con error en el almacenamiento persistente. "
        "Se comunica internamente con admin-service vía Spring Cloud OpenFeign. Puerto: 9096.",
        bold_prefix="gestor-fe-core-service: "
    )

    add_header_styled(doc, "4.3. Capa de Presentación e Identidad (Frontend & Keycloak)", level=2)
    add_bullet_point(
        doc,
        "Desarrollado en Angular 17 con arquitectura Standalone Components y módulos reactivos. "
        "Integra la librería 'keycloak-angular' / 'keycloak-js' para la interceptación automática de tokens Bearer JWT "
        "en cada petición HTTP hacia el Gateway. Empaquetado en Nginx para alto rendimiento en entrega estática.",
        bold_prefix="gestor-fe-frontend: "
    )
    add_bullet_point(
        doc,
        "Servidor de identidad basado en Keycloak 26.0.0. Maneja los flujos de autenticación OpenID Connect (Authorization Code Flow con PKCE). "
        "Cuenta con el Realm 'gestor-fe', el cliente 'gestor-fe-frontend', roles 'ADMIN', 'OPERADOR', 'CONSULTA' y un tema gráfico personalizado "
        "adaptado a la imagen institucional de Mutual Assolidaria.",
        bold_prefix="Keycloak IAM: "
    )

    # =========================================================================
    # CAPÍTULO 5: CATÁLOGO DE ARCHIVOS DE DESPLIEGUE
    # =========================================================================
    add_header_styled(doc, "5. CATÁLOGO TÉCNICO DE ARCHIVOS Y SCRIPTS DE DESPLIEGUE", level=1)
    
    add_body_paragraph(
        doc,
        "A continuación se presenta el desglose funcional de cada archivo, script y configuración que gobierna "
        "el aprovisionamiento y despliegue del ecosistema Gestor FE:"
    )

    files_headers = ["Archivo / Script", "Ubicación", "Lenguaje / Formato", "Responsabilidad Técnica"]
    files_data = [
        ["deploy_infra.py", "Raíz del proyecto", "Python 3", "Compila, empaqueta en Docker .tar y despliega en VM2 los 4 servicios Spring Cloud (Eureka, Config, Gateway, Admin)."],
        ["deploy_back.py", "Raíz del proyecto", "Python 3", "Gestiona service-common, comenta @CrossOrigin, compila y despliega admin-service y core-service en VM2; inicializa BD en VM1."],
        ["deploy_front.py", "Raíz del proyecto", "Python 3", "Construye la imagen Docker de Angular 17, sube a VM3, sincroniza nginx.conf y provisiona el Realm en Keycloak."],
        ["despliegue.txt", "Raíz del proyecto", "Texto plano", "Ficha técnica e inventario de IPs, credenciales, puertos, URLs públicas y directorios de producción."],
        ["docker-compose-infra.yml", "gestor-fe/", "Docker Compose 3.8", "Definición declarativa de orquestación local para levantar los microservicios con healthchecks y dependencias."],
        ["docker-compose-keycloak-local.yml", "Raíz del proyecto", "Docker Compose", "Entorno local de desarrollo de Keycloak 26 (puerto 8082) con importación de temas y realm."],
        ["nginx.conf", "gestor-fe/gestor-fe-frontend/", "Config Nginx", "Reglas de Reverse Proxy para subdominios gestor-fe, auth y api-gestor con soporte History Mode SPA."],
        ["gestor-fe-realm.json", "gestor-fe/gestor-fe-frontend/", "JSON", "Definición exportable del Realm de Keycloak: clientes, roles, flujos de autenticación y redirecciones."],
        ["build_all.bat / .sh", "gestor-fe/gestor-fe-backend/", "Batch / Bash", "Script por lotes para compilar secuencialmente todos los proyectos Maven saltando tests unitarios."],
        ["config-repo/*.yml", "gestor-fe/config-repo/", "YAML", "Configuraciones externas para cada microservicio: Datasources, URLs de Feign, rutas de storage y reglas CORS."]
    ]
    build_table(doc, files_headers, files_data, col_widths=[1.5, 1.4, 1.1, 2.5])

    # =========================================================================
    # CAPÍTULO 6: GUÍA DETALLADA DE EJECUCIÓN DE LOS SCRIPTS PYTHON (.PY)
    # =========================================================================
    add_header_styled(doc, "6. GUÍA DETALLADA DE EJECUCIÓN DE LOS SCRIPTS PYTHON (.PY)", level=1)
    
    add_body_paragraph(
        doc,
        "La automatización del despliegue descansa en tres scripts escritos en Python que encapsulan "
        "el flujo completo de compilación, construcción de imágenes en contenedor, transferencia segura SFTP "
        "y arranque en los servidores de HostDime. Esta sección detalla minuciosamente cómo se ejecutan, "
        "sus argumentos de línea de comandos y el comportamiento de cada opción de sus menús interactivos."
    )

    add_header_styled(doc, "6.1. Ciclo de Vida y Mecánica Interna Común", level=2)
    add_body_paragraph(
        doc,
        "Cada script ejecuta de manera transparente una secuencia estandarizada de operaciones técnicas:"
    )
    add_bullet_point(doc, "Al iniciarse, el script verifica la presencia de 'paramiko', 'rich', 'questionary' y 'requests'. Si alguna no existe, la instala en silencio mediante pip antes de continuar.", bold_prefix="1. Bootstrap de Dependencias: ")
    add_bullet_point(doc, "Para Backend e Infraestructura, ejecuta Maven ('mvn clean package -DskipTests=true') asegurando que el JAR/WAR se construya limpio. Para Frontend, ejecuta 'docker build' con compilación multi-etapa en Node 20.", bold_prefix="2. Compilación Local: ")
    add_bullet_point(doc, "Construye la imagen Docker local con el tag :latest y la serializa a un archivo binario comprimido .tar mediante 'docker save -o <nombre>.tar'.", bold_prefix="3. Empaquetado Docker: ")
    add_bullet_point(doc, "Abre una sesión SSH con Paramiko hacia el servidor remoto correspondiente (VM2 o VM3) y transfiere el archivo .tar vía SFTP mostrando una barra interactiva de progreso con velocidad y tiempo estimado.", bold_prefix="4. Transferencia SFTP: ")
    add_bullet_point(doc, "Detiene y elimina el contenedor antiguo ('docker rm -f'), carga la nueva imagen en el daemon remoto ('docker load -i') y arranca el contenedor con su red, volúmenes y variables de entorno.", bold_prefix="5. Despliegue Remoto: ")
    add_bullet_point(doc, "Espera de 2 a 3 segundos y ejecuta 'docker ps' remoto para certificar que el contenedor se encuentra en estado 'Up' y escuchando en el puerto requerido.", bold_prefix="6. Verificación de Estado: ")

    add_header_styled(doc, "6.2. Script deploy_infra.py: Infraestructura Cloud en VM2 (10.3.26.3)", level=2)
    add_body_paragraph(
        doc,
        "Este script administra el ciclo de vida de los 4 microservicios de soporte en VM2: "
        "Eureka Server (8761), Config Server (8888), Spring Cloud Gateway (8090) y Spring Boot Admin Server (9000)."
    )
    add_body_paragraph(doc, "Sintaxis de ejecución en terminal (PowerShell / Bash):", bold_prefix="Comandos de Ejecución: ")
    add_code_block(doc, "# Modo interactivo con menú gráfico:\npython deploy_infra.py\n\n# Modo automatizado / desatendido (despliega los 4 servicios en orden):\npython deploy_infra.py --all")

    add_body_paragraph(doc, "Desglose de opciones del menú interactivo de deploy_infra.py:", bold_prefix="Opciones Disponibles: ")
    
    infra_options = [
        ["Opción 1", "🚀 Desplegar TODOS", "Ejecuta secuencialmente Eureka -> Config -> Gateway -> Admin. Introduce una pausa de 10s tras Eureka para permitir su registro."],
        ["Opción 2", "📡 Desplegar solo Eureka Server", "Compila, empaqueta y levanta gestor-fe-eureka-server en el puerto 8761."],
        ["Opción 3", "⚙️ Desplegar solo Config Server", "Compila, empaqueta y levanta gestor-fe-config-server en el puerto 8888."],
        ["Opción 4", "🌐 Desplegar solo Gateway", "Compila, empaqueta y levanta gestor-fe-gateway en el puerto 8090."],
        ["Opción 5", "🖥️ Desplegar solo Admin Server", "Compila, empaqueta y levanta gestor-fe-admin-server en el puerto 9000."],
        ["Opción 6", "📊 Ver estado de contenedores", "Ejecuta 'docker ps -a' en VM2 y dibuja una tabla Rich con nombres, estado y puertos."],
        ["Opciones 7-10", "📜 Ver logs de servicios", "Muestra en tiempo real las últimas 100 líneas del log de Eureka, Config, Gateway o Admin Server."],
        ["Opción 11", "🔍 Probar conectividad HTTP", "Realiza peticiones GET a los endpoints HTTP de cada servicio y valida códigos de respuesta 200."],
        ["Opción 12", "🔄 Reiniciar servicios", "Envía la orden 'docker restart' remota a los 4 contenedores de infraestructura."],
        ["Opción 13", "❌ Salir", "Finaliza la ejecución del script limpiamente."]
    ]
    build_table(doc, ["Opción", "Acción del Menú", "Comportamiento Técnico Detallado"], infra_options, col_widths=[1.1, 2.2, 3.2])

    add_header_styled(doc, "6.3. Script deploy_back.py: Backend en VM2 y Base de Datos en VM1", level=2)
    add_body_paragraph(
        doc,
        "Gestiona los dos microservicios centrales de negocio ('admin-service' y 'core-service') y "
        "brinda utilidades para el aprovisionamiento de PostgreSQL en VM1 (10.3.26.2)."
    )
    add_code_block(doc, "# Modo interactivo con menú gráfico:\npython deploy_back.py\n\n# Modo automatizado / desatendido (despliega service-common + admin + core):\npython deploy_back.py --all")

    add_callout(
        doc,
        "Antes de compilar 'admin-service' o 'core-service', deploy_back.py busca en todos los archivos .java "
        "las anotaciones '@CrossOrigin' y las comenta automáticamente mediante expresiones regulares. "
        "Esto evita que el microservicio envíe cabeceras de CORS duplicadas que colisionen con las cabeceras "
        "inyectadas por Spring Cloud Gateway. Al finalizar el empaquetado, un bloque 'finally' SIEMPRE restaura y "
        "descomenta las anotaciones para no romper el entorno de desarrollo local.",
        title="AUTOMATIZACIÓN CRÍTICA DE CORS EN CONTROLADORES"
    )

    back_options = [
        ["Opción 1", "🚀 Desplegar TODO el Backend", "Compila e instala service-common en ~/.m2, comenta CORS, compila admin y core, empaqueta en .tar, transfiere y arranca."],
        ["Opción 2", "📦 Desplegar solo admin-service", "Compila y despliega únicamente gestor-fe-admin-service en puerto 9095 de VM2."],
        ["Opción 3", "⚙️ Desplegar solo core-service", "Compila y despliega únicamente gestor-fe-core-service en puerto 9096 de VM2."],
        ["Opción 4", "🗄️ Inicializar / Migrar BD (VM1)", "Conecta a VM1 (10.3.26.2), crea los esquemas 'admin', 'gestor', 'logs' y ejecuta gestor_fe_core_db.sql."],
        ["Opción 5", "📊 Ver estado en VM2 Backend", "Muestra la tabla de contenedores Docker activos en VM2 con sus volúmenes montados."],
        ["Opciones 6-7", "📜 Ver logs de servicios", "Muestra las últimas 100 líneas de log de admin-service o core-service."],
        ["Opción 8", "🔄 Reiniciar contenedores", "Ejecuta 'docker restart gestor-fe-admin-service gestor-fe-core-service'."],
        ["Opción 9", "🔒 Comentar @CrossOrigin", "Fuerza el comentado manual de @CrossOrigin en todos los controladores Java."],
        ["Opción 10", "🔓 Descomentar @CrossOrigin", "Restaura manualmente @CrossOrigin en todos los controladores Java."],
        ["Opción 11", "❌ Salir", "Cierra el menú interactivo."]
    ]
    build_table(doc, ["Opción", "Acción del Menú", "Comportamiento Técnico Detallado"], back_options, col_widths=[1.1, 2.2, 3.2])

    add_header_styled(doc, "6.4. Script deploy_front.py: Frontend Angular y Keycloak en VM3 (10.3.26.4)", level=2)
    add_body_paragraph(
        doc,
        "Controla el frontend web en Angular 17 y la sincronización declarativa del servidor de identidad Keycloak 26."
    )
    add_code_block(doc, "# Modo interactivo con menú gráfico:\npython deploy_front.py\n\n# Modo automatizado / desatendido (Frontend + Sincronización Keycloak):\npython deploy_front.py --all")

    front_options = [
        ["Opción 1", "🚀 Desplegar Frontend Angular", "Ejecuta Docker build multi-stage (Node 20 -> Nginx Alpine), transfiere el .tar a VM3, sincroniza nginx.conf y levanta contenedor."],
        ["Opción 2", "🔐 Sincronizar Realm Keycloak", "Obtiene token de admin con admin-cli, consulta si el Realm 'gestor-fe' existe; si existe actualiza clientes y Web Origins; si no, importa gestor-fe-realm.json."],
        ["Opción 3", "📊 Ver estado en VM3", "Consulta el estado de contenedores en 10.3.26.4 (gestor-fe-frontend y keycloak_app)."],
        ["Opciones 4-5", "📜 Ver logs", "Muestra las últimas 100 líneas del log de Nginx Frontend o del servidor Keycloak."],
        ["Opción 6", "🔍 Probar conectividad HTTP", "Realiza pruebas HTTP a https://gestor-fe.assolidaria.org.co, https://auth.assolidaria.org.co e IP directa."],
        ["Opción 7", "🔄 Reiniciar contenedor", "Reinicia el contenedor Docker 'gestor-fe-frontend'."],
        ["Opción 8", "❌ Salir", "Cierra la utilidad."]
    ]
    build_table(doc, ["Opción", "Acción del Menú", "Comportamiento Técnico Detallado"], front_options, col_widths=[1.1, 2.2, 3.2])

    # =========================================================================
    # CAPÍTULO 7: CONFIGURACIÓN DE CERTIFICADOS SSL/TLS, KEYCLOAK, FRONTEND Y GATEWAY
    # =========================================================================
    add_header_styled(doc, "7. CONFIGURACIÓN DE CERTIFICADOS SSL/TLS, KEYCLOAK, FRONTEND Y API GATEWAY", level=1)
    
    add_body_paragraph(
        doc,
        "La seguridad en el transporte de datos, la gestión de identidades y la firma electrónica de documentos "
        "en Gestor FE involucra múltiples capas criptográficas: certificados SSL/TLS para el cifrado web HTTPS, "
        "certificados y llaves asimétricas del Realm de Keycloak para la emisión y verificación de tokens JWT, "
        "y certificados digitales corporativos para la firma digital UBL 2.1 ante la DIAN."
    )

    add_header_styled(doc, "7.1. Arquitectura de Terminación SSL/TLS en Nginx (VM3 10.3.26.4)", level=2)
    add_body_paragraph(
        doc,
        "Para evitar la sobrecarga criptográfica de cifrado y descifrado en los microservicios Java (Spring Boot) "
        "y simplificar la gestión centralizada de certificados, Gestor FE adopta el patrón de "
        "'Terminación SSL / SSL Offloading' en el Proxy Reverso Nginx alojado en VM3."
    )
    add_body_paragraph(
        doc,
        "Todos los subdominios institucionales de la organización apuntan en el DNS público a la dirección IP pública "
        "de VM3 (asociada internamente a la interfaz 10.3.26.4):"
    )
    add_bullet_point(doc, "Punto de acceso a la aplicación web SPA de Angular 17.", bold_prefix="https://gestor-fe.assolidaria.org.co: ")
    add_bullet_point(doc, "Punto de autenticación OIDC/OAuth2 y consola administrativa de Keycloak 26.", bold_prefix="https://auth.assolidaria.org.co: ")
    add_bullet_point(doc, "Punto de entrada HTTPS hacia el Spring Cloud Gateway (enrutado a VM2 10.3.26.3:8090).", bold_prefix="https://api-gestor.assolidaria.org.co: ")

    add_callout(
        doc,
        "El servidor Nginx recibe el tráfico cifrado HTTPS (puerto 443) proveniente de internet, valida los certificados "
        "SSL y redirige las peticiones internamente por HTTP sin cifrar hacia Keycloak (10.3.26.4:8080) y hacia el "
        "API Gateway (10.3.26.3:8090) a través de la red privada segura de HostDime. Para que Keycloak y el Gateway "
        "sepan que el usuario original navegaba por HTTPS, Nginx inyecta de manera obligatoria las cabeceras:\n"
        "• proxy_set_header X-Forwarded-Proto $scheme; (o https)\n"
        "• proxy_set_header X-Forwarded-Host $host;\n"
        "• proxy_set_header X-Forwarded-Port $server_port; (443)",
        title="MECANISMO DE CABECERAS X-FORWARDED-*"
    )

    add_header_styled(doc, "7.2. Generación e Instalación de Certificados SSL (Let's Encrypt / ZeroSSL)", level=2)
    add_body_paragraph(
        doc,
        "Los certificados SSL para los subdominios *.assolidaria.org.co se obtienen mediante el protocolo ACME "
        "utilizando la herramienta Certbot o mediante el panel del proveedor DNS / Cloud. Existen dos modalidades de validación:"
    )
    add_bullet_point(
        doc,
        "Se ejecuta 'sudo certbot certonly --manual --preferred-challenges dns -d *.assolidaria.org.co'. "
        "Certbot genera un valor de texto secreto (hash) que debe crearse como registro TXT en el DNS bajo el nombre '_acme-challenge'. "
        "Una vez propagado el registro, la entidad certificadora emite el certificado comodín válido para todos los subdominios.",
        bold_prefix="1. Validación por DNS (Wildcard): "
    )
    add_bullet_point(
        doc,
        "Si los subdominios ya resuelven directamente hacia la IP de VM3 y los puertos 80 y 443 están abiertos en el firewall, "
        "se ejecuta 'sudo certbot certonly --nginx -d gestor-fe.assolidaria.org.co -d auth.assolidaria.org.co -d api-gestor.assolidaria.org.co'. "
        "Certbot valida la pertenencia del servidor respondiendo a un desafío HTTP temporal sin intervención manual de registros DNS.",
        bold_prefix="2. Validación por HTTP (Certbot Standalone / Nginx): "
    )

    add_body_paragraph(doc, "Estructura de archivos de certificados generados en Linux:", bold_prefix="Rutas de Certificados: ")
    add_code_block(doc, "/etc/letsencrypt/live/assolidaria.org.co/fullchain.pem   # Certificado público + cadena intermedia (CA)\n/etc/letsencrypt/live/assolidaria.org.co/privkey.pem     # Llave privada RSA de 2048/4096 bits (Confidencial)")

    add_header_styled(doc, "7.3. Configuración en Nginx (nginx.conf)", level=2)
    add_body_paragraph(
        doc,
        "El archivo de configuración de Nginx (/opt/nginx/conf.d/default.conf) montado en el contenedor define "
        "los bloques de servidor para cada subdominio:"
    )
    add_code_block(doc, """# =========================================================================
# CONFIGURACIÓN NGINX CON TERMINACIÓN SSL PARA GESTOR FE
# =========================================================================

# 1. Frontend Angular (gestor-fe.assolidaria.org.co)
server {
    listen 80;
    server_name gestor-fe.assolidaria.org.co;
    # Redirección obligatoria a HTTPS en producción:
    # return 301 https://$host$request_uri;

    root /usr/share/nginx/html;
    index index.html index.htm;

    location / {
        try_files $uri $uri/ /index.html;  # Soporte History Mode SPA
    }

    client_max_body_size 100M;
}

# 2. Proxy Inverso para Keycloak (auth.assolidaria.org.co)
server {
    listen 80;
    server_name auth.assolidaria.org.co;

    location / {
        proxy_pass http://10.3.26.4:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Forwarded-Port $server_port;

        proxy_buffer_size 128k;
        proxy_buffers 4 256k;
        proxy_busy_buffers_size 256k;
        proxy_read_timeout 600s;
        proxy_connect_timeout 60s;
    }
}

# 3. Proxy Inverso para API Gateway (api-gestor.assolidaria.org.co -> VM2 10.3.26.3:8090)
server {
    listen 80;
    server_name api-gestor.assolidaria.org.co;

    location / {
        proxy_pass http://10.3.26.3:8090;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Forwarded-Port $server_port;

        client_max_body_size 100M;
        proxy_read_timeout 600s;
        proxy_connect_timeout 60s;
    }
}""")

    add_header_styled(doc, "7.4. Configuración Criptográfica en Keycloak (Realm gestor-fe)", level=2)
    add_body_paragraph(
        doc,
        "Keycloak 26 gestiona su propia infraestructura de llaves y certificados digitales para firmar "
        "criptográficamente los tokens JSON Web Tokens (JWT) que consumen Angular y el Gateway:"
    )
    add_bullet_point(
        doc,
        "En el archivo 'gestor-fe-realm.json', la directiva '\"sslRequired\": \"external\"' instruye a Keycloak "
        "a exigir obligatoriamente HTTPS para cualquier conexión que provenga de una IP pública o externa, "
        "mientras permite conexiones HTTP locales desde el proxy inverso en localhost / 10.3.26.4.",
        bold_prefix="Directiva sslRequired: "
    )
    add_bullet_point(
        doc,
        "Keycloak genera un par de llaves asimétricas RSA de 2048 bits (algoritmo RS256) con un certificado X.509 "
        "asociado al Realm. La llave privada se mantiene en la base de datos PostgreSQL de VM1 y nunca sale de Keycloak. "
        "Con ella, Keycloak firma cada access_token emitido tras un login exitoso.",
        bold_prefix="Par de Llaves del Realm (Realm Keys): "
    )
    add_bullet_point(
        doc,
        "Keycloak publica sus certificados y llaves públicas en formato JWK (JSON Web Key) en la URL pública: "
        "https://auth.assolidaria.org.co/realms/gestor-fe/protocol/openid-connect/certs. "
        "Tanto el Frontend como el API Gateway descargan periódicamente estas llaves públicas para verificar que "
        "la firma de los tokens sea auténtica y no haya sido alterada por terceros.",
        bold_prefix="Endpoint de Certificados JWKS: "
    )
    add_bullet_point(
        doc,
        "El cliente 'gestor-fe-frontend' está tipificado como 'Público' con estándar PKCE (Proof Key for Code Exchange). "
        "Tiene configurados como Web Origins permitidos (CORS) la URL: https://gestor-fe.assolidaria.org.co y como "
        "Redirect URIs válidas https://gestor-fe.assolidaria.org.co/*.",
        bold_prefix="Cliente OIDC gestor-fe-frontend: "
    )

    add_header_styled(doc, "7.5. Configuración en el API Gateway y Protección de Recursos", level=2)
    add_body_paragraph(
        doc,
        "El microservicio 'gestor-fe-gateway' actúa como Resource Server y router perimetral. "
        "En su archivo de configuración (application.yml y gestor-fe-gateway.yml en config-repo) se parametriza "
        "la directiva globalcors para permitir que el navegador web conectado al frontend HTTPS pueda "
        "enviar peticiones asíncronas con encabezados Authorization:"
    )
    add_code_block(doc, """spring:
  cloud:
    gateway:
      globalcors:
        add-to-simple-url-handler-mapping: true
        cors-configurations:
          '[/**]':
            allowedOrigins:
              - "https://gestor-fe.assolidaria.org.co"
              - "http://gestor-fe.assolidaria.org.co"
              - "http://localhost:4200"
              - "http://10.3.26.4:4200"
              - "http://10.3.26.4"
            allowedMethods:
              - GET
              - POST
              - PUT
              - DELETE
              - OPTIONS
              - PATCH
            allowedHeaders: "*"
            allowCredentials: true
            maxAge: 3600""")

    add_header_styled(doc, "7.6. Certificados Digitales DIAN para Firma Electrónica (XAdES-BES)", level=2)
    add_body_paragraph(
        doc,
        "Es fundamental no confundir el certificado SSL del navegador con el Certificado Digital de Firma Electrónica. "
        "Este último es un certificado de firma digital de Persona Jurídica regulado por la Ley 527 de 1999 de Colombia, "
        "emitido por entidades de certificación autorizadas por el ONAC (como Andes SCD u OlimpiaIT):"
    )
    add_bullet_point(
        doc,
        "Archivo en formato PKCS#12 (.p12 o .pfx) que contiene el certificado X.509 de la entidad emisora, la cadena de confianza "
        "(Subordinate CA y Root CA) y la clave privada de firma protegida por contraseña alfanumérica.",
        bold_prefix="Formato y Custodia: "
    )
    add_bullet_point(
        doc,
        "El microservicio 'gestor-fe-core-service' extrae el certificado y aplica el estándar de firma avanzada XAdES-BES "
        "(XML Advanced Electronic Signatures) en la sección <ext:UBLExtensions> de cada archivo XML UBL 2.1. "
        "Calcula el DigestValue (SHA-256), estampa la firma RSA (SignatureValue), el código CUFE/CUDE y el emisor (IssuerSerial).",
        bold_prefix="Mecanismo de Firma en core-service: "
    )

    # =========================================================================
    # CAPÍTULO 8: GUÍA PASO A PASO PARA EJECUTAR LOS DESPLIEGUES
    # =========================================================================
    add_header_styled(doc, "8. GUÍA PASO A PASO PARA EJECUTAR LOS DESPLIEGUES", level=1)
    
    add_body_paragraph(
        doc,
        "Para garantizar la consistencia transaccional y la correcta resolución de dependencias, los despliegues deben "
        "ejecutarse siguiendo un orden cronológico obligatorio. Intentar desplegar los microservicios de negocio sin tener "
        "activo el servidor Eureka o la base de datos resultará en fallos de inicio (CrashLoopBackOff)."
    )

    step_headers = ["Paso", "Acción Operativa", "Script / Comando", "Servidor Destino", "Criterio de Aceptación"]
    step_data = [
        ["1", "Conectar a la red corporativa", "OpenVPN Client (perfil ma2.user-1)", "Túnel VPN HostDime", "Ping exitoso a 10.3.26.2, 10.3.26.3 y 10.3.26.4"],
        ["2", "Inicializar / Migrar Base de Datos", "python deploy_back.py (Opción 4)", "VM1 (10.3.26.2)", "Esquemas 'admin', 'gestor' y 'logs' creados en PostgreSQL"],
        ["3", "Desplegar Infraestructura Cloud", "python deploy_infra.py --all", "VM2 (10.3.26.3)", "Eureka (8761), Config (8888), Gateway (8090) y Admin (9000) en estado Up"],
        ["4", "Desplegar Microservicios Backend", "python deploy_back.py --all", "VM2 (10.3.26.3)", "admin-service (9095) y core-service (9096) registrados en Eureka"],
        ["5", "Desplegar Frontend y Keycloak", "python deploy_front.py --all", "VM3 (10.3.26.4)", "Angular activo en puerto 80/4200 y Realm sincronizado en Keycloak"],
        ["6", "Validación Funcional End-to-End", "Navegar a https://gestor-fe.assolidaria.org.co", "Internet / DNS", "Login redirige a Keycloak, autentica y carga dashboard Angular"]
    ]
    build_table(doc, step_headers, step_data, col_widths=[0.6, 2.0, 1.8, 1.1, 1.0])

    # =========================================================================
    # CAPÍTULO 9: MATRIZ DE CREDENCIALES, PUERTOS Y VARIABLES
    # =========================================================================
    add_header_styled(doc, "9. MATRIZ DE CREDENCIALES, PUERTOS, VARIABLES Y RUTAS", level=1)
    
    add_body_paragraph(
        doc,
        "A continuación se consolida la información técnica crítica requerida para la administración y operación del ecosistema:"
    )

    cred_headers = ["Servicio / Entorno", "Host / IP", "Puerto", "Usuario / Realm", "Credencial / Token", "Notas de Conexión"]
    cred_data = [
        ["Panel Cloud HostDime", "cloudh.hostdime.com", "443 (HTTPS)", "ADMIN_ASSOLIDARIA_2", "H*B%j*S0OdKlq2", "Organización: MUTUAL_ASSOLIDARIA_2"],
        ["SSH Servidor VM1", "10.3.26.2", "22", "root", "MTL2026*-", "Servidor de BD (PostgreSQL y Mongo)"],
        ["SSH Servidor VM2", "10.3.26.3", "22", "root", "MTL2026*-", "Servidor de Microservicios Backend"],
        ["SSH Servidor VM3", "10.3.26.4", "22", "root", "MTL2026*-", "Servidor Frontend Web y Keycloak"],
        ["PostgreSQL 16", "10.3.26.2", "5432", "postgres", "MTL2026*-", "BD: gestor_fe_core_db y keycloak"],
        ["MongoDB 7.0", "10.3.26.2", "27017", "admin", "MTL2026*-", "authSource=admin"],
        ["Keycloak Admin", "auth.assolidaria.org.co", "8080 / 443", "admin", "MTL2026*-", "Consola: /admin/ (Master Realm)"],
        ["Keycloak Realm App", "auth.assolidaria.org.co", "8080 / 443", "Realm: gestor-fe", "Client: gestor-fe-frontend", "Autenticación para usuarios finales"],
        ["Eureka Discovery", "10.3.26.3", "8761", "N/A", "Público interno", "Dashboard de estado de instancias"],
        ["Spring Cloud Gateway", "10.3.26.3", "8090", "N/A", "Público interno / Proxy", "Rutas: /api/admin/**, /api/core/**"],
        ["Frontend Angular", "gestor-fe.assolidaria.org.co", "80 / 4200", "N/A", "Acceso Web HTTPS", "Proxy Nginx a 10.3.26.4"]
    ]
    build_table(doc, cred_headers, cred_data, col_widths=[1.2, 1.2, 0.7, 1.1, 1.1, 1.2])

    add_header_styled(doc, "9.1. Rutas Físicas de Almacenamiento en Servidores", level=2)
    add_bullet_point(doc, "Ubicación de archivos .tar transferidos y red Docker gestor-fe-network.", bold_prefix="/opt/gestor-fe-backend/ (VM2): ")
    add_bullet_point(doc, "Directorio raíz montado en admin-service y core-service donde se almacenan temporalmente los XMLs generados y en proceso de validación.", bold_prefix="/opt/storage/gestion-fe/ (VM2): ")
    add_bullet_point(doc, "Directorio permanente donde se archivan las facturas electrónicas con firma digital válida y respuesta 'Aceptada' de la DIAN.", bold_prefix="/opt/storage/gestion-fe-validos/ (VM2): ")
    add_bullet_point(doc, "Configuración activa de proxy reverso cargada en el contenedor Nginx.", bold_prefix="/opt/nginx/conf.d/default.conf (VM3): ")
    add_bullet_point(doc, "Temas visuales personalizados para los formularios de login, registro y recuperación de contraseña de Keycloak.", bold_prefix="/opt/keycloak/themes/ (VM3): ")

    # =========================================================================
    # CAPÍTULO 10: DIAGNÓSTICO, VERIFICACIÓN Y PLAN DE ROLLBACK
    # =========================================================================
    add_header_styled(doc, "10. DIAGNÓSTICO, VERIFICACIÓN Y PLAN DE ROLLBACK", level=1)
    
    add_body_paragraph(
        doc,
        "Una vez culminado el despliegue, el ingeniero responsable debe llevar a cabo una batería de validaciones "
        "para certificar la salud de la plataforma, así como conocer los procedimientos de contingencia en caso de contingencia."
    )

    add_header_styled(doc, "10.1. Verificación de Salud (Health Checks)", level=2)
    add_body_paragraph(
        doc,
        "Los scripts de despliegue incorporan funciones automáticas de comprobación de endpoints mediante peticiones HTTP. "
        "Las URLs clave que deben retornar código HTTP 200 OK son:"
    )
    add_bullet_point(doc, "http://10.3.26.3:8761 -> Debe renderizar la consola de Netflix Eureka con los servicios registrados.", bold_prefix="Eureka: ")
    add_bullet_point(doc, "http://10.3.26.3:8888/actuator/health -> Debe retornar JSON con {\"status\":\"UP\"}.", bold_prefix="Config Server: ")
    add_bullet_point(doc, "http://10.3.26.3:9000 -> Consola de Spring Boot Admin mostrando todos los microservicios en verde.", bold_prefix="Admin Server: ")
    add_bullet_point(doc, "https://auth.assolidaria.org.co/realms/gestor-fe/.well-known/openid-configuration -> Debe retornar la configuración OIDC de Keycloak.", bold_prefix="Keycloak OIDC: ")
    add_bullet_point(doc, "https://gestor-fe.assolidaria.org.co -> Debe responder con el index.html y assets de Angular.", bold_prefix="Frontend Web: ")

    add_header_styled(doc, "10.2. Diagnóstico de Incidentes Comunes", level=2)
    
    trouble_headers = ["Síntoma / Error", "Causa Raíz Probable", "Acción Correctiva Sugerida"]
    trouble_data = [
        ["Connection timed out al ejecutar script", "VPN desconectada o IP local no enrutada", "Verificar que el cliente OpenVPN esté conectado y responda ping a 10.3.26.3."],
        ["Error: Duplicate header Access-Control-Allow-Origin", "Anotación @CrossOrigin activa en controladores junto con Gateway", "Ejecutar deploy_back.py (Opción 9) para comentar @CrossOrigin en producción."],
        ["core-service falla al arrancar por DataSource", "PostgreSQL apagado o red gestor-fe-network aislada", "Conectar a VM1 y verificar docker ps en /opt/databases/. Comprobar credenciales JDBC."],
        ["Angular muestra pantalla en blanco al refrescar F5", "Nginx sin configuración de History Mode", "Comprobar que en default.conf esté la directiva try_files $uri $uri/ /index.html;."],
        ["Keycloak error: 'Invalid parameter: redirect_uri'", "URL pública no registrada en Web Origins o Redirect URIs", "Ejecutar deploy_front.py (Opción 2) para sincronizar gestor-fe-realm.json."]
    ]
    build_table(doc, trouble_headers, trouble_data, col_widths=[2.0, 2.2, 2.3])

    add_header_styled(doc, "10.3. Procedimiento de Rollback", level=2)
    add_body_paragraph(
        doc,
        "Si un despliegue presenta anomalías críticas no subsanables de inmediato, se debe proceder con la reversión "
        "a la versión estable previa:"
    )
    add_bullet_point(doc, "En VM2 o VM3, las imágenes Docker anteriores conservan su ID si no fueron purgadas. Se puede reiniciar el contenedor con la imagen previa ejecutando: docker run con el tag correspondiente.", bold_prefix="1. Reversión de Contenedores: ")
    add_bullet_point(doc, "En caso de falla en migraciones DDL, restaurar el volcado de respaldo de PostgreSQL ejecutando: docker exec -i postgres_db psql -U postgres -d gestor_fe_core_db < backup_previo.sql.", bold_prefix="2. Rollback de Base de Datos: ")
    add_bullet_point(doc, "Si el problema radica en variables externas, basta con revertir el commit en el repositorio Git 'config-repo' y reiniciar los servicios con: docker restart <servicio>.", bold_prefix="3. Reversión de Configuración: ")

    # Guardar documento
    output_filename = "Documento_Tecnico_Arquitectura_y_Despliegue_Gestor_FE.docx"
    output_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), output_filename)
    doc.save(output_path)
    print(f"\n[+] Documento técnico actualizado y guardado exitosamente en: {output_path}")

if __name__ == "__main__":
    main()
