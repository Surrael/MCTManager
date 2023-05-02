package org.braekpo1nt.mctmanager;

import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import io.papermc.paper.entity.LookAnchor;
import net.kyori.adventure.text.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;

import java.util.UUID;

public class MyPlayerMock extends PlayerMock {
    
    public MyPlayerMock(@NotNull ServerMock server, @NotNull String name) {
        super(server, name);
    }
    
    public MyPlayerMock(@NotNull ServerMock server, @NotNull String name, @NotNull UUID uuid) {
        super(server, name, uuid);
    }
    
    @Override
    public void lookAt(double x, double y, double z, @NotNull LookAnchor playerAnchor) {}
    
    /**
     * Asserts that the plaintext version of the next message sent to the player is equal to the 
     * expected message regardless of formatting.
     * @param expected The expected plaintext message
     */
    public void assertSaidPlaintext(@NotNull String expected) {
        Component comp = nextComponentMessage();
        if (comp == null) {
            Assertions.fail("No more messages were sent. Expected \"" + expected + "\"");
        }
        else {
            String plainText = toPlainText(comp);
            Assertions.assertEquals(expected, plainText);
        }
    }
    
    /**
     * Takes in a Component with 1 or more children, and converts it to a plaintext string without formatting.
     * Assumes it is made up of TextComponents and empty components.
     * @param component The component to get the plaintext version of
     * @return The concatenation of the contents() of the TextComponent children that this component is made of
     */
    String toPlainText(Component component) {
        StringBuilder builder = new StringBuilder();
        
        if (component instanceof TextComponent textComponent) {
            builder.append(textComponent.content());
        }
        else if (component instanceof TranslatableComponent) {
            for (Component arg : ((TranslatableComponent) component).args()) {
                builder.append(toPlainText(arg));
            }
        } else if (component instanceof ScoreComponent scoreComponent) {
            builder.append(scoreComponent.name());
        } else if (component instanceof SelectorComponent selectorComponent) {
            builder.append(selectorComponent.pattern());
        } else if (component instanceof KeybindComponent keybindComponent) {
            builder.append(keybindComponent.keybind());
        } else if (component instanceof NBTComponent<?, ?> nbtComponent) {
            builder.append(nbtComponent.nbtPath());
        }
        
        for (Component child : component.children()) {
            builder.append(toPlainText(child));
        }
        
        return builder.toString();
    }
}
