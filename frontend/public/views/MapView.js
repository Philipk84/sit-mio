export class MapView {
  constructor() {
    this.routeSelect = document.querySelector('#routeSelect');
    this.statusEl = document.querySelector('#status');
    this.busValue = document.querySelector('#busValue');
    this.banner = document.querySelector('#mapStatusBanner');
    this.markers = new Map();
    this.stationMarkers = new Map();
    this.trailPolylines = new Map();
    this.basePolyline = null;
    this.map = null;
    this.centeredLineId = 0;
  }

  loadGoogleMaps(apiKey, onReady) {
    if (!apiKey) {
      this.statusEl.textContent = 'Configura GOOGLE_MAPS_API_KEY para cargar el mapa.';
      document.querySelector('#map').textContent = 'Mapa no disponible';
      return;
    }
    window.initMioMap = () => {
      this.map = new google.maps.Map(document.querySelector('#map'), {
        center: { lat: 3.4516, lng: -76.5320 },
        zoom: 12,
        mapId: 'mio-map'
      });
      onReady();
    };
    const script = document.createElement('script');
    script.src = `https://maps.googleapis.com/maps/api/js?key=${apiKey}&callback=initMioMap`;
    script.async = true;
    document.head.appendChild(script);
  }

  onRouteChange(handler) {
    this.routeSelect.addEventListener('change', () => handler(Number(this.routeSelect.value || 0)));
  }

  render(state) {
    this.renderRouteOptions(state);
    this.statusEl.textContent = state.status;
    this.busValue.textContent = String(state.positions.length);
    this.renderStale(state);
    if (!this.map) {
      return;
    }
    this.renderTrail(state);
    this.renderRouteDetails(state);
    this.renderBuses(state);
  }

  renderRouteOptions(state) {
    const previous = String(this.routeSelect.value || '');
    const activeRoutes = Array.from(state.activeLineIds)
      .sort((a, b) => this.routeOptionLabel(state, a).localeCompare(this.routeOptionLabel(state, b), 'es'));
    this.routeSelect.innerHTML = '<option value="">Selecciona una ruta activa</option>' + activeRoutes
      .map(lineId => `<option value="${lineId}">${this.routeOptionLabel(state, lineId)}</option>`)
      .join('');
    if (previous && state.activeLineIds.has(Number(previous))) {
      this.routeSelect.value = previous;
    }
  }

  renderStale(state) {
    if (state.stale) {
      this.banner.textContent = 'La visualizacion puede estar desactualizada: no llegan posiciones recientes.';
      this.banner.classList.remove('hidden');
    } else {
      this.banner.classList.add('hidden');
    }
  }

  renderTrail(state) {
    for (const polyline of this.trailPolylines.values()) {
      polyline.setVisible(false);
    }
    if (state.selectedLineId <= 0) {
      return;
    }
    const trail = state.routeTrails.get(state.selectedLineId) || [];
    if (!this.trailPolylines.has(state.selectedLineId)) {
      this.trailPolylines.set(state.selectedLineId, new google.maps.Polyline({
        map: this.map,
        geodesic: true,
        strokeColor: '#166534',
        strokeOpacity: 0.88,
        strokeWeight: 4
      }));
    }
    const polyline = this.trailPolylines.get(state.selectedLineId);
    polyline.setPath(trail);
    polyline.setVisible(true);
  }

  renderRouteDetails(state) {
    if (this.basePolyline) {
      this.basePolyline.setMap(null);
      this.basePolyline = null;
    }
    if (!state.routeDetails || !this.map) {
      return;
    }
    this.basePolyline = new google.maps.Polyline({
      map: this.map,
      path: state.routeDetails.baseGeometry.map(point => ({ lat: point.latitude, lng: point.longitude })),
      geodesic: true,
      strokeColor: '#1d4ed8',
      strokeOpacity: 0.45,
      strokeWeight: 6
    });
    const visibleStations = new Set();
    for (const station of state.routeDetails.stations) {
      visibleStations.add(station.id);
      const position = { lat: station.latitude, lng: station.longitude };
      const title = `${station.name} - Ruta ${this.routeLabel(state, station.lineId)}`;
      const marker = this.stationMarkers.get(station.id);
      if (marker) {
        marker.setMap(this.map);
        marker.setPosition(position);
        marker.setTitle(title);
      } else {
        this.stationMarkers.set(station.id, new google.maps.Marker({
          position,
          map: this.map,
          title,
          label: { text: station.kind === 'station' ? 'E' : 'P', color: '#ffffff', fontWeight: '700' },
          icon: {
            path: google.maps.SymbolPath.CIRCLE,
            fillColor: station.kind === 'station' ? '#1d4ed8' : '#0f766e',
            fillOpacity: 1,
            strokeColor: '#ffffff',
            strokeWeight: 2,
            scale: 10
          }
        }));
      }
    }
    for (const [stationId, marker] of this.stationMarkers.entries()) {
      if (!visibleStations.has(stationId)) {
        marker.setMap(null);
      }
    }
  }

  renderBuses(state) {
    const active = new Set();
    for (const position of state.positions) {
      active.add(position.busId);
      const latLng = { lat: position.latitude, lng: position.longitude };
      const routeName = this.routeLabel(state, position.lineId);
      const marker = this.markers.get(position.busId);
      if (marker) {
        this.animateMarker(marker, latLng);
        marker.setLabel(this.markerLabel(routeName));
        marker.setTitle(`Bus ${position.busId} - Ruta ${routeName} - ${position.odometer} m desde parada ${position.stopId}`);
      } else {
        this.markers.set(position.busId, new google.maps.Marker({
          position: latLng,
          map: this.map,
          title: `Bus ${position.busId} - Ruta ${routeName} - ${position.odometer} m desde parada ${position.stopId}`,
          label: this.markerLabel(routeName),
          icon: this.markerIcon()
        }));
      }
      if (this.centeredLineId !== state.selectedLineId) {
        this.map.panTo(latLng);
        this.centeredLineId = state.selectedLineId;
      }
    }
    for (const [busId, marker] of this.markers.entries()) {
      if (!active.has(busId)) {
        marker.setMap(null);
        this.markers.delete(busId);
      }
    }
  }

  routeLabel(state, lineId) {
    const route = state.routesByLineId.get(Number(lineId));
    return route ? route.shortName : String(lineId);
  }

  routeOptionLabel(state, lineId) {
    const route = state.routesByLineId.get(Number(lineId));
    return route ? `${route.shortName} - ${route.description}` : String(lineId);
  }

  markerLabel(routeName) {
    return { text: routeName, color: '#ffffff', fontSize: '11px', fontWeight: '700' };
  }

  markerIcon() {
    return {
      path: google.maps.SymbolPath.CIRCLE,
      fillColor: '#d92d20',
      fillOpacity: 1,
      strokeColor: '#ffffff',
      strokeWeight: 2,
      scale: 15,
      labelOrigin: new google.maps.Point(0, 0)
    };
  }

  animateMarker(marker, target) {
    const start = marker.getPosition();
    if (!start) {
      marker.setPosition(target);
      return;
    }
    const from = { lat: start.lat(), lng: start.lng() };
    const startedAt = performance.now();
    const animationId = Symbol('marker-animation');
    marker.__animationId = animationId;
    const step = now => {
      if (marker.__animationId !== animationId) {
        return;
      }
      const progress = Math.min((now - startedAt) / 850, 1);
      marker.setPosition({
        lat: from.lat + (target.lat - from.lat) * progress,
        lng: from.lng + (target.lng - from.lng) * progress
      });
      if (progress < 1) {
        requestAnimationFrame(step);
      }
    };
    requestAnimationFrame(step);
  }
}
