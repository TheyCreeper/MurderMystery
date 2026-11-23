
package dev.theycreeper.murdermystery.manager;

import dev.theycreeper.murdermystery.corpse.FakePlayer;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class CorpseManager implements Listener {

    private final JavaPlugin plugin;

    private record ManagedCorpse(FakePlayer corpse, Chunk chunk) {}

    private final Map<UUID, ManagedCorpse> corpses = new HashMap<>();
    private final Map<Chunk, Integer> chunkReferences = new HashMap<>();

    public CorpseManager(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public FakePlayer spawn(Player player) {
        Location location = player.getLocation();
        Chunk chunk = location.getChunk();
        if (!chunkReferences.containsKey(chunk)) {
            chunk.addPluginChunkTicket(plugin);
        }
        chunkReferences.merge(chunk, 1, Integer::sum);

        FakePlayer corpse;
        try {
            corpse = new FakePlayer(player, location);
        } catch (RuntimeException e) {
            releaseChunk(chunk);
            throw e;
        }
        corpses.put(corpse.getUuid(), new ManagedCorpse(corpse, chunk));

        // Automatically remove after 60 seconds.
        Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> remove(corpse.getUuid()),
                20L * 60
        );

        return corpse;
    }

    public void remove(UUID uuid) {
        ManagedCorpse corpse = corpses.remove(uuid);

        if (corpse == null) return;

        corpse.corpse().remove();
        releaseChunk(corpse.chunk());
    }

    public void removeAll() {
        for (UUID uuid : new ArrayList<>(corpses.keySet())) remove(uuid);
    }

    private void releaseChunk(Chunk chunk) {
        int remaining = chunkReferences.get(chunk) - 1;
        if (remaining == 0) {
            chunkReferences.remove(chunk);
            chunk.removePluginChunkTicket(plugin);
        } else {
            chunkReferences.put(chunk, remaining);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (corpses.containsKey(event.getRightClicked().getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
