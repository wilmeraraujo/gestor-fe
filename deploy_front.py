# python deploy_front.py


import os
import sys
import time
import json
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
# CONFIGURACIÓN DE SERVIDOR Y SERVICIOS
# -------------------------------------------------------------------------
VM_FRONTEND = {
    "host": "10.3.26.4",
    "port": 22,
    "user": "root",
    "passwords": ["MTL2026*-", "Temporal2026*"],
    "remote_dir": "/opt/gestor-fe-frontend"
}

KEYCLOAK_CONFIG = {
    "url": "http://10.3.26.4:8080",
    "admin_user": "admin",
    "admin_pass": "MTL2026*-",
    "target_realm": "gestor-fe"
}

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
if os.path.isdir(os.path.join(BASE_DIR, "gestor-fe-frontend")):
    FRONTEND_ROOT = os.path.join(BASE_DIR, "gestor-fe-frontend")
else:
    FRONTEND_ROOT = os.path.join(BASE_DIR, "gestor-fe", "gestor-fe-frontend")
REALM_JSON_PATH = os.path.join(FRONTEND_ROOT, "gestor-fe-realm.json")

FRONTEND_SERVICE = {
    "title": "Gestor FE Frontend (Angular 17)",
    "folder": FRONTEND_ROOT,
    "image_name": "gestor-fe-frontend",
    "container_name": "gestor-fe-frontend",
    "tar_name": "gestor-fe-frontend.tar",
    "ports": "80:80 y 4200:80",
    "run_cmd": "docker run -d --name gestor-fe-frontend -p 80:80 -p 4200:80 --restart always -v /opt/nginx/conf.d:/etc/nginx/conf.d -e TZ=America/Bogota gestor-fe-frontend:latest"
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
# CONSTRUCCIÓN DE IMAGEN DOCKER Y ARCHIVO TAR
# -------------------------------------------------------------------------
def build_and_save_frontend():
    folder = FRONTEND_SERVICE["folder"]
    image_name = FRONTEND_SERVICE["image_name"]
    tar_path = os.path.join(folder, FRONTEND_SERVICE["tar_name"])

    console.print(f"\n[bold blue]🐳 Construyendo imagen Docker para Frontend ({image_name}:latest)...[/bold blue]")
    console.print("[dim]Utilizando build multi-stage (Node 20 -> Nginx Alpine)...[/dim]")
    
    build_cmd = ["docker", "build", "-t", f"{image_name}:latest", "."]
    ret = subprocess.call(build_cmd, cwd=folder, shell=True)
    if ret != 0:
        console.print(f"[bold red]❌ Error al construir imagen Docker {image_name}[/bold red]")
        return None

    console.print(f"[bold blue]📦 Exportando imagen a archivo TAR: {FRONTEND_SERVICE['tar_name']}...[/bold blue]")
    save_cmd = ["docker", "save", "-o", tar_path, f"{image_name}:latest"]
    ret = subprocess.call(save_cmd, cwd=folder, shell=True)
    if ret != 0 or not os.path.exists(tar_path):
        console.print(f"[bold red]❌ Error al exportar archivo .tar para {image_name}[/bold red]")
        return None

    size_mb = os.path.getsize(tar_path) / (1024 * 1024)
    console.print(f"[bold green]✔ Archivo TAR generado ({size_mb:.2f} MB): {tar_path}[/bold green]")
    return tar_path

# -------------------------------------------------------------------------
# DESPLIEGUE REMOTO EN VM3
# -------------------------------------------------------------------------
def deploy_frontend_remote(tar_path):
    client, pwd = get_ssh_client(VM_FRONTEND)
    if not client:
        return False

    remote_file = f"{VM_FRONTEND['remote_dir']}/{FRONTEND_SERVICE['tar_name']}"

    console.print(f"\n[bold magenta]🚀 Enviando imagen {FRONTEND_SERVICE['tar_name']} a VM3 ({VM_FRONTEND['host']})...[/bold magenta]")
    try:
        transfer_file_sftp(client, tar_path, remote_file)
        console.print(f"[bold green]✔ Transferencia completada.[/bold green]")

        # Sincronizar archivo de configuración nginx con subdominios
        local_nginx = os.path.join(FRONTEND_ROOT, "nginx.conf")
        if os.path.exists(local_nginx):
            console.print("[bold yellow]⚙️ Sincronizando configuración Nginx (/opt/nginx/conf.d/default.conf)...[/bold yellow]")
            transfer_file_sftp(client, local_nginx, "/opt/nginx/conf.d/default.conf")

        console.print(f"[bold yellow]🔄 Deteniendo y eliminando contenedor previo si existe...[/bold yellow]")
        run_remote_command(client, f"docker rm -f {FRONTEND_SERVICE['container_name']} 2>/dev/null || true")

        console.print(f"[bold yellow]📥 Cargando imagen en Docker remoto...[/bold yellow]")
        out, err = run_remote_command(client, f"docker load -i {remote_file}")
        console.print(f"  [dim]{out.strip()}[/dim]")

        console.print(f"[bold yellow]▶️ Iniciando contenedor ({FRONTEND_SERVICE['container_name']})...[/bold yellow]")
        out, err = run_remote_command(client, FRONTEND_SERVICE["run_cmd"])
        if err and "error" in err.lower():
            console.print(f"[bold red]❌ Error al iniciar contenedor: {err}[/bold red]")
            client.close()
            return False

        console.print(f"[bold green]✔ Contenedor {FRONTEND_SERVICE['container_name']} iniciado exitosamente.[/bold green]")
        console.print(f"[bold cyan]🌐 Accesible en:[/bold cyan]")
        console.print(f"   • https://gestor-fe.assolidaria.org.co")
        console.print(f"   • https://auth.assolidaria.org.co")
        console.print(f"   • http://{VM_FRONTEND['host']}")

        time.sleep(2)
        check_out, _ = run_remote_command(client, f"docker ps --filter name={FRONTEND_SERVICE['container_name']} --format '{{{{.Names}}}} | {{{{.Status}}}} | {{{{.Ports}}}}'")
        console.print(f"[bold cyan]📊 Estado del contenedor:[/bold cyan] {check_out.strip()}")

        client.close()
        return True
    except Exception as e:
        console.print(f"[bold red]❌ Error durante el despliegue remoto: {e}[/bold red]")
        client.close()
        return False

# -------------------------------------------------------------------------
# GESTIÓN Y SINCRONIZACIÓN DE KEYCLOAK EN VM3
# -------------------------------------------------------------------------
def get_keycloak_admin_token():
    token_url = f"{KEYCLOAK_CONFIG['url']}/realms/master/protocol/openid-connect/token"
    payload = {
        "client_id": "admin-cli",
        "username": KEYCLOAK_CONFIG["admin_user"],
        "password": KEYCLOAK_CONFIG["admin_pass"],
        "grant_type": "password"
    }
    try:
        r = requests.post(token_url, data=payload, timeout=5)
        if r.status_code == 200:
            return r.json().get("access_token")
        else:
            console.print(f"[bold red]❌ Error al obtener token de Keycloak ({r.status_code}):[/bold red] {r.text}")
            return None
    except Exception as e:
        console.print(f"[bold red]❌ Error de conexión con Keycloak en {token_url}:[/bold red] {e}")
        return None

def sync_keycloak_realm():
    console.print(Panel.fit(
        f"[bold cyan]Sincronización de Keycloak en VM3 ({KEYCLOAK_CONFIG['url']})[/bold cyan]\n"
        f"Realm objetivo: [green]{KEYCLOAK_CONFIG['target_realm']}[/green] | Archivo: [green]{os.path.basename(REALM_JSON_PATH)}[/green]",
        border_style="cyan"
    ))

    if not os.path.exists(REALM_JSON_PATH):
        console.print(f"[bold red]❌ Archivo {REALM_JSON_PATH} no encontrado.[/bold red]")
        return False

    with open(REALM_JSON_PATH, "r", encoding="utf-8") as f:
        realm_data = json.load(f)

    console.print("[bold yellow]🔑 Autenticando con Keycloak Master Admin...[/bold yellow]")
    token = get_keycloak_admin_token()
    if not token:
        return False

    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }

    # Verificar si el realm ya existe
    target_url = f"{KEYCLOAK_CONFIG['url']}/admin/realms/{KEYCLOAK_CONFIG['target_realm']}"
    r_check = requests.get(target_url, headers=headers, timeout=5)

    if r_check.status_code == 200:
        console.print(f"[bold green]✔ El realm '{KEYCLOAK_CONFIG['target_realm']}' ya existe.[/bold green]")
        console.print("[bold yellow]🔄 Sincronizando clientes y URLs de redirección...[/bold yellow]")

        # Actualizar clientes
        clients_url = f"{target_url}/clients"
        r_clients = requests.get(clients_url, headers=headers, timeout=5)
        if r_clients.status_code == 200:
            existing_clients = r_clients.json()
            client_id_map = {c["clientId"]: c["id"] for c in existing_clients}

            for c_new in realm_data.get("clients", []):
                cid = c_new.get("clientId")
                if cid in client_id_map:
                    internal_id = client_id_map[cid]
                    update_url = f"{clients_url}/{internal_id}"
                    r_upd = requests.put(update_url, headers=headers, json=c_new, timeout=5)
                    if r_upd.status_code in [200, 204]:
                        console.print(f"  [green]✔ Cliente '{cid}' actualizado correctamente.[/green]")
                    else:
                        console.print(f"  [yellow]⚠️ No se pudo actualizar cliente '{cid}': {r_upd.text}[/yellow]")
    elif r_check.status_code == 404:
        console.print(f"[bold yellow]⚙️ Creando e importando el realm '{KEYCLOAK_CONFIG['target_realm']}' desde JSON...[/bold yellow]")
        create_url = f"{KEYCLOAK_CONFIG['url']}/admin/realms"
        r_create = requests.post(create_url, headers=headers, json=realm_data, timeout=10)
        if r_create.status_code in [201, 204]:
            console.print(f"[bold green]✔ Realm '{KEYCLOAK_CONFIG['target_realm']}' creado e importado exitosamente.[/bold green]")
        else:
            console.print(f"[bold red]❌ Error al crear realm ({r_create.status_code}): {r_create.text}[/bold red]")
            return False
    else:
        console.print(f"[bold red]❌ Error consultando realm en Keycloak: {r_check.status_code}[/bold red]")
        return False

    return True

