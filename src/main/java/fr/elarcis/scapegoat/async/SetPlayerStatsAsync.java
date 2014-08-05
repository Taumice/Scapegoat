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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.bukkit.scheduler.BukkitRunnable;

import fr.elarcis.scapegoat.ScapegoatPlugin;
import fr.elarcis.scapegoat.players.SGOnline;

/**
 * Save player stats to database.
 * @author Lars
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
		
		Connection c = plugin.getDbConnection();
		
		try
		{
			String uuid = player.getId().toString().replaceAll("-", "");
			
			Statement s = c.createStatement();
			s.executeUpdate("UPDATE players SET "
					+ "kills=" + player.getKills() + ", "
					+ "deaths=" + player.getDeaths() + ", "
					+ "score=" + player.getScore() + ", "
					+ "plays=" + player.getPlays() + ", "
					+ "wins=" + player.getWins() + " "
					+ "WHERE id=UNHEX('" + uuid + "');"
					);
			
		} catch (SQLException e)
		{
			e.printStackTrace();
		}
	}
}
