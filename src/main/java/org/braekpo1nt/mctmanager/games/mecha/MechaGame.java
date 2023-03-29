package org.braekpo1nt.mctmanager.games.mecha;

import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.onarandombox.MultiverseCore.utils.AnchorManager;
import fr.mrmicky.fastboard.FastBoard;
import net.kyori.adventure.text.Component;
import org.braekpo1nt.mctmanager.Main;
import org.braekpo1nt.mctmanager.games.GameManager;
import org.braekpo1nt.mctmanager.games.MCTGame;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.entity.Player;
import org.bukkit.loot.LootTable;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.structure.Structure;
import org.bukkit.util.Vector;

import java.util.*;

public class MechaGame implements MCTGame {
    
    private final Main plugin;
    private final GameManager gameManager;
    private boolean gameActive = false;
    private boolean mechaHasStarted = false;
    private List<Player> participants;
    private final World mechaWorld;
    private Map<UUID, FastBoard> boards = new HashMap<>();
    private int startMechaTaskId;
    /**
     * The coordinates of all the chests in the open world, not including spawn chests
     */
    private List<Vector> mapChestCoords;
    /**
     * The coordinates of all the spawn chests
     */
    private List<Vector> spawnChestCoords;
    /**
     * Holds the mecha loot tables from the mctdatapack, not including the spawn loot
     */
    private List<LootTable> mechaLootTables;
    /**
     * Holds the mecha spawn loot table from the mctdatapack
     */
    private LootTable spawnLootTable;
    
    public MechaGame(Main plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        setChestCoordsAndLootTables();
        MVWorldManager worldManager = Main.multiverseCore.getMVWorldManager();
        this.mechaWorld = worldManager.getMVWorld("FT").getCBWorld();
    }
    
    @Override
    public void start(List<Player> participants) {
        this.participants = participants;
        placePlatforms();
        fillAllChests();
        teleportPlayersToStartingPositions();
        initializeFastboards();
        startStartMechaCountdownTask();
        gameActive = true;
        Bukkit.getLogger().info("Started mecha");
    }

    @Override
    public void stop() {
        hideFastBoards();
        cancelTasks();
        gameActive = false;
        gameManager.gameIsOver();
        Bukkit.getLogger().info("Stopped mecha");
    }
    
    private void cancelTasks() {
        Bukkit.getScheduler().cancelTask(startMechaTaskId);
    }
    
    private void startStartMechaCountdownTask() {
        startMechaTaskId = new BukkitRunnable() {
            int count = 10;
            
            @Override
            public void run() {
                if (count <= 0) {
                    startMecha();
                    this.cancel();
                    return;
                }
                for (Player participant : participants) {
                    participant.sendMessage(Component.text(count));
                }
                count--;
            }
        }.runTaskTimer(plugin, 0L, 20L).getTaskId();
    }
    
    private void startMecha() {
        this.mechaHasStarted = true;
        removePlatforms();
        for (Player participant : participants) {
            participant.sendMessage(Component.text("Go!"));
        }
    }
    
    private void initializeFastboards() {
        for (Player participant : participants) {
            FastBoard board = new FastBoard(participant);
            board.updateTitle(ChatColor.BLUE+"MECHA");
            board.updateLines(
                    "",
                    ChatColor.RED+"Kills: 0",
                    "",
                    ChatColor.DARK_PURPLE+"Boarder: 00:00"
            );
            boards.put(participant.getUniqueId(), board);
        }
    }
    
    private void hideFastBoards() {
        for (FastBoard board : boards.values()) {
            if (!board.isDeleted()) {
                board.delete();
            }
        }
    }
    
