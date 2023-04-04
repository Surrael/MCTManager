package org.braekpo1nt.mctmanager.games.capturetheflag;

import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import com.onarandombox.MultiverseCore.utils.AnchorManager;
import org.braekpo1nt.mctmanager.Main;
import org.braekpo1nt.mctmanager.games.GameManager;
import org.braekpo1nt.mctmanager.games.interfaces.MCTGame;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.util.*;

public class CaptureTheFlagGame implements MCTGame {
    
    private final Main plugin;
    private final GameManager gameManager;
    private final String title = ChatColor.BLUE+"Capture the Flag";
    private final World captureTheFlagWorld;
    private Location spawnObservatory;
    private List<Arena> arenas;
    private List<Player> participants;
    /**
     * a list of lists of TeamPairings. Each element is a list of 1-4 TeamPairings.
     * Each index corresponds to a round.
     */
    private List<List<TeamPairing>> allRoundTeamPairings;
    /**
     * Contains the 1-4 TeamPairings for the current round. 
     * See {@link CaptureTheFlagGame#allRoundTeamPairings}.
     * Each index corresponds to a pair of teams to fight in their own arena. 
     */
    private List<TeamPairing> currentRoundTeamParings;
    private int currentRound = 0;
    private int maxRounds = 0;
    private List<UUID> livingPlayers;
    private List<UUID> deadPlayers;
    private Map<UUID, Integer> killCounts;
    
    
    public CaptureTheFlagGame(Main plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        MVWorldManager worldManager = Main.multiverseCore.getMVWorldManager();
        MultiverseWorld mvCaptureTheFlagWorld = worldManager.getMVWorld("FT");
        this.captureTheFlagWorld = mvCaptureTheFlagWorld.getCBWorld();
        AnchorManager anchorManager = Main.multiverseCore.getAnchorManager();
        spawnObservatory = anchorManager.getAnchorLocation("capture-the-flag");
        initializeArenas();
    }
    
    /**
     * Starts a new Capture the Flag game with the provided participants.
     * Assumes that the provided list of participants collectively belong
     * to at least 2 teams, and at most 8 teams. 
     * @param newParticipants
     */
    @Override
    public void start(List<Player> newParticipants) {
        this.participants = new ArrayList<>(newParticipants.size());
        currentRound = 0;
        this.allRoundTeamPairings = generateAllRoundTeamPairings(newParticipants);
        maxRounds = allRoundTeamPairings.size();
        for (Player participant : newParticipants) {
            initializeParticipant(participant);
        }
        startNextRound();
        Bukkit.getLogger().info("Started Capture the Flag");
    }
    
    private void initializeParticipant(Player participant) {
        participants.add(participant);
        teleportParticipantToSpawnObservatory(participant);
        participant.setGameMode(GameMode.ADVENTURE);
        participant.getInventory().clear();
        resetHealthAndHunger(participant);
        clearStatusEffects(participant);
        initializeFastBoard(participant);
    }
    
    private void initializeParticipantForRound(Player participant) {
        UUID participantUniqueId = participant.getUniqueId();
        livingPlayers.add(participantUniqueId);
        killCounts.put(participantUniqueId, 0);
        participant.setGameMode(GameMode.ADVENTURE);
        participant.getInventory().clear();
        resetHealthAndHunger(participant);
        initializeFastBoard(participant);
    }
    
    
    private void startNextRound() {
        this.currentRound++;
        this.livingPlayers = new ArrayList<>();
        this.deadPlayers = new ArrayList<>();
        this.killCounts = new HashMap<>();
        currentRoundTeamParings = allRoundTeamPairings.get(currentRound-1);
        for (Player participant : participants){
            initializeParticipantForRound(participant);
        }
        teleportTeamPairingsToArenas();
        startClassSelectionPeriod();
        Bukkit.getLogger().info("Starting round " + currentRound);
    }
    
    private void startClassSelectionPeriod() {
        throw new UnsupportedOperationException("Need to implement startClassSelectionPeriod");
    }
    
    /**
     * See {@link CaptureTheFlagGame#allRoundTeamPairings}
     * @param newParticipants The participants whose teams will be used to create the pairings
     * @return A new list of lists of 1-4 TeamPairings. See {@link CaptureTheFlagGame#allRoundTeamPairings}
     */
    public List<List<TeamPairing>> generateAllRoundTeamPairings(List<Player> newParticipants) {
        List<String> teamNames = gameManager.getTeamNames(newParticipants);
        List<TeamPairing> teamPairings = generateAllPairings(teamNames);
        // A list of lists of 1-4 TeamPairings
        List<List<TeamPairing>> newAllRoundTeamPairings = new ArrayList<>();
        int pairingIndex = 0;
        while (pairingIndex < teamPairings.size()) {
            // A list of 1-4 TeamPairings
            List<TeamPairing> singleRoundPairingGroup = new ArrayList<>();
            int j = 0;
            while (j < 4) {
                singleRoundPairingGroup.add(teamPairings.get(pairingIndex));
                pairingIndex++;
                j++;
            }
            newAllRoundTeamPairings.add(singleRoundPairingGroup);
        }
        return newAllRoundTeamPairings;
    }
    
