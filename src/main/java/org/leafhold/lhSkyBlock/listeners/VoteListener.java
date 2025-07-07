package org.leafhold.lhSkyBlock.listeners;

import org.leafhold.lhSkyBlock.lhSkyBlock;
import org.leafhold.lhSkyBlock.utils.DatabaseManager;

import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.vexsoftware.votifier.model.VotifierEvent;
import com.vexsoftware.votifier.model.Vote;

public class VoteListener implements Listener {
    private lhSkyBlock plugin;
    private DatabaseManager databaseManager;

    public VoteListener(lhSkyBlock plugin) {
        this.plugin = plugin;
        databaseManager = DatabaseManager.getInstance();
    }

    @EventHandler
    public void onVotifierEvent(VotifierEvent event) {
        Vote vote = event.getVote();
        String voterName = vote.getUsername();
        OfflinePlayer player = plugin.getServer().getOfflinePlayer(voterName);
        if (player != null && player.hasPlayedBefore()) {
            databaseManager.addVoteKey(player.getUniqueId());
        } else {
            plugin.getLogger().warning("Player " + voterName + " has not played before or does not exist.");
            return;
        }
    }

}
