package Tracker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
        if ("/announce".equals(session.getUri())) {
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
        // String uploaded = params.get("uploaded");
        // String downloaded = params.get("downloaded");
        String left = params.get("left");
        String event = params.get("event");
        String ip = params.get("ip");
        String compact = params.get("compact");
        boolean isSeeder = false;
        if ("0".equals(left)) {
            isSeeder = true;
        }
        if ("stopped".equals(event)) {
            boolean removed = peersList.getOrDefault(info_hash,  ConcurrentHashMap<>().newKeySet()).re
                    moveIf(p -> p.peer_id.equals(peer_id));
            if (removed) {
                if (isSeeder)
                    seeders.decrementAndGet();
                else
                    leechars.decrementAndGet();
            }
        }
        boolean b = peersList.computeIfAbsent(info_hash, k -> ConcurrentHashMap.newKeySet())
                .add(new Peer(ip, Integer.parseInt(port), peer_id, isSeeder));
        if (b) {
            if (isSeeder)
                seeders.incrementAndGet();
            else
                leechars.incrementAndGet();
        }
        if (event.equals("completed")) {
            leechars.decrementAndGet();
        }
        endcode encoder = new endcode();
        Map<String, Object> map = new ConcurrentHashMap<>();
        map.put("interval", ANNOUNCER_TIMER / 1000);
        if (compact.equals("1")) {
            try {
                map.put("peers", getcompact(info_hash));
                byte[] bytes = encoder.encode(map);
                return newFixedLengthResponse(Response.Status.OK, "text/plain",
                        new String(bytes.toString(),StandardCharsets.ISO_8859_1));
            } catch (IOException e) {
                e.printStackTrace();
                Throwable throwable = new Throwable("Getting a problem in getcompact");
                throwable.printStackTrace();
            }
        } else {
            try {
                map.put("peers", getpeers(info_hash));
                byte[] bytes = encoder.encode(map);
                return newFixedLengthResponse(Response.Status.OK, "text/plain",
                        new String(bytes.toString(),StandardCharsets.ISO_8859_1));
            } catch (IOException e) {
                e.printStackTrace();
                Throwable throwable = new Throwable("Getting a problem in getpeers");
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

    public List<Map<String, Object>> getpeers(String info_hash) throws IOException {
        Set<Peer> peers = peersList.getOrDefault(info_hash, ConcurrentHashMap.newKeySet());
        List<Map<String, Object>> list = new java.util.ArrayList<>();
        for (Peer peer : peers) {
            Map<String, Object> map = new ConcurrentHashMap<>();
            map.put("ip", peer.ip);
            map.put("port", peer.port);
            map.put("peer_id", peer.peer_id);
            list.add(map);
        }
        return list;
    }

    public void checkInactivePeers() {
        peersList.forEach((infoHash, peers) -> {
            AtomicInteger seedersRemoved = new AtomicInteger(0);
            AtomicInteger leechersRemoved = new AtomicInteger(0);

            // Thread-safe removal of inactive peers + count tracking
            peers.removeIf(peer -> {
                boolean isInactive = System.currentTimeMillis() - peer.announencetimer.get() > ANNOUNCER_TIMER;
                if (isInactive) {
                    if (peer.isSeeder) { // Track seeder/leecher status
                        seedersRemoved.incrementAndGet();
                    } else {
                        leechersRemoved.incrementAndGet();
                    }
                    return true;
                }
                return false;
            });

            // Atomically update global counters
            seeders.addAndGet(-seedersRemoved.get());
            leechars.addAndGet(-leechersRemoved.get());
        });
    }

    public class Peer {

        private String ip;
        private int port;
        private String peer_id;
        private AtomicLong announencetimer = new AtomicLong();
        private final boolean isSeeder;

        public Peer(String ip, int port, String peer_id, boolean isSeeder) {
            this.ip = ip;
            this.port = port;
            this.peer_id = peer_id;
            this.isSeeder = isSeeder;
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