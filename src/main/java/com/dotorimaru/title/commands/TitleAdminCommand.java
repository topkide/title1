package com.dotorimaru.title.commands;

import com.dotorimaru.title.TitlePlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

/**
 * /칭호관리 명령어 - 관리자 전용
 */
public class TitleAdminCommand implements CommandExecutor, TabCompleter {
    
    private final TitlePlugin plugin;
    
    public TitleAdminCommand(TitlePlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 권한 확인
        if (!sender.hasPermission("title.admin")) {
            sender.sendMessage(plugin.getMessage("no-permission"));
            return true;
        }
        
        // 인자 없음
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        // 리로드 명령어
        if (args[0].equalsIgnoreCase("리로드") || args[0].equalsIgnoreCase("reload")) {
            handleReload(sender);
            return true;
        }
        
        // 알 수 없는 명령어
        sendHelp(sender);
        return true;
    }
    
    /**
     * 리로드 처리
     */
    private void handleReload(CommandSender sender) {
        try {
            // Config 리로드
            plugin.reloadConfig();
            
            sender.sendMessage("§a[칭호] 설정 파일이 리로드되었습니다!");
            plugin.getLogger().info(sender.getName() + "이(가) 칭호 설정을 리로드했습니다.");
            
        } catch (Exception e) {
            sender.sendMessage("§c[칭호] 리로드 중 오류가 발생했습니다: " + e.getMessage());
            plugin.getLogger().severe("리로드 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 도움말 표시
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§8§m                                    ");
        sender.sendMessage("§6§l칭호 관리자 명령어");
        sender.sendMessage("");
        sender.sendMessage("§e/칭호관리 리로드 §7- 설정 파일 리로드");
        sender.sendMessage("§8§m                                    ");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!sender.hasPermission("title.admin")) {
            return completions;
        }
        
        if (args.length == 1) {
            completions.add("리로드");
            completions.add("reload");
        }
        
        return completions;
    }
}
