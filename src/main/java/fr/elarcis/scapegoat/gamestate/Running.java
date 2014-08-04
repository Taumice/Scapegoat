package fr.elarcis.scapegoat.gamestate;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import fr.elarcis.scapegoat.ScapegoatPlugin;
import fr.elarcis.scapegoat.players.PlayerType;
import fr.elarcis.scapegoat.players.SGOnline;
import fr.elarcis.scapegoat.players.SGPlayer;

public class Running extends GameState
{
	protected static final String nextTP = "Prochain TP :";
	protected GameModifier modifier;

	@Override
	public GameStateType getType()
	{
		return GameStateType.RUNNING;
	}

	@Override
	public synchronized void init()
	{
		modifier = GameModifier.NONE;
		plugin.updateTeleporterDelay();

		SGOnline.switchScapegoat(false);
		updatePanelInfo(plugin.getTeleporterDelay());

		if (plugin.getVotemaps() > 0
				&& plugin.getVotemaps() < plugin.getVotemapsRequired())
		{
			Bukkit.broadcastMessage(ChatColor.YELLOW
					+ "Pas assez de votes, la map est conserv�e.");
		}
		Bukkit.broadcastMessage(ChatColor.RED + "La partie a commenc� !");

		String[] startInfo = new String[] {
				ChatColor.RED + "Prochain bouc-�missaire: "
						+ ScapegoatPlugin.SCAPEGOAT_COLOR + ChatColor.MAGIC
						+ "THE_GAME" + ChatColor.RED + ".",
				ChatColor.GREEN + "Soyez le dernier survivant !", };

		// You just lost it, smart ass ~

		String[] startInfoSpectate = new String[] {
				ChatColor.RED + "Prochain bouc-�missaire: "
						+ ScapegoatPlugin.SCAPEGOAT_COLOR
						+ SGOnline.getScapegoat().getName() + ChatColor.RED
						+ ".", ChatColor.GREEN + "Vous �tes spectateur." };

		SGOnline.broadcastPlayers(startInfo);
		SGOnline.broadcastSpectators(startInfoSpectate);

		Bukkit.getWorlds().get(0).setGameRuleValue("doDaylightCycle", "true");

		Random rand = new Random();
		int specialGame = rand.nextInt(4);
		boolean special = false;

		if (specialGame == 0)
		{
			special = true;

			String broadcast = ChatColor.RED + "Modificateur sp�cial : "
					+ ChatColor.YELLOW;
			specialGame = rand.nextInt(4);

			// Not using a switch in case of modifiers having various
			// probabilities.

			switch (specialGame)
			{
			case 0:
				broadcast += "Ultra Hardcore activ�e ! Pas de r�g�n�ration de vie !";
				Bukkit.getWorlds().get(0)
						.setGameRuleValue("naturalRegeneration", "false");
				modifier = GameModifier.UHC;
				
				break;
			case 1:
				broadcast += "Partie de nuit uniquement ! Cachez-vous !";
				Bukkit.getWorlds().get(0)
						.setGameRuleValue("doDaylightCycle", "false");
				Bukkit.getWorlds().get(0).setTime(18000);
				modifier = GameModifier.NIGHT;
				
				break;
			case 2:
			case 3:
				specialGame = rand.nextInt(4);

				switch (specialGame)
				{
				case 0:
					broadcast += "POTION ! Tout le monde a la survitesse !";
					modifier = GameModifier.POTION_SPEED;
					break;
				case 1:
					broadcast += "POTION ! Tout le monde saute comme des lapins !";
					modifier = GameModifier.POTION_JUMP;
					break;
				case 2:
					broadcast += "POTION ! Tout le monde est r�sistant au feu !";
					modifier = GameModifier.POTION_FIRE;
					break;
				case 3:
					broadcast += "POTION ! Tout le monde est invisible !";
					modifier = GameModifier.POTION_INVISIBLE;
					break;
				}
				
				break;
			}

			Bukkit.broadcastMessage(broadcast);
		}

		for (Player p : Bukkit.getOnlinePlayers())
		{
			switch (SGOnline.getType(p.getUniqueId()))
			{
			case PLAYER:
			case SCAPEGOAT:
				p.setGameMode(GameMode.SURVIVAL);
				p.setHealth(p.getMaxHealth());
				p.setFoodLevel(20);
				p.setSaturation(10);
				p.setExp(0);
				
				SGPlayer sgp = SGOnline.getSGPlayer(p.getUniqueId());
				sgp.setPlays(sgp.getPlays() + 1);

				switch (modifier)
				{
				case POTION_SPEED:
					p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,
							Integer.MAX_VALUE, 2, true), true);
					break;
				case POTION_FIRE:
					p.addPotionEffect(new PotionEffect(
							PotionEffectType.FIRE_RESISTANCE,
							Integer.MAX_VALUE, 1, true), true);
					break;
				case POTION_JUMP:
					p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP,
							Integer.MAX_VALUE, 2, true), true);
					break;
				case POTION_INVISIBLE:
					p.addPotionEffect(new PotionEffect(
							PotionEffectType.INVISIBILITY, Integer.MAX_VALUE,
							1, true), true);
					break;
				default:
					break;
				}

				break;
			default:
				break;
			}

			if (special)
			{
				p.playSound(p.getLocation(), Sound.PIG_DEATH, 10, 1);
			} else
			{
				p.playSound(p.getLocation(), Sound.WITHER_SPAWN, 10, 1);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e)
	{
		if (e.getCause() == DamageCause.ENTITY_ATTACK
				&& e.getDamager().getType() == EntityType.PLAYER)
		{
			UUID damager = ((Player) e.getDamager()).getUniqueId();

			if (SGOnline.getType(damager) == PlayerType.SPECTATOR)
			{
				e.setCancelled(true);
			} else if (e.getEntityType() == EntityType.PLAYER
					&& plugin.getTeleportCount() == 0)
			{
				SGPlayer sgAttacked = SGOnline.getSGPlayer(((Player) e
						.getEntity()).getUniqueId());
				SGPlayer sgDamager = SGOnline.getSGPlayer(damager);

				if (sgDamager != null && !sgDamager.hasWeapon() && !sgAttacked.hasWeapon())
				{
					sgDamager.addFistWarning(sgAttacked);
					Player p = Bukkit.getPlayer(damager);
					
					if (p != null)
						p.sendMessage(ChatColor.DARK_RED + "NE FONCE PAS SUR LES AUTRES JOUEURS SANS ARME !");
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerBreakBlock(BlockBreakEvent e)
	{
		if (SGOnline.getType(e.getPlayer().getUniqueId()) == PlayerType.SPECTATOR
				&& !e.getPlayer().isOp())
		{
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent e)
	{
		List<ItemStack> drops = e.getDrops();
		Player p = e.getEntity();

		e.setDeathMessage(ChatColor.YELLOW + e.getDeathMessage());

		if (!SGOnline.getScapegoat().equals(p) && drops.size() > 0)
		{
			int stacksToRemove = (int) (drops.size() * 0.8f);
			Random rand = new Random();

			for (int i = 0; i < stacksToRemove; i++)
			{
				drops.remove(rand.nextInt(drops.size()));
			}
		}

		for (ItemStack i : drops)
		{
			if (i.getType().isRecord())
				drops.remove(i);
		}

		SGOnline.getSGPlayer(p.getUniqueId()).kill(p.getLastDamageCause());
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerDropItem(PlayerDropItemEvent e)
	{
		if (SGOnline.getType(e.getPlayer().getUniqueId()) == PlayerType.SPECTATOR)
		{
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onPlayerExpChange(PlayerExpChangeEvent e)
	{
		e.setAmount((int) (e.getAmount() * 1.5f));
	}

	@Override
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerLogin(PlayerLoginEvent e)
	{
		super.onPlayerLogin(e);

		Player p = e.getPlayer();

		if (!p.isOp()
				&& SGOnline.getType(p.getUniqueId()) != PlayerType.SPECTATOR)
		{
			e.disallow(
					Result.KICK_OTHER,
					"Partie en cours, veuillez r�essayer plus tard ("
							+ ScapegoatPlugin.PLAYER_COLOR
							+ SGOnline.getPlayerCount() + ChatColor.RESET
							+ " joueurs restant).");
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerPickupItem(PlayerPickupItemEvent e)
	{
		if (SGOnline.getType(e.getPlayer().getUniqueId()) == PlayerType.SPECTATOR)
		{
			e.setCancelled(true);
		}

		SGPlayer player = SGOnline.getSGPlayer(((Player) e.getPlayer())
				.getUniqueId());

		if (e.getItem().getItemStack().getType().isRecord())
		{
			player.giveJukebox();
		}
	}

	// TODO: Handle when players take discs from chests.
	@EventHandler
	public void onInventoryMoveItem(InventoryMoveItemEvent e)
	{
		if (e.getDestination().getType() == InventoryType.PLAYER)
		{
			SGPlayer player = SGOnline.getSGPlayer(((Player) e.getDestination()
					.getHolder()).getUniqueId());

			if (e.getItem().getType().isRecord())
			{
				player.giveJukebox();
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerBedEnter(PlayerBedEnterEvent e)
	{
		e.getPlayer().sendMessage(
				ChatColor.DARK_PURPLE + "Pas le temps de dormir !");
		e.setCancelled(true);
	}

	@Override
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onServerListPing(ServerListPingEvent e)
	{
		super.onServerListPing(e);

		if (plugin.isInMaintenanceMode())
		{
			e.setMotd(ChatColor.YELLOW + "En maintenance...");
			return;
		}

		String motd = ScapegoatPlugin.SCAPEGOAT_COLOR + "Bouc-�missaire"
				+ ChatColor.RESET + " | " + ChatColor.DARK_RED
				+ "Partie en cours.";
		e.setMotd(motd);
	}

	@Override
	public int timerTick(int secondsLeft)
	{
		updatePanelInfo(secondsLeft + 1);

		if (secondsLeft < 0)
		{
			SGPlayer.teleport();
			return plugin.updateTeleporterDelay();
		} else if (secondsLeft < 3)
		{
			Bukkit.broadcastMessage(ChatColor.RED
					+ Integer.toString(secondsLeft + 1) + "...");

			for (Player p : Bukkit.getOnlinePlayers())
			{
				p.playSound(p.getLocation(), Sound.CLICK, 1, 1);
			}
		} else if (secondsLeft == 3)
		{
			Bukkit.broadcastMessage(ScapegoatPlugin.SCAPEGOAT_COLOR
					+ "T�l�portation imminente ! Pr�parez-vous !");
		} else if (secondsLeft == 5)
		{
			SGOnline.refeshScapegoatChunk();
		}
		return secondsLeft;
	}

	public void updatePanelTitle()
	{
		Objective tp = plugin.getScoreboard().getObjective("scapegoat");
		tp.setDisplayName("" + ScapegoatPlugin.SCAPEGOAT_COLOR + ChatColor.BOLD
				+ SGOnline.getPlayerCount() + " joueurs");
	}

	public void rebuildPanel()
	{
		Scoreboard board = plugin.getScoreboard();
		Objective tp = board.getObjective("scapegoat");
		int secondsLeft = 0;

		if (tp != null)
		{
			secondsLeft = tp.getScore(nextTP).getScore();
			tp.unregister();
		}

		tp = board.registerNewObjective("scapegoat", "dummy");
		tp.setDisplaySlot(DisplaySlot.SIDEBAR);
		
		updatePanelTitle();

		String scapeGoat = SGOnline.getScapegoat().getName();

		updatePanelInfo(secondsLeft);

		if (SGOnline.getShowScapegoat())
		{
			if (scapeGoat.length() <= 14)
			{
				tp.getScore(ScapegoatPlugin.SCAPEGOAT_COLOR
								+ scapeGoat).setScore(-1);
			} else
			{
				tp.getScore(scapeGoat)
						.setScore(-1);
			}
		} else
		{
			tp.getScore(ScapegoatPlugin.SCAPEGOAT_COLOR + "" + ChatColor.MAGIC + "THE_GAME").setScore(-1);
			// Again ? ~
		}
	}

	public void updatePanelInfo(int secondsLeft)
	{
		Scoreboard board = plugin.getScoreboard();
		Objective tp = board.getObjective("scapegoat");

		if (tp != null)
			tp.getScore(nextTP).setScore(secondsLeft);
	}
}
