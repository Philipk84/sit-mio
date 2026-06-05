export class AnalyticsController {
  constructor(model) {
    this.model = model;
    this.monthSelect = document.querySelector('#monthSelect');
    this.refreshButton = document.querySelector('#refreshMetric');
    this.selectedLineId = 0;
  }

  start(view) {
    this.model.subscribe(state => view.render(state));
    this.monthSelect.addEventListener('change', () => this.calculateForRoute(this.selectedLineId));
    this.refreshButton.addEventListener('click', () => this.calculateForRoute(this.selectedLineId));
  }

  calculateForRoute(lineId) {
    this.selectedLineId = Number(lineId || 0);
    return this.model.calculate(this.selectedLineId, Number(this.monthSelect.value));
  }
}
