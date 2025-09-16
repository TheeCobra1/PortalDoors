package PortalDoors.portalDoors;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.ChatColor;

import java.util.*;

public class DoorNetwork {
    private final String networkName;
    private final UUID owner;
    private final Set<Location> doors;
    private final Map<UUID, Boolean> accessList;
    private boolean isPublic;
    private boolean isOneWay;
    private Location entryDoor;

    public DoorNetwork(String name, UUID owner) {
        this.networkName = name;
        this.owner = owner;
        this.doors = new HashSet<>();
        this.accessList = new HashMap<>();
        this.isPublic = false;
        this.isOneWay = false;
        this.entryDoor = null;
    }

    public void addDoor(Location door) {
        doors.add(getBottomDoorBlock(door));
        if (entryDoor == null && isOneWay) {
            entryDoor = getBottomDoorBlock(door);
        }
    }

    public void removeDoor(Location door) {
        Location bottom = getBottomDoorBlock(door);
        doors.remove(bottom);
        if (bottom.equals(entryDoor)) {
            entryDoor = null;
        }
    }

    public boolean containsDoor(Location door) {
        return doors.contains(getBottomDoorBlock(door));
    }

    public Set<Location> getDoors() {
        return new HashSet<>(doors);
    }

    public List<Location> getDestinations(Location fromDoor) {
        if (isOneWay && !getBottomDoorBlock(fromDoor).equals(entryDoor)) {
            return new ArrayList<>();
        }
        
        List<Location> destinations = new ArrayList<>();
        Location from = getBottomDoorBlock(fromDoor);
        for (Location door : doors) {
            if (!door.equals(from)) {
                destinations.add(door);
            }
        }
        return destinations;
    }

    public boolean hasAccess(UUID player) {
        if (player.equals(owner)) return true;
        if (isPublic) return true;
        return accessList.getOrDefault(player, false);
    }

    public void setAccess(UUID player, boolean hasAccess) {
        accessList.put(player, hasAccess);
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public void setOneWay(boolean oneWay) {
        this.isOneWay = oneWay;
    }

    public void setEntryDoor(Location door) {
        this.entryDoor = getBottomDoorBlock(door);
    }

    public String getName() {
        return networkName;
    }

    public UUID getOwner() {
        return owner;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public boolean isOneWay() {
        return isOneWay;
    }

    public Location getEntryDoor() {
        return entryDoor;
    }
    
    public Map<UUID, Boolean> getAccessList() {
        return new HashMap<>(accessList);
    }

    private Location getBottomDoorBlock(Location loc) {
        if (loc.getBlock().getType().name().endsWith("_DOOR")) {
            if (loc.getBlock().getRelative(0, -1, 0).getType() == loc.getBlock().getType()) {
                return loc.getBlock().getRelative(0, -1, 0).getLocation();
            }
        }
        return loc;
    }

    public Inventory createDestinationGUI(Player player, Location fromDoor) {
        List<Location> destinations = getDestinations(fromDoor);
        int size = Math.min(54, ((destinations.size() / 9) + 1) * 9);
        Inventory gui = player.getServer().createInventory(null, size, ChatColor.DARK_PURPLE + "Portal Network: " + networkName);
        
        for (int i = 0; i < destinations.size() && i < 54; i++) {
            Location dest = destinations.get(i);
            ItemStack doorItem = new ItemStack(Material.OAK_DOOR);
            ItemMeta meta = doorItem.getItemMeta();
            
            String doorName = "Door at " + dest.getBlockX() + ", " + dest.getBlockY() + ", " + dest.getBlockZ();
            String worldName = dest.getWorld().getName();
            
            meta.setDisplayName(ChatColor.AQUA + doorName);
            meta.setLore(Arrays.asList(
                ChatColor.GRAY + "World: " + ChatColor.WHITE + worldName,
                ChatColor.GRAY + "Click to teleport"
            ));
            
            doorItem.setItemMeta(meta);
            gui.setItem(i, doorItem);
        }
        
        return gui;
    }
}