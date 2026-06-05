module Mio
{
    struct Datagram
    {
        int eventType;
        string registerDate;
        int stopId;
        long odometer;
        int latitude;
        int longitude;
        int taskId;
        int lineId;
        int tripId;
        long unknown1;
        string datagramDate;
        int busId;
    }

    struct Position
    {
        int busId;
        int lineId;
        int stopId;
        long odometer;
        double latitude;
        double longitude;
        double speedKmh;
        string timestamp;
    }

    sequence<Position> PositionSeq;

    struct Route
    {
        int lineId;
        string shortName;
        string description;
    }

    sequence<Route> RouteSeq;

    struct Station
    {
        int id;
        int lineId;
        string name;
        double latitude;
        double longitude;
        string kind;
    }

    sequence<Station> StationSeq;

    struct RoutePoint
    {
        int lineId;
        double latitude;
        double longitude;
        int order;
    }

    sequence<RoutePoint> RoutePointSeq;

    struct RouteMapData
    {
        Route route;
        StationSeq stations;
        RoutePointSeq baseGeometry;
    }

    struct AverageSpeed
    {
        int lineId;
        int month;
        double speedKmh;
        long samples;
    }

    interface DatagramReceiver
    {
        bool submit(Datagram datagram);
    }

    interface PositionService
    {
        PositionSeq latestPositions(int lineId);
    }

    interface RouteService
    {
        RouteSeq listRoutes();
        RouteMapData routeDetails(int lineId);
    }

    interface MetricsService
    {
        AverageSpeed averageSpeedByRouteAndMonth(int lineId, int month);
    }

    interface HistoricalRepository
    {
        bool storeDatagram(Datagram datagram);
        PositionSeq positionsByRouteAndMonth(int lineId, int month);
    }

    interface OperationalRepository
    {
        RouteSeq routes();
        RouteMapData routeDetails(int lineId);
    }
}
