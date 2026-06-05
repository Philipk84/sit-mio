import { ObservableModel } from './ObservableModel.js';

export class MapModel extends ObservableModel {
  constructor(routeProxy, positionProxy) {
    super({
      routes: [],
      routesByLineId: new Map(),
      activeLineIds: new Set(),
      selectedLineId: 0,
      positions: [],
      routeDetails: null,
      routeTrails: new Map(),
      stale: false,
      lastChangedAt: 0,
      status: 'Conectando con CCO...'
    });
    this.routeProxy = routeProxy;
    this.positionProxy = positionProxy;
    this.lastSignature = '';
  }

  async initialize() {
    const routes = await this.routeProxy.listRoutes();
    if (routes.ok) {
      this.state.routes = routes.data;
      this.state.routesByLineId = new Map(routes.data.map(route => [Number(route.lineId), route]));
      this.state.status = 'Esperando rutas activas del CCO.';
      this.notify();
    }
    await this.poll();
    setInterval(() => this.poll(), 1000);
    setInterval(() => this.evaluateStale(), 1000);
  }

  async selectRoute(lineId) {
    this.state.selectedLineId = Number(lineId || 0);
    this.state.positions = [];
    this.state.routeDetails = null;
    this.state.stale = false;
    this.notify();
    if (this.state.selectedLineId > 0) {
      await this.refreshSelectedRoute();
    }
  }

  async poll() {
    const result = await this.positionProxy.latestPositions(0);
    if (!result.ok) {
      this.state.status = 'CCO no disponible para posiciones.';
      this.notify();
      return;
    }
    const positions = result.data;
    this.state.activeLineIds = new Set(positions.map(position => Number(position.lineId)));
    this.appendTrails(positions);

    const signature = positions.map(position =>
      `${position.busId}:${position.lineId}:${position.latitude}:${position.longitude}:${position.timestamp}`).join('|');
    if (signature !== this.lastSignature) {
      this.lastSignature = signature;
      this.state.lastChangedAt = Date.now();
      this.state.stale = false;
    }

    if (this.state.selectedLineId > 0) {
      await this.refreshSelectedRoute();
    } else {
      this.state.positions = [];
      this.state.status = this.state.activeLineIds.size
        ? `${this.state.activeLineIds.size} rutas activas. Selecciona una para verla.`
        : 'Esperando datagramas del CCO.';
      this.notify();
    }
  }

  async refreshSelectedRoute() {
    const positions = await this.positionProxy.latestPositions(this.state.selectedLineId);
    const details = await this.routeProxy.routeDetails(this.state.selectedLineId);
    if (positions.ok) {
      this.state.positions = positions.data;
      this.appendTrails(positions.data);
      this.state.status = positions.data.length
        ? `Ruta ${this.routeLabel(this.state.selectedLineId)} actualizada.`
        : `Ruta ${this.routeLabel(this.state.selectedLineId)} sin posiciones recientes.`;
    }
    if (details.ok) {
      this.state.routeDetails = details.data;
    }
    this.notify();
  }

  routeLabel(lineId) {
    const route = this.state.routesByLineId.get(Number(lineId));
    return route ? route.shortName : String(lineId);
  }

  appendTrails(positions) {
    for (const position of positions) {
      const lineId = Number(position.lineId);
      const trail = this.state.routeTrails.get(lineId) || [];
      const latLng = { lat: position.latitude, lng: position.longitude };
      const last = trail[trail.length - 1];
      if (!last || last.lat !== latLng.lat || last.lng !== latLng.lng) {
        trail.push(latLng);
        if (trail.length > 900) {
          trail.shift();
        }
        this.state.routeTrails.set(lineId, trail);
      }
    }
  }

  evaluateStale() {
    if (this.state.lastChangedAt > 0 && Date.now() - this.state.lastChangedAt > 10000) {
      if (!this.state.stale) {
        this.state.stale = true;
        this.notify();
      }
    }
  }
}
