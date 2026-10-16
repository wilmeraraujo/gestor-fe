# python deploy_infra.py

import os
import sys
import time
import subprocess
import socket

# -------------------------------------------------------------------------
# AUTO-INSTALACIÓN DE DEPENDENCIAS
# -------------------------------------------------------------------------
REQUIRED_LIBS = {
    "paramiko": "paramiko>=3.4.0",
    "rich": "rich>=13.7.0",
    "questionary": "questionary>=2.0.1",
    "requests": "requests>=2.31.0"
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
import requests
from rich.console import Console
from rich.panel import Panel
from rich.table import Table
from rich.progress import Progress, SpinnerColumn, BarColumn, TextColumn, TimeRemainingColumn, TransferSpeedColumn
import questionary

console = Console()

# -------------------------------------------------------------------------
# CONFIGURACIÓN DEL SERVIDOR REMOTO (VM2 BACKEND)
# -------------------------------------------------------------------------
VM_BACKEND = {
    "host": "10.3.26.3",
    "port": 22,
    "user": "root",
    "passwords": ["MTL2026*-", "Temporal2026*"],
    "remote_dir": "/opt/gestor-fe-backend",
    "docker_network": "gestor-fe-network"
}

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
if os.path.isdir(os.path.join(BASE_DIR, "gestor-fe-backend")):
    BACKEND_ROOT = os.path.join(BASE_DIR, "gestor-fe-backend")
else:
    BACKEND_ROOT = os.path.join(BASE_DIR, "gestor-fe", "gestor-fe-backend")

# Servicios de configuración e infraestructura Spring Cloud
SERVICES = {
    "eureka": {
        "title": "Netflix Eureka Discovery Server",
        "folder": os.path.join(BACKEND_ROOT, "gestor-fe-eureka-server"),
        "image_name": "gestor-fe-eureka-server",
        "container_name": "gestor-fe-eureka-server",
        "tar_name": "gestor-fe-eureka-server.tar",
        "port": 8761,
        "run_cmd": (
            "docker run -d --name gestor-fe-eureka-server "
            "--network gestor-fe-network --network-alias eureka-server "
            "-p 8761:8761 --restart always "
            "-e TZ=America/Bogota "
            "-e SPRING_APPLICATION_NAME=gestor-fe-eureka-server "
            "-e EUREKA_HOSTNAME=eureka-server "
            "gestor-fe-eureka-server:latest"
        )
    },
    "config": {
        "title": "Spring Cloud Config Server",
        "folder": os.path.join(BACKEND_ROOT, "gestor-fe-config-server"),
        "image_name": "gestor-fe-config-server",
        "container_name": "gestor-fe-config-server",
        "tar_name": "gestor-fe-config-server.tar",
        "port": 8888,
        "run_cmd": (
            "docker run -d --name gestor-fe-config-server "
            "--network gestor-fe-network --network-alias config-server "
            "-p 8888:8888 --restart always "
            "-e TZ=America/Bogota "
            "-e SPRING_APPLICATION_NAME=gestor-fe-config-server "
            "-e EUREKA_SERVER_URL=http://eureka-server:8761/eureka/ "
            "gestor-fe-config-server:latest"
        )
    },
    "gateway": {
        "title": "Spring Cloud Gateway",
        "folder": os.path.join(BACKEND_ROOT, "gestor-fe-gateway"),
        "image_name": "gestor-fe-gateway",
        "container_name": "gestor-fe-gateway",
        "tar_name": "gestor-fe-gateway.tar",
        "port": 8090,
        "run_cmd": (
            "docker run -d --name gestor-fe-gateway "
            "--network gestor-fe-network --network-alias gateway "
            "-p 8090:8090 --restart always "
            "-e TZ=America/Bogota "
            "-e PORT=8090 "
            "-e EUREKA_SERVER_URL=http://eureka-server:8761/eureka/ "
            "gestor-fe-gateway:latest"
        )
    },
    "admin": {
        "title": "Spring Boot Admin Server (Dashboard)",
        "folder": os.path.join(BACKEND_ROOT, "gestor-fe-admin-server"),
        "image_name": "gestor-fe-admin-server",
        "container_name": "gestor-fe-admin-server",
        "tar_name": "gestor-fe-admin-server.tar",
        "port": 9000,
        "run_cmd": (
            "docker run -d --name gestor-fe-admin-server "
            "--network gestor-fe-network --network-alias admin-server "
            "-p 9000:9000 --restart always "
            "-e TZ=America/Bogota "
            "-e PORT=9000 "
            "-e EUREKA_SERVER_URL=http://eureka-server:8761/eureka/ "
            "gestor-fe-admin-server:latest"
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
# COMPILACIÓN LOCAL CON MAVEN
# -------------------------------------------------------------------------
def build_maven_module(module_path):
    mvn_cmd = "mvn"
    mvnw_cmd = os.path.join(module_path, "mvnw.cmd")
    if os.path.exists(mvnw_cmd):
        mvn_cmd = mvnw_cmd

    cmd = [mvn_cmd, "clean", "package", "-DskipTests=true"]

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
            if any(k in line for k in ["BUILD SUCCESS", "BUILD FAILURE", "Compiling", "Building jar", "Replacing main artifact"]):
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

    # Asegurar red Docker en VM2
    run_remote_command(client, f"docker network create {VM_BACKEND['docker_network']} 2>/dev/null || true")

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

        time.sleep(3)
        check_out, _ = run_remote_command(client, f"docker ps --filter name={svc['container_name']} --format '{{{{.Names}}}} | {{{{.Status}}}} | {{{{.Ports}}}}'")
        console.print(f"[bold cyan]📊 Estado del contenedor:[/bold cyan] {check_out.strip()}")

        client.close()
        return True
    except Exception as e:
        console.print(f"[bold red]❌ Error durante el despliegue remoto: {e}[/bold red]")
        client.close()
        return False

# -------------------------------------------------------------------------
# ESTADO, LOGS Y HEALTH CHECK EN VM2
# -------------------------------------------------------------------------
def show_infra_status():
    client, pwd = get_ssh_client(VM_BACKEND)
    if not client:
        return

    out, _ = run_remote_command(client, "docker ps -a --format 'table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}'")
    table = Table(title="Contenedores de Infraestructura en VM2 (10.3.26.3)", show_header=True, header_style="bold magenta")
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

def view_logs(service_key):
    svc = SERVICES[service_key]
    client, pwd = get_ssh_client(VM_BACKEND)
    if not client:
        return

    console.print(f"\n[bold yellow]📜 Últimas 100 líneas de log para {svc['container_name']}:[/bold yellow]\n")
    out, _ = run_remote_command(client, f"docker logs --tail 100 {svc['container_name']}")
    console.print(out)
    client.close()

def test_endpoints():
    console.print("\n[bold yellow]🔍 Probando respuesta HTTP de los servicios de infraestructura en VM2...[/bold yellow]")
    urls = [
        ("Eureka Server", f"http://{VM_BACKEND['host']}:8761"),
        ("Config Server (Health)", f"http://{VM_BACKEND['host']}:8888/actuator/health"),
        ("Spring Cloud Gateway", f"http://{VM_BACKEND['host']}:8090"),
        ("Admin Server Dashboard", f"http://{VM_BACKEND['host']}:9000")
    ]
    for name, url in urls:
        try:
            r = requests.get(url, timeout=4)
            console.print(f"  ✔ [bold green]{name}[/bold green] ({url}) -> HTTP {r.status_code}")
        except Exception as e:
            console.print(f"  ❌ [bold red]{name}[/bold red] ({url}) -> Error: {e}")

# -------------------------------------------------------------------------
# FLUJO COMPLETO DE DESPLIEGUE EN ORDEN SECUENCIAL
# -------------------------------------------------------------------------
def deploy_infra_flow(target="all"):
    console.print(Panel.fit(
        f"[bold green]INICIANDO DESPLIEGUE DE SERVICIOS DE INFRAESTRUCTURA Y CONFIGURACIÓN[/bold green]\n"
        f"Servidor: [bold cyan]VM2 ({VM_BACKEND['host']})[/bold cyan] | Objetivo: [bold yellow]{target.upper()}[/bold yellow]",
        border_style="green"
    ))

    # Orden obligatorio: eureka -> config -> gateway -> admin
    order = ["eureka", "config", "gateway", "admin"] if target == "all" else [target]

    for key in order:
        svc = SERVICES[key]
        console.print(f"\n[bold cyan]============================================================[/bold cyan]")
        console.print(f"[bold cyan]DESPLEGANDO: {svc['title']} (Puerto {svc['port']})[/bold cyan]")
        console.print(f"[bold cyan]============================================================[/bold cyan]")

        # 1. Maven build
        if not build_maven_module(svc["folder"]):
            console.print(f"[bold red]❌ Abortando despliegue de {svc['title']}[/bold red]")
            continue

        # 2. Docker build & save
        tar_path = build_and_save_docker_image(key)
        if not tar_path:
            console.print(f"[bold red]❌ Abortando despliegue de {svc['title']}[/bold red]")
            continue

        # 3. Deploy en VM2
        deploy_service_remote(key, tar_path)

        # Si estamos desplegando Eureka, esperar 10 segundos para que inicie antes de los que dependen de él
        if key == "eureka" and target == "all":
            console.print("[dim]⏳ Esperando 10 segundos para inicialización de Eureka Server...[/dim]")
            time.sleep(10)

    console.print(Panel.fit("[bold green]✔ DESPLIEGUE DE INFRAESTRUCTURA FINALIZADO[/bold green]", border_style="green"))
    show_infra_status()
    test_endpoints()
    return True

# -------------------------------------------------------------------------
# MENÚ INTERACTIVO
# -------------------------------------------------------------------------
def main():
    while True:
        console.print("\n")
        console.print(Panel.fit(
            "[bold white]GESTOR FACTURACIÓN ELECTRÓNICA - GESTOR FE[/bold white]\n"
            f"[bold cyan]Módulo de Despliegue de Infraestructura Cloud -> VM2 ({VM_BACKEND['host']})[/bold cyan]\n"
            "[dim]Eureka (8761) | Config Server (8888) | Gateway (8090) | Admin Server (9000)[/dim]",
            border_style="blue"
        ))

        try:
            choice = questionary.select(
                "Seleccione una acción:",
                choices=[
                    "1. 🚀 Desplegar TODOS (Eureka -> Config -> Gateway -> Admin)",
                    "2. 📡 Desplegar solo Eureka Server (Puerto 8761)",
                    "3. ⚙️ Desplegar solo Config Server (Puerto 8888)",
                    "4. 🌐 Desplegar solo Spring Cloud Gateway (Puerto 8090)",
                    "5. 🖥️ Desplegar solo Spring Boot Admin Server (Puerto 9000)",
                    "6. 📊 Ver estado de contenedores en VM2",
                    "7. 📜 Ver logs de Eureka Server",
                    "8. 📜 Ver logs de Config Server",
                    "9. 📜 Ver logs de Gateway",
                    "10. 📜 Ver logs de Admin Server",
                    "11. 🔍 Probar conectividad HTTP (Health Check)",
                    "12. 🔄 Reiniciar servicios de infraestructura",
                    "13. ❌ Salir"
                ]
            ).ask()
        except KeyboardInterrupt:
            break

        if not choice or "13. ❌ Salir" in choice:
            console.print("[dim]Operación cancelada. ¡Hasta luego![/dim]")
            break
        elif "1. 🚀 Desplegar TODOS" in choice:
            deploy_infra_flow("all")
        elif "2. 📡 Desplegar solo Eureka" in choice:
            deploy_infra_flow("eureka")
        elif "3. ⚙️ Desplegar solo Config" in choice:
            deploy_infra_flow("config")
        elif "4. 🌐 Desplegar solo Spring Cloud Gateway" in choice:
            deploy_infra_flow("gateway")
        elif "5. 🖥️ Desplegar solo Spring Boot Admin" in choice:
            deploy_infra_flow("admin")
        elif "6. 📊 Ver estado de contenedores" in choice:
            show_infra_status()
        elif "7. 📜 Ver logs de Eureka" in choice:
            view_logs("eureka")
        elif "8. 📜 Ver logs de Config" in choice:
            view_logs("config")
        elif "9. 📜 Ver logs de Gateway" in choice:
            view_logs("gateway")
        elif "10. 📜 Ver logs de Admin Server" in choice:
            view_logs("admin")
        elif "11. 🔍 Probar conectividad HTTP" in choice:
            test_endpoints()
        elif "12. 🔄 Reiniciar servicios" in choice:
            client, _ = get_ssh_client(VM_BACKEND)
            if client:
                run_remote_command(
                    client,
                    "docker restart gestor-fe-eureka-server gestor-fe-config-server gestor-fe-gateway gestor-fe-admin-server"
                )
                console.print("[bold green]✔ Contenedores de infraestructura reiniciados.[/bold green]")
                client.close()
                show_infra_status()

if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] == "--all":
        deploy_infra_flow("all")
    else:
        main()
