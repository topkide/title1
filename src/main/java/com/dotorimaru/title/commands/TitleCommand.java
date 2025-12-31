package com.dotorimaru.title.commands;

import com.dotorimaru.title.TitlePlugin;
import com.dotorimaru.title.gui.TitleGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /칭호 명령어 - GUI 열기
 */
public class TitleCommand implements CommandExecutor {
    
    private final TitlePlugin plugin;
    
    public TitleCommand(TitlePlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessage("no-permission"));
            return true;
        }
        
        // 칭호 GUI 열기
        TitleGUI gui = new TitleGUI(plugin, player);
        gui.open();
        
        return true;
    }
}