# -------------------------------------------------------------------------
# ESTADO Y LOGS EN VM3
# -------------------------------------------------------------------------
def show_vm3_status():
    client, pwd = get_ssh_client(VM_FRONTEND)
    if not client:
        return

    out, _ = run_remote_command(client, "docker ps -a --format 'table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}'")
    table = Table(title="Contenedores en VM3 Frontend (10.3.26.4)", show_header=True, header_style="bold magenta")
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
    client, pwd = get_ssh_client(VM_FRONTEND)
    if not client:
        return

    console.print(f"\n[bold yellow]📜 Últimas 100 líneas de log para {service_name}:[/bold yellow]\n")
    out, _ = run_remote_command(client, f"docker logs --tail 100 {service_name}")
    console.print(out)
    client.close()

def test_http_endpoint():
    console.print("\n[bold yellow]🔍 Probando respuesta HTTP del Frontend y Keycloak (Subdominios & IP)...[/bold yellow]")
    endpoints = [
        ("Subdominio Frontend", "https://gestor-fe.assolidaria.org.co"),
        ("Subdominio Auth", "https://auth.assolidaria.org.co"),
        ("IP Directa Frontend", f"http://{VM_FRONTEND['host']}"),
        ("Keycloak Backend Port", f"{KEYCLOAK_CONFIG['url']}")
    ]
    for name, url in endpoints:
        try:
            r = requests.get(url, timeout=5)
            console.print(f"  ✔ [bold green]{name}[/bold green] ({url}) -> Código {r.status_code} ({len(r.content)} bytes)")
        except Exception as e:
            console.print(f"  ❌ [bold red]{name}[/bold red] ({url}) -> Error: {e}")

