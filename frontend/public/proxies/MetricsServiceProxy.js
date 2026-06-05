import { BaseServiceProxy } from './BaseServiceProxy.js';

export class MetricsServiceProxy extends BaseServiceProxy {
  averageSpeed(lineId, month) {
    return this.getJson(`/api/metrics?lineId=${lineId}&month=${month}`, 5000);
  }
}
