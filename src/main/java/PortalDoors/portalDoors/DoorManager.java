package PortalDoors.portalDoors;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

public class DoorManager {
    private final PortalDoors plugin;
    private final Map<Location, Location> doorLinks = new HashMap<>();
    private final Map<Location, UUID> doorOwners = new HashMap<>();
    private final Map<UUID, Location> pendingLinks = new HashMap<>();
    private final Map<String, DoorNetwork> networks = new HashMap<>();
    private final Map<Location, String> doorNetworks = new HashMap<>();
    private final Map<UUID, String> pendingNetworkAdditions = new HashMap<>();
    private final Map<Location, String> doorNames = new HashMap<>();
    private File dataFile;
    private FileConfiguration dataConfig;

    public DoorManager(PortalDoors plugin) {
        this.plugin = plugin;
        loadData();
    }

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "doors.yml");
        if (!dataFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        
        if (dataConfig.contains("doors")) {
            for (String key : dataConfig.getConfigurationSection("doors").getKeys(false)) {
                String[] parts = key.split(",");
                Location from = new Location(
                    plugin.getServer().getWorld(parts[0]),
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3])
                );
                
                String toPath = "doors." + key;
                Location to = new Location(
                    plugin.getServer().getWorld(dataConfig.getString(toPath + ".world")),
                    dataConfig.getInt(toPath + ".x"),
                    dataConfig.getInt(toPath + ".y"),
                    dataConfig.getInt(toPath + ".z"),
                    (float) dataConfig.getDouble(toPath + ".yaw"),
                    (float) dataConfig.getDouble(toPath + ".pitch")
                );
                
                doorLinks.put(from, to);
                
                if (dataConfig.contains(toPath + ".owner")) {
                    String ownerString = dataConfig.getString(toPath + ".owner");
                    if (ownerString != null && !ownerString.isEmpty()) {
                        doorOwners.put(from, UUID.fromString(ownerString));
                    }
                }
            }
        }
        
        if (dataConfig.contains("networks")) {
            for (String networkName : dataConfig.getConfigurationSection("networks").getKeys(false)) {
                String path = "networks." + networkName;
                String ownerString = dataConfig.getString(path + ".owner");
                if (ownerString != null) {
                    DoorNetwork network = new DoorNetwork(networkName, UUID.fromString(ownerString));
                    network.setPublic(dataConfig.getBoolean(path + ".public", false));
                    network.setOneWay(dataConfig.getBoolean(path + ".oneway", false));
                    
                    if (dataConfig.contains(path + ".entry")) {
                        Location entry = new Location(
                            plugin.getServer().getWorld(dataConfig.getString(path + ".entry.world")),
                            dataConfig.getInt(path + ".entry.x"),
                            dataConfig.getInt(path + ".entry.y"),
                            dataConfig.getInt(path + ".entry.z")
                        );
                        network.setEntryDoor(entry);
                    }
                    
                    if (dataConfig.contains(path + ".doors")) {
                        for (String doorKey : dataConfig.getConfigurationSection(path + ".doors").getKeys(false)) {
                            String[] parts = doorKey.split(",");
                            Location doorLoc = new Location(
                                plugin.getServer().getWorld(parts[0]),
                                Integer.parseInt(parts[1]),
                                Integer.parseInt(parts[2]),
                                Integer.parseInt(parts[3])
                            );
                            network.addDoor(doorLoc);
                            doorNetworks.put(doorLoc, networkName.toLowerCase());
                            doorNetworks.put(doorLoc.clone().add(0, 1, 0), networkName.toLowerCase());
                        }
                    }
                    
                    if (dataConfig.contains(path + ".access")) {
                        for (String playerUuid : dataConfig.getConfigurationSection(path + ".access").getKeys(false)) {
                            network.setAccess(UUID.fromString(playerUuid), dataConfig.getBoolean(path + ".access." + playerUuid));
                        }
                    }
                    
                    networks.put(networkName.toLowerCase(), network);
                }
            }
        }
        
        if (dataConfig.contains("doorNames")) {
            for (String key : dataConfig.getConfigurationSection("doorNames").getKeys(false)) {
                String[] parts = key.split(",");
                Location doorLoc = new Location(
                    plugin.getServer().getWorld(parts[0]),
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3])
                );
                String name = dataConfig.getString("doorNames." + key);
                if (name != null) {
                    doorNames.put(doorLoc, name);
                    doorNames.put(doorLoc.clone().add(0, 1, 0), name);
                }
            }
        }
    }

    public void saveData() {
        dataConfig.set("doors", null);
        dataConfig.set("networks", null);
        dataConfig.set("doorNames", null);
        
        for (Map.Entry<Location, Location> entry : doorLinks.entrySet()) {
            Location from = entry.getKey();
            Location to = entry.getValue();
            
            String key = from.getWorld().getName() + "," + from.getBlockX() + "," + from.getBlockY() + "," + from.getBlockZ();
            dataConfig.set("doors." + key + ".world", to.getWorld().getName());
            dataConfig.set("doors." + key + ".x", to.getBlockX());
            dataConfig.set("doors." + key + ".y", to.getBlockY());
            dataConfig.set("doors." + key + ".z", to.getBlockZ());
            dataConfig.set("doors." + key + ".yaw", to.getYaw());
            dataConfig.set("doors." + key + ".pitch", to.getPitch());
            
            UUID owner = doorOwners.get(from);
            if (owner != null) {
                dataConfig.set("doors." + key + ".owner", owner.toString());
            }
        }
        
        for (Map.Entry<String, DoorNetwork> networkEntry : networks.entrySet()) {
            DoorNetwork network = networkEntry.getValue();
            String path = "networks." + network.getName();
            
            dataConfig.set(path + ".owner", network.getOwner().toString());
            dataConfig.set(path + ".public", network.isPublic());
            dataConfig.set(path + ".oneway", network.isOneWay());
            
            if (network.getEntryDoor() != null) {
                Location entry = network.getEntryDoor();
                dataConfig.set(path + ".entry.world", entry.getWorld().getName());
                dataConfig.set(path + ".entry.x", entry.getBlockX());
                dataConfig.set(path + ".entry.y", entry.getBlockY());
                dataConfig.set(path + ".entry.z", entry.getBlockZ());
            }
            
            for (Location door : network.getDoors()) {
                String doorKey = door.getWorld().getName() + "," + door.getBlockX() + "," + door.getBlockY() + "," + door.getBlockZ();
                dataConfig.set(path + ".doors." + doorKey, true);
            }
            
            for (Map.Entry<UUID, Boolean> accessEntry : network.getAccessList().entrySet()) {
                dataConfig.set(path + ".access." + accessEntry.getKey().toString(), accessEntry.getValue());
            }
        }
        
        for (Map.Entry<Location, String> nameEntry : doorNames.entrySet()) {
            Location loc = nameEntry.getKey();
            if (loc.getBlock().getRelative(0, -1, 0).getType().name().endsWith("_DOOR")) {
                continue;
            }
            String key = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
            dataConfig.set("doorNames." + key, nameEntry.getValue());
        }
        
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startLinking(UUID player, Location door) {
        pendingLinks.put(player, door);
    }

    public boolean completeLink(UUID player, Location door2) {
        if (!pendingLinks.containsKey(player)) {
            return false;
        }
        
        Location door1 = pendingLinks.remove(player);
        
        if (isLinked(door1) || isLinked(door2)) {
            return false;
        }
        
        linkDoors(door1, door2, player);
        return true;
    }

    public void linkDoors(Location door1, Location door2, UUID owner) {
        Location door1Bottom = getBottomDoorBlock(door1);
        Location door2Bottom = getBottomDoorBlock(door2);
        
        Location teleport1 = calculateTeleportLocation(door2Bottom);
        Location teleport2 = calculateTeleportLocation(door1Bottom);
        
        doorLinks.put(door1Bottom, teleport1);
        doorLinks.put(door2Bottom, teleport2);
        
        Location door1Top = door1Bottom.clone().add(0, 1, 0);
        Location door2Top = door2Bottom.clone().add(0, 1, 0);
        
        doorLinks.put(door1Top, teleport1);
        doorLinks.put(door2Top, teleport2);
        
        doorOwners.put(door1Bottom, owner);
        doorOwners.put(door1Top, owner);
        doorOwners.put(door2Bottom, owner);
        doorOwners.put(door2Top, owner);
        
        saveData();
    }

    private Location calculateTeleportLocation(Location doorLoc) {
        Block doorBlock = doorLoc.getBlock();
        Location teleportLoc = doorLoc.clone().add(0.5, 0, 0.5);
        
        if (doorBlock.getBlockData() instanceof org.bukkit.block.data.type.Door) {
            org.bukkit.block.data.type.Door door = (org.bukkit.block.data.type.Door) doorBlock.getBlockData();
            org.bukkit.block.BlockFace facing = door.getFacing();
            
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
        
        return teleportLoc;
    }

    public void unlinkDoor(Location door) {
        Location bottom = getBottomDoorBlock(door);
        Location top = bottom.clone().add(0, 1, 0);
        
        Location linkedLocation = doorLinks.get(bottom);
        if (linkedLocation != null) {
            Block linkedBlock = linkedLocation.getBlock();
            Location linkedDoorLoc = null;
            
            if (DoorManager.isDoor(linkedBlock.getType())) {
                linkedDoorLoc = linkedBlock.getLocation();
            } else if (DoorManager.isDoor(linkedBlock.getRelative(0, -1, 0).getType())) {
                linkedDoorLoc = linkedBlock.getRelative(0, -1, 0).getLocation();
            } else if (DoorManager.isDoor(linkedBlock.getRelative(0, 1, 0).getType())) {
                linkedDoorLoc = linkedBlock.getRelative(0, 1, 0).getLocation();
            }
            
            if (linkedDoorLoc != null) {
                Location linkedBottom = getBottomDoorBlock(linkedDoorLoc);
                Location linkedTop = linkedBottom.clone().add(0, 1, 0);
                doorLinks.remove(linkedBottom);
                doorLinks.remove(linkedTop);
                doorOwners.remove(linkedBottom);
                doorOwners.remove(linkedTop);
            }
        }
        
        doorLinks.remove(bottom);
        doorLinks.remove(top);
        doorOwners.remove(bottom);
        doorOwners.remove(top);
        saveData();
    }

    public Location getLinkedDoor(Location door) {
        return doorLinks.get(door);
    }

    public boolean isLinked(Location door) {
        Location bottom = getBottomDoorBlock(door);
        return doorLinks.containsKey(bottom) || doorLinks.containsKey(door);
    }

    public boolean hasPendingLink(UUID player) {
        return pendingLinks.containsKey(player);
    }

    public void cancelPendingLink(UUID player) {
        pendingLinks.remove(player);
    }

    private Location getBottomDoorBlock(Location loc) {
        Block block = loc.getBlock();
        if (isDoor(block.getType())) {
            if (block.getRelative(0, -1, 0).getType() == block.getType()) {
                return block.getRelative(0, -1, 0).getLocation();
            }
        }
        return loc;
    }

    public boolean isOwner(Location door, UUID player) {
        Location bottom = getBottomDoorBlock(door);
        UUID owner = doorOwners.get(bottom);
        if (owner == null) {
            owner = doorOwners.get(door);
        }
        return owner != null && owner.equals(player);
    }
    
    public UUID getOwner(Location door) {
        Location bottom = getBottomDoorBlock(door);
        UUID owner = doorOwners.get(bottom);
        if (owner == null) {
            owner = doorOwners.get(door);
        }
        return owner;
    }
    
    public static boolean isDoor(Material material) {
        return material.name().endsWith("_DOOR") && !material.name().contains("TRAP");
    }

    public void createNetwork(String name, UUID owner) {
        if (!networks.containsKey(name.toLowerCase())) {
            networks.put(name.toLowerCase(), new DoorNetwork(name, owner));
        }
    }

    public boolean addDoorToNetwork(String networkName, Location door, UUID player) {
        DoorNetwork network = networks.get(networkName.toLowerCase());
        if (network == null) return false;
        
        if (!network.hasAccess(player) && !network.getOwner().equals(player)) {
            return false;
        }
        
        Location bottom = getBottomDoorBlock(door);
        if (isLinked(bottom) || isInNetwork(bottom)) {
            return false;
        }
        
        network.addDoor(bottom);
        doorNetworks.put(bottom, networkName.toLowerCase());
        doorNetworks.put(bottom.clone().add(0, 1, 0), networkName.toLowerCase());
        doorOwners.put(bottom, network.getOwner());
        doorOwners.put(bottom.clone().add(0, 1, 0), network.getOwner());
        saveData();
        return true;
    }

    public void removeDoorFromNetwork(Location door) {
        Location bottom = getBottomDoorBlock(door);
        String networkName = doorNetworks.get(bottom);
        if (networkName != null) {
            DoorNetwork network = networks.get(networkName);
            if (network != null) {
                network.removeDoor(bottom);
                if (network.getDoors().isEmpty()) {
                    networks.remove(networkName);
                }
            }
            doorNetworks.remove(bottom);
            doorNetworks.remove(bottom.clone().add(0, 1, 0));
            doorOwners.remove(bottom);
            doorOwners.remove(bottom.clone().add(0, 1, 0));
        }
        saveData();
    }

    public boolean isInNetwork(Location door) {
        Location bottom = getBottomDoorBlock(door);
        return doorNetworks.containsKey(bottom);
    }

    public DoorNetwork getNetwork(Location door) {
        Location bottom = getBottomDoorBlock(door);
        String networkName = doorNetworks.get(bottom);
        return networkName != null ? networks.get(networkName) : null;
    }

    public DoorNetwork getNetworkByName(String name) {
        return networks.get(name.toLowerCase());
    }

    public Map<String, DoorNetwork> getAllNetworks() {
        return new HashMap<>(networks);
    }

    public boolean hasPendingNetworkAddition(UUID player) {
        return pendingNetworkAdditions.containsKey(player);
    }

    public void setPendingNetworkAddition(UUID player, String networkName) {
        pendingNetworkAdditions.put(player, networkName);
    }

    public String getPendingNetworkAddition(UUID player) {
        return pendingNetworkAdditions.get(player);
    }

    public void cancelPendingNetworkAddition(UUID player) {
        pendingNetworkAdditions.remove(player);
    }

    public void setDoorName(Location door, String name) {
        Location bottom = getBottomDoorBlock(door);
        if (name == null || name.trim().isEmpty()) {
            doorNames.remove(bottom);
            doorNames.remove(bottom.clone().add(0, 1, 0));
        } else {
            doorNames.put(bottom, name);
            doorNames.put(bottom.clone().add(0, 1, 0), name);
        }
        saveData();
    }

    public String getDoorName(Location door) {
        Location bottom = getBottomDoorBlock(door);
        return doorNames.get(bottom);
    }

    public boolean hasDoorName(Location door) {
        Location bottom = getBottomDoorBlock(door);
        return doorNames.containsKey(bottom);
    }

    public Map<Location, String> getAllDoorNames() {
        return new HashMap<>(doorNames);
    }
}