# python deploy_back.py

import os
import sys
import time
import subprocess
import socket
import re

# -------------------------------------------------------------------------
# AUTO-INSTALACIÓN DE DEPENDENCIAS
# -------------------------------------------------------------------------
REQUIRED_LIBS = {
    "paramiko": "paramiko>=3.4.0",
    "rich": "rich>=13.7.0",
    "questionary": "questionary>=2.0.1"
}

missing = []
for mod, pkg in REQUIRED_LIBS.items():
    try:
        __import__(mod)
    except ImportError:
        missing.append(pkg)

if missing:
    print(f"[-] Instalando dependencias requeridas: {', '.join(missing)}...")
    try:
        subprocess.check_call([sys.executable, "-m", "pip", "install"] + missing)
        print("[+] Dependencias instaladas correctamente.\n")
    except Exception as e:
        print(f"[!] Error instalando dependencias: {e}")
        sys.exit(1)

import paramiko
from rich.console import Console
from rich.panel import Panel
from rich.table import Table
from rich.progress import Progress, SpinnerColumn, BarColumn, TextColumn, TimeRemainingColumn, TransferSpeedColumn
import questionary

console = Console()

# -------------------------------------------------------------------------
# CONFIGURACIÓN DE SERVIDORES Y SERVICIOS
# -------------------------------------------------------------------------
VM_BACKEND = {
    "host": "10.3.26.3",
    "port": 22,
    "user": "root",
    "passwords": ["MTL2026*-", "Temporal2026*"],
    "remote_dir": "/opt/gestor-fe-backend"
}

VM_DATABASE = {
    "host": "10.3.26.2",
    "port": 22,
    "user": "root",
    "passwords": ["MTL2026*-", "Temporal2026*"],
    "db_container": "postgres_db",
    "db_name": "gestor_fe_core_db",
    "db_user": "postgres"
}

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
if os.path.isdir(os.path.join(BASE_DIR, "gestor-fe-backend")):
    BACKEND_ROOT = os.path.join(BASE_DIR, "gestor-fe-backend")
    SQL_SCRIPT_PATH = os.path.join(BASE_DIR, "gestor_fe_core_db.sql")
else:
    BACKEND_ROOT = os.path.join(BASE_DIR, "gestor-fe", "gestor-fe-backend")
    SQL_SCRIPT_PATH = os.path.join(BASE_DIR, "gestor-fe", "gestor_fe_core_db.sql")

SERVICES = {
    "admin": {
        "title": "Gestor FE Admin Service",
        "folder": os.path.join(BACKEND_ROOT, "gestor-fe-admin-service"),
        "image_name": "gestor-fe-admin-service",
        "container_name": "gestor-fe-admin-service",
        "tar_name": "gestor-fe-admin-service.tar",
        "port": 9095,
        "run_cmd": (
            "mkdir -p /opt/storage/gestion-fe /opt/storage/gestion-fe-validos && "
            "docker run -d --name gestor-fe-admin-service "
            "--network gestor-fe-network --network-alias admin-service "
            "-p 9095:9095 "
            "-v /opt/storage/gestion-fe:/opt/storage/gestion-fe "
            "-v /opt/storage/gestion-fe-validos:/opt/storage/gestion-fe-validos "
            "-e TZ=America/Bogota "
            "-e RUTA_STORAGE=/opt/storage/gestion-fe/ "
            "-e RUTA_STORAGE_VALIDOS=/opt/storage/gestion-fe-validos/ "
            "--restart always gestor-fe-admin-service:latest"
        )
    },
    "core": {
        "title": "Gestor FE Core Service",
        "folder": os.path.join(BACKEND_ROOT, "gestor-fe-core-service"),
        "image_name": "gestor-fe-core-service",
        "container_name": "gestor-fe-core-service",
        "tar_name": "gestor-fe-core-service.tar",
        "port": 9096,
        "run_cmd": (
            "mkdir -p /opt/storage/gestion-fe /opt/storage/gestion-fe-validos && "
            "docker run -d --name gestor-fe-core-service "
            "--network gestor-fe-network --network-alias core-service "
            "-p 9096:9096 "
            "-v /opt/storage/gestion-fe:/opt/storage/gestion-fe "
            "-v /opt/storage/gestion-fe-validos:/opt/storage/gestion-fe-validos "
            "-e TZ=America/Bogota "
            "-e RUTA_STORAGE=/opt/storage/gestion-fe/ "
            "-e RUTA_STORAGE_VALIDOS=/opt/storage/gestion-fe-validos/ "
            "--restart always gestor-fe-core-service:latest"
        )
    }
}

