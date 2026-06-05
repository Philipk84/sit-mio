import { ObservableModel } from './ObservableModel.js';

export class AnalyticsModel extends ObservableModel {
  constructor(metricsProxy) {
    super({
      speedKmh: null,
      samples: null,
      status: 'Selecciona una ruta.',
      unavailable: false
    });
    this.metricsProxy = metricsProxy;
  }

  async calculate(lineId, month) {
    if (!lineId || Number(lineId) <= 0) {
      this.state = { speedKmh: null, samples: null, status: 'Selecciona una ruta.', unavailable: false };
      this.notify();
      return;
    }
    this.state.status = 'Calculando...';
    this.state.unavailable = false;
    this.notify();

    const result = await this.metricsProxy.averageSpeed(lineId, month);
    if (!result.ok) {
      this.state = {
        speedKmh: null,
        samples: null,
        status: 'Consulta historica no disponible.',
        unavailable: true
      };
      this.notify();
      return;
    }
    this.state = {
      speedKmh: result.data.speedKmh,
      samples: result.data.samples,
      status: result.data.samples ? 'Resultado historico disponible.' : 'Sin muestras para la consulta.',
      unavailable: false
    };
    this.notify();
  }
}
