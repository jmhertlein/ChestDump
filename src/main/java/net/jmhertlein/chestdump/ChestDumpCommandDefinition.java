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
package net.jmhertlein.chestdump;

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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 *
 * @author joshua
 */
public class ChestDumpCommandDefinition implements CommandDefinition {
    private final ChestDumpPlugin p;
    private final WorldEditPlugin wep;
    private final Map<OfflinePlayer, List<Location>> dumps;
    private final Map<OfflinePlayer, ChestDumpTask> dumpers;

    public ChestDumpCommandDefinition(ChestDumpPlugin p) {
        wep = (WorldEditPlugin) p.getServer().getPluginManager().getPlugin("WorldEdit");
        dumps = new HashMap<>();
        dumpers = new HashMap<>();
        this.p = p;
    }

    @CommandMethod(path = "cdump start", permNode = "cdump.dump")
    public void dumpChests(CommandSender s, String[] args) {
        beginDump(s);
    }

    @CommandMethod(path = "cdump inspect", permNode = "cdump.dump",
                   requiredArgs = 1, helpMsg = "Usage: /cdump inspect <id>")
    public void inspectChest(CommandSender s, int chestIndex) {
        inspectChest(s, chestIndex, false);
    }

    @CommandMethod(path = "cdump tp", permNode = "cdump.dump",
                   requiredArgs = 1, helpMsg = "Usage: /cdump tp <id>")
    public void tpAndInspectChest(CommandSender s, int chestIndex) {
        inspectChest(s, chestIndex, true);
    }

    @CommandMethod(path = "cdump stop", permNode = "cdump.dump")
    public void stopDump(CommandSender s) {
        if(dumpers.containsKey((Player) s)) {
            dumpers.remove((Player) s).cancel();
            s.sendMessage("Dump cancelled.");
        } else {
            s.sendMessage("You're not currently running cdump.");
        }
    }

    private void beginDump(CommandSender s) {
        if(dumpers.containsKey((Player) s)) {
            s.sendMessage("You're already running cdump. Use \"/cdump stop\" if you want to abort.");
            return;
        }

        Selection sel = wep.getSelection((Player) s);
        if(sel == null) {
            s.sendMessage("You don't have a WorldEdit selection.");
            return;
        }
        Region r;
        try {
            r = sel.getRegionSelector().getRegion();
        } catch(IncompleteRegionException ex) {
            s.sendMessage("Incomplete region: " + ex.getLocalizedMessage());
            return;
        }

        ChestDumpTask t = new ChestDumpTask(dumps, (Player) s, r.getWorld(), r.getArea(), r.iterator());
        t.runTaskTimer(p, 0, 1);
        dumpers.put((Player) s, t);
    }

    private void inspectChest(CommandSender s, int chestIndex, boolean tp) {
        List<Location> chests;
        //lots of boring input validation below
        {
            if(dumps.containsKey((OfflinePlayer) s))
                chests = dumps.get((OfflinePlayer) s);
            else {
                s.sendMessage("Error: No dump to inspect. Prepare a dump with /cdump start.");
                return;
            }

            if(chestIndex < 0 || chestIndex >= chests.size()) {
                s.sendMessage(String.format("Error: index %s is out of range! Range is: [0,%s].", chestIndex, chests.size() - 1));
                return;
            }
        }

        //now that we're finally done with all the boring stuff...
        //...chest dumping time
        Location l = chests.get(chestIndex);
        Chest c = (Chest) l.getBlock().getState();

        if(tp) {
            Location tpLoc = l.add(0, 2, 0);
            ((Player) s).teleport(tpLoc);
        }

        s.sendMessage("Viewing chest at: " + formatLoc(l));
        ((Player) s).openInventory(c.getBlockInventory());
    }

    private static String formatLoc(Location l) {
        return String.format("(%s, %s, %s, %s)", l.getBlockX(), l.getBlockY(), l.getBlockZ(), l.getWorld().getName());
    }
}
