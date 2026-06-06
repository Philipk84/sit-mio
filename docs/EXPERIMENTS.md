# Módulo de experimentos

## Objetivo

Este módulo fue creado para cumplir con la parte del proyecto que exige:

1. una versión 1 monolítica,
2. una versión 2 concurrente,
3. una comparación experimental entre ambas,
4. y una base para justificar a partir de qué punto vale la pena distribuir la solución.

La arquitectura distribuida actual del proyecto se considera como la **versión 3**.

---

## Relación con el enunciado del proyecto

El enunciado del proyecto exige explícitamente:

- implementar inicialmente una solución monolítica,
- implementar una versión 2 concurrente,
- determinar el punto a partir del cual vale la pena distribuir la solución,
- e implementar una versión 3 distribuida usando un patrón de diseño de distribución. :contentReference[oaicite:0]{index=0}

Este módulo `experiments` fue creado para cubrir de forma aislada y controlada las versiones **v1** y **v2**, mientras que la arquitectura principal del sistema corresponde a **v3 distribuida**.

---

## Qué problema resuelven estas versiones

El problema que resuelven estas versiones es:

**calcular la velocidad promedio por ruta por mes**

a partir de un conjunto de datagramas del piloto y un listado de rutas activas.

---

## Archivos de entrada

El módulo usa dos tipos de archivo:

- archivo de rutas activas,
- archivo de datagramas.

En esta implementación local se usaron:

- `lines-241-ActiveGT.csv`
- `chunck.csv`

El archivo de datagramas puede parametrizarse por variable de entorno.

---

## Estructura interna del cálculo

El procesamiento sigue esta idea general:

1. cargar las rutas activas,
2. leer los datagramas del archivo,
3. filtrar solo los datagramas de rutas activas,
4. convertir cada datagrama en una posición,
5. agrupar la información por:
   - ruta,
   - mes,
   - y dentro de cada grupo, por bus,
6. ordenar las posiciones de cada bus por tiempo,
7. calcular velocidades entre pares consecutivos,
8. filtrar velocidades inválidas,
9. acumular resultados,
10. calcular el promedio final por ruta y mes.

---

## Qué representa un resultado

Cada resultado corresponde a una combinación de:

- una ruta
- un mes

Es decir, si la ejecución reporta:

- `Resultados: 52`

eso significa que se calcularon **52 grupos distintos de (ruta, mes)** con datos válidos.

No significa 52 buses ni 52 datagramas.

---

## Clase `RouteMonthKey`

Representa la llave de agrupación principal del problema:

- `lineId`
- `month`

Se usa como clave para almacenar el resultado final de velocidad promedio por ruta y por mes.

---

## Clase `SpeedStats`

La clase `SpeedStats` acumula los resultados parciales del cálculo.

Sus responsabilidades son:

- sumar velocidades válidas,
- contar cuántas muestras válidas se usaron,
- fusionar resultados parciales,
- calcular el promedio final.

### Métodos principales
- `add(double speed)`: agrega una velocidad válida al acumulado.
- `merge(SpeedStats other)`: fusiona otro acumulador parcial.
- `totalSpeed()`: retorna la suma total acumulada.
- `samples()`: retorna la cantidad de muestras válidas.
- `average()`: retorna el promedio o `0.0` si no hay muestras.

### Verificación conceptual
La clase se considera correcta si cumple estas reglas:
- si no hay muestras, el promedio es `0.0`;
- si se agregan dos velocidades, el promedio corresponde a la media aritmética;
- si se fusionan dos objetos `SpeedStats`, las sumas y muestras se acumulan correctamente.

---

## Clase `PilotDatasetLoader`

Esta clase prepara la entrada para las versiones monolítica y concurrente.

### Responsabilidades
1. cargar las rutas activas,
2. leer y parsear el archivo de datagramas,
3. filtrar solo rutas activas,
4. convertir datagramas a posiciones,
5. agrupar por ruta, mes y bus.

