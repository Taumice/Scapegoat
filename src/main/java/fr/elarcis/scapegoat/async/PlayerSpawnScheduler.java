package fr.elarcis.scapegoat.async;

import java.util.UUID;

import org.bukkit.scheduler.BukkitRunnable;

import fr.elarcis.scapegoat.players.SGOnline;
import fr.elarcis.scapegoat.players.SGSpectator;

/**
 * Execute custom operations to apply to a player after he's respawned.
 * @author Lars
 */
public class PlayerSpawnScheduler extends BukkitRunnable
{
	protected UUID id;
	
	/**
	 * @param id The player to which the operations will be applied.
	 */
	public PlayerSpawnScheduler(UUID id)
	{
		this.id = id;
	}

	public void run()
	{
		SGSpectator spec = SGOnline.getSGSpectator(id);
		
		if (spec != null)
			spec.respawn();
	}

}
