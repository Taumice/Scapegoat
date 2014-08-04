package fr.elarcis.scapegoat.async;

import java.util.UUID;

import org.bukkit.scheduler.BukkitRunnable;

import fr.elarcis.scapegoat.players.SGOnline;
import fr.elarcis.scapegoat.players.SGSpectator;

public class PlayerSpawnScheduler extends BukkitRunnable
{
	protected UUID id;
	
	public PlayerSpawnScheduler(UUID id)
	{
		this.id = id;
	}

	public void run()
	{
		SGSpectator spec = SGOnline.getSGSpectator(id);
		
		if (spec != null)
		{
			spec.respawn();
		}
	}

}
