package com.bird.immortal.commands;

import com.bird.immortal.message.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import java.util.Map;

public class ImmortalCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        showDetailedHelp(sender);
        return true;
    }

    private void showDetailedHelp(CommandSender sender) {
        Map<String, String> placeholders = MessageManager.createPlaceholders();
        sender.sendMessage(MessageManager.getMessage("help.title"));
        sender.sendMessage("");
        
        // 境界相關指令
        sender.sendMessage(MessageManager.getMessage("help.realm_title"));
        sender.sendMessage(MessageManager.getMessage("help.realm_commands", placeholders));
        sender.sendMessage("");
        
        // 打坐相關指令
        sender.sendMessage(MessageManager.getMessage("help.meditation_title"));
        sender.sendMessage(MessageManager.getMessage("help.meditation_commands", placeholders));
        sender.sendMessage("");
    }
} 