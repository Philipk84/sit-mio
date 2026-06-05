export class MapView {
  constructor() {
    this.routeSelect = document.querySelector('#routeSelect');
    this.statusEl = document.querySelector('#status');
    this.busValue = document.querySelector('#busValue');
    this.banner = document.querySelector('#mapStatusBanner');
    this.markers = new Map();
    this.stationMarkers = new Map();
    this.busTrailPolylines = new Map();
    this.roadTrails = new Map();
    this.basePolyline = null;
    this.directionsService = null;
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
      this.directionsService = new google.maps.DirectionsService();
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
    for (const polyline of this.busTrailPolylines.values()) {
      polyline.setVisible(false);
    }
    if (state.selectedLineId <= 0) {
      return;
    }
    const trailsByBus = state.routeTrailsByBus.get(state.selectedLineId) || new Map();
    for (const [busId, trail] of trailsByBus.entries()) {
      const key = `${state.selectedLineId}:${busId}`;
      if (!this.busTrailPolylines.has(key)) {
        this.busTrailPolylines.set(key, new google.maps.Polyline({
          map: this.map,
          geodesic: true,
          strokeColor: this.trailColor(busId),
          strokeOpacity: 0.9,
          strokeWeight: 4
        }));
      }
      const polyline = this.busTrailPolylines.get(key);
      this.updateRoadTrail(key, trail, polyline);
      polyline.setVisible(true);
    }
  }

  updateRoadTrail(key, trail, polyline) {
    if (!trail.length) {
      polyline.setPath([]);
      return;
    }
    const roadTrail = this.roadTrailState(key, trail);
    polyline.setPath(roadTrail.path.length > 1 ? roadTrail.path : trail);
    this.resolveNextRoadSegment(key, trail, polyline);
  }

  roadTrailState(key, trail) {
    const firstPointKey = this.pointKey(trail[0]);
    const current = this.roadTrails.get(key);
    if (current && current.firstPointKey === firstPointKey) {
      return current;
    }
    const next = {
      firstPointKey,
      path: [trail[0]],
      resolvedUntil: 0,
      pending: false
    };
    this.roadTrails.set(key, next);
    return next;
  }

  resolveNextRoadSegment(key, trail, polyline) {
    const roadTrail = this.roadTrailState(key, trail);
    if (roadTrail.pending || roadTrail.resolvedUntil >= trail.length - 1) {
      return;
    }
    const from = trail[roadTrail.resolvedUntil];
    const to = trail[roadTrail.resolvedUntil + 1];
    if (!this.directionsService || this.distanceMeters(from, to) < 120) {
      this.appendRoadSegment(roadTrail, [to], polyline);
      this.resolveNextRoadSegment(key, trail, polyline);
      return;
    }
    roadTrail.pending = true;
    this.directionsService.route({
      origin: from,
      destination: to,
      travelMode: google.maps.TravelMode.DRIVING,
      provideRouteAlternatives: false
    }, (response, status) => {
      roadTrail.pending = false;
      const path = status === google.maps.DirectionsStatus.OK
        && response.routes
        && response.routes[0]
        ? response.routes[0].overview_path.map(point => ({ lat: point.lat(), lng: point.lng() }))
        : [to];
      this.appendRoadSegment(roadTrail, path, polyline);
    });
  }

  appendRoadSegment(roadTrail, path, polyline) {
    for (const point of path) {
      const last = roadTrail.path[roadTrail.path.length - 1];
      if (!last || last.lat !== point.lat || last.lng !== point.lng) {
        roadTrail.path.push(point);
      }
    }
    roadTrail.resolvedUntil += 1;
    if (roadTrail.path.length > 1600) {
      roadTrail.path.splice(0, roadTrail.path.length - 1600);
    }
    polyline.setPath(roadTrail.path);
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
      const stationKey = `${station.lineId}:${station.id}`;
      visibleStations.add(stationKey);
      const position = { lat: station.latitude, lng: station.longitude };
      const title = `${station.name} - Ruta ${this.routeLabel(state, station.lineId)}`;
      const marker = this.stationMarkers.get(stationKey);
      if (marker) {
        marker.setMap(this.map);
        marker.setPosition(position);
        marker.setTitle(title);
      } else {
        this.stationMarkers.set(stationKey, new google.maps.Marker({
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
    for (const [stationKey, marker] of this.stationMarkers.entries()) {
      if (!visibleStations.has(stationKey)) {
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

  trailColor(busId) {
    const colors = ['#166534', '#7c2d12', '#0f766e', '#4338ca', '#be123c', '#0369a1'];
    return colors[Math.abs(Number(busId)) % colors.length];
  }

  pointKey(point) {
    return `${point.lat.toFixed(7)},${point.lng.toFixed(7)}`;
  }

  distanceMeters(from, to) {
    const radius = 6371000;
    const dLat = this.toRadians(to.lat - from.lat);
    const dLng = this.toRadians(to.lng - from.lng);
    const lat1 = this.toRadians(from.lat);
    const lat2 = this.toRadians(to.lat);
    const a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
      + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
    return radius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  }

  toRadians(value) {
    return value * Math.PI / 180;
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
