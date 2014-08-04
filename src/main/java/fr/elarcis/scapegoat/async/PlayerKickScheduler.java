package fr.elarcis.scapegoat.async;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import fr.elarcis.scapegoat.ScapegoatPlugin;

public class PlayerKickScheduler extends BukkitRunnable
{
	protected ScapegoatPlugin plugin = ScapegoatPlugin.getPlugin(ScapegoatPlugin.class);
	protected UUID id;
	protected String kickMessage;

	public PlayerKickScheduler(UUID id, String kickMessage)
	{
		this.id = id;
		this.kickMessage = kickMessage;
	}

	public void run()
	{
		Player p = plugin.getServer().getPlayer(id);
		
		if (p != null)
			p.kickPlayer(kickMessage);
		
		if (Bukkit.getOnlinePlayers().length == 0)
		{
			Bukkit.shutdown();
		}
	}

}
