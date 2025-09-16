package PortalDoors.portalDoors;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.type.Door;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.List;

public class DoorLinkListener implements Listener {
    private final PortalDoors plugin;
    private final DoorManager doorManager;
    private final Set<UUID> recentlyTeleported = new HashSet<>();
    private final Map<UUID, Location> lastValidLocation = new HashMap<>();
    private final Map<UUID, Long> lastTeleportTime = new HashMap<>();
    private BukkitTask particleTask;

    public DoorLinkListener(PortalDoors plugin, DoorManager doorManager) {
        this.plugin = plugin;
        this.doorManager = doorManager;
        startParticleTask();
    }

    private void startParticleTask() {
        particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (World world : Bukkit.getWorlds()) {
                    for (Player player : world.getPlayers()) {
                        Location loc = player.getLocation();
                        int radius = 16;
                        
                        for (int x = -radius; x <= radius; x++) {
                            for (int y = -radius; y <= radius; y++) {
                                for (int z = -radius; z <= radius; z++) {
                                    Block block = loc.getWorld().getBlockAt(
                                        loc.getBlockX() + x,
                                        loc.getBlockY() + y,
                                        loc.getBlockZ() + z
                                    );
                                    
                                    if (DoorManager.isDoor(block.getType()) && (doorManager.isLinked(block.getLocation()) || doorManager.isInNetwork(block.getLocation()))) {
                                        if (block.getBlockData() instanceof Openable) {
                                            Openable openable = (Openable) block.getBlockData();
                                            if (openable.isOpen()) {
                                                if (block.getBlockData() instanceof Bisected) {
                                                    Bisected bisected = (Bisected) block.getBlockData();
                                                    if (bisected.getHalf() == Bisected.Half.BOTTOM) {
                                                        Location particleLoc = block.getLocation().add(0.5, 1, 0.5);
                                                        if (doorManager.isInNetwork(block.getLocation())) {
                                                            block.getWorld().spawnParticle(Particle.PORTAL, particleLoc, 3, 0.2, 0.3, 0.2, 0);
                                                            block.getWorld().spawnParticle(Particle.ENCHANT, particleLoc, 2, 0.3, 0.3, 0.3, 0);
                                                            block.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 1, 0.2, 0.2, 0.2, 0.01);
                                                        } else {
                                                            block.getWorld().spawnParticle(Particle.PORTAL, particleLoc, 2, 0.2, 0.3, 0.2, 0);
                                                            block.getWorld().spawnParticle(Particle.ENCHANT, particleLoc, 1, 0.3, 0.3, 0.3, 0);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 10L);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        Location to = event.getTo();
        Block block = to.getBlock();
        
        lastValidLocation.put(playerId, event.getFrom().clone());
        
        if (!DoorManager.isDoor(block.getType())) {
            return;
        }

        if (recentlyTeleported.contains(playerId)) {
            return;
        }
        
        if (!player.hasPermission("portaldoors.bypass.cooldown")) {
            Long lastTeleport = lastTeleportTime.get(playerId);
            double cooldown = plugin.getConfig().getDouble("teleport.cooldown", 1.0) * 1000;
            if (lastTeleport != null && System.currentTimeMillis() - lastTeleport < cooldown) {
                return;
            }
        }

        DoorNetwork network = doorManager.getNetwork(block.getLocation());
        if (network != null) {
            if (!network.hasAccess(playerId)) {
                player.sendMessage(ChatColor.RED + "✗ You don't have access to this network!");
                return;
            }
            
            if (block.getBlockData() instanceof Openable) {
                Openable openable = (Openable) block.getBlockData();
                if (!openable.isOpen()) {
                    return;
                }
            }
            
            List<Location> destinations = network.getDestinations(block.getLocation());
            if (destinations.isEmpty()) {
                if (network.isOneWay()) {
                    player.sendMessage(ChatColor.RED + "✗ This is an exit-only door in a one-way network!");
                }
                return;
            }
            
            if (destinations.size() == 1) {
                teleportToNetworkDoor(player, destinations.get(0));
            } else {
                player.openInventory(network.createDestinationGUI(player, block.getLocation()));
            }
            return;
        }

        Location linkedDoor = doorManager.getLinkedDoor(block.getLocation());
        if (linkedDoor == null) {
            return;
        }

        if (block.getBlockData() instanceof Openable) {
            Openable openable = (Openable) block.getBlockData();
            if (!openable.isOpen()) {
                return;
            }
        }

        recentlyTeleported.add(playerId);
        lastTeleportTime.put(playerId, System.currentTimeMillis());
        
        Location teleportLoc = linkedDoor.clone();
        
        player.setVelocity(new Vector(0, 0, 0));
        
        new BukkitRunnable() {
            @Override
            public void run() {
                player.teleport(teleportLoc, PlayerTeleportEvent.TeleportCause.PLUGIN);
                player.setVelocity(new Vector(0, 0, 0));
                player.setFallDistance(0);
                
                player.playSound(teleportLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                
                Location particleLoc = teleportLoc.clone().add(0, 1, 0);
                particleLoc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, particleLoc, 50, 0.3, 0.8, 0.3, 0.02);
                particleLoc.getWorld().spawnParticle(Particle.ENCHANT, particleLoc, 30, 0.4, 0.6, 0.4, 0.1);
                particleLoc.getWorld().spawnParticle(Particle.WITCH, particleLoc, 15, 0.2, 0.4, 0.2, 0);
                
                lastValidLocation.put(playerId, teleportLoc.clone());
            }
        }.runTask(plugin);

        new BukkitRunnable() {
            @Override
            public void run() {
                recentlyTeleported.remove(playerId);
            }
        }.runTaskLater(plugin, 40L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN) {
            Player player = event.getPlayer();
            lastValidLocation.put(player.getUniqueId(), event.getTo().clone());
        }
    }

    @EventHandler
    public void onDoorInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || !DoorManager.isDoor(block.getType())) {
            return;
        }

        Location doorLoc = block.getLocation();
        boolean isLinked = doorManager.isLinked(doorLoc);
        boolean isInNetwork = doorManager.isInNetwork(doorLoc);

        if (block.getBlockData() instanceof Openable) {
            Openable openable = (Openable) block.getBlockData();
            
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (openable.isOpen() && (isLinked || isInNetwork)) {
                        Location particleLoc = doorLoc.clone().add(0.5, 1, 0.5);
                        if (isInNetwork) {
                            doorLoc.getWorld().spawnParticle(Particle.ENCHANT, particleLoc, 30, 0.3, 0.6, 0.3, 0);
                            doorLoc.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 10, 0.2, 0.4, 0.2, 0.02);
                            doorLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particleLoc, 20, 0.4, 0.5, 0.4, 0.01);
                            doorLoc.getWorld().spawnParticle(Particle.WAX_ON, particleLoc, 5, 0.3, 0.3, 0.3, 0);
                        } else {
                            doorLoc.getWorld().spawnParticle(Particle.ENCHANT, particleLoc, 25, 0.3, 0.6, 0.3, 0);
                            doorLoc.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 8, 0.2, 0.4, 0.2, 0.02);
                            doorLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particleLoc, 15, 0.4, 0.5, 0.4, 0.01);
                        }
                        doorLoc.getWorld().playSound(doorLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.2f);
                        doorLoc.getWorld().playSound(doorLoc, Sound.BLOCK_BEACON_AMBIENT, 0.3f, 1.5f);
                    }
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!DoorManager.isDoor(block.getType())) {
            return;
        }

        Location doorLoc = block.getLocation();
        if (doorManager.isLinked(doorLoc)) {
            Player player = event.getPlayer();
            
            boolean isOwner = doorManager.isOwner(doorLoc, player.getUniqueId());
            
            if (!isOwner && !player.hasPermission("portaldoors.break.linked")) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "✗ You cannot break linked doors that you don't own!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                doorLoc.getWorld().spawnParticle(Particle.FLAME, doorLoc.clone().add(0.5, 1, 0.5), 1, 0, 0, 0, 0);
                return;
            }
            
            if (isOwner) {
                player.sendMessage(ChatColor.YELLOW + "⚠ Breaking your linked door - connection removed!");
            } else {
                player.sendMessage(ChatColor.YELLOW + "⚠ Breaking linked door with permission - connection removed!");
            }
            player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.8f);
            doorLoc.getWorld().spawnParticle(Particle.BLOCK, doorLoc.clone().add(0.5, 1, 0.5), 30, 0.3, 0.5, 0.3, 0.1, block.getBlockData());
            
            doorManager.unlinkDoor(doorLoc);
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        Iterator<Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (DoorManager.isDoor(block.getType()) && doorManager.isLinked(block.getLocation())) {
                if (!plugin.getConfig().getBoolean("allow-explosion-break-linked", false)) {
                    iterator.remove();
                } else {
                    doorManager.unlinkDoor(block.getLocation());
                }
            }
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        Iterator<Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (DoorManager.isDoor(block.getType()) && doorManager.isLinked(block.getLocation())) {
                if (!plugin.getConfig().getBoolean("allow-explosion-break-linked", false)) {
                    iterator.remove();
                } else {
                    doorManager.unlinkDoor(block.getLocation());
                }
            }
        }
    }
    
    private void teleportToNetworkDoor(Player player, Location destination) {
        UUID playerId = player.getUniqueId();
        recentlyTeleported.add(playerId);
        lastTeleportTime.put(playerId, System.currentTimeMillis());
        
        Location teleportLoc = destination.clone().add(0.5, 0, 0.5);
        
        Block doorBlock = destination.getBlock();
        if (doorBlock.getBlockData() instanceof org.bukkit.block.data.type.Door) {
            org.bukkit.block.data.type.Door door = (org.bukkit.block.data.type.Door) doorBlock.getBlockData();
            BlockFace facing = door.getFacing();
            
            double offsetX = 0;
            double offsetZ = 0;
            float yaw = 0;
            
            switch (facing) {
                case NORTH:
                    offsetZ = 0.7;
                    yaw = 0;
                    break;
                case SOUTH:
                    offsetZ = -0.7;
                    yaw = 180;
                    break;
                case EAST:
                    offsetX = -0.7;
                    yaw = 90;
                    break;
                case WEST:
                    offsetX = 0.7;
                    yaw = -90;
                    break;
            }
            
            teleportLoc.add(offsetX, 0, offsetZ);
            teleportLoc.setYaw(yaw);
            teleportLoc.setPitch(0);
        }
        
        player.setVelocity(new Vector(0, 0, 0));
        
        new BukkitRunnable() {
            @Override
            public void run() {
                player.teleport(teleportLoc, PlayerTeleportEvent.TeleportCause.PLUGIN);
                player.setVelocity(new Vector(0, 0, 0));
                player.setFallDistance(0);
                
                player.playSound(teleportLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                
                Location particleLoc = teleportLoc.clone().add(0, 1, 0);
                particleLoc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, particleLoc, 50, 0.3, 0.8, 0.3, 0.02);
                particleLoc.getWorld().spawnParticle(Particle.ENCHANT, particleLoc, 30, 0.4, 0.6, 0.4, 0.1);
                particleLoc.getWorld().spawnParticle(Particle.WITCH, particleLoc, 15, 0.2, 0.4, 0.2, 0);
                
                lastValidLocation.put(playerId, teleportLoc.clone());
            }
        }.runTask(plugin);

        new BukkitRunnable() {
            @Override
            public void run() {
                recentlyTeleported.remove(playerId);
            }
        }.runTaskLater(plugin, 40L);
    }
}