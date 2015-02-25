/*
 * Copyright (C) 2015 Joshua Michael Hertlein
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.jmhertlein.chestsearch;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.Selection;
import com.sk89q.worldedit.regions.Region;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.jmhertlein.reflective.CommandDefinition;
import net.jmhertlein.reflective.annotation.CommandMethod;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;

/**
 *
 * @author joshua
 */
public class ChestSearchCommandDefinition implements CommandDefinition {
    private final ChestSearchPlugin plugin;
    private final WorldEditPlugin wep;
    private final Map<OfflinePlayer, List<Location>> searches;
    private final Map<OfflinePlayer, ChestSearchTask> searchers;

    public ChestSearchCommandDefinition(ChestSearchPlugin p) {
        wep = (WorldEditPlugin) p.getServer().getPluginManager().getPlugin("WorldEdit");
        searches = new HashMap<>();
        searchers = new HashMap<>();
        this.plugin = p;
    }

    @CommandMethod(path = "csearch start", permNode = "csearch.search")
    public void searchChests(Player p, String[] args) {
        beginSearch(p);
    }

    @CommandMethod(path = "csearch inspect", permNode = "csearch.csearch", requiredArgs = 1, helpMsg = "Usage: /csearch inspect <id>")
    public void inspectChest(Player p, Integer chestIndex) {
        inspectChest(p, chestIndex, false);
    }

    @CommandMethod(path = "csearch tp", permNode = "csearch.csearch", requiredArgs = 1, helpMsg = "Usage: /csearch tp <id>")
    public void tpAndInspectChest(Player p, Integer chestIndex) {
        inspectChest(p, chestIndex, true);
    }

    @CommandMethod(path = "csearch stop", permNode = "csearch.csearch")
    public void stopSearch(Player p) {
        if(searchers.containsKey(p)) {
            searchers.remove(p).cancel();
            p.sendMessage("Search cancelled.");
        } else {
            p.sendMessage("You're not currently running csearch.");
        }
    }

    private void beginSearch(Player p) {
        if(searchers.containsKey(p)) {
            p.sendMessage("You're already running csearch. Use \"/csearch stop\" if you want to abort.");
            return;
        }

        Selection sel = wep.getSelection(p);
        if(sel == null) {
            p.sendMessage("You don't have a WorldEdit selection.");
            return;
        }
        Region r;
        try {
            r = sel.getRegionSelector().getRegion();
        } catch(IncompleteRegionException ex) {
            p.sendMessage("Incomplete region: " + ex.getLocalizedMessage());
            return;
        }

        ChestSearchTask t = new ChestSearchTask(searches, p, r.getWorld(), r.getArea(), r.iterator());
        t.runTaskTimer(plugin, 0, 1);
        searchers.put(p, t);
    }

    private void inspectChest(Player p, int chestIndex, boolean tp) {
        List<Location> chests;
        //lots of boring input validation below
        {
            if(searches.containsKey(p))
                chests = searches.get(p);
            else {
                p.sendMessage("Error: No search to inspect. Prepare a search with /csearch start.");
                return;
            }

            if(chestIndex < 0 || chestIndex >= chests.size()) {
                p.sendMessage(String.format("Error: index %s is out of range! Range is: [0,%s].", chestIndex, chests.size() - 1));
                return;
            }
        }

        //now that we're finally done with all the boring stuff...
        //...chest searching time
        Location l = chests.get(chestIndex);
        Chest c = (Chest) l.getBlock().getState();

        if(tp) {
            Location tpLoc = l.add(0, 2, 0);
            p.teleport(tpLoc);
        }

        p.sendMessage("Viewing chest at: " + formatLoc(l));
        p.openInventory(c.getBlockInventory());
    }

    private static String formatLoc(Location l) {
        return String.format("(%s, %s, %s, %s)", l.getBlockX(), l.getBlockY(), l.getBlockZ(), l.getWorld().getName());
    }
}
