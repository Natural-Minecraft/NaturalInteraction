package id.naturalsmp.naturalinteraction.webpanel;

import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.model.Interaction;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * Embedded HTTP server for NaturalInteraction Web Panel.
 *
 * Auth: LuckPerms-style — /ni connect generates a 6-char token.
 * Frontend calls API directly with Bearer token.
 *
 * Endpoints (all token-protected except /api/session/verify):
 *  GET  /api/session/verify?token=XxxYyy  → verify token, return session info
 *  GET  /api/interactions                  → list all interactions (summary)
 *  GET  /api/interaction/:id               → full interaction detail (all nodes)
 *  POST /api/interaction/:id               → save/update interaction
 *  DELETE /api/interaction/:id             → delete interaction
 *  POST /api/interaction-new               → create new interaction
 */
public class WebPanelServer {

    private final NaturalInteraction plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private HttpServer server;
    private String publicUrl;
    private String apiUrl;

    public WebPanelServer(NaturalInteraction plugin) {
        this.plugin = plugin;
        this.publicUrl = plugin.getConfig().getString("webpanel.public-url", "https://story.naturalsmp.net");
        this.apiUrl = plugin.getConfig().getString("webpanel.api-url", "");
    }

    public void start(int port) {
        try {
            server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
            server.setExecutor(Executors.newCachedThreadPool());

            // Token verify (no auth required — this IS the auth endpoint)
            server.createContext("/api/session/verify", this::handleSessionVerify);

            // Protected endpoints
            server.createContext("/api/interactions", ex -> withAuth(ex, this::handleInteractions));
            server.createContext("/api/interaction-new", ex -> withAuth(ex, this::handleCreateNew));
            server.createContext("/api/interaction/", ex -> withAuth(ex, this::handleInteractionCrud));
            server.createContext("/api/facts/", ex -> withAuth(ex, this::handleFacts));

            // Static / health
            server.createContext("/", this::handleHealth);

            server.start();

            // Build API URL (auto-detect if not configured)
            if (apiUrl.isEmpty()) {
                apiUrl = "http://localhost:" + port;
            }

            plugin.getLogger().info("[WebPanel] Started on port " + port);
            plugin.getLogger().info("[WebPanel] Public URL: " + publicUrl);
            plugin.getLogger().info("[WebPanel] API URL: " + apiUrl);

            // Cleanup expired tokens every 5 minutes
            plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
                int cleaned = TokenSession.cleanExpired();
                if (cleaned > 0)
                    plugin.getLogger().info("[WebPanel] Cleaned " + cleaned + " expired token(s).");
            }, 6000L, 6000L);

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

    public String getPublicUrl() { return publicUrl; }
    public String getApiUrl() { return apiUrl; }

    // ─── Auth Guard ───────────────────────────────────────────────────────────

    @FunctionalInterface
    interface AuthedHandler {
        void handle(HttpExchange ex, TokenSession.SessionData session) throws IOException;
    }

    private void withAuth(HttpExchange exchange, AuthedHandler handler) throws IOException {
        setCorsHeaders(exchange);
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        // Extract token from Authorization header
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendJson(exchange, 401, "{\"error\":\"Missing or invalid Authorization header\"}");
            return;
        }

        String token = authHeader.substring(7).trim();
        TokenSession.SessionData session = TokenSession.verify(token);
        if (session == null) {
            sendJson(exchange, 401, "{\"error\":\"Token invalid or expired. Ketik /ni connect lagi di Minecraft.\"}");
            return;
        }

