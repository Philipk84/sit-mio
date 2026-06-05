import { BaseServiceProxy } from './BaseServiceProxy.js';

export class RouteServiceProxy extends BaseServiceProxy {
  listRoutes() {
    return this.getJson('/api/routes');
  }

  routeDetails(lineId) {
    return this.getJson(`/api/route-details?lineId=${lineId}`);
  }
}
