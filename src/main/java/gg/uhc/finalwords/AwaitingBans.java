package gg.uhc.finalwords;

import com.google.common.collect.Maps;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

public class AwaitingBans implements Listener {

    protected static final String WILL_BE_BANNED = ChatColor.RED + "You will be banned from the server in %d seconds or when you leave the game";

    protected final Map<UUID, DelayedBan> waiting = Maps.newHashMap();
    protected final BanList banList = Bukkit.getBanList(BanList.Type.NAME);

    protected final Plugin plugin;
    protected final String banMessage;
    protected final long banDelay;
    protected final long banDelayTicks;
    protected final long banLength;

    public AwaitingBans(Plugin plugin, String banMessage, long banDelaySeconds, long banLengthMillis) {
        this.plugin = plugin;
        this.banMessage = banMessage;
        this.banDelay = banDelaySeconds;
        this.banDelayTicks = this.banDelay * 20;
        this.banLength = banLengthMillis;
    }

    public boolean isWaiting(UUID uuid) {
        return waiting.containsKey(uuid);
    }

    /**
     * Start a new ban for the UUID
     *
     * @return true if no ban started, false if ban is already running
     */
    public boolean add(UUID uuid) {
        DelayedBan ban = waiting.get(uuid);

        if (ban != null) return false;

        ban = new DelayedBan(uuid);
        ban.runTaskLater(plugin, banDelayTicks);

        ThrowawaySender.fromUUID(uuid).sendMessage(String.format(WILL_BE_BANNED, banDelay));
        waiting.put(uuid, ban);
        return true;
    }

    @EventHandler
    public void on(PlayerQuitEvent event) {
        DelayedBan ban = waiting.remove(event.getPlayer().getUniqueId());

        // do nothing if there was nothing waiting
        if (ban == null) return;

        // run now instead
        ban.cancel();
        ban.run();
    }

    class DelayedBan extends BukkitRunnable {

        protected final UUID uuid;

        DelayedBan(UUID uuid) {
            this.uuid = uuid;
        }

        @Override
        public void run() {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);

            banList.addBan(offline.getName(), banMessage, new Date(System.currentTimeMillis() + banLength), plugin.getName());

            // kick them if they're online currently
            if (offline.isOnline()) {
                offline.getPlayer().kickPlayer(banMessage);
            }

            // remove from waiting list
            waiting.remove(uuid);
        }
    }
}
