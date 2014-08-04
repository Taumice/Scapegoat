package fr.elarcis.scapegoat.gamestate;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import fr.elarcis.scapegoat.ScapegoatPlugin;
import fr.elarcis.scapegoat.async.PlayerSpawnScheduler;
import fr.elarcis.scapegoat.players.PlayerType;
import fr.elarcis.scapegoat.players.SGOnline;
import fr.elarcis.scapegoat.players.SGPlayer;
import fr.elarcis.scapegoat.players.SGSpectator;

public abstract class GameState implements Listener
{
	protected static ScapegoatPlugin plugin =
			ScapegoatPlugin.getPlugin(ScapegoatPlugin.class);

	public abstract GameStateType getType();

	public abstract void init();

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
		} else
		{
			String prefix = p.getScoreboard().getPlayerTeam(p).getPrefix();
			
			e.setFormat("§r<" + prefix + e.getPlayer().getName() + "§r> " + e.getMessage());
		}	
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInventoryClick(InventoryClickEvent e)
	{
		UUID id = e.getWhoClicked().getUniqueId();

		if (SGOnline.getType(id) == PlayerType.SPECTATOR)
		{
			ItemStack item = e.getCurrentItem();

			if (item == null)
				return;

			if (e.isRightClick() && item.getType() == Material.SKULL_ITEM)
			{
				SGOnline.getSGSpectator(id).teleport(
						((SkullMeta) item.getItemMeta()).getOwner());
			}
		} else
		{
			SGPlayer player = SGOnline.getSGPlayer(id);
			if (player != null
					&& e.getInventory().getType() == InventoryType.PLAYER
					&& e.getCursor() != null
					&& e.getCursor().getType().isRecord())
			{
				player.giveJukebox();
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerInteract(PlayerInteractEvent e)
	{
		UUID player = e.getPlayer().getUniqueId();

		if (SGOnline.getType(player) == PlayerType.SPECTATOR)
		{
			if (e.getAction() == Action.RIGHT_CLICK_AIR
					|| e.getAction() == Action.RIGHT_CLICK_BLOCK
					&& e.getPlayer().getItemInHand() != null)
			{
				ItemStack item = e.getPlayer().getItemInHand();

				if (item.getType() == Material.COMPASS)
				{
					SGOnline.getSGSpectator(player).openInventory();
					e.setCancelled(true);
				} else if (item.getType() == Material.SKULL_ITEM)
				{
					SGOnline.getSGSpectator(player).teleport(
							((SkullMeta) item.getItemMeta()).getOwner());
					e.setCancelled(true);
				}
			}

			if (!e.getPlayer().isOp() || e.getAction() == Action.PHYSICAL)
			{
				e.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e)
	{
		plugin.createSGPlayer(e.getPlayer());
		e.getPlayer().setScoreboard(plugin.getScoreboard());
		
		// Si c'est un spectateur, ne montrer le message qu'aux spectateurs.
		if (SGOnline.getSGSpectator(e.getPlayer().getUniqueId()) != null)
		{
			SGOnline.broadcastSpectators(e.getJoinMessage());
		}
		
		e.setJoinMessage("");
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerLogin(PlayerLoginEvent e)
	{
		Player p = e.getPlayer();

		if (plugin.isInMaintenanceMode() && !p.isOp())
		{
			e.disallow(Result.KICK_OTHER,
					ChatColor.YELLOW + plugin.getMaintenanceMessage());
		} else if (SGOnline.getPlayerCount() >= plugin.getMaxPlayers())
		{
			e.disallow(Result.KICK_FULL, ChatColor.YELLOW
					+ "Le serveur est plein !");
		}
	}

	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent e)
	{
		SGSpectator spec = SGOnline.getSGSpectator(e.getPlayer().getUniqueId());

		if (spec != null)
		{
			new PlayerSpawnScheduler(spec.getId()).runTaskLater(plugin, 2);
		}
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent e)
	{
		UUID id = e.getPlayer().getUniqueId();
		SGPlayer player = SGOnline.getSGPlayer(id);

		if (player != null)
		{
			player.remove();

		} else
		{
			// Si le joueur n'existe pas, c'est qu'il a été supprimé, à sa mort donc.
			// Si c'est un spectateur, on ne montre le message qu'aux spectateurs. Na.

			if (SGOnline.getSGSpectator(id) != null)
			{
				SGOnline.broadcastSpectators(e.getQuitMessage());
			}
			
			e.setQuitMessage("");
		}

		plugin.removeVotemap(id);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onServerListPing(ServerListPingEvent e)
	{
		e.setMaxPlayers(plugin.getMaxPlayers());
	}

	public final synchronized void register(JavaPlugin plugin)
	{
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	public abstract int timerTick(int secondsLeft);

	public final synchronized void unregister()
	{
		HandlerList.unregisterAll(this);
	}

	public abstract void updatePanelTitle();

	public abstract void rebuildPanel();
}