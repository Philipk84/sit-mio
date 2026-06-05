const state = {
  map: null,
  markers: new Map(),
  routes: [],
  routesByLineId: new Map(),
  activeLineIds: new Set(),
  routeTrails: new Map(),
  routePolylines: new Map(),
  selectedLineId: 0,
  hasCenteredSelection: false,
  monitoring: false
};

const gateway = window.MIO_CONFIG.gatewayUrl;
const routeSelect = document.querySelector('#routeSelect');
const monthSelect = document.querySelector('#monthSelect');
const statusEl = document.querySelector('#status');
const metricStatus = document.querySelector('#metricStatus');
const speedValue = document.querySelector('#speedValue');
const sampleValue = document.querySelector('#sampleValue');
const busValue = document.querySelector('#busValue');

function loadGoogleMaps() {
  if (!window.MIO_CONFIG.googleMapsApiKey) {
    statusEl.textContent = 'Configura GOOGLE_MAPS_API_KEY para cargar el mapa.';
    document.querySelector('#map').textContent = 'Mapa no disponible';
    return;
  }
  window.initMioMap = initMap;
  const script = document.createElement('script');
  script.src = `https://maps.googleapis.com/maps/api/js?key=${window.MIO_CONFIG.googleMapsApiKey}&callback=initMioMap`;
  script.async = true;
  document.head.appendChild(script);
}

function initMap() {
  state.map = new google.maps.Map(document.querySelector('#map'), {
    center: { lat: 3.4516, lng: -76.5320 },
    zoom: 12,
    mapId: 'mio-map'
  });
  hideAllRoutePolylines();
}

function startMonitoring() {
  if (state.monitoring) {
    return;
  }
  state.monitoring = true;
  monitorActiveRoutes();
  setInterval(monitorActiveRoutes, 1000);
}

async function loadRoutes() {
  const routes = await getJson('/api/routes');
  state.routes = routes;
  state.routesByLineId = new Map(routes.map(route => [Number(route.lineId), route]));
  renderRouteOptions();
  statusEl.textContent = 'Esperando rutas activas del CCO.';
}

async function monitorActiveRoutes() {
  try {
    const positions = await getJson('/api/positions?lineId=0');
    state.activeLineIds = new Set(positions.map(position => Number(position.lineId)));
    positions.forEach(position => appendRoutePoint(position));
    renderRouteOptions();

    if (state.selectedLineId > 0) {
      await refreshSelectedRoute();
    } else {
      clearVisibleMap();
      busValue.textContent = '0';
      statusEl.textContent = state.activeLineIds.size
        ? `${state.activeLineIds.size} rutas activas. Selecciona una para verla.`
        : 'Esperando datagramas del CCO.';
    }
  } catch (error) {
    statusEl.textContent = 'CCO no disponible para posiciones.';
  }
}

function renderRouteOptions() {
  const previous = String(routeSelect.value || '');
  const activeRoutes = Array.from(state.activeLineIds)
    .sort((a, b) => routeLabel(a).localeCompare(routeLabel(b), 'es'));

  routeSelect.innerHTML = '<option value="">Selecciona una ruta activa</option>' + activeRoutes
    .map(lineId => `<option value="${lineId}">${routeOptionLabel(lineId)}</option>`)
    .join('');

  if (previous && state.activeLineIds.has(Number(previous))) {
    routeSelect.value = previous;
  } else if (previous) {
    state.selectedLineId = 0;
    resetMetrics();
  }
}

async function refreshSelectedRoute() {
  const positions = await getJson(`/api/positions?lineId=${state.selectedLineId}`);
  busValue.textContent = String(positions.length);
  statusEl.textContent = positions.length
    ? `Ruta ${routeLabel(state.selectedLineId)} actualizada.`
    : `Ruta ${routeLabel(state.selectedLineId)} sin posiciones recientes.`;

  if (!state.map) {
    return;
  }

  const activeBuses = new Set();
  positions.forEach(position => {
    activeBuses.add(position.busId);
    paintPosition(position);
  });

  for (const [busId, marker] of state.markers.entries()) {
    if (!activeBuses.has(busId)) {
      marker.setMap(null);
      state.markers.delete(busId);
    }
  }
}

function paintPosition(position) {
  const latLng = { lat: position.latitude, lng: position.longitude };
  const routeName = routeLabel(position.lineId);
  const marker = state.markers.get(position.busId);

  appendRoutePoint(position);
  if (!state.hasCenteredSelection) {
    state.map.panTo(latLng);
    state.hasCenteredSelection = true;
  }

  if (marker) {
    animateMarker(marker, latLng);
    marker.setLabel(markerLabel(routeName));
    marker.setTitle(`Bus ${position.busId} - Ruta ${routeName}`);
  } else {
    state.markers.set(position.busId, new google.maps.Marker({
      position: latLng,
      map: state.map,
      title: `Bus ${position.busId} - Ruta ${routeName}`,
      label: markerLabel(routeName),
      icon: markerIcon()
    }));
  }
}