# -------------------------------------------------------------------------
# CONEXIÓN SSH Y COMANDOS REMOTOS
# -------------------------------------------------------------------------
def get_ssh_client(vm_config):
    last_err = None
    for pwd in vm_config["passwords"]:
        try:
            client = paramiko.SSHClient()
            client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
            client.connect(
                vm_config["host"],
                port=vm_config["port"],
                username=vm_config["user"],
                password=pwd,
                timeout=8,
                banner_timeout=10
            )
            return client, pwd
        except Exception as e:
            last_err = e
    console.print(f"[bold red]❌ No se pudo conectar a {vm_config['host']} vía SSH:[/bold red] {last_err}")
    return None, None

def run_remote_command(client, cmd):
    stdin, stdout, stderr = client.exec_command(cmd)
    out = stdout.read().decode("utf-8", errors="ignore")
    err = stderr.read().decode("utf-8", errors="ignore")
    return out, err

# -------------------------------------------------------------------------
# TRANSFERENCIA SFTP CON BARRA DE PROGRESO
# -------------------------------------------------------------------------
def transfer_file_sftp(client, local_file, remote_file):
    sftp = client.open_sftp()
    remote_dir = os.path.dirname(remote_file).replace("\\", "/")
    
    # Crear directorio remoto si no existe
    try:
        sftp.stat(remote_dir)
    except IOError:
        run_remote_command(client, f"mkdir -p {remote_dir}")

    file_size = os.path.getsize(local_file)
    with Progress(
        SpinnerColumn(),
        TextColumn("[progress.description]{task.description}"),
        BarColumn(),
        TransferSpeedColumn(),
        TimeRemainingColumn(),
        console=console
    ) as progress:
        task = progress.add_task(f"[cyan]Transfiriendo {os.path.basename(local_file)}...", total=file_size)

        def callback(transferred, total):
            progress.update(task, completed=transferred)

        sftp.put(local_file, remote_file, callback=callback)

    sftp.close()

# -------------------------------------------------------------------------
# GESTIÓN AUTOMÁTICA DE CORS EN CONTROLADORES (@CrossOrigin)
# -------------------------------------------------------------------------
def toggle_cors_in_file(file_path, comment=True):
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
    except Exception as e:
        console.print(f"[bold red]❌ Error leyendo {file_path}: {e}[/bold red]")
        return False

    newline = '\r\n' if '\r\n' in content else '\n'
    lines = content.splitlines()
    new_lines = []
    modified = False

    comment_pat = re.compile(r'^([ \t]*)@(CrossOrigin\b.*)$')
    uncomment_pat = re.compile(r'^([ \t]*)//[ \t]*@(CrossOrigin\b.*)$')

    for line in lines:
        if comment:
            if comment_pat.match(line):
                new_line = comment_pat.sub(r'\1// @\2', line)
                modified = True
            else:
                new_line = line
        else:
            if uncomment_pat.match(line):
                new_line = uncomment_pat.sub(r'\1@\2', line)
                modified = True
            else:
                new_line = line
        new_lines.append(new_line)

    if modified:
        with open(file_path, 'w', encoding='utf-8', newline='') as f:
            f.write(newline.join(new_lines) + newline)
    return modified

