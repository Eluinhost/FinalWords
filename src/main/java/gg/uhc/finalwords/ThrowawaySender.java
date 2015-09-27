package gg.uhc.finalwords;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public abstract class ThrowawaySender {

    static class Exists extends ThrowawaySender {

        protected CommandSender sender;

        protected Exists(CommandSender sender) {
            this.sender = sender;
        }

        @Override
        public void sendMessage(String message) {
            this.sender.sendMessage(message);
        }
    }

    static class DoesntExist extends ThrowawaySender {
        @Override
        public void sendMessage(String message) {
            // skip
        }
    }

    public static ThrowawaySender fromPlayer(Player player) {
        return player == null ? new DoesntExist() : new Exists(player);
    }

    /**
     * Create a new throwaway sender from the UUID. If the player exists this will store a reference to the player
     * so it shouldn't be held in memory, just use for quick access.
     *
     * @param uuid the player UUID
     */
    public static ThrowawaySender fromUUID(UUID uuid) {
        return fromPlayer(Bukkit.getPlayer(uuid));
    }

    public abstract void sendMessage(String message);
}