    private void teleportPlayersToStartingPositions() {
        AnchorManager anchorManager = Main.multiverseCore.getAnchorManager();
        Map<String, Location> teamLocations = new HashMap<>();
        teamLocations.put("orange", anchorManager.getAnchorLocation("mecha-orange"));
        teamLocations.put("yellow", anchorManager.getAnchorLocation("mecha-yellow"));
        teamLocations.put("green", anchorManager.getAnchorLocation("mecha-green"));
        teamLocations.put("dark-green", anchorManager.getAnchorLocation("mecha-dark-green"));
        teamLocations.put("cyan", anchorManager.getAnchorLocation("mecha-cyan"));
        teamLocations.put("blue", anchorManager.getAnchorLocation("mecha-blue"));
        teamLocations.put("purple", anchorManager.getAnchorLocation("mecha-purple"));
        teamLocations.put("red", anchorManager.getAnchorLocation("mecha-red"));
        for (Player participant : participants) {
            String team = gameManager.getTeamName(participant.getUniqueId());
            Location teamLocation = teamLocations.getOrDefault(team, teamLocations.get("yellow"));
            participant.teleport(teamLocation);
        }
    }
    
    
    
    private void placePlatforms() {
        Structure structure = Bukkit.getStructureManager().loadStructure(new NamespacedKey("mctdatapack", "mecha/platforms"));
        structure.place(new Location(this.mechaWorld, -13, -43, -13), true, StructureRotation.NONE, Mirror.NONE, 0, 1, new Random());
    }
    
    private void removePlatforms() {
        Structure structure = Bukkit.getStructureManager().loadStructure(new NamespacedKey("mctdatapack", "mecha/platforms_removed"));
        structure.place(new Location(this.mechaWorld, -13, -43, -13), true, StructureRotation.NONE, Mirror.NONE, 0, 1, new Random());
    }
    
    /**
     * Fill all chests in the mecha world, map chests and spawn chests
     */
    private void fillAllChests() {
        fillSpawnChests();
        fillMapChests();
    }
    
    private void fillSpawnChests() {
        for (Vector coords : spawnChestCoords) {
            Block block = mechaWorld.getBlockAt(coords.getBlockX(), coords.getBlockY(), coords.getBlockZ());
            block.setType(Material.CHEST);
            Chest chest = (Chest) block.getState();
            chest.setLootTable(spawnLootTable);
            chest.update();
        }
    }
    
    private void fillMapChests() {
        for (Vector coords : mapChestCoords) {
            Block block = mechaWorld.getBlockAt(coords.getBlockX(), coords.getBlockY(), coords.getBlockZ());
            block.setType(Material.CHEST);
            fillMapChest(((Chest) block.getState()));
        }
    }
    
    /**
     * Fills the given chest with a random loot table
     * @param chest The chest to fill
     */
    private void fillMapChest(Chest chest) {
        LootTable lootTable = getRandomLootTable();
        chest.setLootTable(lootTable);
        chest.update();
    }
    
    /**
     * Gets a random loot table from the MECHA loot table selection
     * @return A loot table for a chest
     */
    private LootTable getRandomLootTable() {
        Random random = new Random();
        int randomIndex = random.nextInt(this.mechaLootTables.size());
        return this.mechaLootTables.get(randomIndex);
    }
    
