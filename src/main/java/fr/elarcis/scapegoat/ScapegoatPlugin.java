package fr.elarcis.scapegoat;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Scoreboard;

import code.husky.Database;
import code.husky.mysql.MySQL;
import fr.elarcis.scapegoat.async.GetPlayerStatsAsync;
import fr.elarcis.scapegoat.async.PlayerKickScheduler;
import fr.elarcis.scapegoat.async.TimerThread;
import fr.elarcis.scapegoat.gamestate.GameState;
import fr.elarcis.scapegoat.gamestate.GameStateType;
import fr.elarcis.scapegoat.gamestate.Running;
import fr.elarcis.scapegoat.gamestate.Waiting;
import fr.elarcis.scapegoat.players.*;

public final class ScapegoatPlugin extends JavaPlugin
{
	protected boolean running;
	protected TimerThread timer;
	protected GameState state;

	protected int playersRequired;
	protected int maxPlayers;

	protected int waitBeforeStart;
	protected boolean forceStart;

	protected int teleporterDelay;
	protected int teleporterMinimumDelay;
	protected int teleporterMaximumDelay;
	protected int teleporterDelaySubstraction;
	protected int nTeleport;

	protected Scoreboard scoreboard;
	protected ItemStuffer stuffer;

	protected Set<UUID> nVotemap;
	protected Map<String, UUID> nameToUuid;

	protected int maxFistWarnings;

	protected boolean maintenanceMode;
	protected String maintenanceModeMessage;

	public static final ChatColor PLAYER_COLOR = ChatColor.DARK_RED;
	public static final ChatColor SCAPEGOAT_COLOR = ChatColor.DARK_PURPLE;

	protected Database database;
	protected Connection dbConnect;

	public void addTeleport() { nTeleport++; }

	public void createSGPlayer(Player p)
	{
		SGOnline newPlayer = null;
		UUID pId = p.getUniqueId();
		
		switch(SGOnline.getType(pId))
		{
		case SPECTATOR:
			newPlayer = SGOnline.getSGSpectator(pId);
			newPlayer.join();
			break;
		default:
			if (p.isOp())
			{
				newPlayer = new SGSpectator(p);
				newPlayer.welcome();
			}
			else
			{
				newPlayer = new SGPlayer(p);
				newPlayer.welcome();
			}
		}

		new GetPlayerStatsAsync(newPlayer).runTaskAsynchronously(this);
	}

	public void endGame(SGPlayer winner)
	{
		if (winner != null)
		{
			String name = winner.getName();
			winner.setWins(winner.getWins() + 1);
			winner.setScore(winner.getScore() + 3);

			for (Player p : Bukkit.getOnlinePlayers())
			{
				String kickMessage = null;

				if (winner.equals(p))
				{
					kickMessage = ChatColor.GOLD + "GG !";
				}
				else
				{
					ChatColor kColor = ChatColor.DARK_RED;
					if (winner.getType() == PlayerType.SCAPEGOAT)
						kColor = ChatColor.DARK_PURPLE;

					kickMessage = kColor + winner.getName() + ChatColor.RED + " ("
							+ (int) winner.getPlayer().getHealth() + " PV)" + ChatColor.RESET + " a gagné !";
				}

				switch (SGOnline.getType(p.getUniqueId()))
				{
				case PLAYER:
				case SCAPEGOAT:
					SGPlayer sgp = SGOnline.getSGPlayer(p.getUniqueId());
					if (sgp != null)
						sgp.remove();
					break;
				case SPECTATOR:
					SGSpectator sgsp = SGOnline.getSGSpectator(p.getUniqueId());
					if (sgsp != null)
						sgsp.remove();
					break;
				default:
				}

				new PlayerKickScheduler(p.getUniqueId(), kickMessage).runTaskLater(this, 20 * 5);
			}

			getLogger().info(name + " a gagné !");
		}
		else
		{
			for (Player p : Bukkit.getOnlinePlayers())
			{
				p.kickPlayer(ChatColor.YELLOW + "...pas de gagnant o_o");
			}
		}
	}

	public void forceTimer(int secondsLeft)
	{
		timer.setSecondsLeft(secondsLeft);
	}

	public synchronized Connection getDbConnection()
	{
		return dbConnect;
	}

	public boolean getForceStart()
	{
		return forceStart;
	}

	public GameState getGameState()
	{
		return state;
	}

