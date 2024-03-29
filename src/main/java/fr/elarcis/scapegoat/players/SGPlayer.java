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

package fr.elarcis.scapegoat.players;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;

import fr.elarcis.scapegoat.ItemSet;
import fr.elarcis.scapegoat.ScapegoatPlugin;
import fr.elarcis.scapegoat.async.PlayerKickScheduler;
import fr.elarcis.scapegoat.gamestate.GameModifier;
import fr.elarcis.scapegoat.gamestate.GameStateType;
import fr.elarcis.scapegoat.gamestate.Running;

/**
 * An abstraction layer around standard {@link org.bukkit.entity.Player Entity.Player} class.
 * Provides operations related to people actually playing a game.
 * @author Lars
 */
public class SGPlayer extends SGOnline
{
	protected boolean dead;
	protected int nFistWarning;
	protected SGPlayer lastFist;

	/**
	 * Create a new SGPlayer from a Bukkit player and register them in static maps.
	 * There should be only ONE {@link SGOnline} per player, as they are remembered via their UUID.
	 * If you're not sure of that, remove any possible previous {@link SGOnline} before creating one.
	 * @param p The bukkit player linked to that SGPlayer.
	 */
	public SGPlayer(Player player)
	{
		super(player);

		SGOnline.sgPlayers.put(id, this);
		plugin.getScoreboard().getTeam("Players").addPlayer(player);

		if (plugin.getGameStateType() == GameStateType.RUNNING)
			player.kickPlayer("H�l�, on rejoint pas une partie en cours, "
					+ ChatColor.ITALIC + "gros malin" + ChatColor.RESET + ".");
		else
		{
			player.setGameMode(GameMode.SURVIVAL);
			
			if (name.equals("Elarcis"))
				player.setDisplayName("�6E�rlarcis");
			else
				player.setDisplayName(name);

			this.dead = false;
			this.nFistWarning = plugin.getMaxFistWarning();

			player.getInventory().clear();
			plugin.getStuffer().stuff(player, ItemSet.MANUAL);
			giveTrophees();
		}

		join();
		computeMediumScore();
	}

	/**
	 * Teleport a player to another according to the countdown.
	 */
	public static synchronized void teleport()
	{
		Map<UUID, SGPlayer> candidates = new HashMap<UUID, SGPlayer>(sgPlayers);
		candidates.remove(scapegoat.getId());

		int pIndex = new Random().nextInt(candidates.size());
		
		Player t = ((SGPlayer) candidates.values().toArray()[pIndex]).getPlayer();
		Player s = SGOnline.scapegoat.getPlayer();

		// Basic trap detection System
		
		int noDamage = plugin.getNoDamageTicks();
		
		t.setNoDamageTicks(noDamage);
		s.setNoDamageTicks(noDamage);

		Location abs = s.getLocation();

		if (!t.isInsideVehicle())
			t.teleport(s, TeleportCause.PLUGIN);
		else
		{
			Entity v = t.getVehicle();
			v.eject();
			v.teleport(s, TeleportCause.PLUGIN);
			t.teleport(s, TeleportCause.PLUGIN);
			v.setPassenger(t);
		}
		
		int pitSize = plugin.getMaxPitSize();
		
		if (pitSize > 0)
		{
			boolean solidFound = false;
			boolean lavaFound = false;
			
			for (int i = 0; i < pitSize; i++)
			{
				Block b = abs.getWorld().getBlockAt(abs.getBlockX(),
						abs.getBlockY() - i, abs.getBlockZ());

				if (b.getType() == Material.STATIONARY_LAVA
						|| b.getType() == Material.LAVA)
				{
					lavaFound = true;
					break;
				}
				
				Set<Material> nonSolids = new HashSet<Material>();
				nonSolids.add(Material.SIGN);
				nonSolids.add(Material.LADDER);
				nonSolids.add(Material.TORCH);
				nonSolids.add(Material.WEB);
				nonSolids.add(Material.REDSTONE_TORCH_OFF);
				nonSolids.add(Material.REDSTONE_TORCH_ON);
				nonSolids.add(Material.AIR);

				if (!nonSolids.contains(b.getType()))
				{
					solidFound = true;
					break;
				}
			}

			// TODO: BETTER HANDLE THAT TP

			if (lavaFound || !solidFound)
			{
				abs.setX(abs.getBlockX() + 0.5);
				abs.setZ(abs.getBlockZ() + 0.5);
				s.teleport(abs);
				s.setSneaking(false);
			}
		}

		plugin.addTeleport();
		s.getWorld().strikeLightningEffect(s.getLocation());

		for (Player p : Bukkit.getOnlinePlayers())
			p.playSound(p.getLocation(), Sound.AMBIENCE_THUNDER, 1, 1);

		Bukkit.broadcastMessage(ScapegoatPlugin.PLAYER_COLOR + t.getName()
				+ ChatColor.RESET + " a �t� t�l�port� aupr�s du "
				+ ScapegoatPlugin.SCAPEGOAT_COLOR + "bouc-�missaire !");
	}

