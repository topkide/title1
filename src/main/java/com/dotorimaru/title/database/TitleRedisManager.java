package com.dotorimaru.title.database;

import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import redis.clients.jedis.*;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 칭호 시스템 전용 Redis 매니저 (Core와 독립)
 */
public class TitleRedisManager {
    private final JavaPlugin plugin;
    private JedisPool jedisPool;

    private final boolean enabled;
    private final String host;
    private final int port;
    private final String password;
    private final int database;
    @Getter
    private final int cacheTTL;
    private final String pubsubChannel;

    // 여러 핸들러 지원
    private final List<Consumer<String>> messageHandlers = new CopyOnWriteArrayList<>();
    private boolean subscribeStarted = false;

    public TitleRedisManager(JavaPlugin plugin) {
        this.plugin = plugin;

        // Config 로드
        this.enabled = plugin.getConfig().getBoolean("database.redis.enabled", true);
        this.host = plugin.getConfig().getString("database.redis.host", "localhost");
        this.port = plugin.getConfig().getInt("database.redis.port", 1659);
        this.password = plugin.getConfig().getString("database.redis.password", "081516");
        this.database = plugin.getConfig().getInt("database.redis.database", 0);
        this.cacheTTL = plugin.getConfig().getInt("database.redis.cache.ttl", 600);
        this.pubsubChannel = "title:sync";
    }

