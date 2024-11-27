package com.bird.immortal.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

public class RealmTabCompleter implements TabCompleter {
    private static final List<String> COMMANDS = Arrays.asList(
        "info", "查詢", "top", "排行", "breakthrough", "突破"
    );
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterCompletions(COMMANDS, args[0]);
        } 
        else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("info") || args[0].equals("查詢")) {
                return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
        }
        
        return new ArrayList<>();
    }
    
    private List<String> filterCompletions(List<String> completions, String input) {
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
            .collect(Collectors.toList());
    }
} 