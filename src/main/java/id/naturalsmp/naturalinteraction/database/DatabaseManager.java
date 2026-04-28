package id.naturalsmp.naturalinteraction.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import id.naturalsmp.naturalinteraction.NaturalInteraction;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.*;
import java.util.logging.Logger;

/**
 * MySQL connection pool manager using HikariCP.
 * Config diambil dari config.yml plugin — web panel tidak perlu konfigurasi terpisah.
 */
public class DatabaseManager {

    private final NaturalInteraction plugin;
    private final Logger log;
    private HikariDataSource dataSource;
    private boolean connected = false;

    public DatabaseManager(NaturalInteraction plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    public boolean connect() {
        FileConfiguration cfg = plugin.getConfig();

        if (!cfg.getBoolean("mysql.enabled", false)) {
            log.info("[Database] MySQL disabled in config.");
            return false;
        }

        HikariConfig hikari = new HikariConfig();
        String host = cfg.getString("mysql.host", "localhost");
        int port = cfg.getInt("mysql.port", 3306);
        String db = cfg.getString("mysql.database", "naturalsmp_interaction");
        String user = cfg.getString("mysql.username", "root");
        String pass = cfg.getString("mysql.password", "");

        hikari.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + db
                + "?useSSL=false&autoReconnect=true&characterEncoding=utf8");
        hikari.setUsername(user);
        hikari.setPassword(pass);
        hikari.setMaximumPoolSize(cfg.getInt("mysql.pool-size", 5));
        hikari.setConnectionTimeout(cfg.getLong("mysql.connection-timeout", 30000));
        hikari.setPoolName("NaturalInteraction-Pool");

        try {
            dataSource = new HikariDataSource(hikari);
            createTables();
            connected = true;
            log.info("[Database] MySQL connected to " + host + ":" + port + "/" + db);
            return true;
        } catch (Exception e) {
            log.severe("[Database] Failed to connect: " + e.getMessage());
            return false;
        }
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            connected = false;
            log.info("[Database] MySQL disconnected.");
        }
    }

    public boolean isConnected() { return connected && dataSource != null && !dataSource.isClosed(); }

    public Connection getConnection() throws SQLException {
        if (!isConnected()) throw new SQLException("Database not connected");
        return dataSource.getConnection();
    }

    // ─── Table Setup ──────────────────────────────────────────────────────────

    private void createTables() throws SQLException {
        try (Connection con = dataSource.getConnection(); Statement stmt = con.createStatement()) {

            // Admin accounts for web panel login
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS ni_admins (
                    id          INT AUTO_INCREMENT PRIMARY KEY,
                    username    VARCHAR(64) NOT NULL UNIQUE,
                    password    VARCHAR(255) NOT NULL,
                    role        ENUM('superadmin','admin','viewer') DEFAULT 'admin',
                    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    last_login  TIMESTAMP NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                """);

            // Auth tokens (server-side session store)
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS ni_tokens (
                    token       VARCHAR(128) NOT NULL PRIMARY KEY,
                    admin_id    INT NOT NULL,
                    expires_at  TIMESTAMP NOT NULL,
                    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (admin_id) REFERENCES ni_admins(id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                """);

            // Insert default superadmin if table is empty
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM ni_admins");
            rs.next();
            if (rs.getInt(1) == 0) {
                // Default: admin / naturalsmp (BCrypt hash for "naturalsmp")
                stmt.executeUpdate("""
                    INSERT INTO ni_admins (username, password, role) VALUES
                    ('admin', '$2a$12$placeholder_change_this_immediately', 'superadmin');
                    """);
                log.warning("[Database] Default admin created — GANTI PASSWORD lewat /ni admin setpass!");
            }
            rs.close();
        }
    }

    // ─── Admin Queries ────────────────────────────────────────────────────────

    public AdminRecord getAdmin(String username) {
        if (!isConnected()) return null;
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT id, username, password, role FROM ni_admins WHERE username = ?")) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new AdminRecord(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("role"));
            }
        } catch (SQLException e) {
            log.warning("[Database] getAdmin error: " + e.getMessage());
        }
        return null;
    }

    public void updateLastLogin(int adminId) {
        if (!isConnected()) return;
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE ni_admins SET last_login = NOW() WHERE id = ?")) {
            ps.setInt(1, adminId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warning("[Database] updateLastLogin error: " + e.getMessage());
        }
    }

    public void saveToken(String token, int adminId, long expiresAt) {
        if (!isConnected()) return;
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO ni_tokens (token, admin_id, expires_at) VALUES (?, ?, FROM_UNIXTIME(?))")) {
            ps.setString(1, token);
            ps.setInt(2, adminId);
            ps.setLong(3, expiresAt / 1000);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warning("[Database] saveToken error: " + e.getMessage());
        }
    }

    public AdminRecord getAdminByToken(String token) {
        if (!isConnected()) return null;
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement("""
                SELECT a.id, a.username, a.password, a.role
                FROM ni_tokens t
                JOIN ni_admins a ON t.admin_id = a.id
                WHERE t.token = ? AND t.expires_at > NOW()
                """)) {
            ps.setString(1, token);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new AdminRecord(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("role"));
            }
        } catch (SQLException e) {
            log.warning("[Database] getAdminByToken error: " + e.getMessage());
        }
        return null;
    }

    public void deleteToken(String token) {
        if (!isConnected()) return;
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM ni_tokens WHERE token = ?")) {
            ps.setString(1, token);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warning("[Database] deleteToken error: " + e.getMessage());
        }
    }

    public void cleanExpiredTokens() {
        if (!isConnected()) return;
        try (Connection con = getConnection();
             Statement stmt = con.createStatement()) {
            int deleted = stmt.executeUpdate("DELETE FROM ni_tokens WHERE expires_at < NOW()");
            if (deleted > 0) log.info("[Database] Cleaned " + deleted + " expired tokens.");
        } catch (SQLException e) {
            log.warning("[Database] cleanExpiredTokens error: " + e.getMessage());
        }
    }

    public void setAdminPassword(String username, String hashedPassword) {
        if (!isConnected()) return;
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE ni_admins SET password = ? WHERE username = ?")) {
            ps.setString(1, hashedPassword);
            ps.setString(2, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warning("[Database] setAdminPassword error: " + e.getMessage());
        }
    }

    // ─── Record ───────────────────────────────────────────────────────────────

    public record AdminRecord(int id, String username, String passwordHash, String role) {}
}
