/*
Copyright (C) 2014 Elarcis.fr <contact+dev@elarcis.fr>

Scapegoat is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
any later version.

Scapegoat is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Scapegoat.  If not, see <http://www.gnu.org/licenses/>.
*/

package fr.elarcis.scapegoat.async;

import java.sql.SQLException;

import org.bukkit.scheduler.BukkitRunnable;

import code.husky.Database;
import fr.elarcis.scapegoat.ScapegoatPlugin;
import fr.elarcis.scapegoat.players.SGOnline;

/**
 * Save player stats to database.
 * @author Elarcis
 */
public class SetPlayerStatsAsync extends BukkitRunnable
{
	protected static ScapegoatPlugin plugin = ScapegoatPlugin.getPlugin(ScapegoatPlugin.class);
	protected SGOnline player;
	
	/**
	 * @param player The player for whom to save stats.
	 */
	public SetPlayerStatsAsync(SGOnline player)
	{
		this.player = player;
	}
	
	public void run()
	{
		if (!player.getDataFetched())
			return;
		
		Database db = plugin.getDb();
		
		try
		{
			String uuid = player.getId().toString().replaceAll("-", "");

			db.updateSQL("UPDATE players SET "
					+ "kills=" + player.getKills() + ", "
					+ "deaths=" + player.getDeaths() + ", "
					+ "score=" + player.getScore() + ", "
					+ "plays=" + player.getPlays() + ", "
					+ "wins=" + player.getWins() + " "
					+ "WHERE id=UNHEX('" + uuid + "');"
					);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		catch (ClassNotFoundException e)
		{
			plugin.getLogger().warning("Attempting to save data without any database configured.");
			e.printStackTrace();
		}
	}
}
