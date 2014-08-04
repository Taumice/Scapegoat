package fr.elarcis.scapegoat.gamestate;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
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
import org.bukkit.scoreboard.Team;

import fr.elarcis.scapegoat.ScapegoatPlugin;
import fr.elarcis.scapegoat.async.PlayerSpawnScheduler;
import fr.elarcis.scapegoat.players.PlayerType;
import fr.elarcis.scapegoat.players.SGOnline;
import fr.elarcis.scapegoat.players.SGPlayer;
import fr.elarcis.scapegoat.players.SGSpectator;

public abstract class GameState implements Listener
{
	protected static ScapegoatPlugin plugin = ScapegoatPlugin.getPlugin(ScapegoatPlugin.class);
	
	public abstract GameStateType getType();
	public abstract void init();
	public abstract void rebuildPanel();
	public abstract void updatePanelTitle();
	public abstract int timerTick(int secondsLeft);

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
			
			e.setFormat("§r<" + prefix + e.getPlayer().getName() + "§r> " + e.getMessage());
		}	
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent e)
	{
		UUID id = e.getWhoClicked().getUniqueId();

		if (SGOnline.getType(id) == PlayerType.SPECTATOR)
		{
			ItemStack item = e.getCurrentItem();

			if (item == null)
				return;

			if (item.getType() == Material.SKULL_ITEM && e.isRightClick())
			{
				String target = ((SkullMeta) item.getItemMeta()).getOwner();
				SGOnline.getSGSpectator(id).teleport(target);
			}
		} else
		{
			SGPlayer player = SGOnline.getSGPlayer(id);
			
			if (player == null)
				return;
			if ((e.getAction() == InventoryAction.PLACE_ALL || e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY)
					&& e.getCurrentItem().getType().isRecord()
					&& (e.getSlotType() == SlotType.CONTAINER || e.getSlotType() == SlotType.QUICKBAR))		
				player.giveJukebox();
		}
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e)
	{
		UUID id = e.getPlayer().getUniqueId();
		SGSpectator spec = SGOnline.getSGSpectator(id);
		
		if (spec == null)
			return;

		if (e.getAction() == Action.RIGHT_CLICK_AIR
				|| e.getAction() == Action.RIGHT_CLICK_BLOCK
				&& e.getPlayer().getItemInHand() != null)
		{
			ItemStack item = e.getPlayer().getItemInHand();

			if (item.getType() == Material.COMPASS)
			{
				spec.openInventory();
				e.setCancelled(true);
			}
			else if (item.getType() == Material.SKULL_ITEM)
			{
				String target = ((SkullMeta) item.getItemMeta()).getOwner();
				spec.teleport(target);
				e.setCancelled(true);
			}
		}

		// Cancel every interaction for non-op, but cancel physical triggering (redstone, etc.) for everyone.
		if (!e.getPlayer().isOp() || e.getAction() == Action.PHYSICAL)
		{
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e)
	{
		Player p = e.getPlayer();
		
		plugin.createSGPlayer(p);
		p.setScoreboard(plugin.getScoreboard());
		
		// If it's a spectator, only show the message to spectators.
		if (SGOnline.getSGSpectator(p.getUniqueId()) != null)
		{
			SGOnline.broadcastSpectators(e.getJoinMessage());
			e.setJoinMessage("");
		}
	}

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

	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent e)
	{
		SGSpectator spec = SGOnline.getSGSpectator(e.getPlayer().getUniqueId());

		if (spec != null)
			new PlayerSpawnScheduler(spec.getId()).runTaskLater(plugin, 2);
	}

	@EventHandler
	public void onServerListPing(ServerListPingEvent e)
	{
		e.setMaxPlayers(plugin.getMaxPlayers());
	}
	
	public final synchronized void register()
	{
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	public final synchronized void unregister()
	{
		HandlerList.unregisterAll(this);
	}
}