        handler.handle(exchange, session);
    }

    // ─── GET /api/session/verify ──────────────────────────────────────────────

    private void handleSessionVerify(HttpExchange exchange) throws IOException {
        setCorsHeaders(exchange);
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        // Extract token from query string
        String query = exchange.getRequestURI().getQuery();
        String token = null;
        if (query != null) {
            for (String param : query.split("&")) {
                String[] kv = param.split("=", 2);
                if (kv.length == 2 && kv[0].equals("token")) {
                    token = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                }
            }
        }

        if (token == null || token.isBlank()) {
            sendJson(exchange, 400, "{\"error\":\"Missing token parameter\"}");
            return;
        }

        TokenSession.SessionData session = TokenSession.verify(token);
        if (session == null) {
            sendJson(exchange, 401, "{\"error\":\"Token invalid or expired\"}");
            return;
        }

        JsonObject resp = new JsonObject();
        resp.addProperty("valid", true);
        resp.addProperty("token", session.token());
        resp.addProperty("playerName", session.playerName());
        resp.addProperty("playerUUID", session.playerUUID().toString());
        resp.addProperty("expiresIn", (session.expiresAt() - System.currentTimeMillis()) / 1000);
        resp.addProperty("apiUrl", apiUrl);
        sendJson(exchange, 200, gson.toJson(resp));
    }

    // ─── GET /api/interactions ────────────────────────────────────────────────

    private void handleInteractions(HttpExchange exchange, TokenSession.SessionData session) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        Collection<String> ids = plugin.getInteractionManager().getInteractionIds();
        JsonArray result = new JsonArray();
        for (String id : ids) {
            Interaction i = plugin.getInteractionManager().getInteraction(id);
            if (i == null) continue;
            JsonObject obj = new JsonObject();
            obj.addProperty("id", i.getId());
            obj.addProperty("chapter", i.getChapter());
            obj.addProperty("npcDisplayName", i.getNpcDisplayName());
            obj.addProperty("nodeCount", i.getNodes().size());
            obj.addProperty("rootNodeId", i.getRootNodeId());
            obj.addProperty("cooldownSeconds", i.getCooldownSeconds());
            obj.addProperty("mandatory", i.isMandatory());
            obj.addProperty("oneTimeReward", i.isOneTimeReward());
            obj.addProperty("dialogueUnicode", i.getDialogueUnicode());
            // Detect prologue
            obj.addProperty("isPrologue", "prologue".equalsIgnoreCase(i.getId()));
            result.add(obj);
        }
        sendJson(exchange, 200, gson.toJson(result));
    }

    // ─── /api/interaction/:id (CRUD) ──────────────────────────────────────────

    private void handleInteractionCrud(HttpExchange exchange, TokenSession.SessionData session) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String interactionId = path.substring("/api/interaction/".length());
        if (interactionId.isEmpty()) {
            sendJson(exchange, 400, "{\"error\":\"Missing interaction ID\"}");
            return;
        }

        switch (exchange.getRequestMethod()) {
            case "GET" -> handleGetInteraction(exchange, interactionId);
            case "POST", "PUT" -> handleSaveInteraction(exchange, interactionId);
            case "DELETE" -> handleDeleteInteraction(exchange, interactionId);
            default -> sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
        }
    }

    private void handleGetInteraction(HttpExchange exchange, String id) throws IOException {
        // Read raw JSON file to preserve exact format
        File file = findInteractionFile(id);
        if (file == null || !file.exists()) {
            sendJson(exchange, 404, "{\"error\":\"Interaction not found: " + id + "\"}");
            return;
        }

        String json = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        sendJson(exchange, 200, json);
    }

    private void handleSaveInteraction(HttpExchange exchange, String id) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

        // Validate JSON
        try {
            JsonParser.parseString(body);
        } catch (JsonSyntaxException e) {
            sendJson(exchange, 400, "{\"error\":\"Invalid JSON: " + e.getMessage() + "\"}");
            return;
        }

        // Write to file
        File file = findInteractionFile(id);
        if (file == null) {
            // New file
            File folder = new File(plugin.getDataFolder(), "interactions");
            if (!folder.exists()) folder.mkdirs();
            file = new File(folder, id + ".json");
        }

        // Pretty-print the JSON
        JsonElement parsed = JsonParser.parseString(body);
        String prettyJson = gson.toJson(parsed);

        Files.writeString(file.toPath(), prettyJson, StandardCharsets.UTF_8);

        // Hot-reload this interaction
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            plugin.getInteractionManager().reloadInteraction(id);
        });

        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.addProperty("id", id);
        resp.addProperty("message", "Interaction saved and reloaded.");
        sendJson(exchange, 200, gson.toJson(resp));
        plugin.getLogger().info("[WebPanel] Interaction saved: " + id + " (by " +
                exchange.getRequestHeaders().getFirst("Authorization") + ")");
    }

    private void handleDeleteInteraction(HttpExchange exchange, String id) throws IOException {
        File file = findInteractionFile(id);
        if (file == null || !file.exists()) {
            sendJson(exchange, 404, "{\"error\":\"Interaction not found: " + id + "\"}");
            return;
        }

        file.delete();

        // Remove from memory
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            plugin.getInteractionManager().removeInteraction(id);
        });

        sendJson(exchange, 200, "{\"success\":true,\"message\":\"Interaction deleted: " + id + "\"}");
        plugin.getLogger().info("[WebPanel] Interaction deleted: " + id);
    }

    // ─── POST /api/interaction-new ────────────────────────────────────────────

    private void handleCreateNew(HttpExchange exchange, TokenSession.SessionData session) throws IOException {
        setCorsHeaders(exchange);
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonObject req;
        try {
            req = JsonParser.parseString(body).getAsJsonObject();
        } catch (Exception e) {
            sendJson(exchange, 400, "{\"error\":\"Invalid JSON\"}");
            return;
        }

        String id = req.has("id") ? req.get("id").getAsString().trim() : "";
        if (id.isEmpty()) {
            sendJson(exchange, 400, "{\"error\":\"Missing interaction ID\"}");
            return;
        }

        // Check if already exists
        if (plugin.getInteractionManager().hasInteraction(id)) {
            sendJson(exchange, 409, "{\"error\":\"Interaction already exists: " + id + "\"}");
            return;
        }

        // Create default interaction JSON
        JsonObject interaction = new JsonObject();
        interaction.addProperty("id", id);
        interaction.addProperty("rootNodeId", "start");
        interaction.addProperty("cooldownSeconds", 0);
        interaction.addProperty("mandatory", false);
        interaction.addProperty("oneTimeReward", false);
        interaction.addProperty("dialogueUnicode", "");
        interaction.addProperty("chapter", req.has("chapter") ? req.get("chapter").getAsString() : "");
        interaction.addProperty("npcDisplayName", req.has("npcDisplayName") ? req.get("npcDisplayName").getAsString() : "");

        // Default start node
        JsonObject nodes = new JsonObject();
        JsonObject startNode = new JsonObject();
        startNode.addProperty("id", "start");
        startNode.addProperty("text", "&eHello! Edit this dialogue...");
        startNode.add("options", new JsonArray());
        startNode.add("actions", new JsonArray());
        startNode.addProperty("durationSeconds", 5);
        startNode.addProperty("skippable", true);
        startNode.addProperty("giveReward", false);
        startNode.add("commandRewards", new JsonArray());
        startNode.addProperty("delayBeforeNext", 20);
        nodes.add("start", startNode);
        interaction.add("nodes", nodes);

        interaction.add("rewards", new JsonArray());
        interaction.add("commandRewards", new JsonArray());

        // Save to file
        File folder = new File(plugin.getDataFolder(), "interactions");
        if (!folder.exists()) folder.mkdirs();
        File file = new File(folder, id + ".json");
        Files.writeString(file.toPath(), gson.toJson(interaction), StandardCharsets.UTF_8);

        // Load into memory
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            plugin.getInteractionManager().reloadInteraction(id);
        });

        sendJson(exchange, 201, gson.toJson(interaction));
        plugin.getLogger().info("[WebPanel] New interaction created: " + id);
    }

    // ─── GET /api/facts/:uuid ─────────────────────────────────────────────────

    private void handleFacts(HttpExchange exchange, TokenSession.SessionData session) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String[] segments = path.split("/");
        if (segments.length < 4) {
            sendJson(exchange, 400, "{\"error\":\"Usage: /api/facts/<uuid>\"}");
            return;
        }
        try {
            UUID uuid = UUID.fromString(segments[3]);
            Map<String, String> facts = plugin.getFactsManager().getAll(uuid);
            JsonObject result = new JsonObject();
            result.addProperty("uuid", uuid.toString());
            result.addProperty("count", facts.size());
            JsonObject factsObj = new JsonObject();
            facts.forEach(factsObj::addProperty);
            result.add("facts", factsObj);
            sendJson(exchange, 200, gson.toJson(result));
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, "{\"error\":\"Invalid UUID\"}");
        }
    }

    // ─── Health / Root ────────────────────────────────────────────────────────

    private void handleHealth(HttpExchange exchange) throws IOException {
        setCorsHeaders(exchange);
        JsonObject resp = new JsonObject();
        resp.addProperty("status", "ok");
        resp.addProperty("plugin", "NaturalInteraction");
        resp.addProperty("version", "2.0.0");
        resp.addProperty("activeSessions", TokenSession.activeCount());
        sendJson(exchange, 200, gson.toJson(resp));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private File findInteractionFile(String id) {
        // Search in interactions/ folder and chapters/ subfolders
        File folder = new File(plugin.getDataFolder(), "interactions");
        if (folder.exists()) {
            File direct = new File(folder, id + ".json");
            if (direct.exists()) return direct;
            // Search recursively
            File found = findFileRecursive(folder, id + ".json");
            if (found != null) return found;
        }
        File chapters = new File(plugin.getDataFolder(), "chapters");
        if (chapters.exists()) {
            return findFileRecursive(chapters, id + ".json");
        }
        return null;
    }

    private File findFileRecursive(File dir, String filename) {
        File[] files = dir.listFiles();
        if (files == null) return null;
        for (File f : files) {
            if (f.isFile() && f.getName().equals(filename)) return f;
            if (f.isDirectory()) {
                File found = findFileRecursive(f, filename);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void sendJson(HttpExchange ex, int code, String json) throws IOException {
        byte[] b = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        setCorsHeaders(ex);
        ex.sendResponseHeaders(code, b.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(b);
        }
    }

    private void setCorsHeaders(HttpExchange ex) {
        // Allow all origins since API is token-protected
        String origin = ex.getRequestHeaders().getFirst("Origin");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", origin != null ? origin : "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        ex.getResponseHeaders().set("Access-Control-Allow-Credentials", "true");
        ex.getResponseHeaders().set("Access-Control-Max-Age", "86400");
    }
}