def set_service_cors(dir_path, comment=True):
    """
    Recorre dir_path buscando archivos .java (excluyendo target y .git)
    y comenta o descomenta las anotaciones @CrossOrigin.
    """
    count = 0
    modified_files = []
    for root, dirs, files in os.walk(dir_path):
        if "target" in dirs:
            dirs.remove("target")
        if ".git" in dirs:
            dirs.remove(".git")
        for file in files:
            if file.endswith(".java"):
                file_path = os.path.join(root, file)
                if toggle_cors_in_file(file_path, comment=comment):
                    count += 1
                    modified_files.append(os.path.basename(file_path))
    return count, modified_files

# -------------------------------------------------------------------------
# COMPILACIÓN LOCAL CON MAVEN
# -------------------------------------------------------------------------
def build_maven_module(module_path, is_install=False):
    mvn_cmd = "mvn"
    mvnw_cmd = os.path.join(module_path, "mvnw.cmd")
    if os.path.exists(mvnw_cmd):
        mvn_cmd = mvnw_cmd

    action = "clean install" if is_install else "clean package"
    cmd = [mvn_cmd] + action.split() + ["-DskipTests=true"]

    console.print(f"[bold yellow]⚙️ Ejecutando Maven ({' '.join(cmd)}) en: {os.path.basename(module_path)}...[/bold yellow]")
    try:
        process = subprocess.Popen(
            cmd,
            cwd=module_path,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            shell=True,
            text=True,
            encoding="utf-8",
            errors="ignore"
        )
        for line in process.stdout:
            if any(k in line for k in ["BUILD SUCCESS", "BUILD FAILURE", "Compiling", "Building war", "Installing"]):
                console.print(f"  [dim]{line.strip()}[/dim]")
        process.wait()
        if process.returncode != 0:
            console.print(f"[bold red]❌ Falló la compilación de {os.path.basename(module_path)}.[/bold red]")
            return False
        console.print(f"[bold green]✔ Compilación exitosa para {os.path.basename(module_path)}.[/bold green]")
        return True
    except Exception as e:
        console.print(f"[bold red]❌ Error ejecutando Maven: {e}[/bold red]")
        return False

# -------------------------------------------------------------------------
# CONSTRUCCIÓN DE IMAGEN DOCKER Y ARCHIVO TAR
# -------------------------------------------------------------------------
def build_and_save_docker_image(service_key):
    svc = SERVICES[service_key]
    folder = svc["folder"]
    image_name = svc["image_name"]
    tar_path = os.path.join(folder, svc["tar_name"])

    console.print(f"\n[bold blue]🐳 Construyendo imagen Docker: {image_name}:latest...[/bold blue]")
    build_cmd = ["docker", "build", "-t", f"{image_name}:latest", "."]
    ret = subprocess.call(build_cmd, cwd=folder, shell=True)
    if ret != 0:
        console.print(f"[bold red]❌ Error al construir imagen Docker {image_name}[/bold red]")
        return None

    console.print(f"[bold blue]📦 Exportando imagen a archivo TAR: {svc['tar_name']}...[/bold blue]")
    save_cmd = ["docker", "save", "-o", tar_path, f"{image_name}:latest"]
    ret = subprocess.call(save_cmd, cwd=folder, shell=True)
    if ret != 0 or not os.path.exists(tar_path):
        console.print(f"[bold red]❌ Error al exportar archivo .tar para {image_name}[/bold red]")
        return None

    size_mb = os.path.getsize(tar_path) / (1024 * 1024)
    console.print(f"[bold green]✔ Archivo TAR generado ({size_mb:.2f} MB): {tar_path}[/bold green]")
    return tar_path

