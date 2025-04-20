package Tracker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import EncodingFile.endcode;
import fi.iki.elonen.NanoHTTPD;

public class Tracker extends NanoHTTPD {
    private static final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    public ConcurrentHashMap<String, Set<Peer>> peersList = new ConcurrentHashMap<>();
    private static final int ANNOUNCER_TIMER = 30 * 60 * 1000;
    private AtomicInteger leechars = new AtomicInteger(0);
    private AtomicInteger seeders = new AtomicInteger(0);

    public Tracker(int port) {
        super(port);
        executorService.scheduleAtFixedRate(this::checkInactivePeers, 1, 1, TimeUnit.MINUTES);
    }

    public Response serve(IHTTPSession session) {
        if ("/announence".equals(session.getUri())) {
            Map<String, String> params = session.getParms();
            try {
                return AnnounceResponse(params);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found");
    }

    private Response AnnounceResponse(Map<String, String> params) throws IOException {
        String info_hash = params.get("info_hash");
        String peer_id = params.get("peer_id");
        String port = params.get("port");
        String uploaded = params.get("uploaded");
        String downloaded = params.get("downloaded");
        String left = params.get("left");
        String event = params.get("event");
        String ip = params.get("ip");
        String key = params.get("key");
        String compact = params.get("compact");
        boolean isSeeder = "0".equals(left);
        if ("stopped".equals(event)) {
            boolean removed = peersList.get(info_hash).removeIf(p -> p.peer_id.equals(peer_id));
            if (removed) {
                if (isSeeder)
                    seeders.decrementAndGet();
                else
                    leechars.decrementAndGet();
            }
        }
        boolean b = peersList.computeIfAbsent(info_hash, k -> ConcurrentHashMap.newKeySet())
                .add(new Peer(ip, Integer.parseInt(port), peer_id));
        if (b) {
            if (isSeeder)
                seeders.incrementAndGet();
            else
                leechars.incrementAndGet();
        }
        if (event.equals("completed")) {
            leechars.decrementAndGet();
            seeders.incrementAndGet();
        }
        if (event.equals("uploaded")) {
            seeders.incrementAndGet();
        }
        endcode encoder = new endcode();
        if (compact.equals("1")) {
            Map<String, Object> map = new ConcurrentHashMap<>();
            try {
                map.put("peers", getcompact(info_hash));
                map.put("interval", ANNOUNCER_TIMER/1000);
                byte[] bytes = encoder.encode(map);
                return newFixedLengthResponse(Response.Status.OK, "text/plain",
                        new String(bytes.toString()));
            } catch (IOException e) {
                e.printStackTrace();
                Throwable throwable = new Throwable("Getting a problem in getcompact");
                throwable.printStackTrace();
            }
        }
        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found");
    }

    public byte[] getcompact(String info_hash) throws IOException {
        Set<Peer> peers = peersList.getOrDefault(info_hash, ConcurrentHashMap.newKeySet());
        ByteArrayOutputStream bOutputStream = new ByteArrayOutputStream();
        if (peers != null) {
            for (Peer peer : peers) {
                bOutputStream.write(peer.GetCompactBytes());
            }
        }
        return bOutputStream.toByteArray();
    }

    public void checkInactivePeers() {
        AtomicBoolean temp = new AtomicBoolean(false);
        peersList.forEach((info_hash, peers) -> temp.set(
                peers.removeIf(
                        (Peer peer) -> System.currentTimeMillis() - peer.announencetimer.get() > ANNOUNCER_TIMER)));
        if (temp.get()) {
            leechars.decrementAndGet();
        }
    }

    public class Peer {

        private String ip;
        private int port;
        private String peer_id;
        private AtomicLong announencetimer = new AtomicLong();

        public Peer(String ip, int port, String peer_id) {
            this.ip = ip;
            this.port = port;
            this.peer_id = peer_id;
            Refresh();
        }

        public void Refresh() {
            announencetimer.set(System.currentTimeMillis());
        }

        public byte[] GetCompactBytes() {
            ByteBuffer bb = ByteBuffer.allocate(6);
            String[] ipStrings = ip.split("\\.");
            for (String s : ipStrings) {
                bb.put((byte) Integer.parseInt(s));
            }
            bb.putShort((short) port);
            return bb.array();
        }
    }
}