	/**
	 * Add a warning to this player in case they fist-rush another one.
	 * @param victim The player hit by this player.
	 */
	public void addFistWarning(SGPlayer victim)
	{
		if (!victim.equals(lastFist))
		{
			nFistWarning = plugin.getMaxFistWarning();
			lastFist = victim;
		}
		
		nFistWarning--;

		if (nFistWarning == 0)
		{
			getPlayer().kickPlayer("T'es fier ? Maintenant va apprendre � jouer.");
			Bukkit.broadcastMessage("On applaudit bien fort " + ScapegoatPlugin.PLAYER_COLOR
					+ getName() + ChatColor.RESET + " qui tape sur tout ce qui bouge !");
			remove();
		}
	}
	
	@Override
	public PlayerType getType()
	{
		if (equals(scapegoat))
			return PlayerType.SCAPEGOAT;
		else
			return PlayerType.PLAYER;
	}

	/**
	 * Give a jukebox to this player with a nice message in case they picked up a record.
	 */
	public void giveJukebox()
	{
		if (getHasRecord())
			return;
		
		getPlayer().getInventory().addItem(new ItemStack(Material.JUKEBOX));
		getPlayer().sendMessage(ChatColor.YELLOW
				+ "Tiens, un petit cadeau pour lire ton disque, ne le perd pas ! ~Elarcis");
		setHasRecord(true);
	}

	/**
	 * @return true if the item in the hand of this player is considered as a weapon.
	 */
	public boolean hasWeapon()
	{
		Material material = getPlayer().getItemInHand().getType();

		return material == Material.WOOD_SWORD
				|| material == Material.WOOD_PICKAXE
				|| material == Material.WOOD_AXE
				|| material == Material.STONE_SWORD
				|| material == Material.STONE_PICKAXE
				|| material == Material.STONE_AXE
				|| material == Material.IRON_SWORD
				|| material == Material.IRON_PICKAXE
				|| material == Material.IRON_AXE
				|| material == Material.GOLD_SWORD
				|| material == Material.GOLD_PICKAXE
				|| material == Material.GOLD_AXE
				|| material == Material.DIAMOND_SWORD
				|| material == Material.DIAMOND_PICKAXE
				|| material == Material.DIAMOND_AXE;
	}

	@Override
	public void join()
	{
		super.join();
		
		// Hide every spectator to that blessed ignorant.
		for (Entry<UUID, SGSpectator> e : sgSpectators.entrySet())
			if (e.getValue().isOnline())
				getPlayer().hidePlayer(e.getValue().getPlayer());
	}
	
	/**
	 * Consider this player as dead and out of the plugin.<br/>
	 * It is not equal to a Minecraft death since they can respawn and still be "dead".
	 * @param cause
	 */
	public void kill(EntityDamageEvent cause)
	{
		Player killed = Bukkit.getPlayer(id);
		dead = true;

		if (getPlayerCount() > 1)
		{
			Player killer = killed.getKiller();
			String kickMessage = null;

			if (killer != null)
			{
				ChatColor kColor = ScapegoatPlugin.SCAPEGOAT_COLOR;

				if (!killer.isDead())
				{
					if(!scapegoat.equals(killer))
					{
						SGPlayer sgkiller = getSGPlayer(killer.getUniqueId());
						
						if (sgkiller != null)
						{
							setScapegoat(sgkiller, true);
							plugin.getGameState().rebuildPanel();
							kColor = ScapegoatPlugin.PLAYER_COLOR;
						}
							
					}
					
					Running state = (Running) plugin.getGameState();
					
					if (state.getModifier() == GameModifier.UHC)
						killer.setHealth(Math.min(20, killer.getHealth() + plugin.getHealthRestoreOnUHC()));
					
					SGPlayer sgkiller = SGOnline.getSGPlayer(killer.getUniqueId());
					sgkiller.setKills(sgkiller.getKills() + 1);
					sgkiller.setScore(sgkiller.getScore() + 2);
				}
				
				kickMessage = "Tu� par " + kColor + scapegoat.getName()
						+ ChatColor.RED + " (" + (int)Math.ceil(killer.getHealth())
						+ " PV)" + ChatColor.RESET + ".";
			} else
			{
				// TODO: Display custom message for deaths.
				 kickMessage = "Mort(e) comme une patate : "
						+ ScapegoatPlugin.PLAYER_COLOR + cause.getCause();
			}
			
			SGPlayer sgkilled = SGOnline.getSGPlayer(killed.getUniqueId());
			sgkilled.setDeaths(sgkilled.getDeaths() + 1);
			sgkilled.setScore(Math.max(sgkilled.getScore() - 3, 0));
			
			SGOnline.getSGPlayer(killed.getUniqueId()).remove();
			
			new PlayerKickScheduler(killed.getUniqueId(), kickMessage)
			.runTaskLater(plugin, 20 * 5);
		}
	}

	@Override
	public void remove()
	{
		if (sgPlayers.remove(id) == null)
			return;
		
		if (plugin.getGameStateType() == GameStateType.RUNNING)
		{
			if (getPlayerCount() == 1)
			{
				SGPlayer winner = (SGPlayer) sgPlayers.values().toArray()[0];
				plugin.endGame(winner);
			}
			else if (getPlayerCount() > 1)
			{
				if (getScapegoat().equals(id) && getPlayerCount() > 1)
					switchScapegoat(true);
				
				plugin.getGameState().updatePanelTitle();
			}
		}

		plugin.getScoreboard().getTeam("Players").removePlayer(getPlayer());
		super.remove();
	}
}
