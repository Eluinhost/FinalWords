package gg.uhc.finalwords;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

public class FinalWordsHandler implements Listener {

    public static final String ADMIN_PERMISSION = "uhc.finalwords.admin";
    public static final String BYPASS = "uhc.finalwords.bypass";

    protected static final String SENT_FOR_APPROVAL = ChatColor.AQUA + "Sent final words for approval";
    protected static final String NOT_ALLOWED_TO_CHAT = ChatColor.RED + "You are not allowed to chat after death";
    protected static final String MESSAGES_LEFT = ChatColor.DARK_GRAY + "You have %s mesaages left";
    protected static final String NONE_LEFT = ChatColor.RED + "You have no messages left";
    protected static final String NOTIFY_DEATH = ChatColor.AQUA + "You can now send up to %d messages before your ban. Type 'done' to send for approval early";
    protected static final String APPROVAL_HEADER = "Approval for %s's final words required (%ds before auto-deny):";

    enum Stage {
        WRITING,
        APPROVAL,
        COMPLETED
    }

    // stores words associated with a player
    protected final ListMultimap<UUID, String> finalWords = ArrayListMultimap.create(16, 3);

    // track the stage of each player
    protected final ConcurrentMap<UUID, Stage> playerStages = Maps.newConcurrentMap();

    // keeps track of automatic approval denies for each player
    protected final Map<UUID, DelayedDeny> automaticDenies = Maps.newHashMap();

    protected final AwaitingBans bans;
    protected final Plugin plugin;

    protected final int maxLines;
    protected final long approvalTime;
    protected final long approvalTimeSeconds;

    public FinalWordsHandler(AwaitingBans bans, Plugin plugin, int maxLines, long approvalTimeSeconds) {
        this.bans = bans;
        this.plugin = plugin;
        this.maxLines = maxLines;
        this.approvalTime = approvalTimeSeconds * 20;
        this.approvalTimeSeconds = approvalTimeSeconds;
    }

    public boolean approve(UUID uuid, boolean approved) {
        // skip if it wasn't in approval stage in the first place
        if (playerStages.get(uuid) != Stage.APPROVAL) return false;

        // cancel automatic deny
        DelayedDeny deny = automaticDenies.get(uuid);
        if (deny != null) deny.cancel();

        String name = Bukkit.getOfflinePlayer(uuid).getName();

        List<String> messages = finalWords.get(uuid);

        if (approved) {
            // show everyone the final messages

            Bukkit.broadcastMessage(ChatColor.GOLD + name + "'s final words:");
            for (String message : messages) {
                Bukkit.broadcastMessage(ChatColor.DARK_RED + message);
            }
            Bukkit.broadcastMessage(ChatColor.GOLD + "=====================");
        } else {
            // tell the player and all admins they were denied

            String message = ChatColor.RED + name + "'s final words were denied";

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission(ADMIN_PERMISSION)) {
                    player.sendMessage(message);
                }
            }

