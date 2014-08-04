package fr.elarcis.scapegoat.async;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.bukkit.scheduler.BukkitRunnable;

import fr.elarcis.scapegoat.ScapegoatPlugin;
import fr.elarcis.scapegoat.players.SGOnline;

/**
 * Retrieve player stats from the database, or create a record if none is found.
 * @author Lars
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
		Connection c = plugin.getDbConnection();
		
		try
		{
			String uuid = player.getId().toString().replaceAll("-", "");

			Statement s = c.createStatement();
			ResultSet res = s.executeQuery("SELECT kills, deaths, score, plays, wins FROM players "
					+ "WHERE id=UNHEX('" + uuid + "');");
			
			boolean present = res.first();
			
			if (present)
			{
				player.setKills(res.getInt("kills"));
				player.setDeaths(res.getInt("deaths"));
				player.setScore(res.getInt("score"));
				player.setPlays(res.getInt("plays"));
				player.setWins(res.getInt("wins"));
			} else
			{
				player.setKills(0);
				player.setDeaths(0);
				player.setScore(0);
				player.setPlays(0);
				player.setWins(0);
				
				s.executeUpdate("INSERT INTO players (id) VALUES (UNHEX('" + uuid + "'));");
			}
			
			player.setDataFetched(true);
		} catch (SQLException e)
		{
			e.printStackTrace();
		}
	}
}
