package org.braekpo1nt.mctmanager.games.footrace;

import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MVWorldManager;
import org.braekpo1nt.mctmanager.Main;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FootRaceGame implements Listener {

    private boolean gameActive = true;
    /**
     * Holds the Foot Race world
     */
    private final World footRaceWorld;
    private final Map<String, Long> lapCooldowns;
    
    public FootRaceGame(Main plugin, List<Player> participants) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        lapCooldowns = participants.stream().collect(Collectors.toMap(participant -> participant.getName(), key -> System.currentTimeMillis()));
        
        Plugin multiversePlugin = Bukkit.getPluginManager().getPlugin("Multiverse-Core");
        MultiverseCore multiverseCore = ((MultiverseCore) multiversePlugin);
        MVWorldManager worldManager = multiverseCore.getMVWorldManager();
        this.footRaceWorld = worldManager.getMVWorld("NT").getCBWorld();
    }
    
    public void start() {
        gameActive = true;
    }
    
    @EventHandler
    public void onPlayerCrossFinishLine(PlayerMoveEvent event) {
        if (gameActive) {
            Player player = event.getPlayer();
            if (player.getWorld().equals(footRaceWorld)) {
                BoundingBox finishLine = new BoundingBox(2396, 80, 295, 2404, 79, 308);
                if (finishLine.contains(player.getLocation().toVector())) {

                    String name = player.getName();
                    // if this player is a participant
                    if (lapCooldowns.containsKey(name)) {
                        long lastMoveTime = lapCooldowns.get(name);
                        long currentTime = System.currentTimeMillis();
                        long coolDownTime = 3000L; // 3 second
                        if (currentTime - lastMoveTime < coolDownTime) {
                            //Not enough time has elapsed, return without doing anything
                            return;
                        }
                    }
                    lapCooldowns.put(name, System.currentTimeMillis());

                    player.sendMessage("You crossed the finish line!");
                    
                }
            }
        }
    }
    
}