# -------------------------------------------------------------------------
# DESPLIEGUE REMOTO EN VM2
# -------------------------------------------------------------------------
def deploy_service_remote(service_key, tar_path):
    svc = SERVICES[service_key]
    client, pwd = get_ssh_client(VM_BACKEND)
    if not client:
        return False

    remote_file = f"{VM_BACKEND['remote_dir']}/{svc['tar_name']}"

    console.print(f"\n[bold magenta]🚀 Enviando imagen {svc['tar_name']} a VM2 ({VM_BACKEND['host']})...[/bold magenta]")
    try:
        transfer_file_sftp(client, tar_path, remote_file)
        console.print(f"[bold green]✔ Transferencia completada.[/bold green]")

        console.print(f"[bold yellow]🔄 Deteniendo y eliminando contenedor previo si existe...[/bold yellow]")
        run_remote_command(client, f"docker stop {svc['container_name']} 2>/dev/null || true")
        run_remote_command(client, f"docker rm {svc['container_name']} 2>/dev/null || true")

        console.print(f"[bold yellow]📥 Cargando imagen en Docker remoto...[/bold yellow]")
        out, err = run_remote_command(client, f"docker load -i {remote_file}")
        console.print(f"  [dim]{out.strip()}[/dim]")

        console.print(f"[bold yellow]▶️ Iniciando contenedor ({svc['container_name']})...[/bold yellow]")
        out, err = run_remote_command(client, svc["run_cmd"])
        if err and "error" in err.lower():
            console.print(f"[bold red]❌ Error al iniciar contenedor: {err}[/bold red]")
            client.close()
            return False

        console.print(f"[bold green]✔ Contenedor {svc['container_name']} iniciado exitosamente (Puerto {svc['port']}).[/bold green]")

        # Esperar 3 segundos y verificar estado
        time.sleep(3)
        check_out, _ = run_remote_command(client, f"docker ps --filter name={svc['container_name']} --format '{{{{.Names}}}} | {{{{.Status}}}} | {{{{.Ports}}}} | Volúmenes: {{{{.Mounts}}}}'")
        console.print(f"[bold cyan]📊 Estado del contenedor:[/bold cyan] {check_out.strip()}")

        client.close()
        return True
    except Exception as e:
        console.print(f"[bold red]❌ Error durante el despliegue remoto: {e}[/bold red]")
        client.close()
        return False

# -------------------------------------------------------------------------
# INICIALIZACIÓN / MIGRACIÓN DE BASE DE DATOS EN VM1
# -------------------------------------------------------------------------
def init_database_vm1():
    client, pwd = get_ssh_client(VM_DATABASE)
    if not client:
        return False

    console.print(Panel.fit(
        f"[bold cyan]Servidor VM1 ({VM_DATABASE['host']}): Inicialización de Base de Datos[/bold cyan]\n"
        f"Base de datos: [green]{VM_DATABASE['db_name']}[/green] | Contenedor: [green]{VM_DATABASE['db_container']}[/green]",
        border_style="cyan"
    ))

    # 1. Crear esquemas admin, gestor, logs si no existen
    console.print("[bold yellow]1. Verificando/Creando esquemas en PostgreSQL ('admin', 'gestor', 'logs')...[/bold yellow]")
    create_schemas_sql = "CREATE SCHEMA IF NOT EXISTS admin; CREATE SCHEMA IF NOT EXISTS gestor; CREATE SCHEMA IF NOT EXISTS logs;"
    cmd_schemas = f'docker exec {VM_DATABASE["db_container"]} psql -U {VM_DATABASE["db_user"]} -d {VM_DATABASE["db_name"]} -c "{create_schemas_sql}"'
    out, err = run_remote_command(client, cmd_schemas)
    console.print(f"  [dim]{out.strip()}[/dim]")

    # 2. Subir y ejecutar gestor_fe_core_db.sql
    if os.path.exists(SQL_SCRIPT_PATH):
        console.print(f"[bold yellow]2. Transfiriendo script SQL inicial ({os.path.basename(SQL_SCRIPT_PATH)})...[/bold yellow]")
        remote_sql = "/tmp/gestor_fe_core_db.sql"
        try:
            transfer_file_sftp(client, SQL_SCRIPT_PATH, remote_sql)
            console.print("[bold yellow]3. Ejecutando script SQL en la base de datos...[/bold yellow]")
            cmd_run_sql = f'docker exec -i {VM_DATABASE["db_container"]} psql -U {VM_DATABASE["db_user"]} -d {VM_DATABASE["db_name"]} < {remote_sql}'
            out, err = run_remote_command(client, cmd_run_sql)
            run_remote_command(client, f"rm -f {remote_sql}")
            console.print("[bold green]✔ Script SQL ejecutado correctamente.[/bold green]")
        except Exception as e:
            console.print(f"[bold red]❌ Error al transferir/ejecutar SQL: {e}[/bold red]")
    else:
        console.print(f"[yellow]⚠️ No se encontró {SQL_SCRIPT_PATH}. Solo se crearon los esquemas.[/yellow]")

    client.close()
    return True

