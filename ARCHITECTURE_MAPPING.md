# Architecture Mapping

| Componente del deployment | Implementacion | Patron asociado | RF / QAW cubierto |
| --- | --- | --- | --- |
| Gestor de datagramas del BUS SIT-MIO | `bus-simulator/src/main/java/edu/icesi/mio/bus/BusSimulatorApplication.java` | Reliable Messaging client | RF4, flujo GPS hacia CCO |
| Receptor datagramas | `cco-server/src/main/java/edu/icesi/mio/cco/ReceptorDatagramas.java`, `DatagramReceiverServant.java` | Reliable Messaging, Proxy remoto ICE | RF4, Availability |
| DatagramQueue | `cco-server/src/main/java/edu/icesi/mio/cco/DatagramQueue.java` | Asynchronous Queuing | RF4, Availability, Performance |
| ReliableMessagingAgent | `cco-server/src/main/java/edu/icesi/mio/cco/ReliableMessagingAgent.java` | Reliable Messaging | RF4, tolerancia a fallas parciales |
| ThreadPool | `cco-server/src/main/java/edu/icesi/mio/cco/ThreadPoolDispatcher.java` | Thread Pool | RF4, Performance |
| Gestor Posiciones | `cco-server/src/main/java/edu/icesi/mio/cco/PositionRegistry.java` | Repository en memoria operacional | RF4, posiciones tiempo real |
| ResolverRutaBus | `cco-server/src/main/java/edu/icesi/mio/cco/RouteResolver.java` | Resolver / coordinador de flujo | RF4, RF8 refinado, trazabilidad bus-ruta |
| Gestor Rutas | `datacenter/src/main/java/edu/icesi/mio/datacenter/CsvOperationalRepository.java`, `RouteServiceServant.java` | Repository, Proxy ICE | RF4, ServicioRutasParadas |
| Servicio Rutas/Paradas | `slice/Mio.ice` (`RouteMapData`, `Station`, `RoutePoint`), `/api/route-details` | Contrato distribuido ICE | RF4 completo |
| Estimacion de paradas fijas | `PositionRegistry.registerFixedStop(...)` usando `stopId` y `odometer` | Gestor Posiciones / Route details | RF4, paradas y estaciones |
| Repositorio historico | `datacenter/src/main/java/edu/icesi/mio/datacenter/PersistentHistoricalRepository.java` | Repository | RF7, datos historicos |
| MetricsMaster | `cco-server/src/main/java/edu/icesi/mio/cco/MetricsMaster.java` | Master-Worker | RF7, Performance |
| MetricsWorker | `cco-server/src/main/java/edu/icesi/mio/cco/MetricsWorker.java` | Master-Worker | RF7, calculo parcial por bus |
| MetricTask | `cco-server/src/main/java/edu/icesi/mio/cco/MetricTask.java` | Master-Worker | RF7, particion por bus |
| MetricPartialResult | `cco-server/src/main/java/edu/icesi/mio/cco/MetricPartialResult.java` | Master-Worker | RF7, consolidacion de resultados |
| RouteServiceProxy frontend | `frontend/public/proxies/RouteServiceProxy.js` | Proxy | RF4, Availability cliente |
| PositionServiceProxy frontend | `frontend/public/proxies/PositionServiceProxy.js` | Proxy | RF4, posiciones tiempo real |
| MetricsServiceProxy frontend | `frontend/public/proxies/MetricsServiceProxy.js` | Proxy | RF7, indisponibilidad controlada |
| MapModel | `frontend/public/models/MapModel.js` | MVC, Observer | RF4, stale state QAW |
| MapView | `frontend/public/views/MapView.js` | MVC, Observer | RF4, mapa, buses, estaciones, ruta base, trail |
| MapController | `frontend/public/controllers/MapController.js` | MVC | RF4, seleccion de ruta |
| AnalyticsModel | `frontend/public/models/AnalyticsModel.js` | MVC, Observer | RF7, estado analitico |
| AnalyticsView | `frontend/public/views/AnalyticsView.js` | MVC, Observer | RF7, resultado o indisponibilidad |
| AnalyticsController | `frontend/public/controllers/AnalyticsController.js` | MVC | RF7, cambio de mes y consulta |

## Notas de cumplimiento

- La generacion de stubs ICE ya no es obligatoria para compilar: los stubs de `slice/Mio.ice` viven en `common/src/main/java/Mio`.
- El task `:common:generateSlice` queda como herramienta opcional y usa `ICE_HOME` si se necesita regenerar.
- `RouteService.routeDetails(lineId)` entrega `RouteMapData` con ruta, paradas/estaciones y geometria base.
- El frontend muestra estado stale cuando pasan mas de 10 segundos sin cambios en posiciones.