### Por qué se agrupa por bus
La velocidad se calcula entre posiciones consecutivas del **mismo bus**.  
Si se mezclaran posiciones de buses distintos, el cálculo sería incorrecto.

---

## Versión 1: solución monolítica

La versión monolítica resuelve todo el problema en un solo hilo y de forma secuencial.

### Flujo interno
Para cada combinación `(ruta, mes)`:

1. toma todos los buses de ese grupo,
2. ordena las posiciones de cada bus por tiempo,
3. recorre pares consecutivos de posiciones,
4. calcula la velocidad entre ellas,
5. filtra velocidades no válidas,
6. acumula el resultado en `SpeedStats`.

### Características
- un solo proceso,
- un solo hilo,
- sin paralelismo,
- sin distribución.

Esta versión sirve como línea base para comparar rendimiento.

---

## Versión 2: solución concurrente

La versión concurrente usa la misma lógica matemática que la versión monolítica, pero distribuye el cálculo por bus en varias tareas ejecutadas por un pool de hilos.

### Flujo interno
Para cada combinación `(ruta, mes)`:

1. crea una tarea por bus,
2. cada tarea ordena sus posiciones y calcula velocidades parciales,
3. las tareas se envían a un `ExecutorService`,
4. al finalizar, los resultados parciales se consolidan en un solo `SpeedStats`.

### Características
- un solo proceso,
- múltiples hilos,
- paralelismo local,
- sin comunicación remota.

---

## Diferencia entre v1 y v2

La diferencia entre ambas versiones **no está en la fórmula de velocidad**, sino en la forma de ejecutar el trabajo.

### Versión monolítica
Hace todo en secuencia.

### Versión concurrente
Divide el cálculo por bus y lo ejecuta en paralelo.

Por eso ambas versiones deben producir el mismo resultado funcional, aunque el tiempo de ejecución pueda variar.

---

## Comandos de ejecución de v1 y v2

### Compilar
```powershell
gradle --offline :experiments:build

```
### Ejecutar versión monolítica

```powershell
$env:MIO_EXPERIMENT_DATAGRAMS_FILE='chunck.csv'
$env:MIO_EXPERIMENT_ROUTES_FILE='lines-241-ActiveGT.csv'
gradle --offline :experiments:runMonolithic

```

### Ejecutar versión concurrente

```powershell
$env:MIO_EXPERIMENT_DATAGRAMS_FILE='chunck.csv'
$env:MIO_EXPERIMENT_ROUTES_FILE='lines-241-ActiveGT.csv'
$env:MIO_EXPERIMENT_WORKERS='4'
gradle --offline :experiments:runConcurrent

```

### Ejecutar benchmark comparativo

```powershell
$env:MIO_EXPERIMENT_DATAGRAMS_FILE='chunck.csv'
$env:MIO_EXPERIMENT_ROUTES_FILE='lines-241-ActiveGT.csv'
$env:MIO_EXPERIMENT_WORKERS='4'
gradle --offline :experiments:runCompare

```

 ## Criterios de validación de v1 y v2

Se considera que v1 y v2 están funcionando correctamente si:

* ambas terminan sin excepciones,
* ambas producen la misma cantidad de resultados,
* ambas generan archivos de salida,
* los promedios calculados son consistentes entre sí,
* se pueden comparar sus tiempos de ejecución.
  
## Resultados obtenidos para v1 y v2

Versión 1: monolítica
* Resultados: 52
* Tiempo total: 44 ms
  
Versión 2: concurrente
* Resultados: 52
* Tiempo total: 50 ms
## Interpretación de los resultados de v1 y v2

El hecho de que ambas versiones produzcan 52 resultados indica que son consistentes desde el punto de vista funcional.

Esto significa que ambas resolvieron el mismo conjunto de combinaciones de:

* ruta
* mes

