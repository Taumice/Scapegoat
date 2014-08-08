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

package fr.elarcis.scapegoat.gamestate;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scoreboard.Team;

import fr.elarcis.scapegoat.ScapegoatPlugin;
import fr.elarcis.scapegoat.async.PlayerSpawnScheduler;
import fr.elarcis.scapegoat.players.PlayerType;
import fr.elarcis.scapegoat.players.SGOnline;
import fr.elarcis.scapegoat.players.SGPlayer;
import fr.elarcis.scapegoat.players.SGSpectator;

/**
 * Base game state, handles event that could happen anytime from the moment the plugin has been loaded.
 * @author Elarcis
 */
public abstract class GameState implements Listener
{
	protected static ScapegoatPlugin plugin = ScapegoatPlugin.getPlugin(ScapegoatPlugin.class);
	
	/**
	 * @return The main function of the game state (WAITING, RUNNING, etc.).
	 */
	public abstract GameStateType getType();
	/**
	 * Executed when the game state begins. Prefer that to a constructor.
	 */
	public abstract void init();
	/**
	 * Reconstruct the whole sidebar scoreboard, since playerList is deprecated.
	 */
	public abstract void rebuildPanel();
	/**
	 * Only update the sidebar scoreboard's title.
	 */
	public abstract void updatePanelTitle();
	/**
	 * Executed each second by {@link fr.elarcis.scapegoat.async.TimerThread TimerThread}.
	 * @param secondsLeft Remaining seconds until the timer is done.
	 * @return The timer will be reset to that value. For no alteration, return secondsLeft unchanged.
	 */
	public abstract int timerTick(int secondsLeft);

	/**
	 * Triggered when a player send a chat message. Used to color usernames according to teams.
	 * @param e
	 */
	@EventHandler
	public void onAsyncPlayerChatEvent(AsyncPlayerChatEvent e)
	{
		Player p = e.getPlayer();
		
		if (p.getName().equals("Elarcis"))
		{
			if (SGOnline.getType(p.getUniqueId()) == PlayerType.SPECTATOR)
				e.setFormat("§r<§6E§alarcis§r> " + e.getMessage());
			else
				e.setFormat("§r<§6E§rlarcis> " + e.getMessage());
		}
		else
		{
			Team pTeam = p.getScoreboard().getPlayerTeam(p);
			String prefix = (pTeam != null) ? pTeam.getPrefix() : "";
			String format = "§r<" + prefix + e.getPlayer().getName() + "§r> " + e.getMessage();
			e.setFormat(format.replaceAll("%", "\\%"));
		}	
	}

	/**
	 * Triggered on any inventory click. Used for the menu spectator,
	 * and the surprise jukebox when a player gets a disc.
	 * @param e
	 */
	@EventHandler
	public void onInventoryClick(InventoryClickEvent e)
	{
		UUID id = e.getWhoClicked().getUniqueId();

		if (SGOnline.getSGSpectator(id) != null)
		{
			ItemStack item = e.getCurrentItem();

			if (item == null)
				return;

			if (item.getType() == Material.SKULL_ITEM && e.isRightClick())
			{
				String target = ((SkullMeta) item.getItemMeta()).getOwner();
				SGOnline.getSGSpectator(id).teleport(target);
			}
		}
		else
		{
			SGPlayer player = SGOnline.getSGPlayer(id);
			
			if (player == null)
				return;

			if (e.getCurrentItem() != null && e.getCurrentItem().getType().isRecord())
				player.giveJukebox();
		}
	}
	
	//TODO: Customize chest loots
	@EventHandler
	public void onChunkPopulate(ChunkPopulateEvent e)
	{
//		BlockState[] tileEnts = e.getChunk().getTileEntities();
//        for (BlockState state : tileEnts)
//        {
//            if (state.getType() != Material.CHEST)
//                continue;
//            	Chest c = (Chest) state.getBlock();
//            	c.getBlockInventory();
//        }
	}

