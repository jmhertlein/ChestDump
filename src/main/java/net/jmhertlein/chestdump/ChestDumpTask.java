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

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockType;
import com.sk89q.worldedit.world.World;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 *
 * @author joshua
 */
public class ChestDumpTask extends BukkitRunnable {
    private final NumberFormat formatter = new DecimalFormat("#0.00");
    private final int BLOCKS_PER_PASS = 200000;
    private final int SECONDS_PER_MESSAGE = 7;
    private final Map<OfflinePlayer, List<Location>> dumps;
    private final Player p;
    private final Iterator<BlockVector> i;
    private final List<Location> chests;
    private final World w;
    private long lastMessageTime, startTime;
    private int totalProgress;
    private final int totalSize;

    public ChestDumpTask(Map<OfflinePlayer, List<Location>> dumps, Player p, World w, int totalSize, Iterator<BlockVector> i) {
        this.dumps = dumps;
        this.p = p;
        this.i = i;
        this.chests = new ArrayList<>();
        this.w = w;
        p.sendMessage("Starting cdump...");
        lastMessageTime = System.currentTimeMillis();
        this.totalSize = totalSize;
        this.totalProgress = 0;
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public void run() {
        //do 100k blocks at once, this is about 1 tick's worth of time on my machine
        for(int c = 0; c < BLOCKS_PER_PASS; c++) {
            if(i.hasNext()) {
                BaseBlock block;
                BlockVector v = i.next();
                block = w.getBlock(v);
                if(block.getType() == BlockType.CHEST.getID()) {
                    //there might be a better way to go from BlockVector -> Location?
                    chests.add(new Location(
                            p.getServer().getWorld(w.getName()),
                            v.getX(),
                            v.getY(),
                            v.getZ()
                    ));
                }
            }
        }

        totalProgress += BLOCKS_PER_PASS;
        if((System.currentTimeMillis() - lastMessageTime) > 1000 * SECONDS_PER_MESSAGE) {

            p.sendMessage(formatter.format((totalProgress / ((double) totalSize)) * 100) + "% done, " + chests.size() + " chests so far.");
            p.sendMessage("Est: " + estimateRemainingMins() + " mins remaining.");
            lastMessageTime = System.currentTimeMillis();
        }

        if(!i.hasNext()) {
            cancel();
            dumps.put(p, chests);
            p.sendMessage("Ready to inspect " + chests.size() + " chests. Ids are in range: [0," + (chests.size() - 1) + "].");
        }

    }

    private String estimateRemainingMins() {
        double elapsedMins = (System.currentTimeMillis() - startTime) / ((double) (1000 * 60));
        double blocksPerMin = totalProgress / elapsedMins;

        int remaining = totalSize - totalProgress;

        double remainingMins = remaining / blocksPerMin;

        return formatter.format(remainingMins);
    }
}
