export class MapController {
  constructor(model, view, analyticsController) {
    this.model = model;
    this.view = view;
    this.analyticsController = analyticsController;
  }

  start(apiKey) {
    this.model.subscribe(state => this.view.render(state));
    this.view.onRouteChange(lineId => {
      this.model.selectRoute(lineId);
      this.analyticsController.calculateForRoute(lineId);
    });
    this.view.loadGoogleMaps(apiKey, () => this.view.render(this.model.state));
    this.model.initialize();
  }
}