	public synchronized GameStateType getGameStateType()
	{
		return state.getType();
	}

	public String getMaintenanceMessage()
	{
		return maintenanceModeMessage;
	}

	public int getMaxFistWarning()
	{
		return maxFistWarnings;
	}

	public int getMaxPlayers()
	{
		return maxPlayers;
	}

	public int getPlayersRequired()
	{
		return playersRequired;
	}

	public Scoreboard getScoreboard()
	{
		return scoreboard;
	}

	public ItemStuffer getStuffer()
	{
		return stuffer;
	}

	public int getTeleportCount()
	{
		return nTeleport;
	}

	public int getTeleporterDelay()
	{
		return teleporterDelay;
	}

	public UUID getUuid(String player)
	{
		return this.nameToUuid.get(player);
	}

	public int getVotemaps()
	{
		int total = 0;
		for (UUID p : nVotemap)
		{
			if (Bukkit.getPlayer(p) != null)
				total++;
		}

		return total;
	}

	public int getVotemapsRequired()
	{
		return (Math.max(SGOnline.getPlayerCount(), getPlayersRequired()) / 2) + 1;
	}

	public int getWaitBeforeStart()
	{
		return waitBeforeStart;
	}

	public void info(String message)
	{
		getLogger().info(message);
	}

	public boolean isInMaintenanceMode()
	{
		return maintenanceMode;
	}

	public synchronized boolean isRunning()
	{
		return running;
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		String lCmd = cmd.getName().toLowerCase();
		UUID senderId = null;
		String result = "Commande inconnue";

		if (sender instanceof Player)
			senderId = ((Player) sender).getUniqueId();

		if (lCmd.equals("spectate"))
			if (args.length == 2)
				result = spectateCommand(args[0], args[1]);
			else
				result = "Mauvais nombre d'arguments !";
		else if (lCmd.equals("start"))
			result = forceStartCommand();
		else if (lCmd.equals("votemap") && senderId != null)
			result = voteMapCommand((Player) sender);
		else if (lCmd.equals("maintenance"))
		{
			if (args.length > 1)
			{
				String msg = "";
			
				for (int i = 1; i < args.length; i++)
					msg += args[i] + " ";
				
				result = setMaintenanceModeCommand(args[0], msg);
			}
		}

		if (result.equals("")) return true;
		else
		{
			sender.sendMessage(ChatColor.DARK_RED + result);
			return false;
		}
	}
	
	public String spectateCommand(String player, String mode)
	{
		Player p = Bukkit.getPlayer(getUuid(player));

		if (p == null)
			return "Joueur introuvable";
		
		UUID pId = p.getUniqueId();
		
		if (mode.equalsIgnoreCase("on"))
		{
			if (SGOnline.getSGSpectator(pId) == null)
			{
				SGOnline.getSGPlayer(pId).remove();
				new SGSpectator(p);
				return "";
			}
			else
				return "Ce joueur est déjà spectateur.";
		}
		else if (mode.equalsIgnoreCase("off"))
		{
			if (SGOnline.getSGPlayer(pId) == null)
			{
				SGOnline.getSGSpectator(pId).remove();
				new SGPlayer(p);
				return "";
			}
			else
				return "Ce joueur n'est pas spectateur.";
		}
		else
			return "Syntaxe incorrecte.";
	}

	public String voteMapCommand(Player voter)
	{
		if (getGameStateType() != GameStateType.WAITING)
			return "Impossible de voter en cours de partie !";

		if (nVotemap.add(voter.getUniqueId()))
		{
			Bukkit.broadcastMessage(ChatColor.GREEN + voter.getName() + ChatColor.YELLOW
					+ " a demandé un changement de map !");
			Bukkit.broadcastMessage(ChatColor.RED + "(" + nVotemap.size() + "/" + getVotemapsRequired()
					+ " requis)");
			return "";
		}
		else
			return "Vous avez déjà voté >:c";
	}
	
	public String forceStartCommand()
	{
		if (getGameStateType() != GameStateType.WAITING)
			return "Partie déjà démarrée !";
		
		if (SGOnline.getPlayerCount() < 2)
			return "Pas assez de joueurs !";

		this.forceStart = true;
		return "";
	}
	
