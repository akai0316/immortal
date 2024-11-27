package com.bird.immortal.commands;

import com.bird.immortal.meditation.MeditationManager;
import com.bird.immortal.message.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MeditateCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageManager.getMessage("general.player_only"));
            return true;
        }

        Player player = (Player) sender;
        
        if (MeditationManager.isMeditating(player)) {
            MeditationManager.stopMeditation(player);
        } else {
            MeditationManager.startMeditation(player);
        }
        
        return true;
    }
} 