Sin embargo, la versión concurrente tardó más tiempo que la monolítica:

* monolítica: 44 ms
* concurrente: 50 ms

Esto sugiere que, para el tamaño actual del dataset, el costo adicional de la concurrencia todavía supera la ganancia potencial de paralelizar el cálculo.

Posibles causas del mayor tiempo en v2
* creación y administración de tareas,
* uso de múltiples hilos,
* sincronización,
* consolidación de resultados parciales.

Este comportamiento es normal en datasets pequeños.

## Validación de la versión 3 distribuida

La versión 3 corresponde a la solución distribuida del proyecto, alineada con el deployment propuesto y con los drivers arquitectónicos seleccionados en el QAW.

Objetivo

Validar que la arquitectura distribuida implementada resuelve el flujo completo del sistema, desde la recepción de datagramas hasta la visualización operativa y analítica en la Plataforma MIO.

Componentes involucrados

La validación de la versión distribuida incluye la ejecución coordinada de los siguientes nodos:

* Bus Simulator
* CCO
* DataCenter
* Web Gateway
* Frontend / Plataforma MIO
* Flujo validado
1. El simulador de buses envía datagramas al CCO.
2. El CCO recibe, procesa y actualiza posiciones operativas.
3. El DataCenter almacena información operativa e histórica.
4. El Web Gateway expone los servicios consumidos por la Plataforma MIO.
5. El frontend presenta:
   * visualización geográfica en tiempo real,
   * consulta analítica de velocidad promedio por ruta y mes.
  
Evidencia funcional

La validación de la versión distribuida se realizó observando:

* rutas activas detectadas en la interfaz,
* buses visibles en el mapa,
* actualización de marcadores en tiempo real,
* cálculo y visualización de velocidad promedio por ruta,
* cantidad de muestras disponibles para el cálculo histórico.


Relación con RF4

La solución distribuida satisface la visualización del sistema en tiempo real, permitiendo observar el movimiento de buses, rutas y elementos del mapa en la Plataforma MIO.

Relación con RF7

La solución distribuida satisface la consulta histórica del piloto, permitiendo al usuario seleccionar una ruta y un mes y obtener la estimación de velocidad promedio correspondiente.

Relación con v1 y v2

Las versiones 1 y 2 se utilizaron como línea base para la comparación experimental.
La versión 3 representa la solución distribuida final del sistema, consistente con el deployment arquitectónico propuesto.

Ajustes realizados para probar v3 de forma local

La solución distribuida original asume un entorno de despliegue con nombres de host y rutas propios de una red distribuida, por ejemplo:

* mio-datacenter
* mio-cco

y ubicaciones de archivos tipo Linux.

Para poder ejecutar la versión 3 localmente en una sola máquina Windows sin alterar la arquitectura, se realizaron ajustes de configuración por variables de entorno:

1. se reemplazaron los hosts distribuidos por 127.0.0.1,
2. se configuraron endpoints ICE locales,
3. se reemplazaron rutas Linux por archivos locales:
   * chunck.csv
   * lines-241-ActiveGT.csv,
4. se mantuvo la separación lógica entre:
   * DataCenter,
   * CCO,
   * Web Gateway,
   * Bus Simulator,
   * Frontend.

Esto permitió validar la versión distribuida en entorno local conservando el flujo distribuido del sistema.

Nota sobre el frontend

El frontend puede levantarse escuchando en 0.0.0.0:3000, pero en el navegador local la URL correcta para acceder es:

http://127.0.0.1:3000

No debe abrirse http://0.0.0.0:3000, ya que esa dirección no es una URL válida de navegación.

### Comandos utilizados para validar v3 localmente

#### Terminal 1 — DataCenter

