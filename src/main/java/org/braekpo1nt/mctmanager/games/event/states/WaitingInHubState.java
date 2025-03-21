package org.braekpo1nt.mctmanager.games.event.states;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.braekpo1nt.mctmanager.Main;
import org.braekpo1nt.mctmanager.games.GameManager;
import org.braekpo1nt.mctmanager.games.event.EventManager;
import org.braekpo1nt.mctmanager.games.event.config.Tip;
import org.braekpo1nt.mctmanager.games.event.states.delay.ToColossalCombatDelay;
import org.braekpo1nt.mctmanager.games.game.enums.GameType;
import org.braekpo1nt.mctmanager.ui.sidebar.Sidebar;
import org.braekpo1nt.mctmanager.ui.timer.Timer;
import org.braekpo1nt.mctmanager.utils.LogType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class WaitingInHubState implements EventState {
    
    protected final EventManager context;
    protected final GameManager gameManager;
    protected final Sidebar sidebar;
    protected final Sidebar adminSidebar;
    protected final Timer waitingInHubTimer;

    protected Map<Player, Component> playerTips;
    protected List<Integer> taskIds;

    public WaitingInHubState(EventManager context) {
        this.context = context;
        this.gameManager = context.getGameManager();
        this.sidebar = context.getSidebar();
        this.adminSidebar = context.getAdminSidebar();
        gameManager.returnAllParticipantsToHub();
        double scoreMultiplier = context.matchProgressPointMultiplier();
        gameManager.messageOnlineParticipants(Component.text("Score multiplier: ")
                .append(Component.text(scoreMultiplier))
                .color(NamedTextColor.GOLD));
        waitingInHubTimer = startTimer();
        startActionBarTips();
    }
    
    protected Timer startTimer() {
        Component prefix;
        if (context.allGamesHaveBeenPlayed()) {
            prefix = Component.text("Final round: ");
        } else {
            prefix = Component.text("Vote starts in: ");
        }
        return context.getTimerManager().start(Timer.builder()
                .duration(context.getConfig().getWaitingInHubDuration())
                .withSidebar(sidebar, "timer")
                .withSidebar(adminSidebar, "timer")
                .sidebarPrefix(prefix)
                .onCompletion(() -> {
                    for (Integer taskId : taskIds) {
                        context.getPlugin().getServer().getScheduler().cancelTask(taskId);
                    }
                    if (context.allGamesHaveBeenPlayed()) {
                        context.setState(new ToColossalCombatDelay(context));
                    } else {
                        context.setState(new VotingState(context));
                    }
                })
                .build());
    }
    
    @Override
    public void onParticipantJoin(Player participant) {
        gameManager.returnParticipantToHubInstantly(participant);
        context.getParticipants().add(participant);
        if (sidebar != null) {
            sidebar.addPlayer(participant);
            context.updateTeamScores();
            sidebar.updateLine(participant.getUniqueId(), "currentGame", context.getCurrentGameLine());
        }
    }
    
    @Override
    public void onParticipantQuit(Player participant) {
        context.getParticipants().remove(participant);
        if (sidebar != null) {
            sidebar.removePlayer(participant);
        }
    }
    
    @Override
    public void onAdminJoin(Player admin) {
        if (adminSidebar != null) {
            adminSidebar.addPlayer(admin);
            context.updateTeamScores();
            adminSidebar.updateLine(admin.getUniqueId(), "currentGame", context.getCurrentGameLine());
        }
    }
    
    @Override
    public void onAdminQuit(Player admin) {
        context.getAdmins().remove(admin);
        if (adminSidebar != null) {
            adminSidebar.removePlayer(admin);
        }
    }
    
    @Override
    public void startEvent(@NotNull CommandSender sender, int numberOfGames, int currentGameNumber) {
        sender.sendMessage(Component.text("An event is already running.")
                .color(NamedTextColor.RED));
    }
    
    @Override
    public void onPlayerDamage(EntityDamageEvent event) {
        Main.debugLog(LogType.CANCEL_ENTITY_DAMAGE_EVENT, "EventManager.WaitingInHubState.onPlayerDamage() cancelled");
        event.setCancelled(true);
    }
    
    @Override
    public void onClickInventory(InventoryClickEvent event) {
        // do nothing
    }
    
    @Override
    public void onDropItem(PlayerDropItemEvent event) {
        // do nothing
    }
    
    @Override
    public void gameIsOver(@NotNull GameType finishedGameType) {
        // do nothing
    }
    
    @Override
    public void colossalCombatIsOver(@Nullable String winningTeam) {
        // do nothing
    }
    
    @Override
    public void setMaxGames(@NotNull CommandSender sender, int newMaxGames) {
        if (newMaxGames < context.getCurrentGameNumber() - 1) {
            sender.sendMessage(Component.empty()
                    .append(Component.text("Can't set the max games for this event to less than "))
                    .append(Component.text(context.getCurrentGameNumber() - 1)
                            .decorate(TextDecoration.BOLD))
                    .append(Component.text(" because "))
                    .append(Component.text(context.getCurrentGameNumber() - 1))
                    .append(Component.text(" game(s) have been played."))
                    .color(NamedTextColor.RED));
            return;
        }
        context.setMaxGames(newMaxGames);
        context.getSidebar().updateLine("currentGame", context.getCurrentGameLine());
        context.getAdminSidebar().updateLine("currentGame", context.getCurrentGameLine());
        gameManager.updateGameTitle();
        sender.sendMessage(Component.text("Max games has been set to ")
                .append(Component.text(newMaxGames)));
    }
    
    @Override
    public void stopColossalCombat(@NotNull CommandSender sender) {
        sender.sendMessage(Component.text("Colossal Combat is not running")
                .color(NamedTextColor.RED));
    }
    
    @Override
    public void startColossalCombat(@NotNull CommandSender sender, @NotNull String firstTeam, @NotNull String secondTeam) {
        waitingInHubTimer.cancel();
        for (Integer taskId : taskIds) {
            context.getPlugin().getServer().getScheduler().cancelTask(taskId);
        }
        context.setState(new PlayingColossalCombatState(
                context,
                firstTeam,
                secondTeam));
    }

    public void startActionBarTips() {
        taskIds = new ArrayList<>();
        playerTips = new HashMap<>();
        List<Tip> allTips = context.getConfig().getTips();
        int tipsDisplayTimeSeconds = context.getConfig().getTipsDisplayTimeSeconds();

        BukkitScheduler scheduler = context.getPlugin().getServer().getScheduler();

        // Task to compute and update tips
        int updateTipsTaskId = scheduler.scheduleSyncRepeatingTask(context.getPlugin(), () -> {
            Map<String, List<Player>> teamMapping = getTeamPlayerMapping();
            playerTips.clear();

            for (List<Player> teamPlayers : teamMapping.values()) {
                List<Tip> tips = Tip.selectMultipleWeightedRandomTips(allTips, teamPlayers.size());

                for (int i = 0; i < teamPlayers.size(); i++) {
                    Player player = teamPlayers.get(i);
                    Component tip = tips.get(i).getTip();
                    playerTips.put(player, tip);
                }
            }
        }, 0L, tipsDisplayTimeSeconds * 20L);
        taskIds.add(updateTipsTaskId);

        // Task to display the tips
        int displayTipsTaskId = scheduler.scheduleSyncRepeatingTask(context.getPlugin(), () -> {
            for (Player player : playerTips.keySet()) {
                player.sendActionBar(playerTips.get(player));
            }
        }, 0L, 20L);
        taskIds.add(displayTipsTaskId);

        // Recurring task to stop and restart display
        int cycleTaskId = scheduler.scheduleSyncRepeatingTask(context.getPlugin(), () -> {
            scheduler.cancelTask(displayTipsTaskId);
            taskIds.remove(Integer.valueOf(displayTipsTaskId));

            int newDisplayTipsTaskId = scheduler.scheduleSyncRepeatingTask(context.getPlugin(), () -> {
                for (Player player : playerTips.keySet()) {
                    player.sendActionBar(playerTips.get(player));
                }
            }, 0L, 20L);
            taskIds.add(newDisplayTipsTaskId);
        }, tipsDisplayTimeSeconds * 20L, tipsDisplayTimeSeconds * 20L);
        taskIds.add(cycleTaskId);
    }

    /**
     * Returns a mapping of all teamIds to currently online players of those teams
     *
     * @return mapping from all teamIds to their currently online players
     */
    public Map<String, List<Player>> getTeamPlayerMapping() {
        List<Player> participants = context.getParticipants();

        return participants.stream()
                .collect(Collectors.groupingBy(
                        participant -> context.getGameManager().getTeamId(participant.getUniqueId()),
                        Collectors.toList()
                ));
    }

    @Override
    public void cancelAllTasks() {
        // Stop action bar tips
        for (Integer taskId : taskIds) {
            context.getPlugin().getServer().getScheduler().cancelTask(taskId);
        }
    }

}
