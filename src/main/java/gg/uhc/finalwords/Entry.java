package gg.uhc.finalwords;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class Entry extends JavaPlugin {

    @Override
    public void onEnable() {
        FileConfiguration configuration = getConfig();
        configuration.options().copyDefaults(true);
        saveConfig();

        String banMessage = ChatColor.translateAlternateColorCodes('&', configuration.getString("ban message"));
        int banDelaySeconds = configuration.getInt("ban delay seconds");
        int banLengthMinutes = configuration.getInt("ban length minutes");
        int maxLines = configuration.getInt("maximum lines");
        int approvalTimeSeconds = configuration.getInt("automatic deny time seconds");

        AwaitingBans bans = new AwaitingBans(this, banMessage, banDelaySeconds, banLengthMinutes * 60 * 1000);
        FinalWordsHandler finalWordsHandler = new FinalWordsHandler(bans, this, maxLines, approvalTimeSeconds);

        getServer().getPluginManager().registerEvents(bans, this);
        getServer().getPluginManager().registerEvents(finalWordsHandler, this);

        getCommand("finalwords").setExecutor(new ApprovalCommand(finalWordsHandler));
    }
}
