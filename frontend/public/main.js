import { RouteServiceProxy } from './proxies/RouteServiceProxy.js';
import { PositionServiceProxy } from './proxies/PositionServiceProxy.js';
import { MetricsServiceProxy } from './proxies/MetricsServiceProxy.js';
import { MapModel } from './models/MapModel.js';
import { AnalyticsModel } from './models/AnalyticsModel.js';
import { MapView } from './views/MapView.js';
import { AnalyticsView } from './views/AnalyticsView.js';
import { MapController } from './controllers/MapController.js';
import { AnalyticsController } from './controllers/AnalyticsController.js';

const gateway = window.MIO_CONFIG.gatewayUrl;
const routeProxy = new RouteServiceProxy(gateway);
const positionProxy = new PositionServiceProxy(gateway);
const metricsProxy = new MetricsServiceProxy(gateway);

const analyticsModel = new AnalyticsModel(metricsProxy);
const analyticsView = new AnalyticsView();
const analyticsController = new AnalyticsController(analyticsModel);
analyticsController.start(analyticsView);

const mapModel = new MapModel(routeProxy, positionProxy);
const mapView = new MapView();
const mapController = new MapController(mapModel, mapView, analyticsController);
mapController.start(window.MIO_CONFIG.googleMapsApiKey);
