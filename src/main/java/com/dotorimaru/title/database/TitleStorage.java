package com.dotorimaru.title.database;

import com.dotorimaru.title.TitlePlugin;
import com.dotorimaru.title.models.Title;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * 칭호 데이터 통합 스토리지 (MySQL + Redis)
 * 모든 메서드가 CompletableFuture를 반환하여 비동기 처리
 */
public class TitleStorage {
    
    private static final String PLAYER_TITLES_TABLE = "player_titles";
    private static final String SELECTED_TITLE_TABLE = "selected_titles";
    
    private static final String CREATE_PLAYER_TITLES_TABLE = """
        CREATE TABLE IF NOT EXISTS `%s` (
            `uuid` VARCHAR(36) NOT NULL,
            `title_name` VARCHAR(200) NOT NULL,
            `obtained_at` BIGINT NOT NULL,
            PRIMARY KEY (`uuid`, `title_name`),
            INDEX `idx_uuid` (`uuid`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
        """;
    
    private static final String CREATE_SELECTED_TITLE_TABLE = """
        CREATE TABLE IF NOT EXISTS `%s` (
            `uuid` VARCHAR(36) PRIMARY KEY,
            `title_name` VARCHAR(200),
            `updated_at` BIGINT NOT NULL
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
        """;
    
    private final TitlePlugin plugin;
    private final TitleMySQLManager mysql;
    private final TitleRedisManager redis;
    private final Gson gson = new Gson();
    
    public TitleStorage(TitlePlugin plugin, TitleMySQLManager mysql, TitleRedisManager redis) {
        this.plugin = plugin;
        this.mysql = mysql;
        this.redis = redis;
        
        createTables();
    }
    