# -------------------------------------------------------------------------
# ESTADO Y LOGS EN VM2
# -------------------------------------------------------------------------
def show_vm2_status():
    client, pwd = get_ssh_client(VM_BACKEND)
    if not client:
        return

    out, _ = run_remote_command(client, "docker ps -a --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}\t{{.Mounts}}'")
    table = Table(title="Contenedores en VM2 Backend (10.3.26.3)", show_header=True, header_style="bold magenta")
    for idx, line in enumerate(out.strip().split("\n")):
        if idx == 0:
            cols = line.split("\t")
            for c in cols:
                table.add_column(c.strip())
        elif line.strip():
            cols = line.split("\t")
            table.add_row(*[c.strip() for c in cols])
    console.print(table)
    client.close()

def view_logs(service_name):
    client, pwd = get_ssh_client(VM_BACKEND)
    if not client:
        return

    console.print(f"\n[bold yellow]📜 Últimas 100 líneas de log para {service_name}:[/bold yellow]\n")
    out, _ = run_remote_command(client, f"docker logs --tail 100 {service_name}")
    console.print(out)
    client.close()

# -------------------------------------------------------------------------
# FLUJO PRINCIPAL DE DESPLIEGUE DE BACKEND
# -------------------------------------------------------------------------
def deploy_backend_flow(target="all"):
    console.print(Panel.fit(
        f"[bold green]INICIANDO DESPLIEGUE DE BACKEND EN VM2 ({VM_BACKEND['host']})[/bold green]\n"
        f"Objetivo: [bold cyan]{target.upper()}[/bold cyan]",
        border_style="green"
    ))

    # 1. Compilar service-common (librería compartida)
    console.print("\n[bold cyan]PASO 1: Compilación de service-common (librería requerida)[/bold cyan]")
    common_path = os.path.join(BACKEND_ROOT, "service-common")
    if not build_maven_module(common_path, is_install=True):
        console.print("[bold red]❌ Abortando: Falló la compilación de service-common.[/bold red]")
        return False

    targets = ["admin", "core"] if target == "all" else [target]

    for key in targets:
        svc = SERVICES[key]
        console.print(f"\n[bold cyan]------------------------------------------------------------[/bold cyan]")
        console.print(f"[bold cyan]PROCESANDO: {svc['title']}[/bold cyan]")
        console.print(f"[bold cyan]------------------------------------------------------------[/bold cyan]")

        tar_path = None
        try:
            # 2. Comentar CORS en controladores antes de compilar
            c_count, _ = set_service_cors(svc["folder"], comment=True)
            if c_count > 0:
                console.print(f"[bold yellow]🔒 Se comentaron anotaciones @CrossOrigin en {c_count} archivo(s) para despliegue.[/bold yellow]")
            else:
                console.print(f"[dim]ℹ️ Anotaciones @CrossOrigin ya comentadas o no presentes.[/dim]")

            # 3. Compilar módulo Spring Boot
            if not build_maven_module(svc["folder"], is_install=False):
                console.print(f"[bold red]❌ Abortando despliegue de {svc['title']}[/bold red]")
                continue

            # 4. Construir imagen Docker y exportar .tar
            tar_path = build_and_save_docker_image(key)
            if not tar_path:
                console.print(f"[bold red]❌ Abortando despliegue de {svc['title']}[/bold red]")
                continue

        finally:
            # 5. SIEMPRE restaurar anotaciones CORS (descomentadas para desarrollo local)
            r_count, _ = set_service_cors(svc["folder"], comment=False)
            if r_count > 0:
                console.print(f"[bold green]🔓 Se restauraron anotaciones @CrossOrigin en {r_count} archivo(s) (descomentadas para desarrollo local).[/bold green]")

        # 6. Desplegar en VM2 si la imagen se generó con éxito
        if tar_path:
            deploy_service_remote(key, tar_path)

    console.print(Panel.fit("[bold green]✔ PROCESO DE DESPLIEGUE FINALIZADO[/bold green]", border_style="green"))
    show_vm2_status()
    return True