function appendRoutePoint(position) {
  const lineId = Number(position.lineId);
  const latLng = { lat: position.latitude, lng: position.longitude };
  const trail = state.routeTrails.get(lineId) || [];
  const last = trail[trail.length - 1];
  if (last && last.lat === latLng.lat && last.lng === latLng.lng) {
    return;
  }
  trail.push(latLng);
  if (trail.length > 900) {
    trail.shift();
  }
  state.routeTrails.set(lineId, trail);

  if (state.map) {
    const polyline = routePolyline(lineId);
    polyline.setPath(trail);
    polyline.setVisible(lineId === state.selectedLineId);
  }
}

function routePolyline(lineId) {
  if (!state.routePolylines.has(lineId)) {
    state.routePolylines.set(lineId, new google.maps.Polyline({
      map: state.map,
      path: state.routeTrails.get(lineId) || [],
      geodesic: true,
      visible: lineId === state.selectedLineId,
      strokeColor: routeColor(lineId),
      strokeOpacity: 0.88,
      strokeWeight: 4
    }));
  }
  return state.routePolylines.get(lineId);
}

function routeColor(lineId) {
  const colors = ['#166534', '#1d4ed8', '#be123c', '#a16207', '#6d28d9', '#0f766e'];
  return colors[Math.abs(Number(lineId)) % colors.length];
}

function animateMarker(marker, target) {
  const start = marker.getPosition();
  if (!start) {
    marker.setPosition(target);
    return;
  }
  const from = { lat: start.lat(), lng: start.lng() };
  const startedAt = performance.now();
  const durationMs = 850;
  const animationId = Symbol('marker-animation');
  marker.__animationId = animationId;

  function step(now) {
    if (marker.__animationId !== animationId) {
      return;
    }
    const progress = Math.min((now - startedAt) / durationMs, 1);
    const eased = progress < 0.5
      ? 2 * progress * progress
      : 1 - Math.pow(-2 * progress + 2, 2) / 2;
    marker.setPosition({
      lat: from.lat + (target.lat - from.lat) * eased,
      lng: from.lng + (target.lng - from.lng) * eased
    });
    if (progress < 1) {
      requestAnimationFrame(step);
    }
  }

  requestAnimationFrame(step);
}

function clearVisibleMap() {
  for (const marker of state.markers.values()) {
    marker.setMap(null);
  }
  state.markers.clear();
  state.hasCenteredSelection = false;
  hideAllRoutePolylines();
}

function hideAllRoutePolylines() {
  for (const polyline of state.routePolylines.values()) {
    polyline.setVisible(false);
  }
}

function showSelectedRoutePolyline() {
  hideAllRoutePolylines();
  if (state.selectedLineId > 0 && state.map) {
    routePolyline(state.selectedLineId).setVisible(true);
  }
}

function markerLabel(routeName) {
  return {
    text: routeName,
    color: '#ffffff',
    fontSize: '11px',
    fontWeight: '700'
  };
}

function markerIcon() {
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

async function refreshMetric() {
  const lineId = Number(routeSelect.value || 0);
  if (lineId <= 0) {
    metricStatus.textContent = 'Selecciona una ruta.';
    return;
  }
  metricStatus.textContent = 'Calculando...';
  try {
    const month = Number(monthSelect.value);
    const metric = await getJson(`/api/metrics?lineId=${lineId}&month=${month}`);
    speedValue.textContent = `${metric.speedKmh.toFixed(2)} km/h`;
    sampleValue.textContent = String(metric.samples);
    metricStatus.textContent = metric.samples ? 'Resultado historico disponible.' : 'Sin muestras para la consulta.';
  } catch (error) {
    metricStatus.textContent = 'Consulta historica no disponible.';
  }
}

function resetMetrics() {
  speedValue.textContent = '-- km/h';
  sampleValue.textContent = '--';
  metricStatus.textContent = 'Selecciona una ruta.';
}

function routeLabel(lineId) {
  const route = state.routesByLineId.get(Number(lineId));
  return route ? route.shortName : String(lineId);
}

function routeOptionLabel(lineId) {
  const route = state.routesByLineId.get(Number(lineId));
  if (!route) {
    return String(lineId);
  }
  return `${route.shortName} - ${route.description}`;
}

async function getJson(path) {
  const response = await fetch(`${gateway}${path}`);
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }
  return response.json();
}

routeSelect.addEventListener('change', () => {
  state.selectedLineId = Number(routeSelect.value || 0);
  clearVisibleMap();
  if (state.selectedLineId > 0) {
    showSelectedRoutePolyline();
    refreshSelectedRoute();
    refreshMetric();
  } else {
    busValue.textContent = '0';
    resetMetrics();
  }
});
monthSelect.addEventListener('change', refreshMetric);
document.querySelector('#refreshMetric').addEventListener('click', refreshMetric);

loadRoutes().then(() => {
  resetMetrics();
  startMonitoring();
  loadGoogleMaps();
}).catch(() => {
  statusEl.textContent = 'CCO no disponible.';
  loadGoogleMaps();
});
