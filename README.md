ChestSearch is an admin tool for finding chests within a WorldEdit selection. ChestSearch can handle *really* big worldedit selections (on the order of a billion+ blocks) just fine (though it may take a little bit).

# Installation

Install WorldEdit. Then install ChestSearch.

# Permission

`csearch.search` - permission for all csearch commands

# Usage

1. Make a WorldEdit selection
2. Type /csearch start
3. Wait for it to complete (you will get an ETA and updates intermittently)
4. When it's done, type `/csearch inspect <index>` where `<index>` is a non-negative integer in the range reported by the command upon completion of the search.
5. Alternatively, use `/csearch tp <index>` to tp to the chest.

# Commands

* `/csearch start` - start a new search
* `/csearch stop` - stop an in-progress search
* `/csearch inspect <index>` - view the inventory of a chest found by the search 
* `/csearch tp <index>` - teleport to the specified chest

# License

ChestSearch is available under the GNU GPLv3+.