# -------------------------------------------------------------------------
# FLUJO PRINCIPAL DE DESPLIEGUE DE FRONTEND
# -------------------------------------------------------------------------
def deploy_frontend_flow():
    console.print(Panel.fit(
        f"[bold green]INICIANDO DESPLIEGUE DE FRONTEND EN VM3 ({VM_FRONTEND['host']})[/bold green]",
        border_style="green"
    ))

    # 1. Construir imagen Docker multi-etapa y empaquetar .tar
    tar_path = build_and_save_frontend()
    if not tar_path:
        return False

    # 2. Desplegar en VM3
    ok = deploy_frontend_remote(tar_path)
    if not ok:
        return False

    # 3. Sincronizar Keycloak
    sync_keycloak_realm()

    console.print(Panel.fit("[bold green]✔ PROCESO DE DESPLIEGUE FINALIZADO EXITOSAMENTE[/bold green]", border_style="green"))
    show_vm3_status()
    test_http_endpoint()
    return True

# -------------------------------------------------------------------------
# MENÚ INTERACTIVO
# -------------------------------------------------------------------------
def main():
    while True:
        console.print("\n")
        console.print(Panel.fit(
            "[bold white]GESTOR FACTURACIÓN ELECTRÓNICA - GESTOR FE[/bold white]\n"
            f"[bold cyan]Módulo de Despliegue Frontend Angular -> VM3 ({VM_FRONTEND['host']}) & Keycloak[/bold cyan]",
            border_style="blue"
        ))

        try:
            choice = questionary.select(
                "Seleccione una acción:",
                choices=[
                    "1. 🚀 Desplegar Frontend Angular en VM3 (Construir, Transferir, Iniciar)",
                    "2. 🔐 Sincronizar / Importar Realm en Keycloak (10.3.26.4:8080)",
                    "3. 📊 Ver estado de contenedores en VM3",
                    "4. 📜 Ver logs del Frontend (gestor-fe-frontend)",
                    "5. 📜 Ver logs de Keycloak (keycloak_app)",
                    "6. 🔍 Probar conectividad HTTP (Health Check)",
                    "7. 🔄 Reiniciar contenedor Frontend",
                    "8. ❌ Salir"
                ]
            ).ask()
        except KeyboardInterrupt:
            break

        if not choice or "8. ❌ Salir" in choice:
            console.print("[dim]Operación cancelada. ¡Hasta luego![/dim]")
            break
        elif "1. 🚀 Desplegar Frontend" in choice:
            deploy_frontend_flow()
        elif "2. 🔐 Sincronizar / Importar Realm" in choice:
            sync_keycloak_realm()
        elif "3. 📊 Ver estado de contenedores" in choice:
            show_vm3_status()
        elif "4. 📜 Ver logs del Frontend" in choice:
            view_logs("gestor-fe-frontend")
        elif "5. 📜 Ver logs de Keycloak" in choice:
            view_logs("keycloak_app")
        elif "6. 🔍 Probar conectividad HTTP" in choice:
            test_http_endpoint()
        elif "7. 🔄 Reiniciar contenedor Frontend" in choice:
            client, _ = get_ssh_client(VM_FRONTEND)
            if client:
                run_remote_command(client, "docker restart gestor-fe-frontend")
                console.print("[bold green]✔ Contenedor Frontend reiniciado.[/bold green]")
                client.close()
                show_vm3_status()

if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] == "--all":
        deploy_frontend_flow()
    else:
        main()
