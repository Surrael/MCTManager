package org.braekpo1nt.mctmanager.commands.commandmanager.commandresult;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.braekpo1nt.mctmanager.commands.commandmanager.Usage;
import org.jetbrains.annotations.NotNull;

public class UsageCommandResult implements CommandResult {
    private final @NotNull Usage usage;
    
    public UsageCommandResult(@NotNull Usage usage) {
        this.usage = usage;
    }
    
    /**
     * @return this {@link UsageCommandResult}'s {@link Usage} as a {@link Component}
     */
    @Override
    public Component getMessage() {
        return usage.toComponent().color(NamedTextColor.RED);
    }
    
    @Override
    public @NotNull CommandResult and(CommandResult other) {
        return new CompositeCommandResult(this, other);
    }
}
