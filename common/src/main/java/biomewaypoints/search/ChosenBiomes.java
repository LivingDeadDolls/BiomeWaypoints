package biomewaypoints.search;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.resources.Identifier;

/** The biomes of the settings, as ids. */
public final class ChosenBiomes {
    private ChosenBiomes() {
    }

    /** The biomes written in the list, in its order, leaving out what is not an id. */
    public static Set<Identifier> of(Collection<String> biomes) {
        Set<Identifier> ids = new LinkedHashSet<>();
        for (String biome : biomes) {
            Identifier id = biome == null ? null : Identifier.tryParse(biome.strip());
            if (id != null) ids.add(id);
        }
        return ids;
    }

    /** The biomes not to search for: those skipped, and those whose waypoints are not shown. */
    public static Set<Identifier> notSearched(Collection<String> skipped, Collection<String> hidden) {
        Set<Identifier> ids = of(skipped);
        ids.addAll(of(hidden));
        return ids;
    }
}
