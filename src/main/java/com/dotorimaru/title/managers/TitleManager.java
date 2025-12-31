package com.dotorimaru.title.managers;

import com.dotorimaru.title.TitlePlugin;
import com.dotorimaru.title.database.TitleRedisManager;
import com.dotorimaru.title.database.TitleStorage;
import com.dotorimaru.title.models.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 칭호 관리자 (비즈니스 로직)
 */
public class TitleManager {
    private final TitlePlugin plugin;
    private final TitleStorage storage;
    private final TitleRedisManager redis;
    
    // 로컬 캐시 (빠른 접근)
    private final Map<UUID, Map<String, Title>> titleCache = new ConcurrentHashMap<>();
    private final Map<UUID, String> selectedTitleCache = new ConcurrentHashMap<>();
    
    public TitleManager(TitlePlugin plugin, TitleStorage storage, TitleRedisManager redis) {
        this.plugin = plugin;
        this.storage = storage;
        this.redis = redis;
        setupRedisSync();
    }
    
    /**
     * Redis Pub/Sub 동기화 설정
     */
    private void setupRedisSync() {
        if (!redis.isEnabled()) {
            plugin.getLogger().warning("⚠️ Redis가 비활성화되어 멀티서버 동기화가 작동하지 않습니다.");
            return;
        }
        
        redis.addHandler(message -> {
            // 메시지 형식: title-add:{uuid}:{titleName}
            // title-remove:{uuid}:{titleName}
            // title-select:{uuid}:{titleName}
            
            String[] parts = message.split(":", 3);
            if (parts.length < 2) return;
            
            String action = parts[0];
            String uuidStr = parts[1];
            
            try {
                UUID uuid = UUID.fromString(uuidStr);
                
                // 캐시 무효화
                titleCache.remove(uuid);
                
                if ("title-select".equals(action)) {
                    selectedTitleCache.remove(uuid);
                }
                
                plugin.getLogger().fine("🔄 Title Redis 동기화: " + message);
                
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("잘못된 Title Redis 메시지: " + message);
            }
        });
        
        plugin.getLogger().info("✅ Title Redis 핸들러 등록 완료");
    }
    
    /**
     * 플레이어의 모든 칭호 로드
     */
    public CompletableFuture<Map<String, Title>> loadTitles(UUID uuid) {
        // 로컬 캐시 확인
        if (titleCache.containsKey(uuid)) {
            return CompletableFuture.completedFuture(titleCache.get(uuid));
        }
        
        // Storage에서 로드 (Redis → MySQL)
        return storage.loadPlayerTitles(uuid).thenApply(titles -> {
            titleCache.put(uuid, titles);
            return titles;
        });
    }
    
    /**
     * 칭호 추가
     */
    public CompletableFuture<Boolean> addTitle(UUID uuid, String titleName) {
        return storage.addTitle(uuid, titleName).thenApply(success -> {
            if (success) {
                // 로컬 캐시 무효화
                titleCache.remove(uuid);
                
                // 멀티서버 동기화
                if (redis.isEnabled()) {
                    redis.publish("title-add:" + uuid + ":" + titleName);
                }
            }
            return success;
        });
    }
    
    /**
     * 칭호 삭제 (영구 삭제, 칭호북 지급 없음)
     */
    public CompletableFuture<Boolean> deleteTitle(UUID uuid, String titleName) {
        return storage.deleteTitle(uuid, titleName).thenApply(success -> {
            if (success) {
                // 로컬 캐시 무효화
                titleCache.remove(uuid);
                
                // 선택된 칭호였다면 해제
                String selected = selectedTitleCache.get(uuid);
                if (titleName.equals(selected)) {
                    setSelectedTitle(uuid, null);
                }
                
                // 멀티서버 동기화
                if (redis.isEnabled()) {
                    redis.publish("title-remove:" + uuid + ":" + titleName);
                }
            }
            return success;
        });
    }
    
    /**
     * 선택된 칭호 조회 (비동기) - 기본 메서드
     */
    /**
     * 선택된 칭호 조회 (비동기)
     */
    public CompletableFuture<String> getSelectedTitle(UUID uuid) {
        // 로컬 캐시 확인
        if (selectedTitleCache.containsKey(uuid)) {
            String cached = selectedTitleCache.get(uuid);
            return CompletableFuture.completedFuture(cached.isEmpty() ? null : cached);
        }
        
        // Storage에서 비동기 로드
        return storage.getSelectedTitle(uuid).thenApply(title -> {
            // null은 빈 문자열로 저장 (ConcurrentHashMap은 null 불허)
            selectedTitleCache.put(uuid, title != null ? title : "");
            return title;
        }).exceptionally(ex -> {
            plugin.getLogger().severe("칭호 조회 실패: " + ex.getMessage());
            return null;
        });
    }
    
