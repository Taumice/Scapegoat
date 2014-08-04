package fr.elarcis.scapegoat.players;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;

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
import fr.elarcis.scapegoat.async.SetPlayerStatsAsync;
import fr.elarcis.scapegoat.gamestate.GameStateType;

public class SGPlayer extends SGOnline
{
	protected boolean dead;
	protected int nFistWarning;
	protected SGPlayer lastFist;

	public SGPlayer(Player player)
	{
		super(player);

		SGOnline.sgPlayers.put(id, this);
		plugin.getScoreboard().getTeam("Players").addPlayer(player);

		if (plugin.getGameStateType() == GameStateType.RUNNING)
		{
			player.kickPlayer("Hélà, on rejoint pas une partie en cours, "
					+ ChatColor.ITALIC + "gros malin" + ChatColor.RESET + ".");
		} else
		{
			player.setGameMode(GameMode.SURVIVAL);
			if (name.equals("Elarcis"))
			{
				player.setDisplayName("§6E§rlarcis");
			} else
			{
				player.setDisplayName(name);
			}

			this.dead = false;
			this.nFistWarning = plugin.getMaxFistWarning();

			player.getInventory().clear();
			plugin.getStuffer().stuff(player, ItemSet.MANUAL);
			giveTrophees();
		}

		// Hide every spectators to every incoming player.
		for (Entry<UUID, SGSpectator> e : sgSpectators.entrySet())
		{
			if (e.getValue().isOnline())
				getPlayer().hidePlayer(e.getValue().getPlayer());
		}
	}

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
			getPlayer().kickPlayer(
					"T'es fier ? Maintenant va apprendre à jouer.");
			Bukkit.broadcastMessage("On applaudit bien fort "
					+ ScapegoatPlugin.PLAYER_COLOR + getName()
					+ ChatColor.RESET + " qui tape sur tout ce qui bouge !");
			remove();
		}
	}

	@Override
	public PlayerType getType()
	{
		if (equals(scapegoat))
		{
			return PlayerType.SCAPEGOAT;
		} else
			return PlayerType.PLAYER;
	}

	public void giveJukebox()
	{
		if (!hasRecord())
		{
			getPlayer().getInventory().addItem(new ItemStack(Material.JUKEBOX));
			getPlayer()
					.sendMessage(
							ChatColor.YELLOW
									+ "Tiens, un petit cadeau pour lire ton disque, ne le perd pas ! ~Elarcis");
			hasRecord(true);
		}
	}

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

	public boolean isDead()
	{
		return dead;
	}

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

				if (!scapegoat.equals(killer) && !killer.isDead())
				{
					setScapegoat(getSGPlayer(killer.getUniqueId()), true);
					plugin.getGameState().rebuildPanel();
					kColor = ScapegoatPlugin.PLAYER_COLOR;
				}

				SGPlayer sgkiller = SGOnline.getSGPlayer(killer.getUniqueId());
				
				if (sgkiller != null)
				{
					sgkiller.setKills(sgkiller.getKills() + 1);
					sgkiller.setScore(sgkiller.getScore() + 2);			
				}
				
				kickMessage = "Tué par " + kColor + scapegoat.getName()
						+ ChatColor.RED + " (" + (int) killer.getHealth()
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

	public void remove()
	{
		if (sgPlayers.remove(id) != null)
		{
			if (plugin.getGameStateType() == GameStateType.RUNNING)
			{
				if (getPlayerCount() == 1)
				{
					plugin.endGame(((SGPlayer) sgPlayers.values().toArray()[0]));
				} else if (getPlayerCount() > 1)
				{
					if (getScapegoat().equals(id) && getPlayerCount() > 1)
					{
						switchScapegoat(true);
					}

					plugin.getGameState().updatePanelTitle();
				}
			}

			plugin.getScoreboard().getTeam("Players")
					.removePlayer(getPlayer());
			
			new SetPlayerStatsAsync(this).runTaskAsynchronously(plugin);
		}
	}

	public static synchronized void teleport()
	{
		Map<UUID, SGPlayer> candidates = new HashMap<UUID, SGPlayer>(sgPlayers);
		candidates.remove(scapegoat.getId());

		Player t = ((SGPlayer) candidates.values().toArray()[new Random()
				.nextInt(candidates.size())]).getPlayer();
		Player s = SGOnline.scapegoat.getPlayer();

		// Trap detection System
		
		t.setNoDamageTicks(40);
		s.setNoDamageTicks(40);

		Location abs = s.getLocation();

		int pitSize = 4;
		boolean solidFound = false;
		boolean lavaFound = false;

		try
		{
			if (!t.isInsideVehicle())
			{
				t.teleport(s, TeleportCause.PLUGIN);
			} else
			{
				Entity v = t.getVehicle();
				v.eject();
				v.teleport(s, TeleportCause.PLUGIN);
				t.teleport(s, TeleportCause.PLUGIN);
				v.setPassenger(t);
			}

			for (int i = 0; i <= pitSize; i++)
			{
				Block b = abs.getWorld().getBlockAt(abs.getBlockX(),
						abs.getBlockY() - i - 1, abs.getBlockZ());

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

			plugin.addTeleport();
			s.getWorld().strikeLightningEffect(s.getLocation());

			for (Player p : Bukkit.getOnlinePlayers())
			{
				p.playSound(p.getLocation(), Sound.AMBIENCE_THUNDER, 1, 1);
			}

			Bukkit.broadcastMessage(ScapegoatPlugin.PLAYER_COLOR + t.getName()
					+ ChatColor.RESET + " a été téléporté auprès du "
					+ ScapegoatPlugin.SCAPEGOAT_COLOR + "bouc-émissaire !");
		} catch (NullPointerException ex)
		{
			// logging here
		}
	}
}
