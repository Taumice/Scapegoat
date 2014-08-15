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

import java.util.UUID;

import org.bukkit.scheduler.BukkitRunnable;

import fr.elarcis.scapegoat.players.SGOnline;
import fr.elarcis.scapegoat.players.SGSpectator;

/**
 * Execute custom operations to apply to a player after he's respawned.
 * @author Elarcis
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