    /**
     * 선택된 칭호 조회 (동기, PlaceholderAPI용)
     */
    public String getSelectedTitleSync(UUID uuid) {
        String cached = selectedTitleCache.get(uuid);
        return (cached != null && !cached.isEmpty()) ? cached : null;
    }
    
    /**
     * 칭호 선택/해제
     */
    public CompletableFuture<Void> setSelectedTitle(UUID uuid, String titleName) {
        return storage.setSelectedTitle(uuid, titleName).thenRun(() -> {
            // 로컬 캐시 업데이트 (null은 빈 문자열로)
            selectedTitleCache.put(uuid, titleName != null ? titleName : "");
            
            // 멀티서버 동기화
            if (redis.isEnabled()) {
                redis.publish("title-select:" + uuid + ":" + (titleName != null ? titleName : "none"));
            }
        });
    }
    
    /**
     * 칭호 보유 여부 확인
     */
    public CompletableFuture<Boolean> hasTitle(UUID uuid, String titleName) {
        // 로컬 캐시 확인
        if (titleCache.containsKey(uuid)) {
            return CompletableFuture.completedFuture(titleCache.get(uuid).containsKey(titleName));
        }
        
        // Storage에서 확인
        return storage.hasTitle(uuid, titleName);
    }
    
    /**
     * 보유 칭호 개수 조회
     */
    public CompletableFuture<Integer> getTitleCount(UUID uuid) {
        return loadTitles(uuid).thenApply(Map::size);
    }
    
    /**
     * 칭호 포맷팅 (채팅 출력용)
     * Legacy (&) + RGB (#RRGGBB) 색상 지원
     * 칭호가 없으면 빈 문자열 반환 (대괄호 표시 안 함)
     */
    public String formatTitle(String titleName) {
        if (titleName == null || titleName.isEmpty()) {
            return ""; // 칭호 없으면 완전히 사라짐
        }
        
        // Legacy 색상 코드 변환 (&a → §a)
        String formatted = org.bukkit.ChatColor.translateAlternateColorCodes('&', titleName);
        
        // RGB 색상 코드 변환 (#RRGGBB → §x§R§R§G§G§B§B)
        formatted = translateHexColorCodes(formatted);
        
        // 대괄호로 감싸고 리셋 + 공백 추가
        return "[" + formatted + "§r] ";
    }
    
    /**
     * RGB 색상 코드 변환 (#RRGGBB → §x§R§R§G§G§B§B)
     */
    private String translateHexColorCodes(String message) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("#([A-Fa-f0-9]{6})");
        java.util.regex.Matcher matcher = pattern.matcher(message);
        
        StringBuffer buffer = new StringBuffer(message.length() + 4 * 8);
        while (matcher.find()) {
            String group = matcher.group(1);
            matcher.appendReplacement(buffer, "§x"
                + "§" + group.charAt(0) + "§" + group.charAt(1)
                + "§" + group.charAt(2) + "§" + group.charAt(3)
                + "§" + group.charAt(4) + "§" + group.charAt(5)
            );
        }
        return matcher.appendTail(buffer).toString();
    }
    
    /**
     * 플레이어 캐시 제거 (로그아웃 시)
     */
    public void removeCache(UUID uuid) {
        titleCache.remove(uuid);
        selectedTitleCache.remove(uuid);
    }
    
    /**
     * 전체 캐시 초기화
     */
    public void clearAllCache() {
        titleCache.clear();
        selectedTitleCache.clear();
        plugin.getLogger().info("✅ Title 캐시 초기화 완료");
    }
    
    // ========================================
    // 별칭 메서드들 (다른 클래스에서 사용)
    // ========================================
    
    /**
     * getTitles() - loadTitles()의 별칭 (GUI에서 사용)
     */
    public CompletableFuture<Map<String, Title>> getTitles(UUID uuid) {
        return loadTitles(uuid);
    }
    
    /**
     * 칭호가 최대 개수인지 확인 (54개)
     */
    public CompletableFuture<Boolean> isFull(UUID uuid) {
        return getTitleCount(uuid).thenApply(count -> count >= 54);
    }
    
    /**
     * removeTitle() - deleteTitle()의 별칭 (Listener에서 사용)
     */
    public CompletableFuture<Boolean> removeTitle(UUID uuid, String titleName) {
        return deleteTitle(uuid, titleName);
    }
    
    /**
     * 칭호 장착 - setSelectedTitle()의 별칭
     */
    public CompletableFuture<Void> equipTitle(UUID uuid, String titleName) {
        return setSelectedTitle(uuid, titleName);
    }
    
    /**
     * 칭호 해제 - setSelectedTitle(null)의 별칭
     */
    public CompletableFuture<Void> unequipTitle(UUID uuid) {
        return setSelectedTitle(uuid, null);
    }
}
