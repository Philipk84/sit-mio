# Despliegue distribuido en Ubuntu sin sudo

Esta guia usa las IPs reales de la sala y evita depender de `/etc/hosts` o `sudo`.

## IPs finales

```text
DataCenter + Postgres = 192.168.131.38
CCO Server            = 192.168.131.39
Web + Frontend        = 192.168.131.40
CSV grande            = /opt/swarch/datacenter/datagrams-MiniPilot.csv
CSV de rutas          = /opt/swarch/datacenter/lines-241-ActiveGT.csv
DB user               = postgres
DB password           = postgres
```

Los JARs generados desde este codigo ya quedan apuntando a esas IPs por defecto.

## Puertos

```text
10001  DataCenter Ice
10002  DataCenter HTTP para servir datagrams-MiniPilot.csv a los buses
10010  CCO Ice
8080   Web Gateway HTTP
3000   Frontend Node
5432   Postgres local en DataCenter
```

Si algun puerto no responde, deben pedir a quien administra la sala que lo habilite.

## Construccion

En la maquina de desarrollo:

```bash
gradle --offline clean build
```

Copiar a cada computador los JARs y `lib/` correspondientes.

## 1. DataCenter

Carpeta esperada:

```text
/opt/swarch/datacenter/
  common-1.0.0.jar
  datacenter-1.0.0.jar
  datagrams-MiniPilot.csv
  lines-241-ActiveGT.csv
  lib/
    ice-3.7.10.jar
    postgresql-42.3.1.jar
```

Comando normal:

```bash
cd /opt/swarch/datacenter

java -cp "lib/*:common-1.0.0.jar:datacenter-1.0.0.jar" edu.icesi.mio.datacenter.DataCenterApplication
```

Defaults ya embebidos:

```text
MIO_DATACENTER_ENDPOINTS=tcp -h 0.0.0.0 -p 10001
MIO_DB_URL=jdbc:postgresql://localhost:5432/mio
MIO_DB_USER=postgres
MIO_DB_PASSWORD=postgres
MIO_DATAGRAMS_FILE=/opt/swarch/datacenter/datagrams-MiniPilot.csv
MIO_ROUTES_FILE=/opt/swarch/datacenter/lines-241-ActiveGT.csv
MIO_DATAGRAMS_HTTP_PORT=10002
```

El DataCenter tambien publica el CSV para los buses en:

```text
http://192.168.131.38:10002/datagrams-MiniPilot.csv
```

Prueba desde otro computador:

```bash
curl -I http://192.168.131.38:10002/datagrams-MiniPilot.csv
```

## 2. CCO Server

Carpeta esperada:

```text
/opt/swarch/cco/
  common-1.0.0.jar
  cco-server-1.0.0.jar
  lib/
    ice-3.7.10.jar
    postgresql-42.3.1.jar
```

Comando normal:

```bash
cd /opt/swarch/cco

java -cp "lib/*:common-1.0.0.jar:cco-server-1.0.0.jar" edu.icesi.mio.cco.CcoServerApplication
```

Defaults ya embebidos:

```text
MIO_CCO_ENDPOINTS=tcp -h 0.0.0.0 -p 10010
MIO_HISTORICAL_PROXY=HistoricalRepository:tcp -h 192.168.131.38 -p 10001
MIO_OPERATIONAL_PROXY=OperationalRepository:tcp -h 192.168.131.38 -p 10001
MIO_CCO_WORKERS=4
```

## 3. Web Gateway

Carpeta esperada:

```text
/opt/swarch/gateway/
  common-1.0.0.jar
  web-gateway-1.0.0.jar
  lib/
    ice-3.7.10.jar
    postgresql-42.3.1.jar
```

Comando normal:

```bash
cd /opt/swarch/gateway

java -cp "lib/*:common-1.0.0.jar:web-gateway-1.0.0.jar" edu.icesi.mio.gateway.WebGatewayApplication
```

Defaults ya embebidos:

