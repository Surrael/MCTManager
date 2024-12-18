package org.braekpo1nt.mctmanager.participant;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.kyori.adventure.audience.Audience;
import org.braekpo1nt.mctmanager.utils.AudienceDelegate;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Represents a Participant. A participant is always a member of a {@link Team}.
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class Participant extends AudienceDelegate {
    
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
}