```powershell
$env:MIO_DB_URL='jdbc:postgresql://localhost:5432/mio'
$env:MIO_DB_USER='postgres'
$env:MIO_DB_PASSWORD='TU_CONTRASEÑA_REAL'
$env:MIO_DATACENTER_ENDPOINTS='tcp -h 127.0.0.1 -p 10001'
$env:MIO_DATAGRAMS_FILE='chunck.csv'
$env:MIO_ROUTES_FILE='lines-241-ActiveGT.csv'
gradle --offline :datacenter:run

```

#### Terminal 2 — CCO

```powershell
$env:MIO_CCO_ENDPOINTS='tcp -h 127.0.0.1 -p 10010'
$env:MIO_HISTORICAL_PROXY='HistoricalRepository:tcp -h 127.0.0.1 -p 10001'
$env:MIO_OPERATIONAL_PROXY='OperationalRepository:tcp -h 127.0.0.1 -p 10001'
gradle --offline :cco-server:run

```

#### Terminal 3 — Web Gateway

```powershell
$env:MIO_CCO_PROXY_ENDPOINT='tcp -h 127.0.0.1 -p 10010'
$env:MIO_GATEWAY_PORT='8080'
gradle --offline :web-gateway:run

```
#### Terminal 4 — Bus Simulator

```powershell
$env:MIO_BUS_DATAGRAMS_FILE='chunck.csv'
$env:MIO_DATAGRAM_RECEIVER_PROXY='DatagramReceiver:tcp -h 127.0.0.1 -p 10010'
$env:MIO_BUS_DELAY_MS='250'
$env:MIO_SIM_LINE_ID='2274'
$env:MIO_SIM_BUS_ID='9001'
$env:MIO_SOURCE_BUS_ID='159'
gradle --offline :bus-simulator:run

```

#### Terminal 5 — Frontend

```powershell
$env:GOOGLE_MAPS_API_KEY='TU_API_KEY'
npm.cmd --prefix frontend start

```

Acceso en navegador: 

```powershell
http://127.0.0.1:3000

```
#### Terminal 6 — Benchmark distribuido

```powershell
$env:MIO_EXPERIMENT_DATAGRAMS_FILE='chunck.csv'
$env:MIO_EXPERIMENT_ROUTES_FILE='lines-241-ActiveGT.csv'
$env:MIO_DISTRIBUTED_BASE_URL='http://127.0.0.1:8080'
gradle --offline :experiments:runDistributed

```

### Resultados obtenidos para v3 distribuida

La ejecución del benchmark distribuido reportó:

* Consultas esperadas: 52
* Consultas exitosas: 52
* Tiempo total: 2516 ms
* Promedio por consulta: 48.38 ms

Adicionalmente, se generó el archivo:

experiments-output/distributed-results.csv

### Interpretación de los resultados de v3

La versión distribuida fue capaz de responder correctamente las 52 combinaciones esperadas de (ruta, mes), lo que indica consistencia funcional con las versiones 1 y 2.

Sin embargo, el tiempo total de v3 es considerablemente mayor que el de v1 y v2. Esto es esperable porque la versión distribuida no solo mide tiempo de cómputo puro, sino también el overhead arquitectónico asociado a:

* comunicación entre nodos,
* serialización y deserialización,
* acceso remoto a servicios,
* coordinación entre procesos,
* latencia del gateway.

Por esta razón, el tiempo de v3 debe interpretarse como latencia distribuida de consulta y no únicamente como tiempo local de cálculo.

| Versión | Tipo        | Resultados / Consultas | Tiempo total |
| ------- | ----------- | ---------------------- | ------------ |
| v1      | Monolítica  | 52 resultados          | 44 ms        |
| v2      | Concurrente | 52 resultados          | 50 ms        |
| v3      | Distribuida | 52 consultas exitosas  | 2516 ms      |

Lectura:

* v1 es la línea base de cómputo secuencial.
* v2 mantiene el mismo resultado funcional, pero introduce overhead de concurrencia.
* v3 mantiene el mismo volumen lógico de resultados, pero incorpora el costo real de una arquitectura distribuida.