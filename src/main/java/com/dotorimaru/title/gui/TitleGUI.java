package com.dotorimaru.title.gui;

import com.dotorimaru.core.builder.ItemBuilder;
import com.dotorimaru.title.TitlePlugin;
import com.dotorimaru.title.models.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 칭호 GUI (54칸)
 */
public class TitleGUI {
    
    private final TitlePlugin plugin;
    private final Player player;
    private final NamespacedKey guiKey;
    private final NamespacedKey titleKey;
    
    public TitleGUI(TitlePlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.guiKey = new NamespacedKey(plugin, "title_gui");
        this.titleKey = new NamespacedKey(plugin, "title");
    }
    
    /**
     * GUI 열기
     */
    public void open() {
        plugin.getTitleManager().getTitles(player.getUniqueId()).thenAccept(titlesMap -> {
            plugin.getTitleManager().getSelectedTitle(player.getUniqueId()).thenAccept(selectedTitle -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    // Map을 List로 변환
                    List<Title> titles = new ArrayList<>(titlesMap.values());
                    Inventory inv = createInventory(titles, selectedTitle);
                    player.openInventory(inv);
                });
            }).exceptionally(ex -> {
                plugin.getLogger().severe("칭호 조회 실패: " + ex.getMessage());
                return null;
            });
        }).exceptionally(ex -> {
            plugin.getLogger().severe("칭호 목록 로드 실패: " + ex.getMessage());
            player.sendMessage("§c칭호 GUI를 여는 중 오류가 발생했습니다.");
            return null;
        });
    }
    
    /**
     * 인벤토리 생성
     */
    private Inventory createInventory(List<Title> titles, String selectedTitle) {
        int size = plugin.getConfig().getInt("gui.size", 54);
        String titleTemplate = plugin.getConfig().getString("gui.title", "&6&l내 칭호 목록 &7({current}/{max})");
        
        String guiTitle = plugin.colorize(titleTemplate
                .replace("{current}", String.valueOf(titles.size()))
                .replace("{max}", "54"));
        
        Inventory inv = Bukkit.createInventory(null, size, guiTitle);
        
        // 칭호가 없는 경우
        if (titles.isEmpty()) {
            return inv;
        }
        
        // 칭호 아이템 추가
        for (int i = 0; i < Math.min(titles.size(), size); i++) {
            Title title = titles.get(i);
            boolean isSelected = title.getTitleName().equals(selectedTitle);
            
            ItemStack item = createTitleItem(title, isSelected);
            inv.setItem(i, item);
        }
        
        return inv;
    }
    
    /**
     * 칭호 아이템 생성
     */
    private ItemStack createTitleItem(Title title, boolean isSelected) {
        String titleName = title.getTitleName();
        String coloredTitle = plugin.colorize(titleName);
        
        // Config에서 아이템 설정 로드
        String section = isSelected ? "gui.selected-item" : "gui.unselected-item";
        Material material = Material.valueOf(plugin.getConfig().getString(section + ".material", "PAPER"));
        String displayNameTemplate = plugin.getConfig().getString(section + ".display-name", coloredTitle);
        List<String> loreTemplate = plugin.getConfig().getStringList(section + ".lore");
        
        // DisplayName 생성
        String displayName = plugin.colorize(displayNameTemplate.replace("{title}", coloredTitle));
        
        // Lore 생성
        List<String> lore = loreTemplate.stream()
                .map(line -> plugin.colorize(line.replace("{title}", coloredTitle)))
                .toList();
        
        // 아이템 빌더
        ItemBuilder builder = new ItemBuilder(material)
                .setDisplayName(displayName)
                .setLore(lore);
        
        // 선택된 칭호는 인챈트 효과
        if (isSelected && plugin.getConfig().getBoolean(section + ".enchant-glow", true)) {
            builder.addEnchantment(Enchantment.LUCK_OF_THE_SEA, 1)
                   .addItemFlag(ItemFlag.HIDE_ENCHANTS);
        }
        
        ItemStack item = builder.build();
        
        // PDC에 GUI 마커와 칭호 이름 저장 (같은 meta 객체 사용!)
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(guiKey, PersistentDataType.STRING, "title_gui");
            meta.getPersistentDataContainer().set(titleKey, PersistentDataType.STRING, titleName);
            item.setItemMeta(meta);
            
            plugin.getLogger().info("🔍 [DEBUG] 칭호 아이템 PDC 설정 완료: " + titleName);
        } else {
            plugin.getLogger().severe("❌ [DEBUG] ItemMeta가 null!");
        }
        
        return item;
    }
}