	public String setMaintenanceModeCommand(String mode, String msg)
	{
		if (mode.equals("on"))
		{
			maintenanceMode = true;
			getLogger().info("Mode maintenance activé" + ((msg.equals("")) ? "." : " : " + msg));
		}
		else if (mode.equals("off"))
		{
			maintenanceMode = false;
			getLogger().info("Mode maintenance désactivé.");
		}
		else 
			return "Syntaxe incorrecte.";
		
		return "";
	}

	public void onDisable()
	{
		try
		{
			database.closeConnection();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		stop();
	}

	public void onEnable()
	{
		saveDefaultConfig();

		this.running = false;
		this.timer = new TimerThread();

		this.playersRequired = getConfig().getInt("playersRequired");
		this.waitBeforeStart = getConfig().getInt("waitBeforeStart");
		this.maxPlayers = getConfig().getInt("maxPlayers");
		this.forceStart = false;

		this.teleporterMaximumDelay = getConfig().getInt("teleport.maxDelay");
		this.teleporterMinimumDelay = getConfig().getInt("teleport.minDelay");
		this.teleporterDelaySubstraction = getConfig().getInt("teleport.substract");
		this.teleporterDelay = this.teleporterMaximumDelay;

		this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
		this.scoreboard.registerNewTeam("Spectators").setPrefix(ChatColor.GREEN + "");
		this.scoreboard.registerNewTeam("Players");
		this.scoreboard.registerNewTeam("Scapegoat").setPrefix(SCAPEGOAT_COLOR + "");
		this.scoreboard.registerNewObjective("panelInfo", "dummy").setDisplaySlot(DisplaySlot.SIDEBAR);
		this.scoreboard.registerNewObjective("scores", "dummy").setDisplaySlot(DisplaySlot.PLAYER_LIST);

		this.nVotemap = new HashSet<UUID>();
		this.nameToUuid = new HashMap<String, UUID>();

		this.stuffer = new ItemStuffer();
		
		this.maxFistWarnings = getConfig().getInt("security.maxFistWarnings");

		this.database = new MySQL(
				this, getConfig().getString("database.host"),
				getConfig().getString("database.port"),
				getConfig().getString("database.database"),
				getConfig().getString("database.user"),
				getConfig().getString("database.password"));

		try
		{
			dbConnect = database.openConnection();
		}
		catch (ClassNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		
		setGameState(GameStateType.WAITING);

		for (Player p : Bukkit.getOnlinePlayers())
		{
			p.getInventory().clear();
			p.setScoreboard(getScoreboard());
			createSGPlayer(p);
		}
		start();
	}

	public void putPlayer(Player player)
	{
		this.nameToUuid.put(player.getName(), player.getUniqueId());
	}

	public void removeVotemap(UUID player)
	{
		nVotemap.remove(player);
	}

	public synchronized void setGameState(GameStateType gametype)
	{
		if (this.state != null)
			this.state.unregister();

		switch (gametype)
		{
		case WAITING:
			state = new Waiting();
			timer.setSecondsLeft(waitBeforeStart);
			break;
		case RUNNING:
			state = new Running();
			timer.setSecondsLeft(getTeleporterDelay());
			break;
		default:
		}

		this.state.register();
		this.state.init();
	}

	public synchronized void start()
	{
		if (running)
			return;
		
		running = true;
		timer.start();
	}

	public synchronized void stop()
	{
		if (!running)
			return;
		
		running = false;
	}

	public synchronized void timerTick(int secondsLeft)
	{
		timer.setSecondsLeft(state.timerTick(secondsLeft));

		if (timer.isDone() || getForceStart())
		{
			switch (state.getType())
			{
			case WAITING:
				if (getVotemaps() >= getVotemapsRequired())
				{
					for (Player p : Bukkit.getOnlinePlayers())
					{
						p.kickPlayer(ChatColor.YELLOW + "Changement de map voté. Veuillez vous reconnecter.");
					}
					Bukkit.shutdown();
				}
				else
				{
					setGameState(GameStateType.RUNNING);
					getLogger().info("Nouveau bouc-émissaire : " + SGOnline.getScapegoat().getName());
					timer.setSecondsLeft(updateTeleporterDelay());
				}
				break;
			case RUNNING:
				break;
			}
		}
	}

	public int updateTeleporterDelay()
	{
		int substract = Math.max(SGOnline.getPlayerCount() - 3, 0);
		teleporterDelay = Math.min(teleporterMaximumDelay, teleporterMinimumDelay
				+ (teleporterDelaySubstraction * substract));
		return teleporterDelay;
	}
}
