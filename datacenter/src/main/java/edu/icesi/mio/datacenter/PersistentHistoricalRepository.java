package edu.icesi.mio.datacenter;

import Mio.Datagram;
import Mio.HistoricalRepository;
import Mio.Position;
import edu.icesi.mio.common.CsvDatagramParser;
import edu.icesi.mio.common.Env;
import edu.icesi.mio.common.Paths;

import com.zeroc.Ice.Current;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

final class PersistentHistoricalRepository implements HistoricalRepository {
    private final Path seedFile;
    private final List<Datagram> receivedDatagrams = new CopyOnWriteArrayList<>();
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;

    PersistentHistoricalRepository(String seedFile) {
        this.seedFile = Paths.projectFile(seedFile);
        this.dbUrl = Env.value("MIO_DB_URL", "jdbc:postgresql://localhost:5432/mio");
        this.dbUser = Env.value("MIO_DB_USER", "postgres");
        this.dbPassword = Env.value("MIO_DB_PASSWORD", "postgres");
        if (isPostgresEnabled()) {
            initializeSchema();
        }
    }

    @Override
    public boolean storeDatagram(Datagram datagram, Current current) {
        if (isPostgresEnabled()) {
            return insertPostgres(datagram);
        }
        receivedDatagrams.add(datagram);
        return true;
    }

    @Override
    public Position[] positionsByRouteAndMonth(int lineId, int month, Current current) {
        if (isPostgresEnabled()) {
            return queryPostgres(lineId, month);
        }
        List<Position> positions = new ArrayList<>();
        streamSeedDatagrams()
                .filter(datagram -> matches(datagram, lineId, month))
                .map(CsvDatagramParser::toPosition)
                .forEach(positions::add);
        receivedDatagrams.stream()
                .filter(datagram -> matches(datagram, lineId, month))
                .map(CsvDatagramParser::toPosition)
                .forEach(positions::add);
        positions.sort(Comparator.comparing(position -> position.timestamp));
        return positions.toArray(new Position[0]);
    }

    private boolean isPostgresEnabled() {
        return dbUrl != null && !dbUrl.trim().isEmpty();
    }

    private void initializeSchema() {
        String ddl = "create table if not exists mio_datagrams ("
                + "id bigserial primary key,"
                + "event_type integer not null,"
                + "register_date text not null,"
                + "stop_id integer not null,"
                + "odometer bigint not null,"
                + "latitude integer not null,"
                + "longitude integer not null,"
                + "task_id integer not null,"
                + "line_id integer not null,"
                + "trip_id integer not null,"
                + "unknown1 bigint not null,"
                + "datagram_date timestamp not null,"
                + "bus_id integer not null"
                + ")";
        String index = "create index if not exists idx_mio_datagrams_route_bus_date "
                + "on mio_datagrams(line_id, bus_id, datagram_date)";
        try (Connection connection = connection();
             Statement statement = connection.createStatement()) {
            statement.execute(ddl);
            statement.execute(index);
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo inicializar Postgres: " + ex.getMessage(), ex);
        }
    }

    private boolean insertPostgres(Datagram datagram) {
        String sql = "insert into mio_datagrams(event_type, register_date, stop_id, odometer, latitude, longitude, "
                + "task_id, line_id, trip_id, unknown1, datagram_date, bus_id) "
                + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::timestamp, ?)";
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, datagram.eventType);
            statement.setString(2, datagram.registerDate);
            statement.setInt(3, datagram.stopId);
            statement.setLong(4, datagram.odometer);
            statement.setInt(5, datagram.latitude);
            statement.setInt(6, datagram.longitude);
            statement.setInt(7, datagram.taskId);
            statement.setInt(8, datagram.lineId);
            statement.setInt(9, datagram.tripId);
            statement.setLong(10, datagram.unknown1);
            statement.setString(11, datagram.datagramDate);
            statement.setInt(12, datagram.busId);
            return statement.executeUpdate() == 1;
        } catch (SQLException ex) {
            System.err.println("No se pudo persistir datagrama en Postgres: " + ex.getMessage());
            return false;
        }
    }

    private Position[] queryPostgres(int lineId, int month) {
        String sql = "select bus_id, line_id, latitude, longitude, datagram_date "
                + "from mio_datagrams "
                + "where line_id = ? and extract(month from datagram_date) = ? "
                + "order by bus_id, datagram_date";
        List<Position> positions = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, lineId);
            statement.setInt(2, month);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    positions.add(new Position(
                            resultSet.getInt("bus_id"),
                            resultSet.getInt("line_id"),
                            -1,
                            0,
                            resultSet.getInt("latitude") / 10_000_000.0,
                            resultSet.getInt("longitude") / 10_000_000.0,
                            0.0,
                            resultSet.getTimestamp("datagram_date").toLocalDateTime().toString().replace('T', ' ')
                    ));
                }
            }
        } catch (SQLException ex) {
            System.err.println("No se pudo consultar Postgres: " + ex.getMessage());
        }
        return positions.toArray(new Position[0]);
    }

    private Stream<Datagram> streamSeedDatagrams() {
        if (!Files.exists(seedFile)) {
            return Stream.empty();
        }
        try {
            return Files.lines(seedFile)
                    .map(CsvDatagramParser::parse)
                    .filter(optional -> optional.isPresent())
                    .map(optional -> optional.get());
        } catch (IOException ex) {
            System.err.println("No se pudo leer historico " + seedFile + ": " + ex.getMessage());
            return Stream.empty();
        }
    }

    private boolean matches(Datagram datagram, int lineId, int month) {
        return datagram.lineId == lineId && CsvDatagramParser.month(datagram) == month;
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }
}
