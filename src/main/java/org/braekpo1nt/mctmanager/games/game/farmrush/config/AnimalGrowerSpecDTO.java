package org.braekpo1nt.mctmanager.games.game.farmrush.config;

import lombok.Data;
import net.kyori.adventure.text.Component;
import org.braekpo1nt.mctmanager.config.dto.org.bukkit.inventory.recipes.RecipeDTO;
import org.braekpo1nt.mctmanager.config.validation.Validatable;
import org.braekpo1nt.mctmanager.config.validation.Validator;
import org.braekpo1nt.mctmanager.games.game.farmrush.powerups.PowerupType;
import org.braekpo1nt.mctmanager.games.game.farmrush.powerups.specs.AnimalGrowerSpec;
import org.braekpo1nt.mctmanager.io.IOUtils;
import org.braekpo1nt.mctmanager.ui.UIUtils;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Data
class AnimalGrowerSpecDTO implements Validatable {
    
    private final PowerupType type = PowerupType.ANIMAL_GROWER;
    /**
     * The recipe used to craft this powerup. Can't be null. If the result is specified
     * in the config, it will be overwritten by the internal powerup.
     */
    private RecipeDTO recipe;
    /**
     * The lore of the item
     */
    private @Nullable List<Component> lore;
    /**
     * the display name of the item
     */
    private @Nullable Component displayName;
    /**
     * the type of the item, must be a block
     */
    private @Nullable Material blockType;
    /**
     * the radius of the effect range.
     */
    private double radius;
    /**
     * the custom model data to apply to the item. Defaults to 0.
     */
    private int customModelData = 0;
    /**
     * How many ticks pass between checks for new entities in the radius
     */
    private long ticksPerCycle = 20L;
    /**
     * a growable mob's age is multiplied by this factor. To grow faster,
     * make it a number less than 1. E.g. a mob takes 20 ticks to grow, and
     * ageFactor is .75, it will take 15 ticks to grow when within the
     * {@link #radius}
     */
    private double ageMultiplier;
    /**
     * Works the same as {@link #ageMultiplier}, but for the breeding cooldown
     */
    private double breedMultiplier;
    /**
     * An image showing the player the recipe for this powerup
     */
    private @Nullable String recipeImage;
    
    // Particles start
    /**
     * How many ticks pass between each particle spawn cycle.
     * Defaults to 20
     */
    private long ticksPerParticleCycle = 20L;
    /**
     * which particle spawns (defaults to {@link Particle#HAPPY_VILLAGER})
     */
    private Particle particle = Particle.HAPPY_VILLAGER;
    /**
     * how many groups of particles are spawned per spawn cycle
     * Defaults to 1.
     */
    private int numberOfParticles = 1;
    /**
     * the standard "number of particles" number for spawning a single particle,
     * the same as you would expect from the default minecraft command.
     * Defaults to 1.
     */
    private int particleCount = 1;
    // Particles end
    
    /**
     * @param world trivial world used for creating a map. If null, no map item is created
     * @return the specified spec
     */
    public AnimalGrowerSpec toSpec(World world) {
        ItemStack animalGrowerItem = new ItemStack(blockType == null ? Material.FURNACE : blockType);
        animalGrowerItem.editMeta(meta -> {
            meta.displayName(displayName == null ? defaultDisplayName() : displayName);
            meta.lore(lore == null ? defaultLore() : lore);
            meta.setCustomModelData(customModelData);
        });
        ItemStack newRecipeMap = null;
        if (recipeImage != null && world != null) {
            try {
                newRecipeMap = UIUtils.createMapItem(world, new File(recipeImage));
                newRecipeMap.editMeta(meta -> {
                   meta.displayName(Component.text("Animal Grower Recipe")); 
                });
            } catch (IOException ignored) {
            }
        }
        return AnimalGrowerSpec.builder()
                .animalGrowerItem(animalGrowerItem)
                .recipe(recipe.toRecipe(animalGrowerItem))
                .recipeKey(recipe.getNamespacedKey())
                .ticksPerCycle(ticksPerCycle)
                .radius(radius)
                .ageMultiplier(ageMultiplier)
                .breedMultiplier(breedMultiplier)
                .recipeMap(newRecipeMap)
                // Particles start
                .ticksPerParticleCycle(ticksPerParticleCycle)
                .particle(particle)
                .numberOfParticles(numberOfParticles)
                .particleCount(particleCount)
                // Particles end
                .build();
    }
    
    /**
     * @return the default lore if no lore is specified
     */
    private Component defaultDisplayName() {
        return Component.text("Animal Grower");
    }
    
    /**
     * @return the default lore if no lore is specified
     */
    private List<Component> defaultLore() {
        return List.of(
                Component.text("Place this near animals to make"),
                Component.text("them grow/breed faster"),
                Component.text("(more growers = faster breeding/growing)"));
    }
    
    @Override
    public void validate(@NotNull Validator validator) {
        validator.notNull(recipe, "recipe");
        validator.validate(radius >= 0.0, "radius can't be negative");
        validator.validate(ticksPerCycle >= 0, "ticksPerCycle can't be negative");
        validator.validate(ageMultiplier >= 0.0, "ageMultiplier can't be negative");
        validator.validate(breedMultiplier >= 0.0, "breedMultiplier can't be negative");
        if (recipeImage != null) {
            File recipeImageFile = new File(recipeImage);
            validator.fileExists(recipeImage, "recipeImage");
            try {
                IOUtils.toBufferedImage(recipeImageFile);
            } catch (IOException e) {
                validator.invalid("recipeImage could not be read as an image.");
            }
        }
        if (lore != null) {
            validator.validate(!lore.contains(null), "lore can't contain null entries");
        }
        if (blockType != null) {
            validator.validate(blockType.isBlock(), "blockType must be a block");
        }
        
        // Particles start
        validator.validate(ticksPerParticleCycle >= 1L, "ticksPerParticleCycle must be at least 1");
        validator.notNull(particle, "particle");
        validator.validate(numberOfParticles >= 1, "numberOfParticles must be at least 1");
        validator.validate(particleCount >= 1, "particleCount must be at least 1");
        // Particles end
    }
}