    private void setChestCoordsAndLootTables() {
        this.spawnChestCoords = new ArrayList<>(12);
        spawnChestCoords.add(new Vector(-1, -45, 1));
        spawnChestCoords.add(new Vector(0, -45, 1));
        spawnChestCoords.add(new Vector(-2, -45, 0));
        spawnChestCoords.add(new Vector(-1, -44, 0));
        spawnChestCoords.add(new Vector(0, -44, 0));
        spawnChestCoords.add(new Vector(1, -45, 0));
        spawnChestCoords.add(new Vector(-2, -45, -1));
        spawnChestCoords.add(new Vector(-1, -44, -1));
        spawnChestCoords.add(new Vector(0, -44, -1));
        spawnChestCoords.add(new Vector(1, -45, -1));
        spawnChestCoords.add(new Vector(-1, -45, -2));
        spawnChestCoords.add(new Vector(0, -45, -2));
        
        this.mapChestCoords = new ArrayList<>(62);
        mapChestCoords.add(new Vector(-18, -45, -15));
        mapChestCoords.add(new Vector(-10, -37, -17));
        mapChestCoords.add(new Vector(-10, -31, -18));
        mapChestCoords.add(new Vector(-15, -28, -28));
        mapChestCoords.add(new Vector(-13, -28, -28));
        mapChestCoords.add(new Vector(-13, -34, -36));
        mapChestCoords.add(new Vector(-21, -34, -30));
        mapChestCoords.add(new Vector(-21, -40, -27));
        mapChestCoords.add(new Vector(-23, -45, -33));
        mapChestCoords.add(new Vector(-23, -45, 20));
        mapChestCoords.add(new Vector(-25, -44, 9));
        mapChestCoords.add(new Vector(-10, -45, 41));
        mapChestCoords.add(new Vector(-26, -44, 52));
        mapChestCoords.add(new Vector(-10, -38, 43));
        mapChestCoords.add(new Vector(-22, -30, 56));
        mapChestCoords.add(new Vector(-9, -31, 34));
        mapChestCoords.add(new Vector(37, -44, 19));
        mapChestCoords.add(new Vector(24, -51, 3));
        mapChestCoords.add(new Vector(38, -51, 23));
        mapChestCoords.add(new Vector(23, -51, 58));
        mapChestCoords.add(new Vector(-52, -51, 65));
        mapChestCoords.add(new Vector(-58, -51, -11));
        mapChestCoords.add(new Vector(-27, -45, -12));
        mapChestCoords.add(new Vector(-38, -39, -10));
        mapChestCoords.add(new Vector(-31, -33, -10));
        mapChestCoords.add(new Vector(-46, -43, 17));
        mapChestCoords.add(new Vector(-65, -42, 19));
        mapChestCoords.add(new Vector(-60, -43, 30));
        mapChestCoords.add(new Vector(-83, -43, 63));
        mapChestCoords.add(new Vector(-61, -43, 64));
        mapChestCoords.add(new Vector(-50, -43, 33));
        mapChestCoords.add(new Vector(22, -45, -23));
        mapChestCoords.add(new Vector(16, -45, -10));
        mapChestCoords.add(new Vector(30, -45, -44));
        mapChestCoords.add(new Vector(34, -43, -31));
        mapChestCoords.add(new Vector(22, -37, -45));
        mapChestCoords.add(new Vector(9, -27, -44));
        mapChestCoords.add(new Vector(16, -28, -13));
        mapChestCoords.add(new Vector(22, -40, -70));
        mapChestCoords.add(new Vector(8, -40, -81));
        mapChestCoords.add(new Vector(26, -45, 24));
        mapChestCoords.add(new Vector(-14, -45, -57));
        mapChestCoords.add(new Vector(-29, -45, -56));
        mapChestCoords.add(new Vector(-10, -51, -52));
        mapChestCoords.add(new Vector(-36, -51, -66));
        mapChestCoords.add(new Vector(-16, -39, -57));
        mapChestCoords.add(new Vector(-12, -33, -68));
        mapChestCoords.add(new Vector(-66, -45, -26));
        mapChestCoords.add(new Vector(-52, -48, -30));
        mapChestCoords.add(new Vector(-70, -27, -40));
        mapChestCoords.add(new Vector(-74, -44, -37));
        mapChestCoords.add(new Vector(-98, -45, -43));
        mapChestCoords.add(new Vector(-94, -40, -50));
        mapChestCoords.add(new Vector(-93, -44, -27));
        mapChestCoords.add(new Vector(-93, -39, -30));
        mapChestCoords.add(new Vector(-93, -34, -34));
        mapChestCoords.add(new Vector(-42, -45, -58));
        mapChestCoords.add(new Vector(-36, -39, -61));
        mapChestCoords.add(new Vector(-52, -33, -69));
        mapChestCoords.add(new Vector(-52, -27, -71));
        mapChestCoords.add(new Vector(-67, -51, -83));
        mapChestCoords.add(new Vector(-89, -50, -113));
        
        this.mechaLootTables = new ArrayList<>();
        String[] lootTableNames = new String[]{"mecha/poor-chest", "mecha/good-chest", "mecha/better-chest", "mecha/excellent-chest"};
        for (String lootTableName : lootTableNames) {
            LootTable lootTable = Bukkit.getLootTable(new NamespacedKey("mctdatapack", lootTableName));
            mechaLootTables.add(lootTable);
        }
        
        this.spawnLootTable = Bukkit.getLootTable(new NamespacedKey("mctdatapack", "mecha/spawn-chest"));
    }
}
