# Despliegue distribuido en Ubuntu sin sudo

Esta guia es para computadores Ubuntu en la misma red, sin permisos de `sudo`. La idea es copiar los JARs a una carpeta del usuario o una carpeta ya disponible, por ejemplo `/opt/swarch`, y ejecutar cada nodo con `java -cp`.

Como no se puede editar `/etc/hosts`, no dependemos de nombres como `mio-datacenter` o `mio-cco`. En cada comando se pasa la IP real del componente remoto con el que se comunica.

## 1. IPs que deben identificar

Antes de correr nada, anoten las IPs reales:

```text
IP_DATACENTER = IP del computador donde corre Postgres + DataCenter
IP_CCO        = IP del computador donde corre CCO Server
IP_WEB        = IP del computador donde corre Web Gateway + Frontend
IP_CSV        = IP o ruta del computador donde esta el CSV grande
```

En cada Ubuntu pueden ver su IP con:

```bash
hostname -I
```

Ejemplo:

```text
IP_DATACENTER = 192.168.1.10
IP_CCO        = 192.168.1.20
IP_WEB        = 192.168.1.30
```

## 2. Puertos usados

Estos puertos deben poder comunicarse en la red de la sala:

```text
10001  DataCenter Ice
10010  CCO Ice
8080   Web Gateway HTTP
3000   Frontend Node
5432   Postgres, normalmente solo local en DataCenter
```

Si un puerto esta bloqueado por firewall, deben pedir a quien administra la sala que lo habilite. Sin `sudo` no se puede abrir desde el usuario normal.

## 3. Artefactos a copiar

En la maquina de desarrollo construir:

```bash
gradle --offline clean build
```

Copiar a cada maquina estos archivos segun el nodo:

DataCenter:

```text
common-1.0.0.jar
datacenter-1.0.0.jar
lib/ice-3.7.10.jar
lib/postgresql-42.3.1.jar
lines-241-ActiveGT.csv
```

CCO:

```text
common-1.0.0.jar
cco-server-1.0.0.jar
lib/ice-3.7.10.jar
lib/postgresql-42.3.1.jar
```

Web Gateway:

```text
common-1.0.0.jar
web-gateway-1.0.0.jar
lib/ice-3.7.10.jar
lib/postgresql-42.3.1.jar
```

Frontend:

```text
frontend/
```

Bus:

```text
common-1.0.0.jar
bus-simulator-1.0.0.jar
lib/ice-3.7.10.jar
lib/postgresql-42.3.1.jar
```

Ejemplo de estructura como la que ya tienes:

```text
/opt/swarch/datacenter/
  common-1.0.0.jar
  datacenter-1.0.0.jar
  lines-241-ActiveGT.csv
  lib/
    ice-3.7.10.jar
    postgresql-42.3.1.jar
```

El classpath en Linux usa `:`:

```bash
java -cp "lib/*:common-1.0.0.jar:NODO-1.0.0.jar" CLASE_PRINCIPAL
```

## 4. DataCenter

Corre en el computador `IP_DATACENTER`.

Como el DataCenter es servidor Ice, debe escuchar en todas las interfaces:

```bash
MIO_DATACENTER_ENDPOINTS='tcp -h 0.0.0.0 -p 10001'
```

Comando ejemplo:

```bash
cd /opt/swarch/datacenter

MIO_DATACENTER_ENDPOINTS='tcp -h 0.0.0.0 -p 10001' \
MIO_DB_URL='jdbc:postgresql://localhost:5432/mio' \
MIO_DB_USER='postgres' \
MIO_DB_PASSWORD='postgres' \
MIO_DATAGRAMS_FILE='/ruta/real/al/datagrams-MiniPilot.csv' \
MIO_ROUTES_FILE='/opt/swarch/datacenter/lines-241-ActiveGT.csv' \
java -cp "lib/*:common-1.0.0.jar:datacenter-1.0.0.jar" edu.icesi.mio.datacenter.DataCenterApplication
```

Si el DataCenter no necesita cargar historico completo al inicio o quieren usar un CSV pequeño, cambia `MIO_DATAGRAMS_FILE` por el archivo que tengan disponible.

Importante:

- No uses `mio-datacenter` si no existe en DNS.
- No uses la IP del DataCenter en `-h` para arrancar si da problemas; `0.0.0.0` es lo correcto para escuchar.
- Si Postgres esta en el mismo computador, `localhost` en `MIO_DB_URL` esta bien.

## 5. CCO Server

Corre en el computador `IP_CCO`.

El CCO escucha como servidor en `0.0.0.0`, pero se conecta al DataCenter usando `IP_DATACENTER`.

Reemplaza `192.168.1.10` por la IP real del DataCenter:

```bash
cd /opt/swarch/cco

MIO_CCO_ENDPOINTS='tcp -h 0.0.0.0 -p 10010' \
MIO_HISTORICAL_PROXY='HistoricalRepository:tcp -h 192.168.1.10 -p 10001' \
MIO_OPERATIONAL_PROXY='OperationalRepository:tcp -h 192.168.1.10 -p 10001' \
MIO_CCO_WORKERS='4' \
java -cp "lib/*:common-1.0.0.jar:cco-server-1.0.0.jar" edu.icesi.mio.cco.CcoServerApplication
```

Si el DataCenter esta en `10.0.0.25`, entonces ambos proxies deben usar `10.0.0.25`.

## 6. Web Gateway

Corre en el computador `IP_WEB`.

El gateway se conecta al CCO usando `IP_CCO`.

