# Despliegue distribuido en sala

Esta guia asume que los computadores estan en la misma red LAN y que se quiere ejecutar cada nodo pasando JARs y variables de entorno, sin Gradle en las maquinas destino.

## Maquinas sugeridas

Reemplaza las IP por las reales de la sala:

- DataCenter + Postgres: `192.168.1.10`
- CCO Server: `192.168.1.20`
- Web Gateway + Frontend Node: `192.168.1.30`
- Buses simulados: `192.168.1.41`, `192.168.1.42`, etc.
- Computador con CSV grande compartido: `\\PC-SALA\datos\datagrams-MiniPilot.csv`

Puertos que deben estar abiertos:

- DataCenter Ice: `10001/tcp`
- CCO Ice: `10010/tcp`
- Web Gateway HTTP: `8080/tcp`
- Frontend Node HTTP: `3000/tcp`
- Postgres: `5432/tcp`, solo necesario desde el DataCenter si Postgres esta en otra maquina.

## Artefactos a copiar

Primero construir en la maquina de desarrollo:

```powershell
gradle --offline clean build
```

Para cada maquina Java, copiar estos archivos:

- `common/build/libs/common-1.0.0.jar`
- `lib/ice-3.7.10.jar`
- `lib/postgresql-42.3.1.jar`
- El JAR del nodo que corresponda:
  - DataCenter: `datacenter/build/libs/datacenter-1.0.0.jar`
  - CCO: `cco-server/build/libs/cco-server-1.0.0.jar`
  - Bus: `bus-simulator/build/libs/bus-simulator-1.0.0.jar`
  - Web Gateway: `web-gateway/build/libs/web-gateway-1.0.0.jar`

Estructura recomendada en cada maquina:

```text
C:\mio\
  common-1.0.0.jar
  datacenter-1.0.0.jar
  cco-server-1.0.0.jar
  bus-simulator-1.0.0.jar
  web-gateway-1.0.0.jar
  lib\
    ice-3.7.10.jar
    postgresql-42.3.1.jar
  lines-241-ActiveGT.csv
```

No es obligatorio copiar todos los JARs a todas las maquinas; basta con `common`, `lib` y el JAR del nodo que se va a ejecutar.

Si prefieres pasar el path exacto de Ice al correr:

```powershell
$env:ICE_JAR='C:\mio\lib\ice-3.7.10.jar'
$env:PG_JAR='C:\mio\lib\postgresql-42.3.1.jar'
```

Luego usa `"$env:ICE_JAR;$env:PG_JAR;common-1.0.0.jar;NODO-1.0.0.jar"` como classpath.

## 1. DataCenter + Postgres

En la maquina `192.168.1.10`, crear la base de datos `mio` en Postgres y dejar el puerto `5432` disponible localmente para el DataCenter.

```powershell
cd C:\mio
$env:MIO_DATACENTER_ENDPOINTS='tcp -h 192.168.1.10 -p 10001'
$env:MIO_DB_URL='jdbc:postgresql://localhost:5432/mio'
$env:MIO_DB_USER='postgres'
$env:MIO_DB_PASSWORD='postgres'
$env:MIO_DATAGRAMS_FILE='\\PC-SALA\datos\datagrams-MiniPilot.csv'
$env:MIO_ROUTES_FILE='C:\mio\lines-241-ActiveGT.csv'
java -cp "lib\*;common-1.0.0.jar;datacenter-1.0.0.jar" edu.icesi.mio.datacenter.DataCenterApplication
```

Con path explicito de Ice:

```powershell
java -cp "$env:ICE_JAR;$env:PG_JAR;common-1.0.0.jar;datacenter-1.0.0.jar" edu.icesi.mio.datacenter.DataCenterApplication
```

## 2. CCO Server

En la maquina `192.168.1.20`:

```powershell
cd C:\mio
$env:MIO_CCO_ENDPOINTS='tcp -h 192.168.1.20 -p 10010'
$env:MIO_HISTORICAL_PROXY='HistoricalRepository:tcp -h 192.168.1.10 -p 10001'
$env:MIO_OPERATIONAL_PROXY='OperationalRepository:tcp -h 192.168.1.10 -p 10001'
$env:MIO_CCO_WORKERS='4'
$env:MIO_DATAGRAM_QUEUE_CAPACITY='100000'
java -cp "lib\*;common-1.0.0.jar;cco-server-1.0.0.jar" edu.icesi.mio.cco.CcoServerApplication
```

## 3. Web Gateway

En la maquina `192.168.1.30`:

