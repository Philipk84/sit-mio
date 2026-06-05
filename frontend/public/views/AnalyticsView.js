export class AnalyticsView {
  constructor() {
    this.speedValue = document.querySelector('#speedValue');
    this.sampleValue = document.querySelector('#sampleValue');
    this.metricStatus = document.querySelector('#metricStatus');
  }

  render(state) {
    this.speedValue.textContent = state.speedKmh === null ? '-- km/h' : `${state.speedKmh.toFixed(2)} km/h`;
    this.sampleValue.textContent = state.samples === null ? '--' : String(state.samples);
    this.metricStatus.textContent = state.status;
  }
}
