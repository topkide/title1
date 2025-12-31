package com.dotorimaru.title.listeners;

import com.dotorimaru.title.TitlePlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 칭호북 우클릭 감지 리스너
 */
public class TitleBookUseListener implements Listener {
    
    private final TitlePlugin plugin;
    
    public TitleBookUseListener(TitlePlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        // 우클릭 확인
        if (!event.getAction().name().contains("RIGHT_CLICK")) {
            return;
        }
        
        // 칭호북 확인
        if (item == null || !plugin.getTitleBookManager().isTitleBook(item)) {
            return;
        }
        
        event.setCancelled(true);
        
        // 칭호 이름 추출
        String titleName = plugin.getTitleBookManager().getTitleName(item);
        if (titleName == null) {
            return;
        }
        
        // 이미 보유 중인지 확인
        plugin.getTitleManager().hasTitle(player.getUniqueId(), titleName).thenAccept(hasTitle -> {
            if (hasTitle) {
                // 이미 보유 중
                String message = plugin.getMessage("already-owned")
                        .replace("{title}", plugin.colorize(titleName));
                player.sendMessage(message);
                return;
            }
            
            // 슬롯이 가득 찼는지 확인
            plugin.getTitleManager().isFull(player.getUniqueId()).thenAccept(isFull -> {
                if (isFull) {
                    // 슬롯 가득 참
                    player.sendMessage(plugin.getMessage("inventory-full"));
                    return;
                }
                
                // 칭호 추가
                plugin.getTitleManager().addTitle(player.getUniqueId(), titleName);
                
                // 칭호북 제거
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    player.getInventory().setItemInMainHand(null);
                }
                
                // 메시지 전송
                String message = plugin.getMessage("title-obtained")
                        .replace("{title}", plugin.colorize(titleName));
                player.sendMessage(message);
            });
        });
    }
}
