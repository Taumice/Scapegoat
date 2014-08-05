package fr.elarcis.scapegoat.gamestate;

/**
 * Describes the function of a game state. Used for general gameplay segmentation.
 * @author Lars
 */
public enum GameStateType {
	/**
	 * The phase during which the game is in preparation and accepting incoming players.
	 */
	WAITING,
	/**
	 * The actual Scapegoat game.
	 */
	RUNNING
}
