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

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import fr.elarcis.scapegoat.ScapegoatPlugin;

/**
 * Kick a player from the server with a message.
 * @author Elarcis
 */
public class PlayerKickScheduler extends BukkitRunnable
{
	protected ScapegoatPlugin plugin = ScapegoatPlugin.getPlugin(ScapegoatPlugin.class);
	protected UUID id;
	protected String kickMessage;

	/**
	 * @param id The player to kick's UUID.
	 * @param kickMessage The message to show him when kicked.
	 */
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
			Bukkit.shutdown();
	}

}
