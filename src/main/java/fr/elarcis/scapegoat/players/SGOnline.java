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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.SkullMeta;

import fr.elarcis.scapegoat.ScapegoatPlugin;
import fr.elarcis.scapegoat.async.GetPlayerStatsAsync;
import fr.elarcis.scapegoat.async.SetPlayerStatsAsync;
import fr.elarcis.scapegoat.gamestate.GameStateType;

/**
 * Abstraction layer around standard {@link org.bukkit.entity.Player Entity.Player} class.
 * A SGOnline is created on each player's login unless something is terribly wrong with the plugin.
 * Provides custom player operations and optimizations mandatory for a scapegoat game to run.
 * @author Lars
 */
public abstract class SGOnline
{
	//TODO: Clean up some useless duplicated attributes here and directly use Bukkit's ones.
	
	protected static ScapegoatPlugin plugin = ScapegoatPlugin.getPlugin(ScapegoatPlugin.class);

	protected String name;
	protected UUID id;

	protected int kills;
	protected int deaths;
	protected int score;
	protected int plays;
	protected int wins;
	protected boolean dataFetched;

	protected boolean hasRecord;

	protected static int mediumScore = 0;
	
	protected static SGPlayer scapegoat;
	protected static boolean showScapegoat;

	protected static Map<UUID, SGPlayer> sgPlayers = new HashMap<UUID, SGPlayer>();
	protected static Map<UUID, SGSpectator> sgSpectators = new HashMap<UUID, SGSpectator>();
	protected static Map<UUID, SGOnline> sgOnlines = new HashMap<UUID, SGOnline>();

	/**
	 * Create a new SGOnline from a Bukkit player and register them in static maps.
	 * There should be only ONE SGOnline per player, as they are remembered via their UUID.
	 * @param p The bukkit player linked to that SGOnline.
	 */
	public SGOnline(Player p)
	{
		this.name = p.getName();
		this.id = p.getUniqueId();
		this.kills = 0;
		this.deaths = 0;
		this.score = 0;
		this.plays = 0;
		this.wins = 0;
		this.dataFetched = false;
		this.hasRecord = false;

		sgOnlines.put(id, this);
		plugin.putPlayer(p);
	}

	/**
	 * Like {@link Bukkit#broadcastMessage} except the message will only be seen by {@link PlayerType#PLAYER}.
	 * @param msg
	 */
	public static void broadcastPlayers(String msg)
	{
		for (Entry<UUID, SGPlayer> e : sgPlayers.entrySet())
		{
			Player p = e.getValue().getPlayer();
			if (p != null)
				p.sendMessage(msg);
		}
	}

	/**
	 * Like {@link Bukkit#broadcastMessage} except the message will only be seen by {@link PlayerType#PLAYER}.
	 * @param msg
	 */
	public static void broadcastPlayers(String[] msg)
	{
		for (Entry<UUID, SGPlayer> e : sgPlayers.entrySet())
		{
			Player p = e.getValue().getPlayer();
			if (p != null)
				p.sendMessage(msg);
		}
	}

	/**
	 * Like {@link Bukkit#broadcastMessage} except the message will only be seen by {@link PlayerType#SPECTATOR}.
	 * @param msg
	 */
	public static void broadcastSpectators(String msg)
	{
		for (Entry<UUID, SGSpectator> e : sgSpectators.entrySet())
		{
			Player p = e.getValue().getPlayer();
			if (p != null && p.isOnline())
				p.sendMessage(msg);
		}
	}

	/**
	 * Like {@link Bukkit#broadcastMessage} except the message will only be seen by {@link PlayerType#SPECTATOR}.
	 * @param msg
	 */
	public static void broadcastSpectators(String[] msg)
	{
		for (Entry<UUID, SGSpectator> e : sgSpectators.entrySet())
		{
			Player p = e.getValue().getPlayer();
			if (p != null && p.isOnline())
				p.sendMessage(msg);
		}
	}

	/**
	 * @return the total number of {@link PlayerType#PLAYER} registered on the server.
	 */
	public static int getPlayerCount() { return sgPlayers.size(); }

	/**
	 * @return the current {@link PlayerType#SCAPEGOAT}
	 */
	public static SGPlayer getScapegoat() { return scapegoat; }
	
	/**
	 * Gets a {@link SGPlayer} from any UUID.
	 * @param player the UUID to check
	 * @return a {@link SGPlayer} if one exists, null otherwhise.<br/>ALWAYS PERFORM A NULL CHECK !
	 */
	public static SGPlayer getSGPlayer(UUID player) { return sgPlayers.get(player); }
	
	/**
	 * Gets a {@link SGSpectator} from any UUID.
	 * @param player the UUID to check
	 * @return a {@link SGSpectator} if one exists, null otherwhise.<br/>ALWAYS PERFORM A NULL CHECK !
	 */
	public static SGSpectator getSGSpectator(UUID player) {	return sgSpectators.get(player); }
	
