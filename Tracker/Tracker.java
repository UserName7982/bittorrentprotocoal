package Tracker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
        System.out.println("Server started on port " + port);
        executorService.scheduleAtFixedRate(this::checkInactivePeers, 1, 1, TimeUnit.MINUTES);
        // Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        // executorService.shutdown();
        // try {
        // if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
        // executorService.shutdownNow();
        // }
        // } catch (InterruptedException e) {
        // System.out.println( "shutdown interrupted");
        // e.printStackTrace();
        // }
        // }));

    }

    /**
     * Handles an incoming HTTP request.
     *
     * @param session the incoming HTTP request
     * @return the response to the request
     */
    public Response serve(IHTTPSession session) {
        if ("/announce".equals(session.getUri())) {
            Map<String,List<String>> params = session.getParameters();
            System.out.println(params);
            try {
                return AnnounceResponse(params);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found");
    }

    /**
     * Handles the announce request from a peer. This method processes the peer's
     * parameters, updates the tracker's list of peers, and constructs a response
     * based on the request type and parameters.
     *
     * The parameters include:
     * - "info_hash": the info hash of the torrent
     * - "peer_id": the unique identifier of the peer
     * - "port": the port number on which the peer is listening
     * - "left": the number of bytes the peer still has to download
     * - "event": the event type (e.g., "started", "stopped", "completed")
     * - "ip": the IP address of the peer
     * - "compact": whether the response should be in a compact format
     *
     * The method updates the list of peers by adding or removing peers based on
     * the event, and increments or decrements the global seeder and leecher
     * counters accordingly. The response contains either a compact byte array or
     * a list of peer information, depending on the "compact" parameter.
     *
     * @param params the map of parameters from the announce request
     * @return a response object containing the encoded peers list or an error
     * @throws IOException if an I/O error occurs during processing
     */

    private Response AnnounceResponse(Map<String, List<String>> params) throws IOException {
        String info_hash = params.get("info_hash").get(0);
        String peer_id = params.get("peer_id").get(0);
        String port = params.get("port").get(0);
        // String uploaded = params.get("uploaded");
        // String downloaded = params.get("downloaded");
        String left = params.get("left").get(0);
        String event = params.get("event").get(0);
        String ip = params.get("ip").get(0);
        String compact = "1";
        boolean isSeeder = false;
        int portnumber = 0;
        try {
            System.out.println(port);
            portnumber = Integer.parseInt(port);
        } catch (NumberFormatException e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "tracker port error");
        }
        if ("0".equals(left)) {
            isSeeder = true;
        }
        if ("stopped".equals(event)) {
            boolean removed = peersList.getOrDefault(info_hash, ConcurrentHashMap.newKeySet())
                    .removeIf(p -> p.peer_id.equals(peer_id));
            if (removed) {
                if (isSeeder)
                    seeders.decrementAndGet();
                else
                    leechars.decrementAndGet();
            }
        }
        boolean b = peersList.computeIfAbsent(info_hash, k -> ConcurrentHashMap.newKeySet())
                .add(new Peer(ip, portnumber, peer_id, isSeeder));
        if (b) {
            if (isSeeder)
                seeders.incrementAndGet();
            else
                leechars.incrementAndGet();
        }
        if (event.equals("completed")) {
            leechars.decrementAndGet();
        }
        Map<String, Object> map = new ConcurrentHashMap<>();
        map.put("interval", ANNOUNCER_TIMER / 1000);
        if ( compact!=null&&compact.equals("1")) {
            map.put("peers", getcompact(info_hash));
            return buildResponse(map);
        } else {
            map.put("peers", getpeers(info_hash));
           return buildResponse(map);
        }
    }

    /**
     * Builds a response object from the given map of data. The response body is
     * encoded using the bencoding scheme and the response is given a content type
     * of text/plain.
     * 
     * @param map the map of data to encode and return in the response body
     * @return a response object containing the encoded data in the response body
     * @throws IOException if an I/O error occurs while encoding the data
     */
    public Response buildResponse(Map<String, Object> map) throws IOException {
        endcode encoder = new endcode();
        try {
            byte[] bytes = encoder.encode(map);
            System.out.println(map);
            return newFixedLengthResponse(Response.Status.OK, "text/plain",
                    new ByteArrayInputStream(bytes), bytes.length);
        } catch (NumberFormatException e) {
            System.out.println("INTERNAL_ERROR");
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Response error");
        }
    }

    /**
     * Returns a compact byte array containing the peers for the given infoHash.
     *
     * The compact byte array contains each peer's IP address and port number in
     * binary format, as follows:
     * - 4 bytes IP address in network byte order
     * - 2 bytes port number in network byte order
     *
     * @param info_hash the infoHash of the torrent
     * @return a compact byte array containing the peers for the given infoHash
     * @throws IOException if an I/O error occurs
     */
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

    /**
     * Returns a list of peers for the given infoHash.
     * 
     * The list contains maps with the following keys:
     * 
     * - "ip": a string containing the IP address of the peer
     * - "port": an int containing the port number of the peer
     * - "peer_id": a string containing the peer ID of the peer
     * 
     * @param info_hash the infoHash of the torrent
     * @return a list of peers for the given infoHash
     * @throws IOException if an I/O error occurs
     */
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

    /**
     * Periodically removes inactive peers from the tracker's data structure.
     * Checks all peers in all infoHashes for inactivity and removes them. Also
     * atomically updates global counters for seeders and leechers.
     */
    public void checkInactivePeers() {
        peersList.forEach((infoHash, peers) -> {
            AtomicInteger seedersRemoved = new AtomicInteger(0);
            AtomicInteger leechersRemoved = new AtomicInteger(0);

            peers.removeIf(peer -> {
                boolean isInactive = System.currentTimeMillis() - peer.announencetimer.get() > ANNOUNCER_TIMER;
                if (isInactive) {
                    if (peer.isSeeder) {
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

        /**
         * Returns the compact representation of the peer's IP and port number. The
         * compact representation is a byte array that contains the IP address and
         * port number. The IP address is represented as a 4-byte array for IPv4
         * addresses and a 16-byte array for IPv6 addresses. The port number is
         * represented as a 2-byte short integer. The byte array is allocated using
         * ByteBuffer.allocate() and is returned as a byte array from the array()
         * method.
         * 
         * @return the compact representation of the peer's IP and port number
         * @throws IllegalArgumentException
         *                                  if the IP address is invalid
         */
        public byte[] GetCompactBytes() {
            try {
                InetAddress address = InetAddress.getByName(ip);
                ByteBuffer bb;
                if (address instanceof Inet6Address) {
                    bb = ByteBuffer.allocate(18); // 16 bytes IPv6 + 2 bytes port
                } else {
                    System.out.println("IPv4");
                    bb = ByteBuffer.allocate(6); // 4 bytes IPv4 + 2 bytes port
                }
                bb.put(address.getAddress());
                bb.putShort((short) port);
                return bb.array();
            } catch (UnknownHostException e) {
                throw new IllegalArgumentException("Invalid IP: " + ip);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Tracker tracker = new Tracker(6969);
        tracker.start();
    }
}