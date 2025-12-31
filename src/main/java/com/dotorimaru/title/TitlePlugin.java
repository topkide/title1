package com.dotorimaru.title;

import com.dotorimaru.core.CorePlugin;
import com.dotorimaru.core.database.PlayerDataManager;
import com.dotorimaru.title.commands.TitleAdminCommand;
import com.dotorimaru.title.commands.TitleBookCommand;
import com.dotorimaru.title.commands.TitleCommand;
import com.dotorimaru.title.database.TitleMySQLManager;
import com.dotorimaru.title.database.TitleRedisManager;
import com.dotorimaru.title.database.TitleStorage;
import com.dotorimaru.title.listeners.PlayerJoinListener;
import com.dotorimaru.title.listeners.TitleBookUseListener;
import com.dotorimaru.title.listeners.TitleGUIListener;
import com.dotorimaru.title.managers.TitleBookManager;
import com.dotorimaru.title.managers.TitleManager;
import com.dotorimaru.title.placeholders.TitlePlaceholder;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

public final class TitlePlugin extends JavaPlugin {

    @Getter
    private static TitlePlugin instance;
    
    // Core 컴포넌트
    private CorePlugin core;
    @Getter
    private PlayerDataManager playerDataManager;
    
    // 독립 데이터베이스
    @Getter
    private TitleMySQLManager mySQLManager;
    @Getter
    private TitleRedisManager redisManager;
    @Getter
    private TitleStorage titleStorage;
    
    // 매니저
    @Getter
    private TitleManager titleManager;
    @Getter
    private TitleBookManager titleBookManager;

    @Override
    public void onEnable() {
        instance = this;
        
        // Core 플러그인 로드
        if (!loadCore()) {
            getLogger().severe("❌ Core 플러그인을 찾을 수 없습니다!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Config 로드
        saveDefaultConfig();
        
        // 독립 데이터베이스 초기화
        if (!initializeDatabase()) {
            getLogger().severe("❌ 데이터베이스 초기화 실패!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // 매니저 초기화
        initializeManagers();
        
        // 리스너 등록
        registerListeners();
        
        // 명령어 등록
        registerCommands();
        
        // PlaceholderAPI 등록
        registerPlaceholders();
        
        getLogger().info("✅ 칭호 시스템이 활성화되었습니다!");
    }

    private boolean loadCore() {
        try {
            core = JavaPlugin.getPlugin(CorePlugin.class);
            if (core == null) return false;
            
            playerDataManager = core.getPlayerDataManager();
            
            getLogger().info("✅ Core 플러그인 연동 완료");
            return true;
        } catch (Exception e) {
            getLogger().severe("❌ Core 플러그인 로드 실패: " + e.getMessage());
            return false;
        }
    }

    private boolean initializeDatabase() {
        try {
            // MySQL (독립)
            mySQLManager = new TitleMySQLManager(this);
            mySQLManager.connect();
            
            // Redis (독립)
            redisManager = new TitleRedisManager(this);
            redisManager.connect();
            
            // 통합 스토리지
            titleStorage = new TitleStorage(this, mySQLManager, redisManager);
            
            getLogger().info("✅ 독립 데이터베이스 연결 완료");
            return true;
        } catch (Exception e) {
            getLogger().severe("❌ 데이터베이스 연결 실패: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void initializeManagers() {
        // TitleStorage 생성 (MySQL + Redis)
        titleStorage = new TitleStorage(this, mySQLManager, redisManager);
        
        // TitleManager 생성
        titleManager = new TitleManager(this, titleStorage, redisManager);
        titleBookManager = new TitleBookManager(this);
        
        getLogger().info("✅ 매니저 초기화 완료");
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new TitleBookUseListener(this), this);
        getServer().getPluginManager().registerEvents(new TitleGUIListener(this), this);
        
        getLogger().info("✅ 리스너 등록 완료");
    }

    private void registerCommands() {
        getCommand("칭호").setExecutor(new TitleCommand(this));
        getCommand("칭호북").setExecutor(new TitleBookCommand(this));
        
        // 관리자 명령어 등록
        TitleAdminCommand adminCommand = new TitleAdminCommand(this);
        getCommand("칭호관리").setExecutor(adminCommand);
        getCommand("칭호관리").setTabCompleter(adminCommand);
        
        getLogger().info("✅ 명령어 등록 완료");
    }

    private void registerPlaceholders() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            TitlePlaceholder placeholder = new TitlePlaceholder(this);
            boolean registered = placeholder.register();
            
            if (registered) {
                getLogger().info("✅ PlaceholderAPI 연동 완료 (" + placeholder.getIdentifier() + ")");
            } else {
                getLogger().severe("❌ PlaceholderAPI 확장 등록 실패!");
            }
        } else {
            getLogger().warning("⚠️ PlaceholderAPI를 찾을 수 없습니다. 플레이스홀더 기능이 비활성화됩니다.");
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("🛑 칭호 시스템 종료 시작...");
        
        // 데이터베이스 연결 종료
        if (mySQLManager != null) {
            mySQLManager.disconnect();
        }
        if (redisManager != null) {
            redisManager.disconnect();
        }
        
        getLogger().info("✅ 칭호 시스템이 안전하게 종료되었습니다.");
    }
    
    /**
     * 메시지 전송 유틸리티
     */
    public String getMessage(String key) {
        String msg = getConfig().getString("messages." + key, key);
        String prefix = getConfig().getString("messages.prefix", "&8[&6칭호&8]");
        return colorize(msg.replace("%prefix%", prefix));
    }
    
    /**
     * 색상 코드 변환 (& → §, RGB 지원)
     */
    public String colorize(String text) {
        if (text == null) return "";
        
        // RGB 색상 지원: &#RRGGBB 형식을 §x§R§R§G§G§B§B로 변환
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("&#([A-Fa-f0-9]{6})");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        StringBuffer buffer = new StringBuffer();
        
        while (matcher.find()) {
            String hexCode = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            
            // 각 문자마다 § 추가
            for (char c : hexCode.toCharArray()) {
                replacement.append('§').append(c);
            }
            
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);
        text = buffer.toString();
        
        // Legacy 색상 코드: & → §
        text = text.replace('&', '§');
        
        return text;
    }
}
