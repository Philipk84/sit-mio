# Despliegue distribuido en Ubuntu

Esta guia esta pensada para computadores Linux Ubuntu en la misma red LAN. La idea es copiar JARs y dependencias a cada equipo y ejecutar con `java -cp`, sin Gradle en las maquinas de despliegue.

## Defaults embebidos

Los JARs quedan preparados para esta topologia usando nombres de red fijos:

- `mio-datacenter`: maquina con Postgres y DataCenter.
- `mio-cco`: maquina con CCO Server.
- `mio-web`: maquina con Web Gateway y Frontend Node.
- CSV grande compartido/montado en buses y DataCenter: `/mnt/mio-datos/datagrams-MiniPilot.csv`.
- Rutas del MIO en DataCenter: `/opt/mio/lines-241-ActiveGT.csv`.

Con esto no hay que pasar endpoints Ice a cada comando. Solo se debe configurar una vez la resolucion de nombres en DNS o `/etc/hosts`.

Ejemplo de `/etc/hosts` en todos los computadores:

```bash
192.168.1.10 mio-datacenter
192.168.1.20 mio-cco
192.168.1.30 mio-web
```

Reemplaza las IP por las reales de la sala.

## Puertos

Abrir estos puertos en la LAN:

- DataCenter Ice: `10001/tcp`
- CCO Ice: `10010/tcp`
- Web Gateway HTTP: `8080/tcp`
- Frontend Node HTTP: `3000/tcp`
- Postgres: `5432/tcp`, solo si Postgres se consulta desde otra maquina.

En Ubuntu, si `ufw` esta activo:

```bash
sudo ufw allow 10001/tcp
sudo ufw allow 10010/tcp
sudo ufw allow 8080/tcp
sudo ufw allow 3000/tcp
```

## Construir artefactos

En la maquina de desarrollo:

```bash
gradle --offline clean build
```

Copiar a cada maquina solo lo que necesite:

- Siempre:
  - `common/build/libs/common-1.0.0.jar`
  - `lib/ice-3.7.10.jar`
  - `lib/postgresql-42.3.1.jar`
- DataCenter:
  - `datacenter/build/libs/datacenter-1.0.0.jar`
  - `lines-241-ActiveGT.csv`
- CCO:
  - `cco-server/build/libs/cco-server-1.0.0.jar`
- Bus:
  - `bus-simulator/build/libs/bus-simulator-1.0.0.jar`
- Web Gateway:
  - `web-gateway/build/libs/web-gateway-1.0.0.jar`
- Frontend:
  - carpeta `frontend/`

Estructura recomendada:

```text
/opt/mio/
  common-1.0.0.jar
  datacenter-1.0.0.jar
  cco-server-1.0.0.jar
  bus-simulator-1.0.0.jar
  web-gateway-1.0.0.jar
  lines-241-ActiveGT.csv
  lib/
    ice-3.7.10.jar
    postgresql-42.3.1.jar
  frontend/
```

Crear carpeta:

```bash
sudo mkdir -p /opt/mio /mnt/mio-datos
sudo chown -R "$USER:$USER" /opt/mio /mnt/mio-datos
```

## CSV grande

El default de los JARs busca el historico grande en:

```text
/mnt/mio-datos/datagrams-MiniPilot.csv
```

Si el CSV esta en otro computador Linux, se puede montar por NFS o Samba/CIFS en `/mnt/mio-datos`. Ejemplo CIFS:

```bash
sudo apt-get update
sudo apt-get install -y cifs-utils
sudo mount -t cifs //IP_O_PC_DATOS/compartido /mnt/mio-datos -o username=USUARIO,vers=3.0
```

Debe quedar disponible:

```bash
ls -lh /mnt/mio-datos/datagrams-MiniPilot.csv
```

## Comandos por maquina

Los comandos usan classpath Linux con `:`. Si quieres pasar el path de Ice explicitamente:

```bash
export ICE_JAR=/opt/mio/lib/ice-3.7.10.jar
export PG_JAR=/opt/mio/lib/postgresql-42.3.1.jar
```

### 1. DataCenter + Postgres

En `mio-datacenter`:

```bash
cd /opt/mio
java -cp "lib/*:common-1.0.0.jar:datacenter-1.0.0.jar" edu.icesi.mio.datacenter.DataCenterApplication
```

Equivalente con path explicito de Ice:

```bash
cd /opt/mio
java -cp "$ICE_JAR:$PG_JAR:common-1.0.0.jar:datacenter-1.0.0.jar" edu.icesi.mio.datacenter.DataCenterApplication
```

Defaults que ya trae:

- `MIO_DATACENTER_ENDPOINTS=tcp -h mio-datacenter -p 10001`
- `MIO_DB_URL=jdbc:postgresql://localhost:5432/mio`
- `MIO_DATAGRAMS_FILE=/mnt/mio-datos/datagrams-MiniPilot.csv`
- `MIO_ROUTES_FILE=/opt/mio/lines-241-ActiveGT.csv`

### 2. CCO Server

En `mio-cco`:

```bash
cd /opt/mio
java -cp "lib/*:common-1.0.0.jar:cco-server-1.0.0.jar" edu.icesi.mio.cco.CcoServerApplication
```

Defaults que ya trae:

- `MIO_CCO_ENDPOINTS=tcp -h mio-cco -p 10010`
- `MIO_HISTORICAL_PROXY=HistoricalRepository:tcp -h mio-datacenter -p 10001`
- `MIO_OPERATIONAL_PROXY=OperationalRepository:tcp -h mio-datacenter -p 10001`

### 3. Web Gateway

En `mio-web`:

```bash
cd /opt/mio
java -cp "lib/*:common-1.0.0.jar:web-gateway-1.0.0.jar" edu.icesi.mio.gateway.WebGatewayApplication
```

Defaults que ya trae:

- `MIO_CCO_PROXY_ENDPOINT=tcp -h mio-cco -p 10010`
- `MIO_GATEWAY_PORT=8080`

### 4. Frontend Node

En `mio-web`:

```bash
cd /opt/mio
export GOOGLE_MAPS_API_KEY=TU_API_KEY
node frontend/server.cjs
```

El frontend escucha en `0.0.0.0:3000`. La URL del gateway se calcula automaticamente con el mismo host desde donde se abra el navegador, por ejemplo:

```text
http://mio-web:3000
```

Si los computadores cliente no resuelven `mio-web`, abrir con IP:

```text
http://192.168.1.30:3000
```

### 5. Buses simulados

En cada maquina de bus:

```bash
cd /opt/mio
java -cp "lib/*:common-1.0.0.jar:bus-simulator-1.0.0.jar" edu.icesi.mio.bus.BusSimulatorApplication
```

Defaults que ya trae:

- `MIO_DATAGRAM_RECEIVER_PROXY=DatagramReceiver:tcp -h mio-cco -p 10010`
- `MIO_BUS_DATAGRAMS_FILE=/mnt/mio-datos/datagrams-MiniPilot.csv`
- `MIO_BUS_LOOP=true`
- `MIO_BUS_DELAY_MS=250`
- `MIO_SIM_LINE_ID=311`
- `MIO_SIM_BUS_ID=9001`
- `MIO_SOURCE_BUS_ID=846`

Para correr varios buses distintos, cada proceso debe tener un `MIO_SIM_BUS_ID` unico. Esa es la unica configuracion que no conviene dejar igual para todos. Ejemplo de dos buses en la misma ruta:

```bash
cd /opt/mio
MIO_SIM_LINE_ID=306 MIO_SIM_BUS_ID=9101 MIO_SOURCE_BUS_ID=1203 \
java -cp "lib/*:common-1.0.0.jar:bus-simulator-1.0.0.jar" edu.icesi.mio.bus.BusSimulatorApplication
```

```bash
cd /opt/mio
MIO_SIM_LINE_ID=306 MIO_SIM_BUS_ID=9102 MIO_SOURCE_BUS_ID=190 \
java -cp "lib/*:common-1.0.0.jar:bus-simulator-1.0.0.jar" edu.icesi.mio.bus.BusSimulatorApplication
```

## Orden de arranque

1. Postgres en `mio-datacenter`.
2. DataCenter.
3. CCO Server.
4. Web Gateway.
5. Frontend Node.
6. Buses simulados.

## Pruebas rapidas

Desde cualquier computador con acceso:

```bash
curl http://mio-web:8080/api/routes
curl 'http://mio-web:8080/api/positions?lineId=306'
curl 'http://mio-web:8080/api/route-details?lineId=306'
```

Probar resolucion de nombres:

```bash
getent hosts mio-datacenter
getent hosts mio-cco
getent hosts mio-web
```

Probar puertos:

```bash
nc -vz mio-datacenter 10001
nc -vz mio-cco 10010
nc -vz mio-web 8080
nc -vz mio-web 3000
```

## Overrides opcionales

Aunque los defaults ya vienen en el JAR, cualquier valor se puede reemplazar con variables de entorno si cambia la sala:

```bash
MIO_CCO_ENDPOINTS='tcp -h 192.168.1.20 -p 10010' \
java -cp "lib/*:common-1.0.0.jar:cco-server-1.0.0.jar" edu.icesi.mio.cco.CcoServerApplication
```

Esto permite usar los mismos JARs tanto con hostnames como con IPs directas.
