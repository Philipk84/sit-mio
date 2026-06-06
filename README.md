# Plataforma MIO distribuida

Solucion base para los requerimientos RF4 y RF7 del parcial SITM-MIO.

## Alcance

- RF4: visualizacion en tiempo real de buses sobre mapa de Cali, usando datagramas GPS recibidos por el CCO.
- RF7: consulta de velocidad promedio estimada por ruta y mes del ano piloto.
- Comunicacion distribuida entre nodos Java mediante ZeroC Ice.
- Frontend Node.js con JavaScript y Google Maps API.

## Nodos

- `bus-simulator`: representa un nodo BUS SIT-MIO. Lee datagramas CSV de una ruta, fuerza un `busId` configurable y los envia recurrentemente por Ice al CCO para evidenciar movimiento.
- `cco-server`: representa el Servidor CCO. Recibe datagramas, actualiza posiciones, expone rutas y calcula metricas.
- `datacenter`: representa el Centro de Datos. Expone repositorio operativo e historico por Ice y persiste datagramas en Postgres. Por defecto usa `jdbc:postgresql://localhost:5432/mio` y endpoints preparados para despliegue LAN con el hostname `mio-datacenter`.
- `web-gateway`: puente HTTP/JSON para el navegador. Internamente consume servicios Ice del CCO.
- `frontend`: servidor Node.js estatico para la UI. Al abrir, no pinta todas las rutas; primero detecta rutas activas y el usuario elige cual ver.

## Contratos Ice

Los contratos estan en [slice/Mio.ice](D:/IngeSoft4/mio-system/slice/Mio.ice):

- `DatagramReceiver`: datagramas entrantes desde bus.
- `PositionService`: posiciones actuales para RF4.
- `RouteService`: rutas disponibles y `routeDetails(lineId)` con ruta, estaciones/paradas y geometria base.
- `MetricsService`: velocidad promedio por ruta y mes para RF7.
- `HistoricalRepository` y `OperationalRepository`: acceso al Centro de Datos.
- `HistoricalRepository.storeDatagram`: persistencia de datagramas recibidos durante la operacion.
- Los stubs ICE generados desde `slice/Mio.ice` estan versionados en `common/src/main/java/Mio`, por lo que el build no depende de `slice2java` local. Para regenerarlos opcionalmente, ejecutar `gradle :common:generateSlice` con `ICE_HOME` configurado.

## Patrones usados

- Asynchronous Queuing: `DatagramQueueProcessor` desacopla temporalmente el envio de datagramas entre bus y CCO. Mejora Availability y apoya Performance para RF4.
- Reliable Messaging: `DatagramReceiver.submit(...)` retorna ACK booleano y el simulador reintenta si no recibe confirmacion. Mejora Availability ante fallas parciales.
- Thread Pool: `DatagramQueueProcessor` procesa datagramas con un pool configurable por `MIO_CCO_WORKERS`. Mejora Performance y evita crear hilos sin control.
- Master-Worker: `MetricsMaster` coordina el calculo de RF7 en workers. Mejora Performance para velocidad promedio por ruta y mes.
- MVC: el frontend separa `MapModel/MapView/MapController` y `AnalyticsModel/AnalyticsView/AnalyticsController`.
- Observer: `ObservableModel` permite que las vistas se subscriban a cambios de modelos.
- Proxy: `web-gateway` y `cco-server` consumen servicios remotos mediante proxies Ice, desacoplando UI, CCO y DataCenter.
- Repository: `PersistentHistoricalRepository` y `CsvOperationalRepository` encapsulan almacenamiento historico y operativo.

Ver [ARCHITECTURE_MAPPING.md](D:/IngeSoft4/mio-system/ARCHITECTURE_MAPPING.md) para la tabla completa deployment -> codigo -> patron -> RF/QAW.

## Ejecutar

Compilar:

```powershell
gradle --offline build
```

Terminal 1:

```powershell
gradle --offline :datacenter:run
```

Terminal 2:

```powershell
gradle --offline :cco-server:run
```

Terminal 3:

```powershell
gradle --offline :web-gateway:run
```

Terminal 4:

```powershell
$env:MIO_BUS_DELAY_MS='250'
$env:MIO_SIM_LINE_ID='311'
$env:MIO_SIM_BUS_ID='9001'
$env:MIO_SOURCE_BUS_ID='846'
gradle --offline :bus-simulator:run
```

Por defecto el simulador representa un solo bus de una sola ruta, queda en ciclo continuo (`MIO_BUS_LOOP=true`) y reenvia los puntos GPS de esa ruta con timestamp incremental para que el marcador se mueva y se puedan calcular velocidades.

El mapa muestra solo la ruta seleccionada por el usuario. El selector se llena con rutas activas, detectadas a partir de los buses que estan enviando datagramas al CCO. Cada ruta conserva rastros separados por bus para no intercalar posiciones cuando dos buses estan en la misma linea; cuando se selecciona otra ruta, el rastro anterior se oculta y se muestra el de la nueva seleccion. El sistema tambien renderiza detalle de ruta (`routeDetails`), paradas fijas por `stopId` y un trazado base. La posicion de cada parada se estima usando el `odometer` del datagrama, que segun el diccionario de datos representa los metros recorridos desde la ultima parada hasta la ubicacion actual del bus. No corrige ni acomoda distancias, solo interpola visualmente el marcador entre una coordenada real y la siguiente para que el movimiento se vea fluido.

