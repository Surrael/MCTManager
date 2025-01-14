package org.braekpo1nt.mctmanager.participant;

import io.papermc.paper.entity.LookAnchor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.braekpo1nt.mctmanager.utils.AudienceDelegate;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a Participant. A participant is always a member of a {@link Team}.
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class Participant extends AudienceDelegate {
    
    /**
     * @param participants the participants list to get the list of players from
     * @return a list of the players contained in the given list of {@link Participant}s
     */
    public static List<Player> toPlayersList(Collection<Participant> participants) {
        return participants.stream().map(Participant::getPlayer).toList();
    }
    
    /**
     * @param participants the participants map to get the list of teamIds from the values
     * @return the teamIds
     */
    public static List<String> getTeamIds(Map<UUID, Participant> participants) {
        return getTeamIds(participants.values());
    }
    
    /**
     * @param participants the participants list to get the list of teamIds from
     * @return the teamIds
     */
    public static List<String> getTeamIds(Collection<Participant> participants) {
        return participants.stream().map(Participant::getTeamId).toList();
    }
    
    /**
     * @param participants the participants to search through
     * @param teamId the teamId to search for
     * @return the participants who are on the given team
     */
    public static List<Participant> getParticipantsOnTeam(Collection<Participant> participants, String teamId) {
        return participants.stream().filter(p -> p.getTeamId().equals(teamId)).toList();
    }
    /**
     * The player object that this Participant represents
     */
    @EqualsAndHashCode.Include
    protected final @NotNull Player player;
    
    /**
     * The teamId of the team this Participant belongs to
     */
    protected final @NotNull String teamId;
    
    /**
     * @return the UUID of the player this Participant represents
     */
    public UUID getUniqueId() {
        return player.getUniqueId();
    }
    
    /**
     * {@inheritDoc}
     * @return this Participant's {@link #player}. 
     */
    @Override
    public @NotNull Audience getAudience() {
        return player;
    }
    
    /**
     * Delegate for {@link Player#teleport(Location)}
     */
    public boolean teleport(@NotNull Location location) {
        return player.teleport(location);
    }
    
    /**
     * Delegate for {@link Player#setRespawnLocation(Location)}
     */
    public void setRespawnLocation(@Nullable Location location) {
        player.setRespawnLocation(location);
    }
    
    /**
     * Delegate for {@link Player#setRespawnLocation(Location,boolean)}
     */
    public void setRespawnLocation(@Nullable Location location, boolean force) {
        player.setRespawnLocation(location, force);
    }
    
    /**
     * Delegate for {@link Player#setGameMode(GameMode)}
     */
    public void setGameMode(@NotNull GameMode mode) {
        player.setGameMode(mode);
    }
    
    /**
     * Delegate for {@link Player#openInventory(Inventory)}
     */
    @SuppressWarnings("UnusedReturnValue")
    public @Nullable InventoryView openInventory(@NotNull Inventory inventory) {
        return player.openInventory(inventory);
    }
    
    /**
     * Delegate for {@link Player#closeInventory()}
     */
    public void closeInventory() {
        player.closeInventory();
    }
    
    /**
     * Delegate for {@link Player#getInventory()}
     */
    public @NotNull PlayerInventory getInventory() {
        return player.getInventory();
    }
    
    /**
     * Delegate for {@link Player#displayName()}
     */
    public @NotNull Component displayName() {
        return player.displayName();
    }
    
    /**
     * Delegate for {@link Player#displayName(Component)}
     */
    public void displayName(final @Nullable Component displayName) {
        player.displayName(displayName);
    }
    
    /**
     * Delegate for {@link Player#lookAt(double, double, double, LookAnchor)}
     */
    public void lookAt(double x, double y, double z, @NotNull LookAnchor lookAnchor) {
        player.lookAt(x, y, z, lookAnchor);
    }
    
    /**
     * Delegate for {@link Player#getWorld()}
     */
    public @NotNull World getWorld() {
        return player.getWorld();
    }
    
    /**
     * Delegate for {@link Player#getLocation()}
     */
    public @NotNull Location getLocation() {
        return player.getLocation();
    }
    
    /**
     * Delegate for {@link Player#getKiller()}
     */
    public @Nullable Player getKiller() {
        return player.getKiller();
    }
    
    /**
     * Delegate for {@link Player#getEquipment()}
     */
    public @NotNull EntityEquipment getEquipment() {
        return player.getEquipment();
    }
    
    /**
     * Delegate for {@link Player#getFacing()}
     */
    public @NotNull BlockFace getFacing() {
        return player.getFacing();
    }
    
    /**
     * Delegate for {@link Player#setArrowsInBody(int)}
     */
    public void setArrowsInBody(final int count) {
        player.setArrowsInBody(count);
    }
    
    /**
     * Delegate for {@link Player#playSound(Location, String, float, float)} )}
     */
    public void playSound(@NotNull Location location, @NotNull String sound, float volume, float pitch) {
        player.playSound(location, sound, volume, pitch);
    }
    
    /**
     * Delegate for {@link Player#addPotionEffect(PotionEffect)} )}
     */
    public void addPotionEffect(PotionEffect potionEffect) {
        player.addPotionEffect(potionEffect);
    }
    
    /**
     * Delegate for {@link Player#setFoodLevel(int)} )}
     */
    public void setFoodLevel(int value) {
        player.setFoodLevel(value);
    }
}
