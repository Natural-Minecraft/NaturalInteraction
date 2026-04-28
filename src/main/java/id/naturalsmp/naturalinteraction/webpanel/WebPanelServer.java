package id.naturalsmp.naturalinteraction.webpanel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.database.DatabaseManager;
import id.naturalsmp.naturalinteraction.database.PasswordUtil;
import id.naturalsmp.naturalinteraction.model.Interaction;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * Embedded HTTP server for the NaturalInteraction Web Panel.
 *
 * Public URL: https://story.naturalsmp.net (via Cloudflare Tunnel)
 * Local:      http://103.93.129.117:<port>
 *
 * Endpoints:
 *  POST /api/auth/login       → { username, password } → { token, role }
 *  POST /api/auth/logout      → [Auth] → 200
 *  GET  /api/auth/verify      → [Auth] → { username, role }
 *  GET  /api/interactions     → [Auth] → list
 *  GET  /api/chapters         → [Auth] → tree
 *  GET  /api/facts/:uuid      → [Auth] → player facts
 *  WS   /ws                   → WebSocket upgrade
 */
public class WebPanelServer {

    private final NaturalInteraction plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private HttpServer server;
    private String publicUrl;

    public WebPanelServer(NaturalInteraction plugin) {
        this.plugin = plugin;
        this.publicUrl = plugin.getConfig().getString("webpanel.public-url", "https://story.naturalsmp.net");
    }

