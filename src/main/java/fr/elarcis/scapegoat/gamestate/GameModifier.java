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

/**
 * Describes special games that can be set at the beginning.
 * @author Elarcis
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
