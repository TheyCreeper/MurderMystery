package dev.theycreeper.murdermystery.corpse;

import com.destroystokyo.paper.ClientOption;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.Location;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.inventory.EntityEquipment;

import java.util.UUID;

public class FakePlayer {
    private final Mannequin mannequin;

    public FakePlayer(Player player, Location location) {
        Location corpseLocation = location.clone().add(0, 0.25, 0);
        corpseLocation.setPitch(0);
        mannequin = corpseLocation.getWorld().spawn(corpseLocation, Mannequin.class, corpse -> {
            corpse.setProfile(ResolvableProfile.resolvableProfile(player.getPlayerProfile()));
            corpse.setSkinParts(player.getClientOption(ClientOption.SKIN_PARTS));
            corpse.setMainHand(player.getClientOption(ClientOption.MAIN_HAND));
            corpse.setDescription(null);
            corpse.setPose(Pose.SLEEPING, true);
            corpse.setImmovable(true);
            corpse.setAI(false);
            corpse.setGravity(false);
            corpse.setInvulnerable(true);
            corpse.setCollidable(false);
            corpse.setSilent(true);
            corpse.setPersistent(false);

            EntityEquipment equipment = corpse.getEquipment();
            equipment.setArmorContents(player.getInventory().getArmorContents());
            equipment.setItemInMainHand(player.getInventory().getItemInMainHand());
            equipment.setItemInOffHand(player.getInventory().getItemInOffHand());
        });
    }

    public void remove() {
        mannequin.remove();
    }

    public int getEntityId() {
        return mannequin.getEntityId();
    }

    public UUID getUuid() {
        return mannequin.getUniqueId();
    }
}
