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

/**
 * Possible types of players connected to the server and manageable via {@link SGOnline}.
 * @author Lars
 */
public enum PlayerType {
	/**
	 * An actual scapegoat player, participating to the game.
	 */
	PLAYER,
	/**
	 * Like {@link #PLAYER}, but designed as the scapegoat.
	 * If you don't know what a scapegoat is, you shouldn't be reading this code.
	 */
	SCAPEGOAT,
	/**
	 * Not a player, invisible to players and unable to interact with them.
	 */
	SPECTATOR,
	/**
	 * Explicit placeholder type, I'm not even sure I use it anywhere.
	 */
	UNKNOWN
}