Para correr varios buses/rutas, abre una terminal por nodo simulado:

```powershell
$env:MIO_SIM_LINE_ID='2273'
$env:MIO_SIM_BUS_ID='9001'
$env:MIO_SOURCE_BUS_ID='1075'
$env:MIO_BUS_DATAGRAMS_FILE='chunck.csv'
gradle --offline :bus-simulator:run
```

```powershell
$env:MIO_SIM_LINE_ID='140'
$env:MIO_SIM_BUS_ID='9002'
$env:MIO_SOURCE_BUS_ID='705'
gradle --offline :bus-simulator:run
```

```powershell
$env:MIO_SIM_LINE_ID='2471'
$env:MIO_SIM_BUS_ID='9003'
$env:MIO_SOURCE_BUS_ID='255'
gradle --offline :bus-simulator:run
```

Para probar dos buses diferentes al mismo tiempo en la misma ruta, abre dos terminales y usa la misma `MIO_SIM_LINE_ID` con distinto `MIO_SIM_BUS_ID` y distinto `MIO_SOURCE_BUS_ID`. Ejemplo con la ruta `306` (`A06`):

Terminal A:

```powershell
$env:MIO_BUS_DELAY_MS='250'
$env:MIO_SIM_LINE_ID='306'
$env:MIO_SIM_BUS_ID='9101'
$env:MIO_SOURCE_BUS_ID='1203'
gradle --offline :bus-simulator:run
```

Terminal B:

```powershell
$env:MIO_BUS_DELAY_MS='250'
$env:MIO_SIM_LINE_ID='306'
$env:MIO_SIM_BUS_ID='9102'
$env:MIO_SOURCE_BUS_ID='190'
gradle --offline :bus-simulator:run
```

Para una prueba finita:

```powershell
$env:MIO_BUS_LOOP='false'
$env:MIO_BUS_LIMIT='80'
$env:MIO_SIM_LINE_ID='2273'
$env:MIO_SIM_BUS_ID='9001'
$env:MIO_SOURCE_BUS_ID='1075'
gradle --offline :bus-simulator:run
```

Terminal 5:

```powershell
$env:GOOGLE_MAPS_API_KEY='TU_API_KEY'
npm.cmd --prefix frontend start
```

Luego abrir `http://127.0.0.1:3000`.

## Variables utiles

- `MIO_DATAGRAMS_FILE`: CSV historico para DataCenter. Default de despliegue: `/mnt/mio-datos/datagrams-MiniPilot.csv`.
- `MIO_BUS_DATAGRAMS_FILE`: CSV usado por el simulador. Default de despliegue: `/mnt/mio-datos/datagrams-MiniPilot.csv`.
  El resolvedor busca tambien carpetas hermanas como `D:\IngeSoft4\datagrams-MiniPilot\datagrams-MiniPilot.csv`. Si no lo encuentra, usa `chunck.csv` como fallback para demo.
- `MIO_ROUTES_FILE`: CSV de rutas. Default de despliegue: `/opt/mio/lines-241-ActiveGT.csv`.
- `MIO_GATEWAY_PORT`: puerto HTTP del gateway. Default: `8080`.
- `FRONTEND_PORT`: puerto del frontend Node. Default: `3000`.
- `GOOGLE_MAPS_API_KEY`: llave de Google Maps para renderizar el mapa.
- `MIO_DB_URL`: JDBC URL de Postgres. Default: `jdbc:postgresql://localhost:5432/mio`.
- `MIO_DB_USER`: usuario de Postgres. Default: `postgres`.
- `MIO_DB_PASSWORD`: password de Postgres. Default: `postgres`.
- `MIO_CCO_WORKERS`: numero de workers para procesar datagramas. Default: `4`.
- `MIO_BUS_LOOP`: relectura continua del archivo de datagramas. Default: `true`.
- `MIO_SIM_LINE_ID`: ruta que representa esta instancia del simulador. Default: `311`.
- `MIO_SIM_BUS_ID`: bus que representa esta instancia del simulador. Default: `9001`.
- `MIO_SOURCE_BUS_ID`: bus real del CSV usado como fuente de puntos GPS. Default: `846`.
- `MIO_BUS_DELAY_MS`: pausa entre datagramas enviados. Default: `250`.
- `MIO_BUS_LIMIT`: cantidad maxima de datagramas por vuelta del archivo. Default: sin limite.

Para usar el historico grande del piloto en Ubuntu, montar el archivo en `/mnt/mio-datos/datagrams-MiniPilot.csv`.

Ejemplo Postgres:

```powershell
$env:MIO_DB_URL='jdbc:postgresql://localhost:5432/mio'
$env:MIO_DB_USER='postgres'
$env:MIO_DB_PASSWORD='postgres'
gradle --offline :datacenter:run
```

## Despliegue en maquinas separadas

La guia de despliegue Ubuntu por JAR, classpath de Ice, hostnames LAN, puertos y variables minimas por bus esta en [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md).
