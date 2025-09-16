package PortalDoors.portalDoors;

import org.bukkit.plugin.java.JavaPlugin;

public final class PortalDoors extends JavaPlugin {
    private DoorManager doorManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        doorManager = new DoorManager(this);
        
        getServer().getPluginManager().registerEvents(new DoorLinkListener(this, doorManager), this);
        
        LinkCommand linkCommand = new LinkCommand(this, doorManager);
        getCommand("door").setExecutor(linkCommand);
        getCommand("door").setTabCompleter(linkCommand);
        
        getLogger().info("PortalDoors has been enabled!");
    }

    @Override
    public void onDisable() {
        if (doorManager != null) {
            doorManager.saveData();
        }
        
        getLogger().info("PortalDoors has been disabled!");
    }
}
