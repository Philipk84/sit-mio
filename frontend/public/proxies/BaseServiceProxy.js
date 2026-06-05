export class BaseServiceProxy {
  constructor(gatewayUrl) {
    this.gatewayUrl = gatewayUrl;
  }

  async getJson(path, timeoutMs = 5000) {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), timeoutMs);
    try {
      const response = await fetch(`${this.gatewayUrl}${path}`, { signal: controller.signal });
      if (!response.ok) {
        return { ok: false, unavailable: true, message: `HTTP ${response.status}` };
      }
      return { ok: true, data: await response.json() };
    } catch (error) {
      return { ok: false, unavailable: true, message: error.name === 'AbortError' ? 'timeout' : error.message };
    } finally {
      clearTimeout(timeout);
    }
  }
}
