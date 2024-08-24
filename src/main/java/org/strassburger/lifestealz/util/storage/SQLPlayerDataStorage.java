package org.strassburger.lifestealz.util.storage;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.strassburger.lifestealz.LifeStealZ;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class SQLPlayerDataStorage implements PlayerDataStorage {
    private static final String CSV_SEPARATOR = ",";

    @Override
    public void init() {
        try (Connection connection = createConnection()) {
            if (connection == null) return;
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS hearts (uuid VARCHAR(36) PRIMARY KEY, name VARCHAR(255), maxhp REAL, hasbeenRevived INTEGER, craftedHearts INTEGER, craftedRevives INTEGER, killedOtherPlayers INTEGER)");
            } catch (SQLException e) {
                LifeStealZ.getInstance().getLogger().severe("Failed to initialize SQLite database: " + e.getMessage());
            }
        } catch (SQLException e) {
            LifeStealZ.getInstance().getLogger().severe("Failed to initialize SQLite database: " + e.getMessage());
        }
    }

    abstract Connection createConnection();

    @Override
    public void save(PlayerData playerData) {
        try (Connection connection = createConnection()) {
            if (connection == null) return;
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("INSERT OR REPLACE INTO hearts (uuid, name, maxhp, hasbeenRevived, craftedHearts, craftedRevives, killedOtherPlayers) VALUES ('" + playerData.getUuid() + "', '" + playerData.getName() + "', " + playerData.getMaxhp() + ", " + playerData.getHasbeenRevived() + ", " + playerData.getCraftedHearts() + ", " + playerData.getCraftedRevives() + ", " + playerData.getKilledOtherPlayers() + ")");
            } catch (SQLException e) {
                LifeStealZ.getInstance().getLogger().severe("Failed to save player data to SQLite database: " + e.getMessage());
            }
        } catch (SQLException e) {
            LifeStealZ.getInstance().getLogger().severe("Failed to save player data to SQLite database: " + e.getMessage());
        }
    }

    @Override
    public PlayerData load(UUID uuid) {
        try (Connection connection = createConnection()) {
            if (connection == null) return null;
            try (Statement statement = connection.createStatement()) {
                statement.setQueryTimeout(30);
                try (ResultSet resultSet = statement.executeQuery("SELECT * FROM hearts WHERE uuid = '" + uuid + "'")) {

                    if (!resultSet.next()) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player == null) return null;
                        PlayerData newPlayerData = new PlayerData(player.getName(), uuid);
                        save(newPlayerData);
                        return newPlayerData;
                    }

                    PlayerData playerData = new PlayerData(resultSet.getString("name"), uuid);
                    playerData.setMaxhp(resultSet.getDouble("maxhp"));
                    playerData.setHasbeenRevived(resultSet.getInt("hasbeenRevived"));
                    playerData.setCraftedHearts(resultSet.getInt("craftedHearts"));
                    playerData.setCraftedRevives(resultSet.getInt("craftedRevives"));
                    playerData.setKilledOtherPlayers(resultSet.getInt("killedOtherPlayers"));

                    return playerData;
                } catch (SQLException e) {
                    LifeStealZ.getInstance().getLogger().severe("Failed to load player data from SQLite database: " + e.getMessage());
                    return null;
                }
            } catch (SQLException e) {
                LifeStealZ.getInstance().getLogger().severe("Failed to load player data from SQLite database: " + e.getMessage());
                return null;
            }
        } catch (SQLException e) {
            LifeStealZ.getInstance().getLogger().severe("Failed to load player data from SQLite database: " + e.getMessage());
            return null;
        }
    }

    @Override
    public PlayerData load(String uuid) {
        return load(UUID.fromString(uuid));
    }

    @Override
    public List<UUID> getEliminatedPlayers() {
        List<UUID> eliminatedPlayers = new ArrayList<>();

        int minHearts = LifeStealZ.getInstance().getConfig().getInt("minHearts");

        try (Connection connection = createConnection()) {
            if (connection == null) return eliminatedPlayers;

            try (Statement statement = connection.createStatement()) {
                statement.setQueryTimeout(30);

                ResultSet resultSet = statement.executeQuery("SELECT uuid FROM hearts WHERE maxhp <= " + minHearts * 2 + ".0");

                while (resultSet.next()) {
                    eliminatedPlayers.add(UUID.fromString(resultSet.getString("uuid")));
                }
            } catch (SQLException e) {
                LifeStealZ.getInstance().getLogger().severe("Failed to load player data from SQLite database: " + e.getMessage());
            }
        } catch (SQLException e) {
            LifeStealZ.getInstance().getLogger().severe("Failed to load player data from SQLite database: " + e.getMessage());
        }

        return eliminatedPlayers;
    }


    @Override
    public String export(String fileName) {
        String filePath = LifeStealZ.getInstance().getDataFolder().getPath() + "/" + fileName + ".csv";
        System.out.println(filePath);
        try (Connection connection = createConnection()) {
            if (connection == null) return null;

            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery("SELECT * FROM hearts");

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
                    while (resultSet.next()) {
                        String line = resultSet.getString("uuid") + CSV_SEPARATOR +
                                resultSet.getString("name") + CSV_SEPARATOR +
                                resultSet.getDouble("maxhp") + CSV_SEPARATOR +
                                resultSet.getInt("hasbeenRevived") + CSV_SEPARATOR +
                                resultSet.getInt("craftedHearts") + CSV_SEPARATOR +
                                resultSet.getInt("craftedRevives") + CSV_SEPARATOR +
                                resultSet.getInt("killedOtherPlayers");
                        writer.write(line);
                        writer.newLine();
                    }
                }
            } catch (SQLException | IOException e) {
                LifeStealZ.getInstance().getLogger().severe("Failed to export player data to CSV file: " + e.getMessage());
                return null;
            }
        } catch (SQLException e) {
            LifeStealZ.getInstance().getLogger().severe("Failed to export player data to CSV file: " + e.getMessage());
            return null;
        }
        return filePath;
    }

    @Override
    public void importData(String fileName) {
        String filePath = LifeStealZ.getInstance().getDataFolder().getPath() + "/" + fileName;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] data = line.split(CSV_SEPARATOR);

                if (data.length != 7) {
                    LifeStealZ.getInstance().getLogger().severe("Invalid CSV format.");
                    continue;
                }

                try (Connection connection = createConnection()) {
                    if (connection == null) return;
                    try (Statement statement = connection.createStatement()) {
                        statement.executeUpdate("INSERT OR REPLACE INTO hearts (uuid, name, maxhp, hasbeenRevived, craftedHearts, craftedRevives, killedOtherPlayers) VALUES ('" + data[0] + "', '" + data[1] + "', " + Double.parseDouble(data[2]) + ", " + Integer.parseInt(data[3]) + ", " + Integer.parseInt(data[4]) + ", " + Integer.parseInt(data[5]) + ", " + Integer.parseInt(data[6]) + ")");
                    } catch (SQLException e) {
                        LifeStealZ.getInstance().getLogger().severe("Failed to import player data from CSV file: " + e.getMessage());
                    }
                } catch (SQLException e) {
                    LifeStealZ.getInstance().getLogger().severe("Failed to import player data from CSV file: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            LifeStealZ.getInstance().getLogger().severe("Failed to read CSV file: " + e.getMessage());
        }
    }

    @Override
    public List<String> getPlayerNames() {
        List<String> playerNames = new ArrayList<>();

        try (Connection connection = createConnection()) {
            if (connection == null) return playerNames;

            try (Statement statement = connection.createStatement()) {
                statement.setQueryTimeout(30);

                ResultSet resultSet = statement.executeQuery("SELECT name FROM hearts");

                while (resultSet.next()) {
                    playerNames.add(resultSet.getString("name"));
                }
            } catch (SQLException e) {
                LifeStealZ.getInstance().getLogger().severe("Failed to load player data from SQLite database: " + e.getMessage());
            }
        } catch (SQLException e) {
            LifeStealZ.getInstance().getLogger().severe("Failed to load player data from SQLite database: " + e.getMessage());
        }

        return playerNames;
    }
}
