/*
Copyright (C) 2014 Elarcis.fr <contact+dev@elarcis.fr>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package fr.elarcis.scapegoat.async;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.scheduler.BukkitRunnable;

import code.husky.Database;
import fr.elarcis.scapegoat.ScapegoatPlugin;
import fr.elarcis.scapegoat.players.SGOnline;

/**
 * Retrieve player stats from the database, or create a record if none is found.
 * @author Elarcis
 */
public class GetPlayerStatsAsync extends BukkitRunnable
{
	protected static ScapegoatPlugin plugin = ScapegoatPlugin.getPlugin(ScapegoatPlugin.class);
	protected SGOnline player;
	
	/**
	 * @param player The player for whom to retrieve stats.
	 */
	public GetPlayerStatsAsync(SGOnline player)
	{
		this.player = player;
	}
	
	public void run()
	{
		Database db = plugin.getDb();
		
		try
		{
			String uuid = player.getId().toString().replaceAll("-", "");

			ResultSet res = db.querySQL("SELECT kills, deaths, score, plays, wins FROM players "
					+ "WHERE id=UNHEX('" + uuid + "');");
			
			if (res.first())
			{
				player.setKills(res.getInt("kills"));
				player.setDeaths(res.getInt("deaths"));
				player.setScore(res.getInt("score"));
				player.setPlays(res.getInt("plays"));
				player.setWins(res.getInt("wins"));
			}
			else
			{
				player.setKills(0);
				player.setDeaths(0);
				player.setScore(0);
				player.setPlays(0);
				player.setWins(0);

				db.updateSQL("INSERT INTO players (id) VALUES (UNHEX('" + uuid + "'));");
			}
			
			player.setDataFetched(true);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		catch (ClassNotFoundException e)
		{
			plugin.getLogger().warning("Attempting to retrieve data without any database configured.");
			e.printStackTrace();
		}
	}
}
