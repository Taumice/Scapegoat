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

import code.husky.mysql.MySQL;
import fr.elarcis.scapegoat.async.GetPlayerStatsAsync;
import fr.elarcis.scapegoat.async.PlayerKickScheduler;
import fr.elarcis.scapegoat.async.TimerThread;
import fr.elarcis.scapegoat.gamestate.GameState;
import fr.elarcis.scapegoat.gamestate.GameStateType;
import fr.elarcis.scapegoat.gamestate.Running;
import fr.elarcis.scapegoat.gamestate.Waiting;
import fr.elarcis.scapegoat.players.PlayerType;
import fr.elarcis.scapegoat.players.SGOnline;
import fr.elarcis.scapegoat.players.SGPlayer;
import fr.elarcis.scapegoat.players.SGSpectator;

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

	protected MySQL mySQL;
	protected Connection dbConnect;

	public void addTeleport() { nTeleport++; }
	
	public void putPlayer(Player player)
	{
		this.nameToUuid.put(player.getName(), player.getUniqueId());
	}
	
	public UUID getUuid(String player)
	{
		return this.nameToUuid.get(player);
	}

	public void createSGPlayer(Player p)
	{
		SGOnline newPlayer = null;

		if (!p.isOp() && SGOnline.getType(p.getUniqueId()) != PlayerType.SPECTATOR)
		{
			newPlayer = new SGPlayer(this, p);
			newPlayer.welcome();
		} else if (SGOnline.getType(p.getUniqueId()) != PlayerType.SPECTATOR)
		{
			newPlayer = new SGSpectator(this, p);
			newPlayer.welcome();
		} else
		{
			newPlayer = SGOnline.getSGSpectator(p.getUniqueId());
			((SGSpectator)newPlayer).join();
		}
		
		new GetPlayerStatsAsync(this, newPlayer).runTaskAsynchronously(this);
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
				} else
				{
					ChatColor kColor = ChatColor.DARK_RED;
					if (winner.getType() == PlayerType.SCAPEGOAT)
						kColor = ChatColor.DARK_PURPLE;

					kickMessage = kColor + winner.getName()
							+ ChatColor.RED + " (" + (int)winner.getPlayer().getHealth() + " PV)"
							+ ChatColor.RESET + " a gagné !";
				}
				
				switch(SGOnline.getType(p.getUniqueId()))
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
				
				new PlayerKickScheduler(this, p.getUniqueId(), kickMessage)
				.runTaskLater(this, 20 * 5);
			}

			getLogger().info(name + " a gagné !");
		} else
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

	public String getMaintenanceMessage() { return maintenanceModeMessage; }
	
	public int getMaxFistWarning() { return maxFistWarnings; }
	
	public int getMaxPlayers() { return maxPlayers; }
	public int getPlayersRequired() { return playersRequired; }
	public Scoreboard getScoreboard(){ return scoreboard; }

	public ItemStuffer getStuffer() { return stuffer; }

	public int getTeleportCount() { return nTeleport; }
	public int getTeleporterDelay() { return teleporterDelay; }

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

	public void removeVotemap(UUID player)
	{
		nVotemap.remove(player);
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

	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args)
	{
		String lCmd = cmd.getName().toLowerCase();

		UUID senderId = null;

		if (sender instanceof Player)
		{
			senderId = ((Player) sender).getUniqueId();
		}

		if (lCmd.equals("spectate"))
		{
			if (args.length == 2)
			{
				Player p = Bukkit.getPlayer(getUuid(args[0]));

				if (p != null)
				{
					if (args[1].equalsIgnoreCase("on"))
					{
						if (SGOnline.getType(p.getUniqueId()) != PlayerType.SPECTATOR)
						{
							if (p.isOnline())
								SGOnline.getSGPlayer(p.getUniqueId()).remove();
							new SGSpectator(this, p);
							return true;
						} else
						{
							sender.sendMessage("Ce joueur est déjà spectateur.");
						}
					} else if (args[1].equalsIgnoreCase("off"))
					{
						if (SGOnline.getType(p.getUniqueId()) == PlayerType.SPECTATOR)
						{
							SGOnline.getSGSpectator(p.getUniqueId()).remove();
							if (p.isOnline())
								new SGPlayer(this, p.getPlayer());
							return true;
						} else
						{
							sender.sendMessage("Ce joueur n'est pas spectateur.");
						}
					} else
					{
						sender.sendMessage("Commande incorrecte.");
					}
				} else
				{
					sender.sendMessage("Joueur introuvable.");
				}
			} else
			{
				sender.sendMessage("Mauvais nombre d'arguments !");
			}
		} else if (lCmd.equals("start"))
		{
			if (getGameStateType() == GameStateType.WAITING)
			{
				setForceStart(true);
				return true;
			} else
				return false;
		} else if (lCmd.equals("votemap") && senderId != null)
		{
			if (getGameStateType() == GameStateType.WAITING)
			{
				if (nVotemap.add(senderId))
				{
					Bukkit.broadcastMessage(ChatColor.GREEN + sender.getName()
							+ ChatColor.YELLOW
							+ " a demandé un changement de map !");
					Bukkit.broadcastMessage(ChatColor.RED + "("
							+ nVotemap.size() + "/" + getVotemapsRequired()
							+ " requis)");
				} else
				{
					sender.sendMessage(ChatColor.RED
							+ "Vous avez déjà voté >:c");
				}
				return true;
			} else
			{
				sender.sendMessage(ChatColor.RED
						+ "Impossible de voter en cours de partie !");
				return true;
			}
		} else if (lCmd.equals("maintenance"))
		{
			if (args.length >= 1)
			{
				if (args[0].equals("on") && args.length >= 2)
				{
					maintenanceMode = true;
					maintenanceModeMessage = "";
					for (int i = 1; i < args.length; i++)
					{
						maintenanceModeMessage += args[i] + " ";
					}
					getLogger().info(
							"Mode maintenance activé : "
									+ maintenanceModeMessage);
					return true;
				} else if (args[0].equals("off"))
				{
					maintenanceMode = false;
					getLogger().info("Mode maintenance désactivé.");
					return true;
				}
			}
		}
		return false;
	}

	public void onDisable()
	{
		try
		{
			mySQL.closeConnection();
		} catch (SQLException e)
		{
			e.printStackTrace();
		}
		stop();
	}

	public void onEnable()
	{
		saveDefaultConfig();

		this.running = false;
		this.timer = new TimerThread(this);

		this.playersRequired = getConfig().getInt("playersRequired");
		this.waitBeforeStart = getConfig().getInt("waitBeforeStart");
		this.maxPlayers = getConfig().getInt("maxPlayers");
		this.forceStart = false;

		this.teleporterMaximumDelay = getConfig().getInt("teleport.maxDelay");
		this.teleporterMinimumDelay = getConfig().getInt("teleport.minDelay");
		this.teleporterDelaySubstraction = getConfig().getInt(
				"teleport.substract");
		this.teleporterDelay = this.teleporterMaximumDelay;

		this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
		this.scoreboard.registerNewTeam("Spectators").setPrefix(ChatColor.GREEN + "");
		this.scoreboard.registerNewTeam("Players");
		this.scoreboard.registerNewTeam("Scapegoat").setPrefix(SCAPEGOAT_COLOR + "");
		this.scoreboard.registerNewObjective("panelInfo", "dummy")
				.setDisplaySlot(DisplaySlot.SIDEBAR);
		this.scoreboard.registerNewObjective("scores", "dummy").setDisplaySlot(
				DisplaySlot.PLAYER_LIST);

		this.nVotemap = new HashSet<UUID>();
		this.nameToUuid = new HashMap<String, UUID>();
		
		this.stuffer = new ItemStuffer();

		setGameState(GameStateType.WAITING);
		this.maxFistWarnings = getConfig().getInt("security.maxFistWarnings");

		this.mySQL = new MySQL(this,
				getConfig().getString("database.host"),
				getConfig().getString("database.port"),
				getConfig().getString("database.database"),
				getConfig().getString("database.user"),
				getConfig().getString("database.password"));
		
		try
		{
			dbConnect = mySQL.openConnection();
		} catch (ClassNotFoundException e)
		{
			e.printStackTrace();
		} catch (SQLException e)
		{
			e.printStackTrace();
		}

		for (Player p : Bukkit.getOnlinePlayers())
		{
			p.getInventory().clear();
			p.setScoreboard(getScoreboard());
			createSGPlayer(p);
		}

		start();
	}

	public void setForceStart(boolean forceStart)
	{
		this.forceStart = forceStart;
	}

	public synchronized void setGameState(GameStateType gametype)
	{
		if (this.state != null)
		{
			this.state.unregister();
		}

		switch (gametype)
		{
		case WAITING:
			state = new Waiting(this);
			timer.setSecondsLeft(waitBeforeStart);
			break;
		case RUNNING:
			state = new Running(this);
			timer.setSecondsLeft(getTeleporterDelay());
			break;
		}

		this.state.register(this);
		this.state.init();
	}

	public synchronized void start()
	{
		if (!running)
		{
			running = true;
			timer.start();
		}
	}

	public synchronized void stop()
	{
		if (running)
		{
			running = false;
		}
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
						p.kickPlayer(ChatColor.YELLOW
								+ "Changement de map voté. Veuillez vous reconnecter.");
					}
					Bukkit.shutdown();
				} else
				{
					setGameState(GameStateType.RUNNING);
					getLogger().info(
							"Nouveau bouc-émissaire : "
									+ SGOnline.getScapegoat().getName());
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
		teleporterDelay = Math.min(teleporterMaximumDelay,
				teleporterMinimumDelay
						+ (teleporterDelaySubstraction * substract));
		return teleporterDelay;
	}
}