	/**
	 * Triggered by any interaction. Used for spectator teleportation and security.
	 * @param e
	 */
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e)
	{
		UUID id = e.getPlayer().getUniqueId();
		SGSpectator spec = SGOnline.getSGSpectator(id);
		
		if (spec == null)
			return;

		if (e.getPlayer().getItemInHand() != null)
		{
			ItemStack item = e.getPlayer().getItemInHand();
			
			switch (e.getAction())
			{
			case RIGHT_CLICK_AIR:
			case RIGHT_CLICK_BLOCK:
				switch (item.getType())
				{
				case COMPASS:
					spec.openInventory();
					e.setCancelled(true);
					break;
				case SKULL_ITEM:
					String target = ((SkullMeta) item.getItemMeta()).getOwner();
					spec.teleport(target);
					e.setCancelled(true);
					break;
				default:
				}
				break;
				// TODO: Wait for proper replacement of getLineOfSight().
			// case LEFT_CLICK_AIR:
			// case LEFT_CLICK_BLOCK:
			default:
			}
		}

		// Cancel every interaction for non-op, but cancel physical triggering (redstone, etc.) for everyone.
		if (!e.getPlayer().isOp() || e.getAction() == Action.PHYSICAL)
		{
			e.setCancelled(true);
		}
	}

	/**
	 * Triggered when a player succeeded at connecting to the server.
	 * @param e
	 */
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e)
	{
		Player sgp = e.getPlayer();
		
		plugin.createSGPlayer(sgp);
		sgp.setScoreboard(plugin.getScoreboard());
		
		boolean isSpec = SGOnline.getSGSpectator(sgp.getUniqueId()) != null;
		
		// If it's a spectator, only show the message to spectators.
		if (isSpec)
		{
			SGOnline.broadcastSpectators(e.getJoinMessage());
			e.setJoinMessage("");
		}
		
		for (Player p : Bukkit.getOnlinePlayers())
		{
			if (!isSpec || SGOnline.getSGSpectator(p.getUniqueId()) != null)
				p.playSound(e.getPlayer().getLocation(), Sound.CHICKEN_EGG_POP, 1, 1);
		}
	}

	/**
	 * Triggered when a player tries to join the server.
	 * @param e
	 */
	@EventHandler
	public void onPlayerLogin(PlayerLoginEvent e)
	{
		Player p = e.getPlayer();

		if (plugin.isInMaintenanceMode() && !p.isOp())
		{
			e.disallow(Result.KICK_OTHER,
					ChatColor.YELLOW + plugin.getMaintenanceMessage());
		}
		else if (SGOnline.getPlayerCount() >= plugin.getMaxPlayers())
		{
			e.disallow(Result.KICK_FULL, ChatColor.YELLOW
					+ "Le serveur est plein !");
		}
	}

	/**
	 * Triggered when a player quits the server (by themselves or kicked/banned)
	 * @param e
	 */
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent e)
	{
		UUID id = e.getPlayer().getUniqueId();
		SGPlayer player = SGOnline.getSGPlayer(id);

		if (player != null)
			player.remove();
		else
		{
			// If it's a spectator, ony show the message to spectators.
			if (SGOnline.getSGSpectator(id) != null)
				SGOnline.broadcastSpectators(e.getQuitMessage());
			
			// If the player doesn't exists, he's either spectating or dead.
			e.setQuitMessage("");
		}

		plugin.removeVotemap(id);
	}

	/**
	 * Triggered JUST BEFORE the player respawned.
	 * If you want to give stuff to a player, use {@link SGOnline#respawn()}.
	 * @param e
	 */
	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent e)
	{
		SGSpectator spec = SGOnline.getSGSpectator(e.getPlayer().getUniqueId());

		if (spec != null)
			new PlayerSpawnScheduler(spec.getId()).runTaskLater(plugin, 2);
	}
	
	/**
	 * Triggered when a client retrieves server infos for the server list.
	 * @param e
	 */
	@EventHandler
	public void onServerListPing(ServerListPingEvent e)
	{
		e.setMaxPlayers(plugin.getMaxPlayers());
	}
	
	/**
	 * Subscribe this game state to bukkit events.
	 */
	public final synchronized void register()
	{
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	/**
	 * Unsubscribe this game state from bukkit events.
	 */
	public final synchronized void unregister()
	{
		HandlerList.unregisterAll(this);
	}
}