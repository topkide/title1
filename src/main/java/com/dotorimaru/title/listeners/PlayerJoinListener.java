package com.dotorimaru.title.listeners;

import com.dotorimaru.title.TitlePlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * 플레이어 접속/종료 시 칭호 캐시 관리
 */
public class PlayerJoinListener implements Listener {
    
    private final TitlePlugin plugin;
    
    public PlayerJoinListener(TitlePlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        
        // 칭호 미리 로드 (캐시에 적재)
        plugin.getTitleManager().loadTitles(player.getUniqueId());
        plugin.getTitleManager().getSelectedTitle(player.getUniqueId());
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 캐시 정리
        plugin.getTitleManager().removeCache(event.getPlayer().getUniqueId());
    }
}