Reemplaza `192.168.1.20` por la IP real del CCO:

```bash
cd /opt/swarch/gateway

MIO_CCO_PROXY_ENDPOINT='tcp -h 192.168.1.20 -p 10010' \
MIO_GATEWAY_PORT='8080' \
java -cp "lib/*:common-1.0.0.jar:web-gateway-1.0.0.jar" edu.icesi.mio.gateway.WebGatewayApplication
```

Prueba desde el computador del gateway:

```bash
curl http://localhost:8080/api/routes
```

Prueba desde otro computador:

```bash
curl http://IP_WEB:8080/api/routes
```

## 7. Frontend Node

Corre en el computador `IP_WEB`, normalmente junto al Web Gateway.

Reemplaza `192.168.1.30` por la IP real del computador web. Esa URL debe ser alcanzable desde el navegador.

```bash
cd /opt/swarch/frontend

MIO_GATEWAY_URL='http://192.168.1.30:8080' \
GOOGLE_MAPS_API_KEY='TU_API_KEY' \
FRONTEND_PORT='3000' \
node server.cjs
```

Abrir en el navegador:

```text
http://192.168.1.30:3000
```

Si en tu copia la carpeta es `/opt/swarch/frontend/frontend`, entonces entra a la carpeta padre que contiene `server.cjs` o ejecuta:

```bash
node frontend/server.cjs
```

## 8. Buses simulados

Cada bus corre como proceso independiente, en uno o varios computadores.

Cada bus se conecta al CCO usando `IP_CCO`.

Reemplaza:

- `192.168.1.20` por la IP real del CCO.
- `/ruta/real/al/datagrams-MiniPilot.csv` por el path donde ese computador pueda leer el CSV grande.
- `MIO_SIM_BUS_ID`, `MIO_SIM_LINE_ID`, `MIO_SOURCE_BUS_ID` segun el bus que quieran simular.

Ejemplo:

```bash
cd /opt/swarch/bus

MIO_DATAGRAM_RECEIVER_PROXY='DatagramReceiver:tcp -h 192.168.1.20 -p 10010' \
MIO_BUS_DATAGRAMS_FILE='/ruta/real/al/datagrams-MiniPilot.csv' \
MIO_BUS_DELAY_MS='1000' \
MIO_BUS_LOOP='true' \
MIO_SIM_LINE_ID='306' \
MIO_SIM_BUS_ID='9101' \
MIO_SOURCE_BUS_ID='1203' \
java -cp "lib/*:common-1.0.0.jar:bus-simulator-1.0.0.jar" edu.icesi.mio.bus.BusSimulatorApplication
```

Segundo bus en la misma ruta:

```bash
cd /opt/swarch/bus

MIO_DATAGRAM_RECEIVER_PROXY='DatagramReceiver:tcp -h 192.168.1.20 -p 10010' \
MIO_BUS_DATAGRAMS_FILE='/ruta/real/al/datagrams-MiniPilot.csv' \
MIO_BUS_DELAY_MS='1000' \
MIO_BUS_LOOP='true' \
MIO_SIM_LINE_ID='306' \
MIO_SIM_BUS_ID='9102' \
MIO_SOURCE_BUS_ID='190' \
java -cp "lib/*:common-1.0.0.jar:bus-simulator-1.0.0.jar" edu.icesi.mio.bus.BusSimulatorApplication
```

Reglas:

- `MIO_SIM_BUS_ID` debe ser unico por cada bus activo.
- `MIO_SIM_LINE_ID` puede repetirse si quieren dos buses en la misma ruta.
- `MIO_SOURCE_BUS_ID` debe existir en el CSV para esa ruta, si no el simulador no tendra puntos para enviar.

## 9. Orden de arranque

1. Postgres en el computador del DataCenter.
2. DataCenter.
3. CCO Server.
4. Web Gateway.
5. Frontend Node.
6. Buses simulados.

## 10. Pruebas rapidas sin sudo

Ver IP local:

```bash
hostname -I
```

Ver si un puerto responde:

```bash
nc -vz IP_DATACENTER 10001
nc -vz IP_CCO 10010
nc -vz IP_WEB 8080
nc -vz IP_WEB 3000
```

Probar API:

```bash
curl http://IP_WEB:8080/api/routes
curl 'http://IP_WEB:8080/api/positions?lineId=306'
curl 'http://IP_WEB:8080/api/route-details?lineId=306'
```

Si `nc` no existe:

```bash
timeout 3 bash -c '</dev/tcp/IP_CCO/10010' && echo OK || echo FALLO
```

## 11. Errores comunes

`UnknownHostException: mio-datacenter`

Significa que el JAR intento usar el hostname embebido, pero esa maquina no resuelve el nombre. Solucion: pasar la IP o escuchar en `0.0.0.0`:

```bash
MIO_DATACENTER_ENDPOINTS='tcp -h 0.0.0.0 -p 10001'
```

`Connection refused`

El proceso destino no esta corriendo o el puerto no esta abierto.

`No route to host` o timeout

Las maquinas no se ven en la red o hay firewall de la sala.

`NoSuchFileException` para el CSV

El path del CSV no existe en ese computador. Revisar:

```bash
ls -lh /ruta/real/al/datagrams-MiniPilot.csv
```

## 12. Plantilla rapida de IPs

Antes de iniciar, llenen esto:

```text
IP_DATACENTER =
IP_CCO =
IP_WEB =
CSV_PATH_DATACENTER =
CSV_PATH_BUSES =
ROUTES_PATH =
```

Y luego reemplacen esos valores en los comandos anteriores.
