package Tracker;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import fi.iki.elonen.NanoHTTPD;

public class Tracker extends NanoHTTPD {
    public ConcurrentHashMap<String, Set<Peer>> peersList = new ConcurrentHashMap<>();
    private static final int ANNOUNCER_TIMER=30*60*1000;
    public Tracker(int port) {
        super(port);
    }

    public Response serve(IHTTPSession session) {
        if ("/announence".equals(session.getUri())) {
            Map<String, String> params = session.getParms();
            return AnnounceResponse(params);
        }
        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found");
    }

    private Response AnnounceResponse(Map<String, String> params) {
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
        peersList.computeIfAbsent(info_hash, k -> ConcurrentHashMap.newKeySet())
                .add(new Peer(Integer.parseInt(ip), Integer.parseInt(port), Integer.parseInt(peer_id)));
        return newFixedLengthResponse(Response.Status.OK, "text/plain", "Not Found");
    }

    public class Peer {

        private int ip;
        private int port;
        private int peer_id;
        private long announencetimer;

        public Peer(int ip, int port, int peer_id) {
            this.ip = ip;
            this.port = port;
            this.peer_id = peer_id;
            announencetimer = System.currentTimeMillis();
        }
    }
}
