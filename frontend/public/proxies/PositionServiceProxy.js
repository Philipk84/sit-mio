import { BaseServiceProxy } from './BaseServiceProxy.js';

export class PositionServiceProxy extends BaseServiceProxy {
  latestPositions(lineId = 0) {
    return this.getJson(`/api/positions?lineId=${lineId}`);
  }
}
