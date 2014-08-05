package fr.elarcis.scapegoat.async;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.bukkit.scheduler.BukkitRunnable;

import fr.elarcis.scapegoat.ScapegoatPlugin;
import fr.elarcis.scapegoat.players.SGOnline;

public class SetPlayerStatsAsync extends BukkitRunnable
{
	protected static ScapegoatPlugin plugin = ScapegoatPlugin.getPlugin(ScapegoatPlugin.class);
	protected SGOnline player;
	
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
