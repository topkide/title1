package com.dotorimaru.title.commands;

import com.dotorimaru.title.TitlePlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * /칭호북 [칭호이름] 명령어 - OP 전용
 */
public class TitleBookCommand implements CommandExecutor {
    
    private final TitlePlugin plugin;
    
    public TitleBookCommand(TitlePlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // OP 권한 확인
        if (!sender.isOp()) {
            sender.sendMessage(plugin.getMessage("no-permission"));
            return true;
        }
        
        // 플레이어 확인
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessage("no-permission"));
            return true;
        }
        
        // 사용법 확인
        if (args.length == 0) {
            player.sendMessage(plugin.colorize("&c사용법: /칭호북 [칭호이름]"));
            player.sendMessage(plugin.colorize("&7예시: /칭호북 &c&l전설의 용사"));
            player.sendMessage(plugin.colorize("&7예시: /칭호북 #FF5733빨간칭호"));
            return true;
        }
        
        // 칭호 이름 조합 (띄어쓰기 포함)
        String titleName = String.join(" ", args);
        
        // 칭호북 생성
        ItemStack titleBook = plugin.getTitleBookManager().createTitleBook(titleName);
        
        // 인벤토리에 추가 (가득 차면 바닥에 드롭)
        var leftover = player.getInventory().addItem(titleBook);
        if (!leftover.isEmpty()) {
            // 인벤토리 가득 참 - 바닥에 드롭
            for (ItemStack item : leftover.values()) {
                player.getWorld().dropItem(player.getLocation(), item);
            }
            player.sendMessage(plugin.colorize("&e인벤토리가 가득 차서 칭호북을 바닥에 떨어뜨렸습니다."));
        }
        
        // 메시지 전송
        String message = plugin.getMessage("book-created")
                .replace("{title}", plugin.colorize(titleName));
        player.sendMessage(message);
        
        return true;
    }
}