# -------------------------------------------------------------------------
# MENÚ INTERACTIVO
# -------------------------------------------------------------------------
def main():
    while True:
        console.print("\n")
        console.print(Panel.fit(
            "[bold white]GESTOR FACTURACIÓN ELECTRÓNICA - GESTOR FE[/bold white]\n"
            f"[bold cyan]Módulo de Despliegue Backend -> VM2 ({VM_BACKEND['host']}) & BD VM1 ({VM_DATABASE['host']})[/bold cyan]",
            border_style="blue"
        ))

        try:
            choice = questionary.select(
                "Seleccione una acción:",
                choices=[
                    "1. 🚀 Desplegar TODO el Backend (service-common + admin + core)",
                    "2. 📦 Desplegar solo gestor-fe-admin-service (Puerto 9095)",
                    "3. ⚙️ Desplegar solo gestor-fe-core-service (Puerto 9096)",
                    "4. 🗄️ Inicializar / Migrar Base de Datos en VM1 (10.3.26.2)",
                    "5. 📊 Ver estado de contenedores en VM2 Backend",
                    "6. 📜 Ver logs de gestor-fe-admin-service",
                    "7. 📜 Ver logs de gestor-fe-core-service",
                    "8. 🔄 Reiniciar contenedores en VM2",
                    "9. 🔒 Comentar @CrossOrigin en controladores manualmente",
                    "10. 🔓 Descomentar @CrossOrigin en controladores manualmente",
                    "11. ❌ Salir"
                ]
            ).ask()
        except KeyboardInterrupt:
            break

        if not choice or "11. ❌ Salir" in choice:
            console.print("[dim]Operación cancelada. ¡Hasta luego![/dim]")
            break
        elif "1. 🚀 Desplegar TODO" in choice:
            deploy_backend_flow("all")
        elif "2. 📦 Desplegar solo gestor-fe-admin-service" in choice:
            deploy_backend_flow("admin")
        elif "3. ⚙️ Desplegar solo gestor-fe-core-service" in choice:
            deploy_backend_flow("core")
        elif "4. 🗄️ Inicializar / Migrar Base de Datos" in choice:
            init_database_vm1()
        elif "5. 📊 Ver estado de contenedores" in choice:
            show_vm2_status()
        elif "6. 📜 Ver logs de gestor-fe-admin-service" in choice:
            view_logs("gestor-fe-admin-service")
        elif "7. 📜 Ver logs de gestor-fe-core-service" in choice:
            view_logs("gestor-fe-core-service")
        elif "8. 🔄 Reiniciar contenedores" in choice:
            client, _ = get_ssh_client(VM_BACKEND)
            if client:
                run_remote_command(client, "docker restart gestor-fe-admin-service gestor-fe-core-service")
                console.print("[bold green]✔ Contenedores reiniciados.[/bold green]")
                client.close()
                show_vm2_status()
        elif "9. 🔒 Comentar @CrossOrigin" in choice:
            count, _ = set_service_cors(BACKEND_ROOT, comment=True)
            console.print(f"[bold yellow]✔ Se comentaron anotaciones @CrossOrigin en {count} archivo(s).[/bold yellow]")
        elif "10. 🔓 Descomentar @CrossOrigin" in choice:
            count, _ = set_service_cors(BACKEND_ROOT, comment=False)
            console.print(f"[bold green]✔ Se descomentaron anotaciones @CrossOrigin en {count} archivo(s).[/bold green]")

if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] == "--all":
        deploy_backend_flow("all")
    else:
        main()
