package com.dotorimaru.title.managers;

import com.dotorimaru.core.builder.ItemBuilder;
import com.dotorimaru.title.TitlePlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 칭호북 생성 및 검증 매니저
 */
public class TitleBookManager {
    
    private final TitlePlugin plugin;
    private final NamespacedKey titleNameKey;
    
    public TitleBookManager(TitlePlugin plugin) {
        this.plugin = plugin;
        this.titleNameKey = new NamespacedKey(plugin, "title_name");
    }
    
    /**
     * 칭호북 생성
     * 
     * @param titleName 칭호 이름 (색상 코드 포함)
     * @return 칭호북 아이템
     */
    public ItemStack createTitleBook(String titleName) {
        Material material = Material.valueOf(
            plugin.getConfig().getString("title-book.material", "BOOK")
        );
        
        int customModelData = plugin.getConfig().getInt("title-book.custom-model-data", 0);
        String displayName = plugin.getConfig().getString("title-book.display-name", "&6&l칭호북");
        List<String> loreTemplate = plugin.getConfig().getStringList("title-book.lore");
        
        // 색상 처리된 칭호 이름
        String coloredTitle = plugin.colorize(titleName);
        
        // Lore 생성 ({title} 치환)
        List<String> lore = loreTemplate.stream()
                .map(line -> line.replace("{title}", coloredTitle))
                .collect(Collectors.toList());
        
        ItemBuilder builder = new ItemBuilder(material)
                .setDisplayName(plugin.colorize(displayName))
                .setLore(lore);
        
        // 커스텀 모델 데이터 (0이 아닌 경우에만)
        if (customModelData != 0) {
            builder.setCustomModelData(customModelData);
        }
        
        // PDC에 칭호 이름 저장
        ItemStack book = builder.build();
        var meta = book.getItemMeta();
        
        if (meta != null) {
            meta.getPersistentDataContainer().set(
                titleNameKey, 
                PersistentDataType.STRING, 
                titleName
            );
            book.setItemMeta(meta);
        } else {
            plugin.getLogger().severe("칭호북 생성 실패: ItemMeta가 null");
        }
        
        return book;
    }
    
    /**
     * 아이템이 칭호북인지 확인
     */
    public boolean isTitleBook(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        return item.getItemMeta().getPersistentDataContainer()
                .has(titleNameKey, PersistentDataType.STRING);
    }
    
    /**
     * 칭호북에서 칭호 이름 추출
     * 
     * @param item 칭호북 아이템
     * @return 칭호 이름 (없으면 null)
     */
    public String getTitleName(ItemStack item) {
        if (!isTitleBook(item)) {
            return null;
        }
        
        return item.getItemMeta().getPersistentDataContainer()
                .get(titleNameKey, PersistentDataType.STRING);
    }
}
