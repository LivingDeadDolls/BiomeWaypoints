package biomewaypoints.config;

import biomewaypoints.Constants;
import biomewaypoints.platform.Services;
import biomewaypoints.search.ChosenBiomes;
import biomewaypoints.waypoints.BiomeWaypointsPlugin;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import waypointsapi.api.KindSettingsOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import waypointsapi.api.KindSettingsOptions;

/**
 * Settings saved to config/biomewaypoints.json5: which biomes are searched for, the border lines, and how the
 * waypoints are shown (for Waypoints API, through {@code BiomeKindSettings}).
 */
public final class BiomeWaypointsConfig {
    public static final ConfigClassHandler<BiomeWaypointsConfig> HANDLER = ConfigClassHandler.createBuilder(BiomeWaypointsConfig.class)
            .id(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "config"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(Services.PLATFORM.getConfigDir().resolve(Constants.MOD_ID + ".json5"))
                    .setJson5(true)
                    .build())
            .build();

    /** The biomes not to find, by id (e.g. {@code minecraft:cherry_grove}): every other biome of the dimension is found. */
    @SerialEntry
    public List<String> skippedBiomes = new ArrayList<>();

    /** How far (in blocks, along x and z) the player goes before the biomes are searched for again from there. */
    @SerialEntry
    public int searchAgainDistance = 256;

    /** Whether thin lines on the ground show where the biomes around the player meet. */
    @SerialEntry
    public boolean borderLines = true;

    /** Whether the waypoints of the biomes are shown (they are searched for anyway). */
    @SerialEntry
    public boolean shown = true;

    /** How far (in blocks) the waypoints are shown. */
    @SerialEntry
    public int displayDistance = 1024;

    /** How many of the waypoints are shown at most, the nearest. */
    @SerialEntry
    public int maxShown = 16;

    /** The biomes whose waypoints are not shown, by id: they are not searched for either. */
    @SerialEntry
    public List<String> hiddenBiomes = new ArrayList<>();

    /** The colors chosen for biomes (opaque RGB), by id; the other biomes have their own color. */
    @SerialEntry
    public Map<String, Integer> biomeColors = new HashMap<>();

    /** Whether only the nearest waypoint of a biome is shown, by id, for the biomes set otherwise than by default. */
    @SerialEntry
    public Map<String, Boolean> biomeNearestOnly = new HashMap<>();

    private static List<Identifier> gameBiomes;
    /** The size of the pictures of the biomes (16:9). */
    private static final int PICTURE_WIDTH = 320;
    private static final int PICTURE_HEIGHT = 180;

    public static BiomeWaypointsConfig get() {
        return HANDLER.instance();
    }

    /** {@link #skippedBiomes} as ids, leaving out what is not one. */
    public Set<Identifier> skippedBiomeIds() {
        return ChosenBiomes.of(skippedBiomes);
    }

    /** The biomes not to search for: {@link #skippedBiomes} and {@link #hiddenBiomes}. */
    public Set<Identifier> notSearchedBiomeIds() {
        return ChosenBiomes.notSearched(skippedBiomes, hiddenBiomes);
    }

    public static Screen createScreen(Screen parent) {
        return YetAnotherConfigLib.create(HANDLER, (defaults, config, builder) -> {
            ConfigCategory.Builder general = ConfigCategory.createBuilder()
                        .name(Component.translatable("biomewaypoints.config.category.general"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("biomewaypoints.config.borderLines"))
                                .description(OptionDescription.of(Component.translatable("biomewaypoints.config.borderLines.desc")))
                                .binding(defaults.borderLines, () -> config.borderLines, value -> config.borderLines = value)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("biomewaypoints.config.searchAgainDistance"))
                                .description(OptionDescription.of(Component.translatable("biomewaypoints.config.searchAgainDistance.desc")))
                                .binding(defaults.searchAgainDistance, () -> config.searchAgainDistance, value -> config.searchAgainDistance = value)
                                .controller(option -> IntegerSliderControllerBuilder.create(option).range(32, 2048).step(32))
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Component.translatable("biomewaypoints.config.biomes"))
                                .description(OptionDescription.of(Component.translatable("biomewaypoints.config.biomes.desc")))
                                .options(listedBiomes(config).stream().map(biome -> biomeOption(config, biome)).toList())
                                .build());
            return builder.title(Component.translatable("biomewaypoints.config.title"))
                    .category(general.build())
                    // Waypoints API's options of the kind and of each biome; they write to this config, saved with the rest.
                    .category(ConfigCategory.createBuilder()
                            .name(Component.translatable("biomewaypoints.config.category.display"))
                            .groups(KindSettingsOptions.of(BiomeWaypointsPlugin.kind()))
                            .build());
        }).generateScreen(parent);
    }

    /** The game's own biomes, worked out once (making the game's registries takes a while). */
    private static synchronized List<Identifier> gameBiomes() {
        if (gameBiomes == null) {
            gameBiomes = VanillaRegistries.createLookup().lookupOrThrow(Registries.BIOME).listElementIds().map(ResourceKey::identifier).toList();
        }
        return gameBiomes;
    }

    /**
     * What the screen tells of a biome: its picture and what it is like (for the game's own biomes, taken in the game
     * and written for this mod), and its id. A biome of a mod or a data pack has its id only.
     */
    private static OptionDescription biomeDescription(Identifier biome) {
        OptionDescription.Builder description = OptionDescription.createBuilder();
        Identifier picture = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/biomes/" + biome.getPath() + ".png");
        if (biome.getNamespace().equals("minecraft") && Minecraft.getInstance().getResourceManager().getResource(picture).isPresent()) {
            description.image(picture, PICTURE_WIDTH, PICTURE_HEIGHT);
        }
        String text = "biomewaypoints.biome." + biome.getNamespace() + "." + biome.getPath() + ".desc";
        if (Language.getInstance().has(text)) description.text(Component.translatable(text));
        return description.text(Component.literal(biome.toString()).withStyle(ChatFormatting.GRAY)).build();
    }

    /** A tick box for a biome, by its name: ticked when it is found, unticked when skipped. */
    private static Option<Boolean> biomeOption(BiomeWaypointsConfig config, Identifier biome) {
        String id = biome.toString();
        return Option.<Boolean>createBuilder()
                .name(Component.translatable(biome.toLanguageKey("biome")))
                .description(biomeDescription(biome))
                .binding(true, () -> !config.skippedBiomeIds().contains(biome), found -> {
                    config.skippedBiomes.removeIf(skipped -> biome.equals(skipped == null ? null : Identifier.tryParse(skipped.strip())));
                    if (!found) config.skippedBiomes.add(id);
                })
                .controller(TickBoxControllerBuilder::create)
                .build();
    }

    /**
     * The biomes of the game, by name: those of the world the player is in (with the ones mods and data packs add), the
     * game's own before joining one, and any skipped one besides. The void is left out: no world generation makes it.
     */
    public static List<Identifier> listedBiomes(BiomeWaypointsConfig config) {
        Minecraft minecraft = Minecraft.getInstance();
        Set<Identifier> biomes = new HashSet<>(config.skippedBiomeIds());
        if (minecraft.level != null) {
            minecraft.level.registryAccess().lookupOrThrow(Registries.BIOME).listElementIds().forEach(key -> biomes.add(key.identifier()));
        } else {
            biomes.addAll(gameBiomes());
        }
        biomes.remove(Identifier.withDefaultNamespace("the_void"));
        return biomes.stream()
                .sorted(Comparator.comparing((Identifier biome) -> Component.translatable(biome.toLanguageKey("biome")).getString()).thenComparing(Identifier::toString))
                .toList();
    }
}