    /** 테이블 생성 */
    private void createTables() {
        try (Connection conn = mysql.getConnection();
             var stmt = conn.createStatement()) {
            
            stmt.execute(CREATE_PLAYER_TITLES_TABLE.formatted(PLAYER_TITLES_TABLE));
            stmt.execute(CREATE_SELECTED_TITLE_TABLE.formatted(SELECTED_TITLE_TABLE));
            
            plugin.getLogger().info("✅ 칭호 테이블 확인/생성 완료");
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "❌ 테이블 생성 실패", e);
        }
    }
    
    /**
     * 플레이어의 모든 칭호 로드 (비동기)
     */
    public CompletableFuture<Map<String, Title>> loadPlayerTitles(UUID uuid) {
        CompletableFuture<Map<String, Title>> future = new CompletableFuture<>();
        
        // 1. Redis 캐시 확인
        if (redis.isEnabled()) {
            String cached = redis.getCache("titles:" + uuid);
            if (cached != null) {
                try {
                    Map<String, Title> titles = gson.fromJson(cached, 
                        new TypeToken<Map<String, Title>>(){}.getType());
                    future.complete(titles);
                    return future;
                } catch (Exception e) {
                    plugin.getLogger().warning("Redis 캐시 파싱 실패: " + e.getMessage());
                }
            }
        }
        
        // 2. MySQL에서 로드 (Core 1.1.7 호환)
        CompletableFuture.runAsync(() -> {
            String sql = "SELECT title_name, obtained_at FROM %s WHERE uuid = ?".formatted(PLAYER_TITLES_TABLE);
            
            try (var conn = mysql.getConnection();
                 var stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, uuid.toString());
                
                try (var rs = stmt.executeQuery()) {
                    Map<String, Title> titles = new HashMap<>();
                    
                    while (rs.next()) {
                        Title title = Title.builder()
                            .playerUUID(uuid)
                            .titleName(rs.getString("title_name"))
                            .obtainedAt(rs.getLong("obtained_at"))
                            .build();
                        titles.put(title.getTitleName(), title);
                    }
                    
                    // Redis 캐시 저장
                    if (redis.isEnabled()) {
                        redis.setCache("titles:" + uuid, gson.toJson(titles));
                    }
                    
                    future.complete(titles);
                }
                
            } catch (SQLException e) {
                plugin.getLogger().severe("칭호 로드 실패: " + e.getMessage());
                future.completeExceptionally(e);
            }
        });
        
        return future;
    }
    
    /**
     * 칭호 추가 (비동기)
     */
    public CompletableFuture<Boolean> addTitle(UUID uuid, String titleName) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        String sql = "INSERT INTO %s (uuid, title_name, obtained_at) VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE obtained_at = VALUES(obtained_at)";
        
        mysql.asyncUpdate(sql.formatted(PLAYER_TITLES_TABLE), result -> {
            boolean success = result != null && result > 0;
            
            if (success && redis.isEnabled()) {
                redis.deleteCache("titles:" + uuid);
                redis.publish("title-add:" + uuid + ":" + titleName);
            }
            
            future.complete(success);
        }, uuid.toString(), titleName, System.currentTimeMillis());
        
        return future;
    }
    
    /**
     * 칭호 삭제 (비동기)
     */
    public CompletableFuture<Boolean> deleteTitle(UUID uuid, String titleName) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        String sql = "DELETE FROM %s WHERE uuid = ? AND title_name = ?";
        
        mysql.asyncUpdate(sql.formatted(PLAYER_TITLES_TABLE), result -> {
            boolean success = result != null && result > 0;
            
            if (success) {
                // 선택된 칭호였다면 해제
                getSelectedTitle(uuid).thenAccept(selected -> {
                    if (titleName.equals(selected)) {
                        setSelectedTitle(uuid, null);
                    }
                });
                
                if (redis.isEnabled()) {
                    redis.deleteCache("titles:" + uuid);
                    redis.deleteCache("selected:" + uuid);
                    redis.publish("title-remove:" + uuid + ":" + titleName);
                }
            }
            
            future.complete(success);
        }, uuid.toString(), titleName);
        
        return future;
    }
    
    /**
     * 선택된 칭호 조회 (비동기)
     */
    public CompletableFuture<String> getSelectedTitle(UUID uuid) {
        CompletableFuture<String> future = new CompletableFuture<>();
        
        // 1. Redis 캐시 확인
        if (redis.isEnabled()) {
            String cached = redis.getCache("selected:" + uuid);
            if (cached != null) {
                String result = cached.equals("null") ? null : cached;
                future.complete(result);
                return future;
            }
        }
        
        // 2. MySQL에서 로드 (Core 1.1.7 호환)
        CompletableFuture.runAsync(() -> {
            String sql = "SELECT title_name FROM %s WHERE uuid = ?".formatted(SELECTED_TITLE_TABLE);
            
            try (var conn = mysql.getConnection();
                 var stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, uuid.toString());
                
                try (var rs = stmt.executeQuery()) {
                    String title = null;
                    if (rs.next()) {
                        title = rs.getString("title_name");
                    }
                    
                    // Redis 캐시 저장
                    if (redis.isEnabled()) {
                        redis.setCache("selected:" + uuid, title != null ? title : "null");
                    }
                    
                    future.complete(title);
                }
                
            } catch (SQLException e) {
                plugin.getLogger().severe("❌ 선택된 칭호 조회 실패: " + e.getMessage());
                future.complete(null);
            } catch (Exception e) {
                plugin.getLogger().severe("❌ 예상치 못한 오류: " + e.getMessage());
                future.complete(null);
            }
        });
        
        return future;
    }
    
    /**
     * 칭호 선택/해제 (비동기)
     */
    public CompletableFuture<Void> setSelectedTitle(UUID uuid, String titleName) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        String sql = titleName != null ?
            "INSERT INTO %s (uuid, title_name, updated_at) VALUES (?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE title_name = VALUES(title_name), updated_at = VALUES(updated_at)" :
            "DELETE FROM %s WHERE uuid = ?";
        
        Object[] params = titleName != null ?
            new Object[]{uuid.toString(), titleName, System.currentTimeMillis()} :
            new Object[]{uuid.toString()};
        
        mysql.asyncUpdate(sql.formatted(SELECTED_TITLE_TABLE), result -> {
            if (redis.isEnabled()) {
                redis.setCache("selected:" + uuid, titleName != null ? titleName : "null");
                redis.publish("title-select:" + uuid + ":" + (titleName != null ? titleName : "none"));
            }
            
            future.complete(null);
        }, params);
        
        return future;
    }
    
    /**
     * 칭호 보유 여부 확인 (비동기)
     */
    public CompletableFuture<Boolean> hasTitle(UUID uuid, String titleName) {
        return loadPlayerTitles(uuid).thenApply(titles -> titles.containsKey(titleName));
    }
}