	/**
	 * Gets a {@link SGOnline} from any UUID.
	 * @param player the UUID to check
	 * @return a {@link SGOnline} if one exists, null otherwhise.<br/>ALWAYS PERFORM A NULL CHECK !
	 */
	public static SGOnline getSGOnline(UUID player) { return sgOnlines.get(player); }

	/**
	 * @return true if the scapegoat is visually different from other players
	 * (colored name and displayed below the countdown) or false if not.
	 */
	public static boolean getShowScapegoat() { return showScapegoat; }

	/**
	 * Gets the type of a player from their UUID.
	 * @param player the UUID to check
	 * @return A {@link PlayerType} according to the checked player, {@link PlayerType#UNKNOWN} if not found.
	 */
	public static PlayerType getType(UUID player)
	{
		SGOnline online = sgOnlines.get(player);
		
		if (online == null)
			return PlayerType.UNKNOWN;
		else
			return online.getType();
	}

	/**
	 * Theoretically sends the scapegoat's chunk to every player before teleporting one of them.
	 * Could possibly improve performances, I'm not even sure of that.<br/><br/>
	 * TODO: Check if refeshScapegoatChunk() actually is any useful.
	 */
	public static void refeshScapegoatChunk()
	{
		if (scapegoat == null || !scapegoat.isOnline())
			return;
		
		Player s = scapegoat.getPlayer();
		Chunk sChunk = s.getWorld().getChunkAt(s.getLocation());
		s.getWorld().refreshChunk(sChunk.getX(), sChunk.getZ());
	}

	/**
	 * Change the current {@link PlayerType#SCAPEGOAT}, and proceed to any necessary operation related to that.
	 * @param scapegoat The new scapegoat.
	 * @param warn Wether players should be aware of who the new scapegoat is or not.
	 */
	public static void setScapegoat(SGPlayer scapegoat, boolean warn)
	{
		SGPlayer currScapegoat = SGOnline.scapegoat;
		Player pScapegoat;

		if (currScapegoat != null)
		{
			pScapegoat = currScapegoat.getPlayer();
			plugin.getScoreboard().getTeam("Scapegoat").removePlayer(pScapegoat);
			plugin.getScoreboard().getTeam("Players").addPlayer(pScapegoat);
		}

		SGOnline.scapegoat = scapegoat;

		if (warn)
		{
			pScapegoat = scapegoat.getPlayer();
			plugin.getScoreboard().getTeam("Players").removePlayer(pScapegoat);
			plugin.getScoreboard().getTeam("Scapegoat").addPlayer(pScapegoat);

			Bukkit.broadcastMessage("Nouveau bouc-émissaire : "
					+ ScapegoatPlugin.SCAPEGOAT_COLOR + scapegoat.getName());
			showScapegoat = true;

			for (Player p : Bukkit.getOnlinePlayers())
				p.playSound(p.getLocation(), Sound.SUCCESSFUL_HIT, 1, 1);
		}
		else
		{
			scapegoat.sendMessage(ScapegoatPlugin.SCAPEGOAT_COLOR
					+ "Vous êtes le bouc-émissaire ! Défendez votre peau !");
			showScapegoat = false;
		}
	}

	/**
	 * Randomly select a new scapegoat among players.
	 * @param warn Wether players should be aware of who the new scapegoat is or not.
	 */
	public static void switchScapegoat(boolean warn)
	{
		if (getPlayerCount() < 2)
			return;
		
		int sIndex = new Random().nextInt(getPlayerCount());
		setScapegoat((SGPlayer) sgPlayers.values().toArray()[sIndex], warn);
		plugin.getGameState().rebuildPanel();
	}

	@Override
	public boolean equals(Object o)
	{
		if (o instanceof SGOnline)
			return this.id.equals(((SGOnline) o).getId());
		else if (o instanceof UUID)
			return this.id.equals((UUID) o);
		else if (o instanceof OfflinePlayer)
			return this.id.equals(((OfflinePlayer) o).getUniqueId());
		else
			return false;
	}
	
	/**
	 * Compute the value of the medium of the scores of every {@link PlayerType.PLAYER},<br/>
	 * for score nerfing matters.
	 */
	public static void computeMediumScore()
	{
		double medium = 0.0;
		
		for (Entry<UUID, SGPlayer> p : sgPlayers.entrySet())
		{
			medium += p.getValue().getScore();
		}
		
		medium /= sgPlayers.size();
		mediumScore = (int) medium;
		
		plugin.info("Medium Score updated : " + mediumScore);
	}
	
	/**
	 * @return The UUID of the player associated to that instance.
	 */
	public UUID getId() { return id; }
	
	/**
	 * @return The name of the player associated to that instance.
	 */
	public String getName() { return name; }
	
	/**
	 * @return The {@link org.bukkit.entity.Player Entity.Player} associated to that instance.
	 */
	public Player getPlayer() { return Bukkit.getPlayer(id); }

	/**
	 * @return The {@link PlayerType} corresponding to that player.
	 */
	public abstract PlayerType getType();
	
