package gg.uhc.finalwords;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.UUID;

public class ApprovalCommand implements CommandExecutor {

    protected static final String NO_LONGER_VALID = ChatColor.RED + "Those last words are no longer applicable for approval";

    protected final FinalWordsHandler finalWordsHandler;

    public ApprovalCommand(FinalWordsHandler finalWordsHandler) {
        this.finalWordsHandler = finalWordsHandler;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if (args.length != 2) {
            return false;
        }

        boolean approve;
        switch (args[0]) {
            case "deny":
                approve = false; break;
            case "accept":
                approve = true; break;
            default:
                return false;
        }

        try {
            UUID uuid = UUID.fromString(args[1]);

            boolean valid = finalWordsHandler.approve(uuid, approve);

            if (!valid) {
                sender.sendMessage(NO_LONGER_VALID);
            }

            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