    /** Redis 연결 풀 초기화 */
    public void connect() {
        if (!enabled) {
            plugin.getLogger().warning("⚠️ Redis가 비활성화되어 있습니다. (캐싱 없이 작동)");
            return;
        }

        if (jedisPool != null && !jedisPool.isClosed()) {
            plugin.getLogger().warning("Redis가 이미 연결되어 있습니다.");
            return;
        }

        try {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(20);
            poolConfig.setMaxIdle(10);
            poolConfig.setMinIdle(2);
            poolConfig.setTestOnBorrow(true);
            poolConfig.setTestOnReturn(true);
            poolConfig.setTestWhileIdle(true);

            if (password != null && !password.isEmpty()) {
                jedisPool = new JedisPool(poolConfig, host, port, 2000, password, database);
            } else {
                jedisPool = new JedisPool(poolConfig, host, port, 2000);
            }

            // 연결 테스트
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.ping();
            }

            plugin.getLogger().info("✅ Title Redis 연결 성공! (%s:%d DB:%d)".formatted(host, port, database));

        } catch (Exception e) {
            plugin.getLogger().severe("❌ Title Redis 연결 실패: " + e.getMessage());
            plugin.getLogger().warning("⚠️ Redis 없이 작동합니다. (캐싱 비활성화)");
            jedisPool = null;
        }
    }

    /** Redis 연결 종료 */
    public void disconnect() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
            plugin.getLogger().info("✅ Title Redis 연결이 종료되었습니다.");
        }
    }

    /** Redis 활성화 여부 */
    public boolean isEnabled() {
        return enabled && jedisPool != null && !jedisPool.isClosed();
    }

    /** Jedis 리소스 가져오기 */
    public Jedis getResource() {
        if (!isEnabled()) return null;
        return jedisPool.getResource();
    }

    /** 캐시 키 생성 */
    public String getCacheKey(String key) {
        return "title:" + key;
    }

    /** 캐시 저장 (기본 TTL 사용) */
    public void setCache(String key, String value) {
        setCache(key, value, cacheTTL);
    }

    /** 캐시 저장 (커스텀 TTL 가능) */
    public void setCache(String key, String value, int ttlSeconds) {
        if (!isEnabled()) return;

        try (Jedis jedis = getResource()) {
            jedis.setex(getCacheKey(key), ttlSeconds, value);
        } catch (Exception e) {
            plugin.getLogger().warning("Redis 캐시 저장 실패: " + e.getMessage());
        }
    }

    /** 캐시 조회 */
    public String getCache(String key) {
        if (!isEnabled()) return null;

        try (Jedis jedis = getResource()) {
            return jedis.get(getCacheKey(key));
        } catch (Exception e) {
            plugin.getLogger().warning("Redis 캐시 조회 실패: " + e.getMessage());
            return null;
        }
    }

    /** 캐시 삭제 */
    public void deleteCache(String key) {
        if (!isEnabled()) return;

        try (Jedis jedis = getResource()) {
            jedis.del(getCacheKey(key));
        } catch (Exception e) {
            plugin.getLogger().warning("Redis 캐시 삭제 실패: " + e.getMessage());
        }
    }

    /** 안전한 SCAN 기반 패턴 캐시 삭제 */
    public void deleteCachePattern(String pattern) {
        if (!isEnabled()) return;

        try (Jedis jedis = getResource()) {
            String cursor = "0";
            String fullPattern = getCacheKey(pattern);
            do {
                ScanResult<String> scan = jedis.scan(cursor, new ScanParams().match(fullPattern).count(100));
                cursor = scan.getCursor();
                var keys = scan.getResult();
                if (!keys.isEmpty()) jedis.del(keys.toArray(new String[0]));
            } while (!cursor.equals("0"));
        } catch (Exception e) {
            plugin.getLogger().warning("Redis SCAN 캐시 삭제 실패: " + e.getMessage());
        }
    }

    /** Pub/Sub 메시지 발행 */
    public void publish(String message) {
        if (!isEnabled()) return;

        try (Jedis jedis = getResource()) {
            long subscribers = jedis.publish(pubsubChannel, message);
            plugin.getLogger().fine("📢 Title Redis Pub: '%s' → %d subscribers".formatted(message, subscribers));
        } catch (Exception e) {
            plugin.getLogger().warning("Redis Pub 실패: " + e.getMessage());
        }
    }

    /**
     * Pub/Sub 메시지 핸들러 추가
     */
    public synchronized void addHandler(Consumer<String> messageHandler) {
        if (!isEnabled()) {
            plugin.getLogger().warning("⚠️ Redis가 비활성화되어 있어 핸들러를 추가할 수 없습니다.");
            return;
        }

        messageHandlers.add(messageHandler);
        plugin.getLogger().info("✅ Title Redis 핸들러 추가됨 (총 %d개)".formatted(messageHandlers.size()));

        // 첫 번째 핸들러가 추가될 때 Pub/Sub 시작
        if (!subscribeStarted) {
            subscribeStarted = true;
            startSubscribe();
        }
    }

    /**
     * Pub/Sub 구독 시작 (비동기)
     */
    private void startSubscribe() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Jedis jedis = getResource()) {
                plugin.getLogger().info("🔧 Title Redis Pub/Sub 구독 시작: %s".formatted(pubsubChannel));

                jedis.subscribe(new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        // 메인 스레드에서 모든 핸들러 실행
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            plugin.getLogger().fine("📨 Title Redis 메시지 수신: '%s'".formatted(message));

                            for (Consumer<String> handler : messageHandlers) {
                                try {
                                    handler.accept(message);
                                } catch (Exception e) {
                                    plugin.getLogger().warning("Title Redis 핸들러 실행 중 오류: " + e.getMessage());
                                    e.printStackTrace();
                                }
                            }
                        });
                    }

                    @Override
                    public void onSubscribe(String channel, int subscribedChannels) {
                        plugin.getLogger().info("✅ Title Redis 구독 완료: %s".formatted(channel));
                    }

                    @Override
                    public void onUnsubscribe(String channel, int subscribedChannels) {
                        plugin.getLogger().info("🔕 Title Redis 구독 해제: %s".formatted(channel));
                    }
                }, pubsubChannel);

            } catch (Exception e) {
                plugin.getLogger().severe("❌ Title Redis Pub/Sub 오류: " + e.getMessage());
                e.printStackTrace();
                subscribeStarted = false; // 재시도 가능하도록
            }
        });
    }

    /** Pub/Sub 채널 이름 */
    public String getPubSubChannel() {
        return pubsubChannel;
    }

    /** 등록된 핸들러 개수 */
    public int getHandlerCount() {
        return messageHandlers.size();
    }
}
