package id.naturalsmp.naturalinteraction.webpanel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.model.Interaction;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * Lightweight WebSocket + HTTP server embedded in the plugin.
 * Provides REST-like API and WebSocket for real-time sync with the Web Panel.
 *
 * Endpoints:
 *  GET  /api/interactions     → list all interactions
 *  GET  /api/chapters         → chapter tree
 *  GET  /api/facts/:uuid      → player facts
 *  WS   /ws                   → real-time updates
 */
public class WebPanelServer {

    private final NaturalInteraction plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private HttpServer server;
    private final Map<String, WebSocketSession> wsSessions = new ConcurrentHashMap<>();

    public WebPanelServer(NaturalInteraction plugin) {
        this.plugin = plugin;
    }

    public void start(int port) {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.setExecutor(Executors.newCachedThreadPool());

            // REST endpoints
            server.createContext("/api/interactions", this::handleInteractions);
            server.createContext("/api/chapters", this::handleChapters);
            server.createContext("/api/facts", this::handleFacts);

            // WebSocket upgrade
            server.createContext("/ws", this::handleWebSocket);

            // Static fallback (for local dev — Vite handles prod)
            server.createContext("/", this::handleStatic);

            server.start();
            plugin.getLogger().info("[WebPanel] Server started on port " + port);
        } catch (Exception e) {
            plugin.getLogger().severe("[WebPanel] Failed to start: " + e.getMessage());
        }
    }

    public void stop() {
        if (server != null) {
            wsSessions.values().forEach(WebSocketSession::close);
            wsSessions.clear();
            server.stop(0);
            plugin.getLogger().info("[WebPanel] Server stopped.");
        }
    }

    // ─── REST: Interactions ───────────────────────────────────────────────────

    private void handleInteractions(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }
        setCorsHeaders(exchange);
        Collection<String> ids = plugin.getInteractionManager().getInteractionIds();
        List<Map<String, Object>> result = new ArrayList<>();
        for (String id : ids) {
            Interaction i = plugin.getInteractionManager().getInteraction(id);
            if (i == null) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", i.getId());
            m.put("chapter", i.getChapter());
            m.put("npcDisplayName", i.getNpcDisplayName());
            m.put("nodes", i.getNodes().size());
            m.put("rootNodeId", i.getRootNodeId());
            m.put("cooldownSeconds", i.getCooldownSeconds());
            m.put("mandatory", i.isMandatory());
            m.put("oneTimeReward", i.isOneTimeReward());
            result.add(m);
        }
        sendJson(exchange, 200, gson.toJson(result));
    }

    // ─── REST: Chapters ───────────────────────────────────────────────────────

    private void handleChapters(HttpExchange exchange) throws IOException {
        setCorsHeaders(exchange);
        Map<String, Object> tree = new LinkedHashMap<>();
        for (String id : plugin.getInteractionManager().getInteractionIds()) {
            Interaction i = plugin.getInteractionManager().getInteraction(id);
            if (i == null) continue;
            String chapter = i.getChapter().isEmpty() ? "uncategorized" : i.getChapter();
            String[] parts = chapter.split("\\.");
            Map<String, Object> current = tree;
            for (String part : parts) {
                current = (Map<String, Object>) current.computeIfAbsent(part, k -> new LinkedHashMap<>());
            }
            ((Map<String, Object>) current).put(id, i.getNpcDisplayName().isEmpty() ? id : i.getNpcDisplayName());
        }
        sendJson(exchange, 200, gson.toJson(tree));
    }

    // ─── REST: Facts ──────────────────────────────────────────────────────────

    private void handleFacts(HttpExchange exchange) throws IOException {
        setCorsHeaders(exchange);
        String path = exchange.getRequestURI().getPath();
        String[] segments = path.split("/");
        if (segments.length < 4) {
            sendJson(exchange, 400, "{\"error\":\"Usage: /api/facts/<uuid>\"}");
            return;
        }
        try {
            UUID uuid = UUID.fromString(segments[3]);
            Map<String, String> facts = plugin.getFactsManager().getAll(uuid);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("uuid", uuid.toString());
            result.put("facts", facts);
            sendJson(exchange, 200, gson.toJson(result));
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, "{\"error\":\"Invalid UUID\"}");
        }
    }

    // ─── WebSocket Upgrade ────────────────────────────────────────────────────

    private void handleWebSocket(HttpExchange exchange) throws IOException {
        String upgrade = exchange.getRequestHeaders().getFirst("Upgrade");
        String wsKey = exchange.getRequestHeaders().getFirst("Sec-WebSocket-Key");

        if (upgrade == null || !upgrade.equalsIgnoreCase("websocket") || wsKey == null) {
            sendJson(exchange, 400, "{\"error\":\"WebSocket upgrade required\"}");
            return;
        }

        // WebSocket handshake
        String acceptKey = computeWebSocketAccept(wsKey);
        exchange.getResponseHeaders().set("Upgrade", "websocket");
        exchange.getResponseHeaders().set("Connection", "Upgrade");
        exchange.getResponseHeaders().set("Sec-WebSocket-Accept", acceptKey);
        exchange.sendResponseHeaders(101, -1);

        // NOTE: Full WebSocket frame parsing is complex — for production,
        // use Java-WebSocket library. This is a placeholder handshake.
        String sessionId = UUID.randomUUID().toString();
        plugin.getLogger().info("[WebPanel] WebSocket connected: " + sessionId);
    }

    private String computeWebSocketAccept(String key) {
        try {
            String magic = key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(magic.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            return "";
        }
    }

    // ─── Static Files ─────────────────────────────────────────────────────────

    private void handleStatic(HttpExchange exchange) throws IOException {
        setCorsHeaders(exchange);
        // In dev, Vite serves files. This is a minimal fallback.
        String response = "<html><body><h1>NaturalInteraction WebPanel</h1>"
                + "<p>Run <code>npm run dev</code> in the webpanel/ directory for the full UI.</p></body></html>";
        sendHtml(exchange, 200, response);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void sendJson(HttpExchange exchange, int code, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(code, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().close();
    }

    private void sendHtml(HttpExchange exchange, int code, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(code, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().close();
    }

    private void setCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    // ─── Inner: WebSocket Session (placeholder) ───────────────────────────────

    private static class WebSocketSession {
        private final String id;
        WebSocketSession(String id) { this.id = id; }
        void close() { /* TODO: implement frame close */ }
    }
}
