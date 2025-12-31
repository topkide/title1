package com.dotorimaru.title.placeholders;

import com.dotorimaru.title.TitlePlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * PlaceholderAPI 확장
 * 
 * 사용 가능한 플레이스홀더:
 * - %titlesystem_title% : 착용 중인 칭호 (색상 적용)
 * - %titlesystem_title_raw% : 착용 중인 칭호 (색상 코드 그대로)
 * - %titlesystem_title_count% : 보유 칭호 개수
 */
public class TitlePlaceholder extends PlaceholderExpansion {
    
    private final TitlePlugin plugin;
    
    public TitlePlaceholder(TitlePlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public @NotNull String getIdentifier() {
        return "titlesystem";
    }
    
    @Override
    public @NotNull String getAuthor() {
        return "명노준";
    }
    
    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean canRegister() {
        return true; // 항상 등록 가능
    }
    
    /**
     * 구버전 PlaceholderAPI 호환
     */
    @Override
    public String onRequest(org.bukkit.OfflinePlayer offlinePlayer, @NotNull String params) {
        if (offlinePlayer == null || !offlinePlayer.isOnline()) {
            return "";
        }
        return onPlaceholderRequest(offlinePlayer.getPlayer(), params);
    }
    
    /**
     * 신버전 PlaceholderAPI 호환
     */
    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) {
            return "";
        }
        
        switch (identifier) {
            case "title":
                return getSelectedTitleColored(player);
            case "title_raw":
                return getSelectedTitleRaw(player);
            case "title_count":
                return getTitleCount(player);
            default:
                return null;
        }
    }
    
    /**
     * 착용 중인 칭호 (색상 적용)
     */
    private String getSelectedTitleColored(Player player) {
        String titleName = plugin.getTitleManager().getSelectedTitleSync(player.getUniqueId());
        
        if (titleName == null || titleName.isEmpty()) {
            return "";
        }
        
        // [칭호] 형식 - 대괄호는 항상 흰색
        return "§f[" + plugin.colorize(titleName) + "§f] ";
    }
    
    /**
     * 착용 중인 칭호 (색상 코드 그대로)
     */
    private String getSelectedTitleRaw(Player player) {
        String titleName = plugin.getTitleManager().getSelectedTitleSync(player.getUniqueId());
        return titleName == null ? "" : titleName;
    }
    
    /**
     * 보유 칭호 개수
     */
    private String getTitleCount(Player player) {
        try {
            CompletableFuture<Integer> future = plugin.getTitleManager()
                    .getTitleCount(player.getUniqueId());
            
            Integer count = future.get(100, TimeUnit.MILLISECONDS);
            
            return String.valueOf(count == null ? 0 : count);
            
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            return "0";
        }
    }
}
