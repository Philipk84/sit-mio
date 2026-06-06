## Respuesta explícita a la pregunta del enunciado:
## ¿A partir de qué punto vale la pena distribuir la solución?

Con la evidencia experimental obtenida en este proyecto, **todavía no se alcanza el punto en el que vale la pena distribuir la solución**.

### Fundamentación
Los resultados obtenidos fueron:

- **v1 monolítica:** 52 resultados, 44 ms
- **v2 concurrente:** 52 resultados, 50 ms
- **v3 distribuida:** 52 consultas exitosas, 2516 ms

Esto indica que:

1. v1 y v2 son funcionalmente consistentes,
2. la concurrencia local aún no mejora a la solución monolítica para el tamaño actual del dataset,
3. la versión distribuida introduce un overhead mucho mayor asociado a red, serialización y coordinación entre nodos.

### Conclusión
Para el volumen de datos probado en este experimento, **no vale la pena distribuir la solución**, porque el costo de distribución supera el beneficio computacional esperado.

### Implicación
El punto a partir del cual sí valdría la pena distribuir no se alcanzó con el dataset actual. Para estimarlo con mayor precisión sería necesario repetir los experimentos con:

- datasets más grandes,
- mayor número de rutas activas,
- más buses por ruta,
- mayor volumen histórico,
- y múltiples repeticiones por versión para promediar tiempos.