```powershell
cd C:\mio
$env:MIO_CCO_PROXY_ENDPOINT='tcp -h 192.168.1.20 -p 10010'
$env:MIO_GATEWAY_PORT='8080'
java -cp "lib\*;common-1.0.0.jar;web-gateway-1.0.0.jar" edu.icesi.mio.gateway.WebGatewayApplication
```

El navegador consumira este gateway por HTTP, por ejemplo `http://192.168.1.30:8080`.

## 4. Frontend Node

Copiar la carpeta `frontend` completa a la maquina `192.168.1.30`. Luego:

```powershell
cd C:\mio
$env:MIO_GATEWAY_URL='http://192.168.1.30:8080'
$env:GOOGLE_MAPS_API_KEY='TU_API_KEY'
$env:FRONTEND_PORT='3000'
node frontend\server.cjs
```

Abrir desde cualquier equipo de la red:

```text
http://192.168.1.30:3000
```

## 5. Buses simulados

Cada bus se ejecuta como un proceso independiente. En cada maquina de bus:

```powershell
cd C:\mio
$env:MIO_DATAGRAM_RECEIVER_PROXY='DatagramReceiver:tcp -h 192.168.1.20 -p 10010'
$env:MIO_BUS_DATAGRAMS_FILE='\\PC-SALA\datos\datagrams-MiniPilot.csv'
$env:MIO_BUS_DELAY_MS='250'
$env:MIO_BUS_LOOP='true'
$env:MIO_SIM_LINE_ID='306'
$env:MIO_SIM_BUS_ID='9101'
$env:MIO_SOURCE_BUS_ID='1203'
java -cp "lib\*;common-1.0.0.jar;bus-simulator-1.0.0.jar" edu.icesi.mio.bus.BusSimulatorApplication
```

Otro bus de la misma ruta, en otra terminal o maquina:

```powershell
cd C:\mio
$env:MIO_DATAGRAM_RECEIVER_PROXY='DatagramReceiver:tcp -h 192.168.1.20 -p 10010'
$env:MIO_BUS_DATAGRAMS_FILE='\\PC-SALA\datos\datagrams-MiniPilot.csv'
$env:MIO_BUS_DELAY_MS='250'
$env:MIO_BUS_LOOP='true'
$env:MIO_SIM_LINE_ID='306'
$env:MIO_SIM_BUS_ID='9102'
$env:MIO_SOURCE_BUS_ID='190'
java -cp "lib\*;common-1.0.0.jar;bus-simulator-1.0.0.jar" edu.icesi.mio.bus.BusSimulatorApplication
```

Regla practica:

- `MIO_SIM_LINE_ID`: ruta que el bus representara en la demo.
- `MIO_SIM_BUS_ID`: ID unico del bus simulado; no repetirlo entre procesos activos.
- `MIO_SOURCE_BUS_ID`: bus real del CSV usado como fuente de puntos.

## Orden de arranque

1. Postgres.
2. DataCenter.
3. CCO Server.
4. Web Gateway.
5. Frontend Node.
6. Buses simulados.

## Pruebas rapidas

Desde la maquina del frontend o cualquier equipo con acceso:

```powershell
Invoke-RestMethod 'http://192.168.1.30:8080/api/routes'
Invoke-RestMethod 'http://192.168.1.30:8080/api/positions?lineId=306'
Invoke-RestMethod 'http://192.168.1.30:8080/api/route-details?lineId=306'
```

Si `positions` devuelve arreglo vacio, revisar que los buses apunten al CCO correcto:

```powershell
$env:MIO_DATAGRAM_RECEIVER_PROXY
```

Si el CCO no conecta al DataCenter, revisar que los proxies apunten a la IP del DataCenter y no a `127.0.0.1`:

```powershell
$env:MIO_HISTORICAL_PROXY
$env:MIO_OPERATIONAL_PROXY
```

## Notas para la sala

- No usar `127.0.0.1` entre maquinas; usar siempre la IP LAN de la maquina destino.
- El CSV grande puede estar en una ruta compartida UNC como `\\PC-SALA\datos\datagrams-MiniPilot.csv`, siempre que el usuario de Windows que ejecuta Java tenga permisos de lectura.
- Si la red bloquea puertos, permitir `10001`, `10010`, `8080` y `3000` en Firewall de Windows.
- Si Postgres esta en la misma maquina del DataCenter, `localhost` en `MIO_DB_URL` esta bien. Si Postgres esta en otra maquina, usar la IP de esa maquina.
