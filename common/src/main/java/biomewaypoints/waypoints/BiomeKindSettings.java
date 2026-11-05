package biomewaypoints.waypoints;

import biomewaypoints.config.BiomeWaypointsConfig;
import biomewaypoints.search.BiomeFinder;
import java.util.Collection;
import java.util.Optional;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import waypointsapi.api.KindSettings;

/** How the waypoints of the biomes are shown, kept in {@link BiomeWaypointsConfig}. A biome not shown is not searched for. */
final class BiomeKindSettings implements KindSettings {
    private static final BiomeWaypointsConfig DEFAULTS = new BiomeWaypointsConfig();

    private static BiomeWaypointsConfig config() {
        return BiomeWaypointsConfig.get();
    }

    @Override
    public boolean shown() {
        return config().shown;
    }

    @Override
    public void setShown(boolean shown) {
        config().shown = shown;
    }

    @Override
    public int displayDistance() {
        return config().displayDistance;
    }

    @Override
    public void setDisplayDistance(int blocks) {
        config().displayDistance = blocks;
    }

    @Override
    public int defaultDisplayDistance() {
        return DEFAULTS.displayDistance;
    }

    /** No place is found farther than {@code /locate biome} looks. */
    @Override
    public int maxDisplayDistance() {
        return BiomeFinder.RADIUS;
    }

    @Override
    public int maxShown() {
        return config().maxShown;
    }

    @Override
    public void setMaxShown(int count) {
        config().maxShown = count;
    }

    @Override
    public int defaultMaxShown() {
        return DEFAULTS.maxShown;
    }

    @Override
    public boolean groupShown(Identifier group) {
        return !config().hiddenBiomes.contains(group.toString());
    }

    @Override
    public void setGroupShown(Identifier group, boolean shown) {
        config().hiddenBiomes.remove(group.toString());
        if (!shown) config().hiddenBiomes.add(group.toString());
    }

    @Override
    public int groupColor(Identifier group) {
        return config().biomeColors.getOrDefault(group.toString(), defaultGroupColor(group));
    }

    @Override
    public void setGroupColor(Identifier group, int rgb) {
        if (rgb == -1) config().biomeColors.remove(group.toString());
        else config().biomeColors.put(group.toString(), rgb & 0xFFFFFF);
    }

    @Override
    public int defaultGroupColor(Identifier group) {
        return BiomeColors.of(group);
    }

    /** Only the nearest place of each biome by default: each search adds the nearest from where the player is, so they pile up. */
    @Override
    public boolean defaultGroupNearestOnly(Identifier group) {
        return true;
    }

    @Override
    public boolean groupNearestOnly(Identifier group) {
        return config().biomeNearestOnly.getOrDefault(group.toString(), defaultGroupNearestOnly(group));
    }

    @Override
    public void setGroupNearestOnly(Identifier group, boolean nearestOnly) {
        if (nearestOnly == defaultGroupNearestOnly(group)) config().biomeNearestOnly.remove(group.toString());
        else config().biomeNearestOnly.put(group.toString(), nearestOnly);
    }

    @Override
    public Collection<Identifier> groups() {
        return BiomeWaypointsConfig.listedBiomes(config());
    }

    @Override
    public void save() {
        BiomeWaypointsConfig.HANDLER.save();
    }

    @Override
    public Optional<Screen> settingsScreen(Screen parent) {
        return Optional.of(BiomeWaypointsConfig.createScreen(parent));
    }
}
