package org.strassburger.lifestealz.util.uuids;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.strassburger.lifestealz.LifeStealZ;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public class UUIDFile {
    private final JavaPlugin plugin = LifeStealZ.getInstance();
    private File uuidFile;
    private FileConfiguration uuidConfig;

    public UUIDFile() {
        createUUIDFile();
    }

    private void createUUIDFile() {
        uuidFile = new File(plugin.getDataFolder(), "player_uuids.yml");

        if (!uuidFile.exists()) {
            uuidFile.getParentFile().mkdirs();
            plugin.saveResource("player_uuids.yml", false);
        }

        uuidConfig = YamlConfiguration.loadConfiguration(uuidFile);
    }

    public void savePlayer(UUID uuid, String name) {
        uuidConfig.set("players." + uuid.toString() + ".name", name);
        saveConfig();
    }

    public String getPlayerName(UUID uuid) {
        return uuidConfig.getString("players." + uuid.toString() + ".name");
    }

    public UUID getPlayerUUID(String name) {
        if(name == null) return null;
        ConfigurationSection playersSection = uuidConfig.getConfigurationSection("players");
        if (playersSection == null) return null;
        Set<String> keys = playersSection.getKeys(false);
        for (String uuidString : keys) {
            String storedName = uuidConfig.getString("players." + uuidString + ".name");
            if (storedName != null && storedName.equalsIgnoreCase(name)) {
                return UUID.fromString(uuidString);
            }
        }
        return null;
    }

    private void saveConfig() {
        try {
            uuidConfig.save(uuidFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isPlayerStored(UUID uuid) {
        return uuidConfig.contains("players." + uuid.toString());
    }

    public boolean isPlayerStored(String name) {
        if(getPlayerUUID(name) == null) return false;
        else return true;
    }
}