```text
MIO_CCO_PROXY_ENDPOINT=tcp -h 192.168.131.39 -p 10010
MIO_GATEWAY_PORT=8080
```

Prueba:

```bash
curl http://192.168.131.40:8080/api/routes
```

## 4. Frontend Node

Carpeta esperada:

```text
/opt/swarch/frontend/
  server.cjs
  public/
```

Comando:

```bash
cd /opt/swarch/frontend

GOOGLE_MAPS_API_KEY='TU_API_KEY' \
FRONTEND_PORT='3000' \
node server.cjs
```

Default ya embebido:

```text
MIO_GATEWAY_URL=http://192.168.131.40:8080
```

Abrir:

```text
http://192.168.131.40:3000
```

## 5. Buses simulados

Carpeta esperada:

```text
/opt/swarch/bus/
  common-1.0.0.jar
  bus-simulator-1.0.0.jar
  lib/
    ice-3.7.10.jar
    postgresql-42.3.1.jar
```

Los buses no necesitan tener el CSV grande localmente. Por defecto lo leen desde el DataCenter:

```text
MIO_BUS_DATAGRAMS_FILE=http://192.168.131.38:10002/datagrams-MiniPilot.csv
MIO_DATAGRAM_RECEIVER_PROXY=DatagramReceiver:tcp -h 192.168.131.39 -p 10010
MIO_BUS_DELAY_MS=1000
```

Bus ejemplo:

```bash
cd /opt/swarch/bus

MIO_SIM_LINE_ID='306' \
MIO_SIM_BUS_ID='9101' \
MIO_SOURCE_BUS_ID='1203' \
java -cp "lib/*:common-1.0.0.jar:bus-simulator-1.0.0.jar" edu.icesi.mio.bus.BusSimulatorApplication
```

Segundo bus en la misma ruta:

```bash
cd /opt/swarch/bus

MIO_SIM_LINE_ID='306' \
MIO_SIM_BUS_ID='9102' \
MIO_SOURCE_BUS_ID='190' \
java -cp "lib/*:common-1.0.0.jar:bus-simulator-1.0.0.jar" edu.icesi.mio.bus.BusSimulatorApplication
```

Reglas:

- `MIO_SIM_BUS_ID` debe ser unico por cada proceso activo.
- `MIO_SIM_LINE_ID` puede repetirse si quieren varios buses en la misma ruta.
- `MIO_SOURCE_BUS_ID` debe existir en el CSV para esa ruta.

## Orden de arranque

1. Postgres en `192.168.131.38`.
2. DataCenter en `192.168.131.38`.
3. CCO Server en `192.168.131.39`.
4. Web Gateway en `192.168.131.40`.
5. Frontend Node en `192.168.131.40`.
6. Buses simulados.

## Pruebas

DataCenter Ice:

```bash
nc -vz 192.168.131.38 10001
```

CSV remoto para buses:

```bash
curl -I http://192.168.131.38:10002/datagrams-MiniPilot.csv
```

CCO Ice:

```bash
nc -vz 192.168.131.39 10010
```

Gateway:

```bash
curl http://192.168.131.40:8080/api/routes
curl 'http://192.168.131.40:8080/api/positions?lineId=306'
curl 'http://192.168.131.40:8080/api/route-details?lineId=306'
```

Frontend:

```text
http://192.168.131.40:3000
```

## Errores comunes

`UnknownHostException`

No deberia pasar con estos JARs porque ya no dependen de `mio-datacenter` ni `mio-cco`. Si pasa, estan usando un JAR viejo.

`NoSuchFileException: datagrams-MiniPilot.csv`

En DataCenter, revisar:

```bash
ls -lh /opt/swarch/datacenter/datagrams-MiniPilot.csv
```

En buses, probar:

```bash
curl -I http://192.168.131.38:10002/datagrams-MiniPilot.csv
```

`Connection refused`

El proceso destino no esta corriendo o el puerto no esta disponible.

`No route to host` o timeout

Las maquinas no se ven entre si o hay firewall de la sala.
