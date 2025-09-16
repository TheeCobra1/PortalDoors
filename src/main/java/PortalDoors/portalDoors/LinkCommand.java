package PortalDoors.portalDoors;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class LinkCommand implements CommandExecutor, TabCompleter {
    private final PortalDoors plugin;
    private final DoorManager doorManager;

    public LinkCommand(PortalDoors plugin, DoorManager doorManager) {
        this.plugin = plugin;
        this.doorManager = doorManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            Block targetBlock = player.getTargetBlockExact(5);
            if (targetBlock == null || !DoorManager.isDoor(targetBlock.getType())) {
                player.sendMessage(ChatColor.RED + "Look at a door to link!");
                return true;
            }

            Location doorLoc = targetBlock.getLocation();

            if (doorManager.isLinked(doorLoc)) {
                player.sendMessage(ChatColor.RED + "✗ This door is already linked! Use /door unlink first.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return true;
            }

            if (doorManager.hasPendingLink(player.getUniqueId())) {
                if (doorManager.completeLink(player.getUniqueId(), doorLoc)) {
                    player.sendMessage(ChatColor.GREEN + "✓ Doors linked successfully!");
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                    Location pLoc = doorLoc.clone().add(0.5, 1, 0.5);
                    doorLoc.getWorld().spawnParticle(Particle.WAX_ON, pLoc, 30, 0.5, 0.5, 0.5, 0);
                    doorLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, pLoc, 20, 0.4, 0.4, 0.4, 0.05);
                } else {
                    player.sendMessage(ChatColor.RED + "✗ Failed to link doors. One may already be linked.");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
                }
            } else {
                doorManager.startLinking(player.getUniqueId(), doorLoc);
                player.sendMessage(ChatColor.YELLOW + "⚡ Click another door to complete the link!");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                doorLoc.getWorld().spawnParticle(Particle.END_ROD, doorLoc.clone().add(0.5, 1, 0.5), 10, 0.3, 0.5, 0.3, 0.05);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("unlink")) {
            Block targetBlock = player.getTargetBlockExact(5);
            if (targetBlock == null || !DoorManager.isDoor(targetBlock.getType())) {
                player.sendMessage(ChatColor.RED + "Look at a door to unlink!");
                return true;
            }

            if (!doorManager.isLinked(targetBlock.getLocation())) {
                player.sendMessage(ChatColor.RED + "This door is not linked!");
                return true;
            }
            
            boolean isOwner = doorManager.isOwner(targetBlock.getLocation(), player.getUniqueId());
            if (!isOwner && !player.hasPermission("portaldoors.unlink.others")) {
                player.sendMessage(ChatColor.RED + "✗ You can only unlink doors you own!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return true;
            }

            doorManager.unlinkDoor(targetBlock.getLocation());
            if (isOwner) {
                player.sendMessage(ChatColor.GREEN + "✓ Your door has been unlinked!");
            } else {
                player.sendMessage(ChatColor.GREEN + "✓ Door unlinked with permission!");
            }
            player.playSound(player.getLocation(), Sound.BLOCK_CHAIN_BREAK, 1.0f, 1.0f);
            targetBlock.getWorld().spawnParticle(Particle.SMOKE, targetBlock.getLocation().add(0.5, 1, 0.5), 20, 0.3, 0.5, 0.3, 0.1);
            return true;
        }

        if (args[0].equalsIgnoreCase("cancel")) {
            if (doorManager.hasPendingLink(player.getUniqueId())) {
                doorManager.cancelPendingLink(player.getUniqueId());
                player.sendMessage(ChatColor.YELLOW + "Link cancelled.");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
            } else {
                player.sendMessage(ChatColor.RED + "You have no pending link!");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("info")) {
            Block targetBlock = player.getTargetBlockExact(5);
            if (targetBlock == null || !DoorManager.isDoor(targetBlock.getType())) {
                player.sendMessage(ChatColor.RED + "Look at a door to check!");
                return true;
            }

            if (doorManager.isLinked(targetBlock.getLocation())) {
                Location linked = doorManager.getLinkedDoor(targetBlock.getLocation());
                java.util.UUID owner = doorManager.getOwner(targetBlock.getLocation());
                String ownerName = owner != null ? plugin.getServer().getOfflinePlayer(owner).getName() : "Unknown";
                
                player.sendMessage(ChatColor.AQUA + "⬆ Door is linked to: " + 
                    ChatColor.WHITE + linked.getWorld().getName() + " " +
                    linked.getBlockX() + ", " + linked.getBlockY() + ", " + linked.getBlockZ());
                player.sendMessage(ChatColor.GRAY + "  Owner: " + ChatColor.WHITE + ownerName);
                
                targetBlock.getWorld().spawnParticle(Particle.PORTAL, targetBlock.getLocation().add(0.5, 1, 0.5), 15, 0.3, 0.5, 0.3, 0.02);
                targetBlock.getWorld().spawnParticle(Particle.ENCHANT, targetBlock.getLocation().add(0.5, 1, 0.5), 10, 0.3, 0.5, 0.3, 0);
            } else {
                player.sendMessage(ChatColor.GRAY + "This door is not linked.");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("network")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /door network <create|add|remove|list|info> [name]");
                return true;
            }
            
            if (args[1].equalsIgnoreCase("create")) {
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /door network create <name>");
                    return true;
                }
                String networkName = args[2];
                if (doorManager.getNetworkByName(networkName) != null) {
                    player.sendMessage(ChatColor.RED + "✗ Network '" + networkName + "' already exists!");
                    return true;
                }
                doorManager.createNetwork(networkName, player.getUniqueId());
                player.sendMessage(ChatColor.GREEN + "✓ Created network: " + networkName);
                player.sendMessage(ChatColor.YELLOW + "Use /door network add " + networkName + " while looking at doors to add them");
                return true;
            }
            
            if (args[1].equalsIgnoreCase("add")) {
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /door network add <name>");
                    return true;
                }
                String networkName = args[2];
                DoorNetwork network = doorManager.getNetworkByName(networkName);
                if (network == null) {
                    player.sendMessage(ChatColor.RED + "✗ Network '" + networkName + "' doesn't exist!");
                    return true;
                }
                
                Block targetBlock = player.getTargetBlockExact(5);
                if (targetBlock == null || !DoorManager.isDoor(targetBlock.getType())) {
                    player.sendMessage(ChatColor.RED + "Look at a door to add to the network!");
                    return true;
                }
                
                if (doorManager.addDoorToNetwork(networkName, targetBlock.getLocation(), player.getUniqueId())) {
                    player.sendMessage(ChatColor.GREEN + "✓ Door added to network: " + networkName);
                    targetBlock.getWorld().spawnParticle(Particle.WAX_ON, targetBlock.getLocation().add(0.5, 1, 0.5), 20, 0.3, 0.5, 0.3, 0);
                } else {
                    player.sendMessage(ChatColor.RED + "✗ Cannot add door - it may already be linked or you lack permission!");
                }
                return true;
            }
            
            if (args[1].equalsIgnoreCase("list")) {
                Map<String, DoorNetwork> allNetworks = doorManager.getAllNetworks();
                if (allNetworks.isEmpty()) {
                    player.sendMessage(ChatColor.GRAY + "No networks exist yet.");
                    return true;
                }
                player.sendMessage(ChatColor.AQUA + "=== Portal Networks ===");
                for (DoorNetwork network : allNetworks.values()) {
                    String access = network.isPublic() ? "Public" : (network.hasAccess(player.getUniqueId()) ? "Access" : "No Access");
                    String type = network.isOneWay() ? " [One-Way]" : "";
                    player.sendMessage(ChatColor.WHITE + "• " + network.getName() + type + ChatColor.GRAY + " (" + network.getDoors().size() + " doors, " + access + ")");
                }
                return true;
            }
            
            if (args[1].equalsIgnoreCase("public")) {
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /door network public <name>");
                    return true;
                }
                String networkName = args[2];
                DoorNetwork network = doorManager.getNetworkByName(networkName);
                if (network == null) {
                    player.sendMessage(ChatColor.RED + "✗ Network '" + networkName + "' doesn't exist!");
                    return true;
                }
                if (!network.getOwner().equals(player.getUniqueId()) && !player.hasPermission("portaldoors.network.admin")) {
                    player.sendMessage(ChatColor.RED + "✗ You can only modify networks you own!");
                    return true;
                }
                network.setPublic(!network.isPublic());
                player.sendMessage(ChatColor.GREEN + "✓ Network '" + networkName + "' is now " + (network.isPublic() ? "public" : "private"));
                return true;
            }
            
            if (args[1].equalsIgnoreCase("oneway")) {
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /door network oneway <name>");
                    return true;
                }
                String networkName = args[2];
                DoorNetwork network = doorManager.getNetworkByName(networkName);
                if (network == null) {
                    player.sendMessage(ChatColor.RED + "✗ Network '" + networkName + "' doesn't exist!");
                    return true;
                }
                if (!network.getOwner().equals(player.getUniqueId()) && !player.hasPermission("portaldoors.network.admin")) {
                    player.sendMessage(ChatColor.RED + "✗ You can only modify networks you own!");
                    return true;
                }
                network.setOneWay(!network.isOneWay());
                player.sendMessage(ChatColor.GREEN + "✓ Network '" + networkName + "' is now " + (network.isOneWay() ? "one-way" : "two-way"));
                if (network.isOneWay()) {
                    player.sendMessage(ChatColor.YELLOW + "Set entry door with: /door network entry " + networkName);
                }
                return true;
            }
            
            if (args[1].equalsIgnoreCase("entry")) {
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /door network entry <name>");
                    return true;
                }
                String networkName = args[2];
                DoorNetwork network = doorManager.getNetworkByName(networkName);
                if (network == null) {
                    player.sendMessage(ChatColor.RED + "✗ Network '" + networkName + "' doesn't exist!");
                    return true;
                }
                if (!network.getOwner().equals(player.getUniqueId()) && !player.hasPermission("portaldoors.network.admin")) {
                    player.sendMessage(ChatColor.RED + "✗ You can only modify networks you own!");
                    return true;
                }
                if (!network.isOneWay()) {
                    player.sendMessage(ChatColor.RED + "✗ Network must be one-way to set an entry door!");
                    return true;
                }
                
                Block targetBlock = player.getTargetBlockExact(5);
                if (targetBlock == null || !DoorManager.isDoor(targetBlock.getType())) {
                    player.sendMessage(ChatColor.RED + "Look at a door in the network to set as entry!");
                    return true;
                }
                
                if (!network.containsDoor(targetBlock.getLocation())) {
                    player.sendMessage(ChatColor.RED + "✗ This door is not in the network!");
                    return true;
                }
                
                network.setEntryDoor(targetBlock.getLocation());
                player.sendMessage(ChatColor.GREEN + "✓ Entry door set for network: " + networkName);
                return true;
            }
            
            if (args[1].equalsIgnoreCase("access")) {
                if (args.length < 4) {
                    player.sendMessage(ChatColor.RED + "Usage: /door network access <name> <player>");
                    return true;
                }
                String networkName = args[2];
                DoorNetwork network = doorManager.getNetworkByName(networkName);
                if (network == null) {
                    player.sendMessage(ChatColor.RED + "✗ Network '" + networkName + "' doesn't exist!");
                    return true;
                }
                if (!network.getOwner().equals(player.getUniqueId()) && !player.hasPermission("portaldoors.network.admin")) {
                    player.sendMessage(ChatColor.RED + "✗ You can only modify networks you own!");
                    return true;
                }
                
                org.bukkit.OfflinePlayer target = plugin.getServer().getOfflinePlayer(args[3]);
                if (!target.hasPlayedBefore() && !target.isOnline()) {
                    player.sendMessage(ChatColor.RED + "✗ Player '" + args[3] + "' not found!");
                    return true;
                }
                
                boolean currentAccess = network.hasAccess(target.getUniqueId());
                network.setAccess(target.getUniqueId(), !currentAccess);
                player.sendMessage(ChatColor.GREEN + "✓ " + target.getName() + " now " + (!currentAccess ? "has" : "doesn't have") + " access to network: " + networkName);
                return true;
            }
            
            player.sendMessage(ChatColor.RED + "Usage: /door network <create|add|remove|list|public|oneway|entry|access> [name]");
            return true;
        }

        if (args[0].equalsIgnoreCase("name")) {
            Block targetBlock = player.getTargetBlockExact(5);
            if (targetBlock == null || !DoorManager.isDoor(targetBlock.getType())) {
                player.sendMessage(ChatColor.RED + "Look at a door to name!");
                return true;
            }
            
            if (!doorManager.isLinked(targetBlock.getLocation()) && !doorManager.isInNetwork(targetBlock.getLocation())) {
                player.sendMessage(ChatColor.RED + "✗ Door must be linked or in a network to be named!");
                return true;
            }
            
            boolean isOwner = doorManager.isOwner(targetBlock.getLocation(), player.getUniqueId());
            if (!isOwner && !player.hasPermission("portaldoors.name.others")) {
                player.sendMessage(ChatColor.RED + "✗ You can only name doors you own!");
                return true;
            }
            
            if (args.length < 2) {
                String currentName = doorManager.getDoorName(targetBlock.getLocation());
                if (currentName != null) {
                    player.sendMessage(ChatColor.AQUA + "Current name: " + ChatColor.WHITE + currentName);
                    player.sendMessage(ChatColor.GRAY + "Use /door name <name> to change it or /door name clear to remove");
                } else {
                    player.sendMessage(ChatColor.GRAY + "This door has no name. Use /door name <name> to set one");
                }
                return true;
            }
            
            if (args[1].equalsIgnoreCase("clear")) {
                doorManager.setDoorName(targetBlock.getLocation(), null);
                player.sendMessage(ChatColor.GREEN + "✓ Door name cleared!");
                return true;
            }
            
            StringBuilder nameBuilder = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                if (i > 1) nameBuilder.append(" ");
                nameBuilder.append(args[i]);
            }
            String name = ChatColor.translateAlternateColorCodes('&', nameBuilder.toString());
            
            doorManager.setDoorName(targetBlock.getLocation(), name);
            player.sendMessage(ChatColor.GREEN + "✓ Door named: " + name);
            return true;
        }

        player.sendMessage(ChatColor.RED + "Usage: /door [unlink|cancel|info|network|name]");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = Arrays.asList("unlink", "cancel", "info", "network", "name");
            List<String> matches = new ArrayList<>();
            for (String s : completions) {
                if (s.toLowerCase().startsWith(args[0].toLowerCase())) {
                    matches.add(s);
                }
            }
            return matches;
        }
        
        if (args.length == 2 && args[0].equalsIgnoreCase("network")) {
            List<String> completions = Arrays.asList("create", "add", "remove", "list", "public", "oneway", "entry", "access");
            List<String> matches = new ArrayList<>();
            for (String s : completions) {
                if (s.toLowerCase().startsWith(args[1].toLowerCase())) {
                    matches.add(s);
                }
            }
            return matches;
        }
        
        if (args.length == 3 && args[0].equalsIgnoreCase("network") && 
            (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove") || 
             args[1].equalsIgnoreCase("public") || args[1].equalsIgnoreCase("oneway") || 
             args[1].equalsIgnoreCase("entry") || args[1].equalsIgnoreCase("access"))) {
            List<String> matches = new ArrayList<>();
            for (String networkName : doorManager.getAllNetworks().keySet()) {
                if (networkName.toLowerCase().startsWith(args[2].toLowerCase())) {
                    matches.add(networkName);
                }
            }
            return matches;
        }
        
        if (args.length == 4 && args[0].equalsIgnoreCase("network") && args[1].equalsIgnoreCase("access")) {
            List<String> matches = new ArrayList<>();
            for (org.bukkit.entity.Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                if (onlinePlayer.getName().toLowerCase().startsWith(args[3].toLowerCase())) {
                    matches.add(onlinePlayer.getName());
                }
            }
            return matches;
        }
        
        if (args.length == 2 && args[0].equalsIgnoreCase("name")) {
            if (args[1].toLowerCase().startsWith("clear".toLowerCase())) {
                return Arrays.asList("clear");
            }
        }
        
        return new ArrayList<>();
    }
}