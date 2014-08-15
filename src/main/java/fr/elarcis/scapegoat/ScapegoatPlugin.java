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
import code.husky.sqlite.SQLite;
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

/**
 * The main Scapegoat class.<br/>
 * Handles every setting and general operation related to the plugin itself and not the games.
 * @author Lars
 */
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
	protected int maxPitSize;
	protected int noDamageTicks;
	
	protected int healthRestoreOnUHC;

	protected boolean maintenanceMode;
	protected String maintenanceModeMessage;

	public static final ChatColor PLAYER_COLOR = ChatColor.DARK_RED;
	public static final ChatColor SCAPEGOAT_COLOR = ChatColor.DARK_PURPLE;

	protected Database database;
	protected Connection dbConnect;

	/**
	 * Add a teleport to the teleport counter.
	 */
	public void addTeleport() { nTeleport++; }

	/**
	 * Create and register a {@link SGOnline} based on a Bukkit player.
	 * @param p
	 */
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
	}

	/**
	 * End the game and kick everyone !
	 * @param winner
	 */
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
							+ (int) winner.getPlayer().getHealth() + " PV)"
							+ ChatColor.RESET + " a gagné !";
				}

				SGPlayer sgp = SGOnline.getSGPlayer(p.getUniqueId());
				
				if (sgp != null) sgp.remove();
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

	/**
	 * @return The plugin's database, if configured, null otherwise.
	 */
	public synchronized Database getDb() { return database; }
	
	/**
	 * @return true if the game should start right away.
	 */
	public boolean getForceStart() { return forceStart; }
	
	/**
	 * @return the current game state.
	 */
	public GameState getGameState() { return state; }
	
	/**
	 * @return How many HP should be given to player who kill someone when in UHC modifier.
	 */
	public int getHealthRestoreOnUHC() { return healthRestoreOnUHC; }
	
	/**
	 * @return the type of the current game state.
	 */
	public synchronized GameStateType getGameStateType() { return state.getType(); }
	
	/**
	 * @return the message to display when in maintenance mode.
	 */
	public String getMaintenanceMessage() { return maintenanceModeMessage; }
	
	/**
	 * @return How many hits until a kick for fist-rush ?
	 */
	public int getMaxFistWarning() { return maxFistWarnings; }
	
	/**
	 * @return How many blocks to check for pit or lava traps ?
	 */
	public int getMaxPitSize() { return maxPitSize; }
	
	/**
	 * @return How many players are allowed to connect.
	 * That value can be overriden if spectators change team.
	 */
	public int getMaxPlayers() { return maxPlayers; }
	
	/**
	 * @return Invincibility time after a teleportation.
	 */
	public int getNoDamageTicks() { return noDamageTicks; }
	
	/**
	 * @return The minimum amount of players to start a game.
	 * Can be overriden with {@link ScapegoatPlugin#forceStartCommand()}.
	 */
	public int getPlayersRequired() { return playersRequired; }
	
	/**
	 * @return the scoreboard used by the plugin.
	 * Is not accessible from ingame commands.
	 */
	public Scoreboard getScoreboard() { return scoreboard; }
	
	/**
	 * @return the stuffer that possesses stuff data to give to players.
	 */
	public ItemStuffer getStuffer() { return stuffer; }
	
	/**
	 * @return the number of teleports since the beginning of the game.
	 */
	public int getTeleportCount() { return nTeleport; }
	
	/**
	 * @return the next teleporter delay that will be set on countdown end.
	 */
	public int getTeleporterDelay() { return teleporterDelay; }
	
	/**
	 * @return how many seconds to wait until a game starts from the moment enough players are connected.
	 */
	public int getWaitBeforeStart() { return waitBeforeStart; }

	/**
	 * Get a player's UUID from their name. The value is fetched from a local database resetted at each reload,
	 * so it should NOT be used to fetch offline player's UUID.
	 * @param player
	 * @return
	 */
	public UUID getUuid(String player) { return this.nameToUuid.get(player); }

	/**
	 * @return how many valid votemaps have been emitted.
	 */
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

	/**
	 * @return how many votemaps are required to change the map at game start.
	 */
	public int getVotemapsRequired()
	{
		return (Math.max(SGOnline.getPlayerCount(), getPlayersRequired()) / 2) + 1;
	}

	/**
	 * Log a message to the console. Not visible to players.
	 * @param message
	 */
	public void info(String message) { getLogger().info(message); }
	
	/**
	 * Init the database connection according to plugin settings.
	 */
	protected void initDatabase()
	{
		String engine = getConfig().getString("database.engine");
		
		if (engine.equals("none"))
			database = null;
		else 
		{
			if (engine.equals("mysql"))
	
			database = new MySQL(this,
					getConfig().getString("database.host"),
					getConfig().getString("database.port"),
					getConfig().getString("database.database"),
					getConfig().getString("database.user"),
					getConfig().getString("database.password")
					);
			else if (engine.equals("sqlite"))
				database = new SQLite(this, "data.db");
			
			try
			{
				database.openConnection();
			}
			catch (ClassNotFoundException e)
			{
				e.printStackTrace();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
	}

	/**
	 * @return Is the plugin in maintenance mode ?
	 */
	public boolean isInMaintenanceMode() { return maintenanceMode; }
	
	/**
	 * Should {@link TimerThread} continue running ?
	 * @return
	 */
	public synchronized boolean isRunning() { return running; }

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		String lCmd = cmd.getName().toLowerCase();
		UUID senderId = null;
		String result = "Commande inconnue";

		if (sender instanceof Player)
			senderId = ((Player) sender).getUniqueId();

		//TODO: Score command
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

		if (result.equals(""))
			return true;
		else
		{
			sender.sendMessage(ChatColor.DARK_RED + result);
			return false;
		}
	}
	
	/**
	 * Switch a player between {@link PlayerType#PLAYER} and {@link PlayerType#SPECTATOR}.
	 * @param player
	 * @param mode
	 * @return an error message if the command fails, "" otherwise.
	 */
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

	/**
	 * Vote to change the map at game start.
	 * @param voter
	 * @return an error message if the command fails, "" otherwise.
	 */
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
	
	/**
	 * Force the game start. 
	 * @return an error message if the command fails, "" otherwise.
	 */
	public String forceStartCommand()
	{
		if (getGameStateType() != GameStateType.WAITING)
			return "Partie déjà démarrée !";
		
		if (SGOnline.getPlayerCount() < 2)
			return "Pas assez de joueurs !";

		this.forceStart = true;
		return "";
	}
	
	/**
	 * Set maintenance mode.
	 * @param mode
	 * @param msg
	 * @return an error message if the command fails, "" otherwise.
	 */
	public String setMaintenanceModeCommand(String mode, String msg)
	{
		if (mode.equals("on"))
		{
			maintenanceMode = true;
			info("Mode maintenance activé" + ((msg.equals("")) ? "." : " : " + msg));
		}
		else if (mode.equals("off"))
		{
			maintenanceMode = false;
			info("Mode maintenance désactivé.");
		}
		else 
			return "Syntaxe incorrecte.";
		
		return "";
	}

	@Override
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

	@Override
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
		this.scoreboard.registerNewTeam("Players").setCanSeeFriendlyInvisibles(false);
		this.scoreboard.registerNewTeam("Scapegoat").setPrefix(SCAPEGOAT_COLOR + "");
		this.scoreboard.registerNewObjective("panelInfo", "dummy").setDisplaySlot(DisplaySlot.SIDEBAR);
		this.scoreboard.registerNewObjective("scores", "dummy").setDisplaySlot(DisplaySlot.PLAYER_LIST);

		this.nVotemap = new HashSet<UUID>();
		this.nameToUuid = new HashMap<String, UUID>();

		this.stuffer = new ItemStuffer();
		
		this.healthRestoreOnUHC = getConfig().getInt("gameplay.healthRestoreOnUHC");
		this.maxFistWarnings = getConfig().getInt("security.maxFistWarnings");
		this.maxPitSize = getConfig().getInt("security.maxPitSize");
		this.noDamageTicks = getConfig().getInt("security.noDamageTicks");

		initDatabase();
		
		setGameState(GameStateType.WAITING);

		for (Player p : Bukkit.getOnlinePlayers())
		{
			p.getInventory().clear();
			p.setScoreboard(getScoreboard());
			createSGPlayer(p);
		}
		start();
	}

	/**
	 * Save a player's UUID to the local temporary database.
	 * @param player
	 */
	public void putPlayer(Player player)
	{
		this.nameToUuid.put(player.getName(), player.getUniqueId());
	}

	/**
	 * Delete an invalid votemap (on player disconnection, i.e.)
	 * @param player
	 */
	public void removeVotemap(UUID player) { nVotemap.remove(player); }
	
	/**
	 * Change the current game state.
	 * @param gametype
	 */
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

	/**
	 * Start {@link TimerThread}.
	 */
	public synchronized void start()
	{
		if (running)
			return;
		
		running = true;
		timer.start();
	}

	/**
	 * Set {@link TimerThread} to stop at next iteration.
	 */
	public synchronized void stop()
	{
		if (!running)
			return;
		
		running = false;
	}

	/**
	 * Executed around each second via {@link TimerThread#run}.
	 * @param secondsLeft How many seconds to display in the countdown panel.
	 */
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

	/**
	 * Recompute teleporter delay, mainly because of a change in the number of players.
	 * @return the new delay, in seconds.
	 */
	public int updateTeleporterDelay()
	{
		int substract = Math.max(SGOnline.getPlayerCount() - 3, 0);
		teleporterDelay = Math.min(teleporterMaximumDelay, teleporterMinimumDelay
				+ (teleporterDelaySubstraction * substract));
		return teleporterDelay;
	}
}
