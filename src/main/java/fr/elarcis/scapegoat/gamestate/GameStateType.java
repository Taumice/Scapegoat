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

package fr.elarcis.scapegoat.gamestate;

/**
 * Describes the function of a game state. Used for general gameplay segmentation.
 * @author Elarcis
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
