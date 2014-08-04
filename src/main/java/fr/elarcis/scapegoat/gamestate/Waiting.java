package fr.elarcis.scapegoat.gamestate;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;

import fr.elarcis.scapegoat.ScapegoatPlugin;
import fr.elarcis.scapegoat.players.PlayerType;
import fr.elarcis.scapegoat.players.SGOnline;

public class Waiting extends GameState
{
	protected static final String missingPlayers = "Manquants :";
	protected static final String gameStart = "Début :";
	protected boolean countdown;
	protected int secondsLeft;

	@Override
	public GameStateType getType()
	{
		return GameStateType.WAITING;
	}

	@Override
	public synchronized void init()
	{
		for (Player p : Bukkit.getOnlinePlayers())
			p.setFoodLevel(20);

		Bukkit.getWorlds().get(0).setTime(2000);
		Bukkit.getWorlds().get(0).setGameRuleValue("doDaylightCycle", "false");
		rebuildPanel();

		countdown = false;
	}

	@EventHandler
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e)
	{
		e.setCancelled(true);
	}

	@EventHandler
	public void onPlayerBreakBlock(BlockBreakEvent e)
	{
		if (!(e.getPlayer().isOp() && SGOnline.getType(e.getPlayer()
				.getUniqueId()) == PlayerType.SPECTATOR))
			e.setCancelled(true);
	}

	@EventHandler
	public void onPlayerDropItem(PlayerDropItemEvent e)
	{
		e.setCancelled(true);
	}

	@EventHandler
	public void onPlayerHunger(FoodLevelChangeEvent e)
	{
		e.setCancelled(true);
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e)
	{
		super.onPlayerInteract(e);

		if (!(e.getPlayer().isOp()
				&& SGOnline.getType(e.getPlayer().getUniqueId()) == PlayerType.SPECTATOR && e
					.getAction() != Action.PHYSICAL))
			e.setCancelled(true);
	}
	
	@EventHandler
	public void onPlayerPickupItem(PlayerPickupItemEvent e)
	{
		e.setCancelled(true);
	}

	@EventHandler
	public void onServerListPing(ServerListPingEvent e)
	{
		super.onServerListPing(e);

		if (plugin.isInMaintenanceMode())
		{
			e.setMotd(ChatColor.YELLOW + "En maintenance...");
			return;
		}

		String motd = ScapegoatPlugin.SCAPEGOAT_COLOR + "Bouc-émissaire"
				+ ChatColor.RESET + " | ";

		int minutes = secondsLeft / 60;
		int seconds = secondsLeft % 60;

		if (countdown)
			motd += "Début dans " + ChatColor.DARK_RED + minutes + "m" + seconds + ChatColor.RESET + ".";
		else
		{
			int required = plugin.getPlayersRequired() - SGOnline.getPlayerCount();
			motd += "" + ChatColor.DARK_RED + required + ChatColor.RESET
					+ " joueur" + (required > 1 ? "s" : "") + " requis.";
		}
		
		e.setMotd(motd);
	}

	public void rebuildPanel()
	{
		plugin.getScoreboard().getObjective("panelInfo").unregister();
		plugin.getScoreboard().registerNewObjective("panelInfo", "dummy").setDisplaySlot(DisplaySlot.SIDEBAR);
	}

	@Override
	public int timerTick(int secondsLeft)
	{
		int waitBeforeStart = plugin.getWaitBeforeStart();
		int missing = plugin.getPlayersRequired() - SGOnline.getPlayerCount();

		if (missing > 0)
		{
			if (countdown)
			{
				rebuildPanel();
				countdown = false;
			}

			this.secondsLeft = waitBeforeStart;
			updatePanelInfo(missingPlayers, missing);
		}
		else
		{
			this.secondsLeft = secondsLeft;

			if (secondsLeft < 3 && secondsLeft >= 0)
			{
				Bukkit.broadcastMessage(ChatColor.RED
						+ Integer.toString(secondsLeft + 1) + "...");

				for (Player p : Bukkit.getOnlinePlayers())
					p.playSound(p.getLocation(), Sound.CLICK, 1, 1);
			}
			else if (secondsLeft == 3)
				Bukkit.broadcastMessage(ChatColor.RED
						+ "Début de la partie imminent !");
			else if (!countdown)
			{
				rebuildPanel();
				countdown = true;

				for (Player p : Bukkit.getOnlinePlayers())
					p.playSound(p.getLocation(), Sound.SUCCESSFUL_HIT, 1, 1);

				Bukkit.broadcastMessage(ChatColor.RED
						+ "Début de la partie dans " + (secondsLeft + 1)
						+ " secondes !");
				Bukkit.broadcastMessage(ChatColor.GOLD + "Dispersez-vous !");	
			}

			updatePanelInfo(gameStart, secondsLeft + 1);
		}
		return this.secondsLeft;
	}

	public void updatePanelTitle()
	{
		Objective mPlayers = plugin.getScoreboard().getObjective("panelInfo");

		if (plugin.getPlayersRequired() <= SGOnline.getPlayerCount())
			mPlayers.setDisplayName("" + ChatColor.GREEN + ChatColor.BOLD
					+ "En attente");
		else
			mPlayers.setDisplayName("" + ChatColor.RED + ChatColor.BOLD
					+ "En attente");
	}

	protected void updatePanelInfo(String message, int score)
	{
		updatePanelTitle();

		Objective mPlayers = plugin.getScoreboard().getObjective("panelInfo");
		mPlayers.getScore(message).setScore(score);
	}
}
