#!/bin/bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"

echo "========================================================"
echo "Compilando todos los microservicios de Gestor FE Backend"
echo "========================================================"

echo "[1/6] Compilando service-common..."
cd "$DIR/service-common"
mvn clean install -DskipTests=true

echo "[2/6] Compilando gestor-fe-eureka-server..."
cd "$DIR/gestor-fe-eureka-server"
mvn clean package -DskipTests=true

echo "[3/6] Compilando gestor-fe-config-server..."
cd "$DIR/gestor-fe-config-server"
mvn clean package -DskipTests=true

echo "[4/6] Compilando gestor-fe-gateway..."
cd "$DIR/gestor-fe-gateway"
mvn clean package -DskipTests=true

echo "[5/6] Compilando gestor-fe-admin-server..."
cd "$DIR/gestor-fe-admin-server"
mvn clean package -DskipTests=true

echo "[6/6] Compilando gestor-fe-admin-service y core-service..."
cd "$DIR/gestor-fe-admin-service"
mvn clean package -DskipTests=true

cd "$DIR/gestor-fe-core-service"
mvn clean package -DskipTests=true

echo "========================================================"
echo "TODOS LOS MICROSERVICIOS FUERON COMPILADOS EXITOSAMENTE!"
echo "========================================================"
cd "$DIR"
