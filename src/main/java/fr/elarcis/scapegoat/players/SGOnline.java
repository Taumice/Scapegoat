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
import fr.elarcis.scapegoat.gamestate.GameStateType;

public abstract class SGOnline {
	protected static ScapegoatPlugin plugin =
			ScapegoatPlugin.getPlugin(ScapegoatPlugin.class);
	
	protected String name;
	protected UUID id;
	
	protected int kills;
	protected int deaths;
	protected int score;
	protected int plays;
	protected int wins;
	protected boolean dataFetched;
	
	protected boolean hasRecord;
	
	protected static SGPlayer scapegoat;
	protected static boolean showScapegoat;
	
	protected static Map<UUID, SGPlayer> sgPlayers = new HashMap<UUID, SGPlayer>();
	protected static Map<UUID, SGSpectator> sgSpectators = new HashMap<UUID, SGSpectator>();
	
	public SGOnline(OfflinePlayer p) {
		this.name = p.getName();
		this.id = p.getUniqueId();
		this.kills = 0;
		this.deaths = 0;
		this.score = 0;
		this.plays = 0;
		this.wins = 0;
		this.dataFetched = false;
		this.hasRecord = false;
		
		plugin.putPlayer(getPlayer());
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof SGOnline) {
			return this.id.equals(((SGOnline)o).getId());
		} else if (o instanceof UUID) {
			return this.id.equals((UUID)o);
		} else if (o instanceof OfflinePlayer) {
			return this.id.equals(((OfflinePlayer)o).getUniqueId());
		} else return false;
	}
	
	public UUID getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public synchronized int getKills() { return kills; }
	public synchronized int getDeaths() { return deaths; }
	public synchronized int getScore() { return score; }
	public synchronized int getPlays() { return plays; }
	public synchronized int getWins() { return wins; }
	
	public synchronized void setKills(int kills) { this.kills = kills; }
	public synchronized void setDeaths(int deaths) { this.deaths = deaths; }
	public synchronized void setScore(int score)
	{
		this.score = score;
		plugin.getScoreboard().getObjective("scores").getScore(getName()).setScore(score);
	}
	public synchronized void setPlays(int plays) { this.plays = plays; }
	public synchronized void setWins(int wins)
	{
		this.wins = wins;
		if (wins > 0 && plugin.getGameStateType() == GameStateType.WAITING)
			giveTrophees();
	}
	
	public synchronized void setDataFetched(boolean dataFetched)
	{
		this.dataFetched = dataFetched;
	}
	
	public synchronized boolean getDataFetched()
	{
		return dataFetched;
	}
	
	public void giveTrophees()
	{
		if (wins > 0)
		{
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
	}
	
	public boolean hasRecord() {
		return hasRecord;
	}
	
	public void hasRecord(boolean hasRecord) {
		this.hasRecord = hasRecord;
	}
	
	public OfflinePlayer getOfflinePlayer() {
		return Bukkit.getOfflinePlayer(id);
	}
	
	public Player getPlayer() {
		return Bukkit.getPlayer(id);
	}
	
	public abstract PlayerType getType();
	
	public boolean isOnline() {
		return Bukkit.getPlayer(id) != null;
	}
	
	public abstract void remove();
	
	public void sendMessage(String msg) {
		Player p = Bukkit.getPlayer(id);
		if (p != null) p.sendMessage(msg);
	}
	
	public void welcome() {
		if (getOfflinePlayer().isOnline()) {
			String[] welcome = new String[] {
					ChatColor.GREEN + "Bienvenue sur le bouc-émissaire !",
					ChatColor.RED + "Le livre des règles du jeu est régulièrement mis à jour, n'oubliez pas de le lire !",
					ChatColor.GREEN + "Le lag n'est pas considéré comme un bug. Veuillez ne pas me reprocher votre mauvaise connexion.",
			};
			
			getPlayer().sendMessage(welcome);
		}
	}
	
	public static void broadcastPlayers(String msg) {
		for (Entry<UUID, SGPlayer> e : sgPlayers.entrySet()) {
			Player p = e.getValue().getPlayer();
			if (p != null)
				p.sendMessage(msg);
		}
	}
	
	public static void broadcastPlayers(String[] msg) {
		for (Entry<UUID, SGPlayer> e : sgPlayers.entrySet()) {
			Player p = e.getValue().getPlayer();
			if (p != null)
				p.sendMessage(msg);
		}
	}

	
	public static void broadcastSpectators(String msg) {
		for (Entry<UUID, SGSpectator> e : sgSpectators.entrySet()) {
			Player p = e.getValue().getPlayer();
			if (p != null && p.isOnline())
				p.sendMessage(msg);
		}
	}
	
	public static void broadcastSpectators(String[] msg) {
		for (Entry<UUID, SGSpectator> e : sgSpectators.entrySet()) {
			Player p = e.getValue().getPlayer();
			if (p != null && p.isOnline())
				p.sendMessage(msg);
		}
	}
	
	public static int getPlayerCount() {
		return sgPlayers.size();
	}
	
	public static SGPlayer getScapegoat() {
		return scapegoat;
	}
	
	public static SGPlayer getSGPlayer(UUID player) {
		return sgPlayers.get(player);
	}
	
	public static SGSpectator getSGSpectator(UUID player) {
		return sgSpectators.get(player);
	}
	
	public static boolean getShowScapegoat() {
		return showScapegoat;
	}

	public static int getSpectatorCount() {
		return sgSpectators.size();
	}
	
	public static PlayerType getType(UUID player) {
		if (sgPlayers.containsKey(player)) return sgPlayers.get(player).getType();
		else if (sgSpectators.containsKey(player)) return sgSpectators.get(player).getType();
		else return PlayerType.UNKNOWN;
	}
	
	public static void refeshScapegoatChunk() {
		if (scapegoat != null && scapegoat.isOnline()) {
			Player s = scapegoat.getPlayer();
			Chunk sChunk = s.getWorld().getChunkAt(s.getLocation());
			s.getWorld().refreshChunk(sChunk.getX(), sChunk.getZ());
		}
	}
	
	public static void setScapegoat(SGPlayer scapegoat, boolean warn) {
		
		SGPlayer currScapegoat = SGOnline.scapegoat;
		Player pScapegoat;
		
		if (currScapegoat != null)
		{
			pScapegoat = currScapegoat.getPlayer();
			plugin.getScoreboard().getTeam("Scapegoat").removePlayer(pScapegoat);
			plugin.getScoreboard().getTeam("Players").addPlayer(pScapegoat);
		}
		
		SGOnline.scapegoat = scapegoat;

		if (warn) {
			pScapegoat = scapegoat.getPlayer();
			plugin.getScoreboard().getTeam("Players").removePlayer(pScapegoat);
			plugin.getScoreboard().getTeam("Scapegoat").addPlayer(pScapegoat);
			
			Bukkit.broadcastMessage("Nouveau bouc-émissaire : " + ScapegoatPlugin.SCAPEGOAT_COLOR + scapegoat.getName());
			showScapegoat = true;
			
			for (Player p : Bukkit.getOnlinePlayers()) {
				p.playSound(p.getLocation(), Sound.SUCCESSFUL_HIT, 1, 1);
			}
		} else {
			scapegoat.sendMessage(ScapegoatPlugin.SCAPEGOAT_COLOR + "Vous êtes le bouc-émissaire ! Défendez votre peau !");
			showScapegoat = false;
		}
	}
	
	public static void switchScapegoat(boolean warn) {
		setScapegoat((SGPlayer)sgPlayers.values().toArray()[new Random().nextInt(sgPlayers.size())], warn);
		plugin.getGameState().rebuildPanel();
	}
}