	/**
	 * @return Wether this player's stats were already successfully fetched in database or not.
	 * If you're not using a database, false will always be returned.
	 */
	public synchronized boolean getDataFetched() { return dataFetched; }

	/**
	 * @return How many people this player killed.
	 * If using a database, that number is stored between games.
	 */
	public synchronized int getKills() { return kills; }
	
	/**
	 * @return How many times this player died, killed by another player or not.
	 * If using a database, that number is stored between games.
	 */
	public synchronized int getDeaths() { return deaths; }
	
	/**
	 * @return the medium of each player's score, for score nerfing matters.
	 */
	public static int getMediumScore() { return mediumScore; }
	
	/**
	 * @return The score of this player.
	 * If using a database, that number is stored between games.
	 */
	public synchronized int getScore() { return score; }
	
	/**
	 * @return How many games this player started.
	 * If using a database, that number is stored between games. If not, well it's not very useful.
	 */
	public synchronized int getPlays() { return plays; }
	
	/**
	 * @return How many games this player won.
	 * If using a database, that number is stored between games.
	 */
	public synchronized int getWins() { return wins; }

	/**
	 * Should be set to true once this player's stats have been successfuly fetched from database.
	 * @param dataFetched
	 */
	public synchronized void setDataFetched(boolean dataFetched) { this.dataFetched = dataFetched; }
	
	/**
	 * Modify the number of kills of this player.
	 * @param kills
	 */
	public synchronized void setKills(int kills) { this.kills = kills; }
	
	/**
	 * Modify the number of deaths of this player.
	 * @param deaths
	 */
	public synchronized void setDeaths(int deaths) { this.deaths = deaths; }

	/**
	 * Modify the score of this player and show it to others.
	 * @param score
	 */
	public synchronized void setScore(int score)
	{
		this.score = score;
		plugin.getScoreboard().getObjective("scores").getScore(getName()).setScore(score);
	}
	
	/**
	 * Modify the number of games this player started.
	 * @param plays
	 */
	public synchronized void setPlays(int plays) { this.plays = plays; }

	/**
	 * Modify the number of games this player won.
	 * @param wins
	 */
	public synchronized void setWins(int wins)
	{
		this.wins = wins;
		if (plugin.getGameStateType() == GameStateType.WAITING)
			giveTrophees();
	}

	/**
	 * Give any achievement related item to this player, according to their score.
	 */
	public void giveTrophees()
	{
		if (wins <= 0)
			return;
		
		PlayerInventory inv = getPlayer().getInventory();

		byte damage;
		ChatColor skullColor;

		if (wins < 5)
		{
			damage = 2;
			skullColor = ChatColor.AQUA;
		}
		else if (wins < 10)
		{
			damage = 0;
			skullColor = ChatColor.GREEN;
		}
		else if (wins < 50)
		{
			damage = 4;
			skullColor = ChatColor.YELLOW;
		}
		else
		{
			damage = 1;
			skullColor = ChatColor.RED;
		}

		ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, damage);

		String plural = (wins > 1 ? "s" : "");
		String skullName = wins + " partie" + plural + " gagnée" + plural + " !";

		SkullMeta hMeta = (SkullMeta) head.getItemMeta();
		hMeta.setDisplayName(skullColor + skullName);
		head.setItemMeta(hMeta);

		inv.addItem(head);
	}

	/**
	 * @return true if that player already got a jukebox from finding a record.
	 */
	public boolean getHasRecord() { return hasRecord; }
	
	/**
	 * Should be set to true once a player got a jukebox from finding a record.
	 * @param hasRecord
	 */
	public void setHasRecord(boolean hasRecord) { this.hasRecord = hasRecord; }

	/**
	 * Wether the player is found by {@link org.bukkit.Bukkit#getPlayer Bukkit.getPlayer}.
	 * @return
	 */
	public boolean isOnline() { return Bukkit.getPlayer(id) != null; }

	/**
	 * Should be run each time a new player is created or connects to the server.
	 */
	public void join()
	{
		if (plugin.getDb() != null)
			new GetPlayerStatsAsync(this).runTaskAsynchronously(plugin);
	}

	/**
	 * Dereference the player from lists and update their score to the database.
	 */
	public void remove()
	{
		sgOnlines.remove(id);
		if (plugin.getDb() != null)
			new SetPlayerStatsAsync(this).runTaskAsynchronously(plugin);
	}

	/**
	 * Send a message to that player.
	 * @param msg
	 */
	public void sendMessage(String msg)
	{
		getPlayer().sendMessage(msg);
	}

	/**
	 * Should be run the first time the player is created.
	 */
	public void welcome()
	{
		String[] welcome = new String[] {
				ChatColor.GREEN + "Bienvenue sur le bouc-émissaire !",
				ChatColor.RED
						+ "Le livre des règles du jeu est régulièrement mis à jour, n'oubliez pas de le lire !",
				ChatColor.GREEN
						+ "Le lag n'est pas considéré comme un bug. "
						+ "Veuillez ne pas me reprocher votre mauvaise connexion.", };

		getPlayer().sendMessage(welcome);
	}
}
