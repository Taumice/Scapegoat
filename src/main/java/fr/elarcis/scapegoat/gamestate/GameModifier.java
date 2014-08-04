package fr.elarcis.scapegoat.gamestate;

/**
 * Describes special games that can be set at the beginning.
 * @author Lars
 *
 */
public enum GameModifier
{
	/**
	 * A normal game.
	 */
	NONE,
	/**
	 * Server time is stuck on night mode.
	 */
	NIGHT,
	/**
	 * Server time is stuck on day mode (unused)
	 */
	@Deprecated
	DAY,
	/**
	 * No natural regeneration for players.
	 * Healing is only possible through magic.
	 */
	UHC,
	/**
	 * Everyone has a speed II potion applied to them.
	 */
	POTION_SPEED,
	/**
	 * Everyone has a Jump II potion applied to them.
	 */
	POTION_JUMP,
	/**
	 * Everyone has a Fire resistance potion applied to them.
	 */
	POTION_FIRE,
	/**
	 * Everyone has an invisibility potion applied to them.
	 */
	POTION_INVISIBLE;
}