            ThrowawaySender.fromUUID(uuid).sendMessage(message);
        }

        goToCompleted(uuid);
        return true;
    }

    protected void goToCompleted(UUID uuid) {
        boolean moved = playerStages.replace(uuid, Stage.APPROVAL, Stage.COMPLETED);

        if (!moved) return;

        // remove all of the data about the player if their ban been completed
        if (!bans.isWaiting(uuid)) {
            removeTracking(uuid);
        }
    }

    protected void goToApprove(UUID uuid) {
        boolean moved = playerStages.replace(uuid, Stage.WRITING, Stage.APPROVAL);

        if (!moved) return;

        List<String> messages = finalWords.get(uuid);

        if (messages.size() == 0) {
            // skip straight to completed bypassing #approve(UUID, boolean) if no lines were provided
            goToCompleted(uuid);
            return;
        }

        // build display for admins with clickables
        String name = Bukkit.getOfflinePlayer(uuid).getName();

        TextComponent base = new TextComponent("");
        base.setColor(ChatColor.GRAY);

        TextComponent header = new TextComponent(String.format(APPROVAL_HEADER, name, approvalTimeSeconds));
        header.setColor(ChatColor.RED);
        header.setBold(true);
        base.addExtra(header);

        for (String message : messages) {
            base.addExtra("\n" + message);
        }

        base.addExtra("\n");

        String stringUUID = uuid.toString();

        TextComponent approve = new TextComponent("Approve");
        approve.setColor(ChatColor.GREEN);
        approve.setBold(true);
        approve.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/finalwords accept " + stringUUID));

        TextComponent deny = new TextComponent("Deny");
        deny.setColor(ChatColor.RED);
        deny.setBold(true);
        deny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/finalwords deny " + stringUUID));

        base.addExtra(approve);
        base.addExtra(" | ");
        base.addExtra(deny);

        // send the message to admins
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(ADMIN_PERMISSION)) {
                player.spigot().sendMessage(base);
            }
        }

        // automatically deny later if no response
        new DelayedDeny(uuid).runTaskLater(plugin, approvalTime);
    }

    protected void onActualMessageEvent(UUID uuid, String message) {
        ThrowawaySender sender = ThrowawaySender.fromUUID(uuid);

        switch (playerStages.get(uuid)) {
            case WRITING:
                // if they're not allowed lines
                if (maxLines <= 0) {
                    sender.sendMessage(NOT_ALLOWED_TO_CHAT);
                    return;
                }

                // check for special case 'done'
                if (message.equalsIgnoreCase("done")) {
                    sender.sendMessage(SENT_FOR_APPROVAL);
                    goToApprove(uuid);
                    return;
                }

                // add the message
                List<String> messages = finalWords.get(uuid);
                messages.add(message);

                // check if full
                int count = messages.size();
                if (count >= maxLines) {
                    sender.sendMessage(SENT_FOR_APPROVAL);
                    goToApprove(uuid);
                    return;
                }

                sender.sendMessage(String.format(MESSAGES_LEFT, maxLines - count));
                break;
            case APPROVAL:
            case COMPLETED:
                sender.sendMessage(NONE_LEFT);
                break;
        }
    }

    protected void removeTracking(UUID uuid) {
        playerStages.remove(uuid);
        finalWords.removeAll(uuid);

        DelayedDeny deny = automaticDenies.get(uuid);
        if (deny != null) deny.cancel();
    }


    @EventHandler
    public void on(PlayerDeathEvent event) {
        // skip if they have bypass permissions
        if (event.getEntity().hasPermission(BYPASS)) return;

        UUID uuid = event.getEntity().getUniqueId();

        Stage previous = playerStages.putIfAbsent(uuid, Stage.WRITING);

        // if they are new to the tracking add their ban
        if (previous == null) {
            ThrowawaySender.fromUUID(uuid).sendMessage(String.format(NOTIFY_DEATH, maxLines));
            bans.add(uuid);
        }
    }

    @EventHandler
    public void on(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        Stage stage = playerStages.get(uuid);

        if (stage == Stage.WRITING) {
            goToApprove(uuid);
        } else if (stage == Stage.COMPLETED) {
            removeTracking(uuid);
        }
    }

    @EventHandler
    public void on(AsyncPlayerChatEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        if (!playerStages.containsKey(uuid)) return;

        event.setCancelled(true);

        // run now if we can otherwise wait for the next tick to process it
        if (!event.isAsynchronous()) {
            onActualMessageEvent(event.getPlayer().getUniqueId(), event.getMessage());
        } else {
            new DelayedChat(event.getPlayer().getUniqueId(), event.getMessage()).runTask(plugin);
        }
    }

    class DelayedChat extends BukkitRunnable {
        protected final UUID uuid;
        protected final String message;

        DelayedChat(UUID uuid, String message) {
            this.uuid = uuid;
            this.message = message;
        }

        @Override
        public void run() {
            onActualMessageEvent(uuid, message);
        }
    }

    class DelayedDeny extends BukkitRunnable {
        protected final UUID uuid;

        DelayedDeny(UUID uuid) {
            this.uuid = uuid;
        }

        @Override
        public void run() {
            automaticDenies.remove(uuid);
            approve(uuid, false);
        }
    }


}
