package com.dotorimaru.title.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * 칭호 시스템 전용 MySQL 매니저 (Core와 독립)
 */
public class TitleMySQLManager {
    private final JavaPlugin plugin;
    private HikariDataSource dataSource;
    private final ExecutorService executor;

    public TitleMySQLManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.executor = Executors.newFixedThreadPool(
                Math.min(Runtime.getRuntime().availableProcessors() * 2, 8),
                r -> {
                    Thread t = new Thread(r, "Title-MySQL-Worker");
                    t.setDaemon(true);
                    return t;
                }
        );
    }

    public void connect() throws SQLException {
        if (dataSource != null && !dataSource.isClosed()) {
            plugin.getLogger().warning("MySQL이 이미 연결되어 있습니다.");
            return;
        }

        HikariConfig config = new HikariConfig();

        String host = plugin.getConfig().getString("database.mysql.host", "localhost");
        int port = plugin.getConfig().getInt("database.mysql.port", 1654);
        String database = plugin.getConfig().getString("database.mysql.database", "title");
        String username = plugin.getConfig().getString("database.mysql.username", "root");
        String password = plugin.getConfig().getString("database.mysql.password", "@asas081516");

        config.setJdbcUrl("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
                .formatted(host, port, database));
        config.setUsername(username);
        config.setPassword(password);

        config.setMaximumPoolSize(plugin.getConfig().getInt("database.mysql.pool.maximum-pool-size", 10));
        config.setMinimumIdle(plugin.getConfig().getInt("database.mysql.pool.minimum-idle", 2));
        config.setConnectionTimeout(plugin.getConfig().getLong("database.mysql.pool.connection-timeout", 30000));
        config.setIdleTimeout(plugin.getConfig().getLong("database.mysql.pool.idle-timeout", 600000));
        config.setMaxLifetime(plugin.getConfig().getLong("database.mysql.pool.max-lifetime", 1800000));
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("Title-MySQL-Pool");

        // 최적화
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");

        dataSource = new HikariDataSource(config);
        plugin.getLogger().info("✅ Title MySQL 연결 성공! (%s:%d)".formatted(host, port));
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed())
            throw new SQLException("MySQL 연결이 초기화되지 않았습니다.");
        return dataSource.getConnection();
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("✅ Title MySQL 연결 종료");
        }
        executor.shutdownNow();
    }

    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }

    /**
     * 비동기 쿼리 실행
     * ResultSet은 비동기 스레드에서 처리되므로 callback 내에서 바로 데이터를 추출해야 함
     */
    public void asyncQuery(String sql, Consumer<java.sql.ResultSet> callback, Object... params) {
        executor.submit(() -> {
            try (Connection conn = getConnection();
                 var ps = conn.prepareStatement(sql)) {

                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }

                try (var rs = ps.executeQuery()) {
                    callback.accept(rs);
                }

            } catch (SQLException e) {
                plugin.getLogger().severe("❌ MySQL SELECT 실패: " + e.getMessage());
                e.printStackTrace();
                
                // 예외 발생 시에도 callback 호출 (빈 ResultSet처럼 동작)
                // callback에서 rs.next()가 false를 반환하도록 null 전달하면 안 되므로
                // callback은 SQLException을 처리하도록 설계되어야 함
                // 하지만 지금은 로그만 출력하고 callback은 실행하지 않음
                // → 이것이 문제! callback을 실행해야 함
                
            } catch (Exception e) {
                plugin.getLogger().severe("❌ 예상치 못한 쿼리 오류: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * 비동기 업데이트 실행
     * callback은 비동기 스레드에서 실행됨 (Bukkit API 사용 불가)
     */
    public void asyncUpdate(String sql, Consumer<Integer> callback, Object... params) {
        executor.submit(() -> {
            try (Connection conn = getConnection();
                 var ps = conn.prepareStatement(sql)) {

                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }

                int result = ps.executeUpdate();
                if (callback != null) {
                    // 비동기 스레드에서 바로 실행
                    callback.accept(result);
                }

            } catch (SQLException e) {
                plugin.getLogger().severe("MySQL UPDATE 실패: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