    private static List<TeamPairing> generateAllPairings(List<String> teamNames) {
        List<TeamPairing> teamPairings = new ArrayList<>();
        int n = teamNames.size();
        // Generate all possible combinations of indices
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                String teamA = teamNames.get(i);
                String teamB = teamNames.get(j);
                TeamPairing teamPairing = new TeamPairing(teamA, teamB);
                teamPairings.add(teamPairing);
            }
        }
        return teamPairings;
    }
    
    @Override
    public void stop() {
        Bukkit.getLogger().info("Stopped Capture the Flag");
    }
    
    @Override
    public void onParticipantJoin(Player participant) {
        
    }
    
    @Override
    public void onParticipantQuit(Player participant) {
        
    }
    
    private void teleportParticipantToSpawnObservatory(Player participant) {
        participant.teleport(spawnObservatory);
    }
    
    private void teleportTeamPairingsToArenas() {
        for (int i = 0; i < currentRoundTeamParings.size(); i++) {
            TeamPairing teamPairing = currentRoundTeamParings.get(i);
            Arena arena = arenas.get(i);
            teleportTeamPairingToArena(teamPairing, arena);
        }
    }
    
    /**
     * Teleports all participants whose teams are in the given pairing to their
     * respective spawn positions in the given arena
     * @param teamPairing The TeamPairing to teleport to the arena
     * @param arena The arena to teleport the TeamPairing to
     */
    private void teleportTeamPairingToArena(TeamPairing teamPairing, Arena arena) {
        for (Player participant : participants) {
            String teamName = gameManager.getTeamName(participant.getUniqueId());
            if (teamPairing.getNorthTeam().equals(teamName)) {
                participant.teleport(arena.getNorthSpawn());
            }
            if (teamPairing.getSouthTeam().equals(teamName)) {
                participant.teleport(arena.getSouthSpawn());
            }
        }
    }
    
    private void resetHealthAndHunger(Player participant) {
        participant.setHealth(participant.getAttribute(Attribute.GENERIC_MAX_HEALTH).getDefaultValue());
        participant.setFoodLevel(20);
        participant.setSaturation(5);
    }
    
    private void clearStatusEffects(Player participant) {
        for (PotionEffect effect : participant.getActivePotionEffects()) {
            participant.removePotionEffect(effect.getType());
        }
    }
    
    private void initializeFastBoard(Player participant) {
        int killCount = killCounts.get(participant.getUniqueId());
        gameManager.getFastBoardManager().updateLines(
                participant.getUniqueId(),
                title,
                "",
                ChatColor.RED+"Kills: "+killCount,
                "",
                "Round time:",
                "3:00",
                "",
                "Round: " + currentRound + "/" + maxRounds
        );
    }
    
    private void initializeArenas() {
        this.arenas = new ArrayList<>(4);
        //NorthWest
        arenas.add(new Arena(
                new Location(captureTheFlagWorld, -15, -16, -1043), // North
                new Location(captureTheFlagWorld, -15, -16, -1003), // South
                new Location(captureTheFlagWorld, -6, -13, -1040), // North
                new Location(captureTheFlagWorld, -24, -13, -1006) // South
        ));
        //NorthEast
        arenas.add(new Arena(
                new Location(captureTheFlagWorld, 15, -16, -1043), // North
                new Location(captureTheFlagWorld, 15, -16, -1003), // South
                new Location(captureTheFlagWorld, 24, -13, -1040), // North
                new Location(captureTheFlagWorld, 6, -13, -1006) // South
        ));
        //SouthWest
        arenas.add(new Arena(
                new Location(captureTheFlagWorld, -15, -16, -997), // North
                new Location(captureTheFlagWorld, -15, -16, -957), // South
                new Location(captureTheFlagWorld, -6, -13, -994), // North
                new Location(captureTheFlagWorld, -24, -13, -960) // South
        ));
        //SouthEast
        arenas.add(new Arena(
                new Location(captureTheFlagWorld, 15, -16, -997), // North
                new Location(captureTheFlagWorld, 15, -16, -957), // South
                new Location(captureTheFlagWorld, 24, -13, -994), // North
                new Location(captureTheFlagWorld, 6, -13, -960) // South
        ));
    }
}
