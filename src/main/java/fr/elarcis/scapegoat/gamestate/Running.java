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

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import fr.elarcis.scapegoat.ItemStuffer;
import fr.elarcis.scapegoat.ScapegoatPlugin;
import fr.elarcis.scapegoat.players.PlayerType;
import fr.elarcis.scapegoat.players.SGOnline;
import fr.elarcis.scapegoat.players.SGPlayer;

/**
 * Handles events that would occur after the actual game started and be different from default game state.
 * @author Lars
 */
public class Running extends GameState
{
	protected static final String nextTP = "Prochain TP :";
	protected GameModifier modifier;
	
	public Running() 
	{
		super();
		modifier = GameModifier.NONE;
	}

	@Override
	public GameStateType getType() { return GameStateType.RUNNING; }

	@Override
	public synchronized void init()
	{
		modifier = GameModifier.NONE;
		plugin.updateTeleporterDelay();

		SGOnline.switchScapegoat(false);
		updatePanelInfo(plugin.getTeleporterDelay());

		if (plugin.getVotemaps() > 0 && plugin.getVotemaps() < plugin.getVotemapsRequired())
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Pas assez de votes, la map est conservée.");
		
		Bukkit.broadcastMessage(ChatColor.RED + "La partie a commencé !");

		String[] startInfo = new String[] {
			ChatColor.RED + "Prochain bouc-émissaire: " + ScapegoatPlugin.SCAPEGOAT_COLOR + ChatColor.MAGIC
					+ "THE_GAME" + ChatColor.RED + ".",
			ChatColor.GREEN + "Soyez le dernier survivant !",
		};
		// You just lost it, smart ass ~

		String[] startInfoSpectate = new String[] {
			ChatColor.RED + "Prochain bouc-émissaire: " + ScapegoatPlugin.SCAPEGOAT_COLOR
					+ SGOnline.getScapegoat().getName() + ChatColor.RED + ".",
			ChatColor.GREEN + "Vous êtes spectateur." };

		SGOnline.broadcastPlayers(startInfo);
		SGOnline.broadcastSpectators(startInfoSpectate);

		Bukkit.getWorlds().get(0).setGameRuleValue("doDaylightCycle", "true");

		Random rand = new Random();
		int specialGame = rand.nextInt(4);
		boolean special = false;

		if (specialGame == 0)
		{
			special = true;

			String broadcast = ChatColor.RED + "Modificateur spécial : " + ChatColor.YELLOW;
			specialGame = rand.nextInt(4);

			switch (specialGame)
			{
			case 0:
				broadcast += "Ultra Hardcore activée ! Pas de régénération de vie !";
				Bukkit.getWorlds().get(0).setGameRuleValue("naturalRegeneration", "false");
				modifier = GameModifier.UHC;
				break;
			case 1:
				broadcast += "Partie de nuit uniquement ! Cachez-vous !";
				Bukkit.getWorlds().get(0).setGameRuleValue("doDaylightCycle", "false");
				Bukkit.getWorlds().get(0).setTime(15000);
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
					broadcast += "POTION ! Tout le monde est résistant au feu !";
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
					p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 3, true), true);
					break;
				case POTION_FIRE:
					p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 1, true),
							true);
					break;
				case POTION_JUMP:
					p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 2, true), true);
					break;
				case POTION_INVISIBLE:
					p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, true), true);
					break;
				default:
					break;
				}

				break;
			default:
			}

			if (special)
				p.playSound(p.getLocation(), Sound.PIG_DEATH, 10, 1);
			else
				p.playSound(p.getLocation(), Sound.WITHER_SPAWN, 10, 1);
		}
		
		SGOnline.computeMediumScore();
	}
	
	/**
	 * @return if that game has a special behavior. Returns {@link GameModifier#NONE} is the game hasn't started.
	 */
	public GameModifier getModifier() { return modifier; }

	/**
	 * Triggered each time an entity takes damaged by another entity.
	 * This event is not reserved to players.
	 * @param e
	 */
	@EventHandler
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e)
	{
		if (e.getCause() == DamageCause.ENTITY_ATTACK && e.getDamager().getType() == EntityType.PLAYER)
		{
			UUID damager = ((Player) e.getDamager()).getUniqueId();

			if (SGOnline.getSGSpectator(damager) != null)
				e.setCancelled(true);
			else if (e.getEntityType() == EntityType.PLAYER)
			{
				SGPlayer sgAttacked = SGOnline.getSGPlayer(((Player) e.getEntity()).getUniqueId());
				SGPlayer sgDamager = SGOnline.getSGPlayer(damager);
				
				if (sgAttacked == null || sgDamager == null)
					return;
				
				if(plugin.getTeleportCount() == 0)
				{
					if (sgDamager != null && !sgDamager.hasWeapon() && !sgAttacked.hasWeapon())
					{
						sgDamager.addFistWarning(sgAttacked);
						Player p = Bukkit.getPlayer(damager);

						if (p != null) {
							p.sendMessage(ChatColor.DARK_RED + "NE FONCE PAS SUR LES AUTRES JOUEURS SANS ARME !");
							e.setCancelled(true);
						}
					}
				}

				int scoreDiff = Math.max(0, sgAttacked.getScore() - SGOnline.getMediumScore());
				double handicap = 1. + (scoreDiff / 100.);
				
				e.setDamage(e.getDamage() * handicap);
			}
		}
	}

	/**
	 * Triggered everytime a player right clicks on a bed.
	 * @param e
	 */
	@EventHandler
	public void onPlayerBedEnter(PlayerBedEnterEvent e)
	{
		e.getPlayer().sendMessage(ChatColor.DARK_PURPLE + "Pas le temps de dormir !");
		e.setCancelled(true);
	}

	/**
	 * Triggered everytime a player ends breaking a block.
	 * @param e
	 */
	@EventHandler
	public void onPlayerBreakBlock(BlockBreakEvent e)
	{
		if (SGOnline.getType(e.getPlayer().getUniqueId()) == PlayerType.SPECTATOR && !e.getPlayer().isOp())
			e.setCancelled(true);
	}

	/**
	 * Triggered on a player's death, just before the actual death.
	 * @param e
	 */
	@EventHandler
	public synchronized void onPlayerDeath(PlayerDeathEvent e)
	{
		List<ItemStack> drops = e.getDrops();
		Player p = e.getEntity();

		e.setDeathMessage(ChatColor.YELLOW + e.getDeathMessage());
		
		ItemStack book = null;
		boolean addBook = false;
		boolean scapegoat = SGOnline.getScapegoat().equals(p);
		
		if (!scapegoat && drops.size() > 0)
		{
			int stacksToRemove = (int) (drops.size() * 0.8f);
			Random rand = new Random();

			for (int i = 0; i < stacksToRemove; i++)
			{
				int r = rand.nextInt(drops.size());
				Material m = drops.get(r).getType();
				
				if (m != Material.WRITTEN_BOOK && !m.isRecord())
					drops.remove(r);
			}
		}
		
		Iterator<ItemStack> it = drops.iterator();

		while (it.hasNext())
		{
			ItemStack item = it.next();
			
			switch (item.getType())
			{
			case WRITTEN_BOOK:
				// When a player drops their book, we give the killer a dull book
				// with the victim's name. Fun !
				
				book = new ItemStack(Material.BOOK);
				BookMeta bookMeta = (BookMeta) item.getItemMeta();
				
				if (bookMeta.getTitle().equals(ItemStuffer.MANUAL_TITLE))
				{
					ItemMeta meta = book.getItemMeta();
					
					meta.setDisplayName(ChatColor.RED + "Livre de " + p.getName());
					book.setItemMeta(meta);
					
					it.remove();
					addBook = true;
				}	
				break;
			case JUKEBOX:
				it.remove();
			default:
			}
		}
		
		if (addBook)
			drops.add(book);
			
		SGOnline.getSGPlayer(p.getUniqueId()).kill(p.getLastDamageCause());
	}

	/**
	 * Triggered each time a player drops an item, may them be alive or dead.
	 * @param e
	 */
	@EventHandler
	public void onPlayerDropItem(PlayerDropItemEvent e)
	{
		if (SGOnline.getType(e.getPlayer().getUniqueId()) == PlayerType.SPECTATOR)
			e.setCancelled(true);
	}

	/**
	 * Triggered each time a player gets xp orbs. This DOES NOT handles "xp attraction".
	 * @param e
	 */
	@EventHandler
	public void onPlayerExpChange(PlayerExpChangeEvent e)
	{
		e.setAmount((int) (e.getAmount() * 1.5f));
	}

	@Override
	@EventHandler
	public void onPlayerLogin(PlayerLoginEvent e)
	{
		super.onPlayerLogin(e);

		Player p = e.getPlayer();

		if (!p.isOp() && SGOnline.getType(p.getUniqueId()) != PlayerType.SPECTATOR)
			e.disallow(Result.KICK_OTHER, "Partie en cours, veuillez réessayer plus tard ("
					+ ScapegoatPlugin.PLAYER_COLOR + SGOnline.getPlayerCount() + ChatColor.RESET + " joueurs restant).");
	}

	/**
	 * Triggered each time a player picks up an item from the ground.
	 * @param e
	 */
	@EventHandler
	public void onPlayerPickupItem(PlayerPickupItemEvent e)
	{
		Player p = e.getPlayer();
		
		if (SGOnline.getSGSpectator(p.getUniqueId()) != null)
			e.setCancelled(true);

		SGPlayer player = SGOnline.getSGPlayer(p.getUniqueId());

		if (player != null && e.getItem().getItemStack().getType().isRecord())
			player.giveJukebox();
	}

	@Override
	@EventHandler
	public void onServerListPing(ServerListPingEvent e)
	{
		super.onServerListPing(e);

		if (plugin.isInMaintenanceMode())
		{
			e.setMotd(ChatColor.YELLOW + "En maintenance...");
			return;
		}

		String motd = ScapegoatPlugin.SCAPEGOAT_COLOR + "Bouc-émissaire" + ChatColor.RESET + " | " + ChatColor.DARK_RED
				+ "Partie en cours.";
		e.setMotd(motd);
	}

	@Override
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
			if (scapeGoat.length() <= 14)
				tp.getScore(ScapegoatPlugin.SCAPEGOAT_COLOR + scapeGoat).setScore(-1);
			else
				tp.getScore(scapeGoat).setScore(-1);
		else
			tp.getScore(ScapegoatPlugin.SCAPEGOAT_COLOR + "" + ChatColor.MAGIC + "THE_GAME").setScore(-1);
			// Again ? ~
	}

	@Override
	public int timerTick(int secondsLeft)
	{
		updatePanelInfo(secondsLeft + 1);

		if (secondsLeft < 0)
		{
			SGPlayer.teleport();
			return plugin.updateTeleporterDelay();
		}
		else if (secondsLeft < 3)
		{
			Bukkit.broadcastMessage(ChatColor.RED + Integer.toString(secondsLeft + 1) + "...");

			for (Player p : Bukkit.getOnlinePlayers())
				p.playSound(p.getLocation(), Sound.CLICK, 1, 1);
		}
		else if (secondsLeft == 3)
		{
			Bukkit.broadcastMessage(ScapegoatPlugin.SCAPEGOAT_COLOR + "Téléportation imminente ! Préparez-vous !");
			SGOnline.refeshScapegoatChunk();
		}
			
		return secondsLeft;
	}

	/**
	 *  Only update the sidebar scoreboard's scores.
	 * @param secondsLeft How many seconds should the timer display.
	 */
	public void updatePanelInfo(int secondsLeft)
	{
		Scoreboard board = plugin.getScoreboard();
		Objective tp = board.getObjective("scapegoat");

		if (tp != null)
			tp.getScore(nextTP).setScore(secondsLeft);
	}
	@Override
	public void updatePanelTitle()
	{
		Objective tp = plugin.getScoreboard().getObjective("scapegoat");
		tp.setDisplayName("" + ScapegoatPlugin.SCAPEGOAT_COLOR + ChatColor.BOLD + SGOnline.getPlayerCount()
				+ " joueurs");
	}
}
