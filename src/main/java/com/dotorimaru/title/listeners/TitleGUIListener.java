package com.dotorimaru.title.listeners;

import com.dotorimaru.title.TitlePlugin;
import com.dotorimaru.title.gui.TitleGUI;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * 칭호 GUI 클릭 감지 리스너
 */
public class TitleGUIListener implements Listener {
    
    private final TitlePlugin plugin;
    private final NamespacedKey guiKey;
    private final NamespacedKey titleKey;
    
    public TitleGUIListener(TitlePlugin plugin) {
        this.plugin = plugin;
        this.guiKey = new NamespacedKey(plugin, "title_gui");
        this.titleKey = new NamespacedKey(plugin, "title");
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        // 칭호 GUI인지 제목으로 확인
        String title = event.getView().getTitle();
        
        // 제목에 "칭호"가 포함되어 있는지 확인
        if (!title.contains("칭호")) {
            return;
        }
        
        // 모든 클릭 취소 (아이템을 집지 못하도록)
        event.setCancelled(true);
        
        // 클릭한 아이템 확인
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
        }
        
        // 칭호 아이템인지 확인 (PDC에 title 키가 있는지)
        if (!clickedItem.getItemMeta().getPersistentDataContainer()
                .has(titleKey, PersistentDataType.STRING)) {
            return;
        }
        
        // 칭호 이름 추출
        String titleName = clickedItem.getItemMeta().getPersistentDataContainer()
                .get(titleKey, PersistentDataType.STRING);
        
        if (titleName == null) {
            return;
        }
        
        // 클릭 타입에 따른 처리
        if (event.isShiftClick() && event.isRightClick()) {
            // Shift + 우클릭 = 삭제
            handleDelete(player, titleName);
        } else if (event.isLeftClick()) {
            // 좌클릭 = 착용/해제
            handleEquip(player, titleName);
        }
    }
    
    /**
     * 칭호 삭제 처리
     */
    private void handleDelete(Player player, String titleName) {
        // 칭호 삭제
        plugin.getTitleManager().removeTitle(player.getUniqueId(), titleName);
        
        // 메시지 전송
        String message = plugin.getMessage("title-deleted")
                .replace("{title}", plugin.colorize(titleName));
        player.sendMessage(message);
        
        // GUI 새로고침
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            TitleGUI gui = new TitleGUI(plugin, player);
            gui.open();
        }, 2L);
    }
    
    /**
     * 칭호 착용/해제 처리
     */
    private void handleEquip(Player player, String titleName) {
        plugin.getTitleManager().getSelectedTitle(player.getUniqueId()).thenAccept(currentTitle -> {
            if (titleName.equals(currentTitle)) {
                // 이미 착용 중 → 해제
                plugin.getTitleManager().unequipTitle(player.getUniqueId());
                player.sendMessage(plugin.getMessage("title-unequipped"));
            } else {
                // 착용
                plugin.getTitleManager().equipTitle(player.getUniqueId(), titleName);
                String message = plugin.getMessage("title-equipped")
                        .replace("{title}", plugin.colorize(titleName));
                player.sendMessage(message);
            }
            
            // GUI 새로고침
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                TitleGUI gui = new TitleGUI(plugin, player);
                gui.open();
            }, 2L);
        });
    }
}