    public void start(int port) {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.setExecutor(Executors.newCachedThreadPool());

            // Auth endpoints (no auth required)
            server.createContext("/api/auth/login",  this::handleLogin);

            // Auth-protected endpoints
            server.createContext("/api/auth/logout", ex -> withAuth(ex, this::handleLogout));
            server.createContext("/api/auth/verify",  ex -> withAuth(ex, this::handleVerify));
            server.createContext("/api/interactions", ex -> withAuth(ex, this::handleInteractions));
            server.createContext("/api/chapters",     ex -> withAuth(ex, this::handleChapters));
            server.createContext("/api/facts",        ex -> withAuth(ex, this::handleFacts));

            // WebSocket + static fallback
            server.createContext("/ws", this::handleWebSocket);
            server.createContext("/",   this::handleStatic);

            server.start();
            plugin.getLogger().info("[WebPanel] Started on port " + port + " — " + publicUrl);

            // Schedule token cleanup every hour
            plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
                if (plugin.getDatabaseManager() != null && plugin.getDatabaseManager().isConnected()) {
                    plugin.getDatabaseManager().cleanExpiredTokens();
                }
            }, 72000L, 72000L); // every hour

        } catch (Exception e) {
            plugin.getLogger().severe("[WebPanel] Failed to start: " + e.getMessage());
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            plugin.getLogger().info("[WebPanel] Stopped.");
        }
    }

    // ─── Auth Guard ───────────────────────────────────────────────────────────

    @FunctionalInterface
    interface AuthedHandler {
        void handle(HttpExchange ex, DatabaseManager.AdminRecord admin) throws IOException;
    }

    private void withAuth(HttpExchange exchange, AuthedHandler handler) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            setCorsHeaders(exchange);
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        setCorsHeaders(exchange);

        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendJson(exchange, 401, "{\"error\":\"Unauthorized\"}");
            return;
        }

        String token = authHeader.substring(7).trim();
        DatabaseManager db = plugin.getDatabaseManager();

        if (db == null || !db.isConnected()) {
            // Fallback mode: jika MySQL disabled, pakai in-memory token check
            sendJson(exchange, 503, "{\"error\":\"Database not connected\"}");
            return;
        }

        DatabaseManager.AdminRecord admin = db.getAdminByToken(token);
        if (admin == null) {
            sendJson(exchange, 401, "{\"error\":\"Token invalid or expired\"}");
            return;
        }

        handler.handle(exchange, admin);
    }

    // ─── POST /api/auth/login ─────────────────────────────────────────────────

    private void handleLogin(HttpExchange exchange) throws IOException {
        setCorsHeaders(exchange);
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonObject req;
        try { req = JsonParser.parseString(body).getAsJsonObject(); }
        catch (Exception e) { sendJson(exchange, 400, "{\"error\":\"Invalid JSON\"}"); return; }

        String username = req.has("username") ? req.get("username").getAsString() : "";
        String password = req.has("password") ? req.get("password").getAsString() : "";

        if (username.isBlank() || password.isBlank()) {
            sendJson(exchange, 400, "{\"error\":\"Username dan password wajib diisi\"}");
            return;
        }

        DatabaseManager db = plugin.getDatabaseManager();
        if (db == null || !db.isConnected()) {
            sendJson(exchange, 503, "{\"error\":\"Database tidak terhubung. Aktifkan mysql di config.yml\"}");
            return;
        }

        DatabaseManager.AdminRecord admin = db.getAdmin(username);
        if (admin == null || !PasswordUtil.verify(password, admin.passwordHash())) {
            sendJson(exchange, 401, "{\"error\":\"Username atau password salah\"}");
            plugin.getLogger().warning("[WebPanel] Failed login attempt for: " + username);
            return;
        }

        // Generate token
        String token = PasswordUtil.generateToken();
        long expire = plugin.getConfig().getLong("webpanel.token-expire-seconds", 28800);
        long expiresAt = System.currentTimeMillis() + (expire * 1000);
        db.saveToken(token, admin.id(), expiresAt);
        db.updateLastLogin(admin.id());

        JsonObject resp = new JsonObject();
        resp.addProperty("token", token);
        resp.addProperty("username", admin.username());
        resp.addProperty("role", admin.role());
        resp.addProperty("expiresIn", expire);
        sendJson(exchange, 200, gson.toJson(resp));
        plugin.getLogger().info("[WebPanel] Admin logged in: " + username);
    }

    // ─── POST /api/auth/logout ────────────────────────────────────────────────

    private void handleLogout(HttpExchange exchange, DatabaseManager.AdminRecord admin) throws IOException {
        String token = exchange.getRequestHeaders().getFirst("Authorization").substring(7).trim();
        plugin.getDatabaseManager().deleteToken(token);
        sendJson(exchange, 200, "{\"message\":\"Logged out\"}");
    }

    // ─── GET /api/auth/verify ─────────────────────────────────────────────────

    private void handleVerify(HttpExchange exchange, DatabaseManager.AdminRecord admin) throws IOException {
        JsonObject resp = new JsonObject();
        resp.addProperty("username", admin.username());
        resp.addProperty("role", admin.role());
        sendJson(exchange, 200, gson.toJson(resp));
    }

    // ─── GET /api/interactions ────────────────────────────────────────────────

    private void handleInteractions(HttpExchange exchange, DatabaseManager.AdminRecord admin) throws IOException {
        Collection<String> ids = plugin.getInteractionManager().getInteractionIds();
        List<Map<String, Object>> result = new ArrayList<>();
        for (String id : ids) {
            Interaction i = plugin.getInteractionManager().getInteraction(id);
            if (i == null) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", i.getId());
            m.put("chapter", i.getChapter());
            m.put("npcDisplayName", i.getNpcDisplayName());
            m.put("nodeCount", i.getNodes().size());
            m.put("rootNodeId", i.getRootNodeId());
            m.put("cooldownSeconds", i.getCooldownSeconds());
            m.put("mandatory", i.isMandatory());
            m.put("oneTimeReward", i.isOneTimeReward());
            result.add(m);
        }
        sendJson(exchange, 200, gson.toJson(result));
    }

    // ─── GET /api/chapters ────────────────────────────────────────────────────

    private void handleChapters(HttpExchange exchange, DatabaseManager.AdminRecord admin) throws IOException {
        Map<String, Object> tree = new LinkedHashMap<>();
        for (String id : plugin.getInteractionManager().getInteractionIds()) {
            Interaction i = plugin.getInteractionManager().getInteraction(id);
            if (i == null) continue;
            String chapter = i.getChapter().isEmpty() ? "uncategorized" : i.getChapter();
            String[] parts = chapter.split("\\.");
            @SuppressWarnings("unchecked")
            Map<String, Object> current = tree;
            for (String part : parts) {
                current = (Map<String, Object>) current.computeIfAbsent(part, k -> new LinkedHashMap<>());
            }
            current.put(id, i.getNpcDisplayName().isEmpty() ? id : i.getNpcDisplayName());
        }
        sendJson(exchange, 200, gson.toJson(tree));
    }

    // ─── GET /api/facts/:uuid ─────────────────────────────────────────────────

    private void handleFacts(HttpExchange exchange, DatabaseManager.AdminRecord admin) throws IOException {
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
            result.put("count", facts.size());
            result.put("facts", facts);
            sendJson(exchange, 200, gson.toJson(result));
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, "{\"error\":\"Invalid UUID\"}");
        }
    }

    // ─── WebSocket (handshake) ────────────────────────────────────────────────

    private void handleWebSocket(HttpExchange exchange) throws IOException {
        String wsKey = exchange.getRequestHeaders().getFirst("Sec-WebSocket-Key");
        if (wsKey == null) { sendJson(exchange, 400, "{\"error\":\"Not a WS request\"}"); return; }
        String acceptKey = computeWebSocketAccept(wsKey);
        exchange.getResponseHeaders().set("Upgrade", "websocket");
        exchange.getResponseHeaders().set("Connection", "Upgrade");
        exchange.getResponseHeaders().set("Sec-WebSocket-Accept", acceptKey);
        exchange.sendResponseHeaders(101, -1);
    }

    private String computeWebSocketAccept(String key) {
        try {
            String magic = key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            return Base64.getEncoder().encodeToString(
                    md.digest(magic.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { return ""; }
    }

    // ─── Static fallback ──────────────────────────────────────────────────────

    private void handleStatic(HttpExchange exchange) throws IOException {
        setCorsHeaders(exchange);
        String html = """
            <!DOCTYPE html>
            <html><head><meta charset="UTF-8">
            <title>NaturalInteraction Web Panel</title>
            <style>body{font-family:sans-serif;background:#0e1117;color:#e6edf3;display:flex;
            align-items:center;justify-content:center;height:100vh;margin:0;flex-direction:column}
            h1{background:linear-gradient(135deg,#4facfe,#00f2fe);-webkit-background-clip:text;
            -webkit-text-fill-color:transparent;font-size:2rem}
            p{color:#8b949e}</style></head>
            <body>
            <h1>✦ NaturalInteraction</h1>
            <p>Panel sedang berjalan. Buka <strong>https://story.naturalsmp.net</strong></p>
            </body></html>
            """;
        sendHtml(exchange, 200, html);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void sendJson(HttpExchange ex, int code, String json) throws IOException {
        byte[] b = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, b.length);
        ex.getResponseBody().write(b);
        ex.getResponseBody().close();
    }

    private void sendHtml(HttpExchange ex, int code, String html) throws IOException {
        byte[] b = html.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        ex.sendResponseHeaders(code, b.length);
        ex.getResponseBody().write(b);
        ex.getResponseBody().close();
    }

    private void setCorsHeaders(HttpExchange ex) {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", publicUrl);
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        ex.getResponseHeaders().set("Access-Control-Allow-Credentials", "true");
    }

    public String getPublicUrl() { return publicUrl; }
}
