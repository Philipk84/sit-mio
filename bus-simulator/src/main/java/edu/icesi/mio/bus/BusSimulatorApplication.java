package edu.icesi.mio.bus;

import Mio.Datagram;
import Mio.DatagramReceiverPrx;
import edu.icesi.mio.common.CsvDatagramParser;
import edu.icesi.mio.common.Env;
import edu.icesi.mio.common.IceSupport;
import edu.icesi.mio.common.Paths;

import com.zeroc.Ice.Communicator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public final class BusSimulatorApplication {
    private static final DateTimeFormatter DATAGRAM_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private BusSimulatorApplication() {
    }

    public static void main(String[] args) throws Exception {
        String datagramsSource = Env.value("MIO_BUS_DATAGRAMS_FILE",
                "http://192.168.131.38:10002/datagrams-MiniPilot.csv");
        String ccoProxy = Env.value("MIO_DATAGRAM_RECEIVER_PROXY",
                "DatagramReceiver:tcp -h 192.168.131.39 -p 10010");
        int delayMs = Env.intValue("MIO_BUS_DELAY_MS", 1000);
        int limit = Env.intValue("MIO_BUS_LIMIT", 0);
        boolean loop = Env.booleanValue("MIO_BUS_LOOP", true);
        int simulatedLineId = Env.intValue("MIO_SIM_LINE_ID", 311);
        int simulatedBusId = Env.intValue("MIO_SIM_BUS_ID", 9001);
        int sourceBusId = Env.intValue("MIO_SOURCE_BUS_ID", 846);
        AtomicReference<LocalDateTime> simulatedClock = new AtomicReference<>(null);

        try (Communicator communicator = IceSupport.communicator(args)) {
            DatagramReceiverPrx receiver = DatagramReceiverPrx.checkedCast(communicator.stringToProxy(ccoProxy));
            AtomicInteger sent = new AtomicInteger();
            System.out.println("Bus simulator usando archivo=" + datagramsSource
                    + ", ruta=" + simulatedLineId
                    + ", busSimulado=" + simulatedBusId
                    + ", busFuente=" + sourceBusId);
            do {
                try (Stream<String> lines = openDatagramLines(datagramsSource)) {
                    lines.map(CsvDatagramParser::parse)
                            .filter(optional -> optional.isPresent())
                            .map(optional -> optional.get())
                            .filter(datagram -> simulatedLineId <= 0 || datagram.lineId == simulatedLineId)
                            .filter(datagram -> sourceBusId <= 0 || datagram.busId == sourceBusId)
                            .filter(BusSimulatorApplication::hasValidGps)
                            .map(datagram -> simulatedDatagram(datagram, simulatedLineId, simulatedBusId, simulatedClock))
                            .limit(limit > 0 ? limit : Long.MAX_VALUE)
                            .forEach(datagram -> send(receiver, datagram, delayMs, sent));
                }
                System.out.println("Bus simulator envio " + sent.get() + " datagramas");
            } while (loop);
        }
    }

    private static boolean hasValidGps(Datagram datagram) {
        return datagram.latitude > 0 && datagram.longitude < 0;
    }

    private static Stream<String> openDatagramLines(String configuredSource) throws IOException {
        if (isHttp(configuredSource)) {
            InputStream input = new URL(configuredSource).openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            return reader.lines().onClose(() -> close(reader));
        }
        return Files.lines(resolveDatagramsFile(configuredSource));
    }

    private static Path resolveDatagramsFile(String configuredFile) {
        Path configured = Paths.projectFile(configuredFile);
        if (Files.exists(configured)) {
            return configured;
        }
        Path fallback = Paths.projectFile("chunck.csv");
        if (Files.exists(fallback)) {
            System.err.println("No se encontro " + configuredFile + "; usando fallback " + fallback);
            return fallback;
        }
        throw new IllegalStateException("No se encontro archivo de datagramas: " + configuredFile
                + ". Define MIO_BUS_DATAGRAMS_FILE con una ruta valida.");
    }

    private static boolean isHttp(String value) {
        return value.startsWith("http://") || value.startsWith("https://");
    }

    private static void close(BufferedReader reader) {
        try {
            reader.close();
        } catch (IOException ignored) {
        }
    }

    private static Datagram simulatedDatagram(Datagram source, int simulatedLineId, int simulatedBusId,
                                              AtomicReference<LocalDateTime> simulatedClock) {
        LocalDateTime nextTime = nextTime(source, simulatedClock);
        return new Datagram(
                source.eventType,
                source.registerDate,
                source.stopId,
                source.odometer,
                source.latitude,
                source.longitude,
                source.taskId,
                simulatedLineId > 0 ? simulatedLineId : source.lineId,
                source.tripId,
                source.unknown1,
                DATAGRAM_TIME.format(nextTime),
                simulatedBusId > 0 ? simulatedBusId : source.busId
        );
    }

    private static LocalDateTime nextTime(Datagram source, AtomicReference<LocalDateTime> simulatedClock) {
        LocalDateTime current = simulatedClock.get();
        if (current == null) {
            current = parseTime(source.datagramDate);
        } else {
            current = current.plusSeconds(1);
        }
        simulatedClock.set(current);
        return current;
    }

    private static LocalDateTime parseTime(String value) {
        try {
            return LocalDateTime.parse(value, DATAGRAM_TIME);
        } catch (Exception ex) {
            return LocalDateTime.of(2019, 5, 27, 20, 14, 43);
        }
    }

    private static void send(DatagramReceiverPrx receiver, Datagram datagram, int delayMs, AtomicInteger sent) {
        boolean ack = submitWithRetry(receiver, datagram);
        if (ack) {
            int count = sent.incrementAndGet();
            if (count == 1 || count % 25 == 0) {
                System.out.println("Enviado #" + count
                        + " ruta=" + datagram.lineId
                        + " bus=" + datagram.busId
                        + " lat=" + (datagram.latitude / 10_000_000.0)
                        + " lon=" + (datagram.longitude / 10_000_000.0)
                        + " t=" + datagram.datagramDate);
            }
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private static boolean submitWithRetry(DatagramReceiverPrx receiver, Datagram datagram) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                if (receiver.submit(datagram)) {
                    return true;
                }
                Thread.sleep(100L * (attempt + 1));
            } catch (Exception ex) {
                try {
                    Thread.sleep(250L * (attempt + 1));
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }
}
