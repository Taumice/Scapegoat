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

import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Generates common stuff to be given to players via {@link ItemSet}.
 * @author Lars
 */
public class ItemStuffer
{
	public static final String MANUAL_TITLE = ChatColor.GREEN + "Règles du jeu";
	
	protected ItemStack manual;
	protected ItemStack manual_bonus;
	protected ItemStack spectating_compass;

	/**
	 * Generate common stuff data.
	 */
	public ItemStuffer()
	{
		String[] mpages = new String[] {
				  "\n\n\n\n\n\n§c = BOUC-EMISSAIRE =\n   -Règles du jeu-\n           -o-",
				  "§cBut du jeu:§0\nEtre le dernier joueur vivant.\n\n"
				+ "§cBouc-émissaire:§0\nChaque meurtrier devient bouc-émissaire.\n\n"
				+ "§cTP:§0\nUn joueur tiré au sort est TP au bouc-émissaire.",
				  "§c/!\\ TRICHE§0\n\nToute prise en flagrant délit de triche quelconque :"
				+ "\n\n- X-Ray\n- FlyHack\n- etc.\n\nest punie d'un ban §4permanent§0 et §4irréversible§0.",
				  "§cCOMMANDES:§0\n\n§c/votemap§0\nDemander un changement de map au lieu de commencer la partie.",
				  "\n\n\n\n\n\n   §5 Bonne chance !",
				  };

		String[] mpages_bonus = new String[] {
				"\n\n\n\n\n\n       Ne pas lire\n        plus loin\n             ...",
				"\n\n\n\n\n\n       The game.\n\n (On t'avait prévenu)",
				};

		manual = new ItemStack(Material.WRITTEN_BOOK);
		
		BookMeta bmeta = (BookMeta) manual.getItemMeta();
		bmeta.setAuthor("§6E§flarcis");
		bmeta.setTitle(MANUAL_TITLE);
		bmeta.addPage(mpages);
		
		manual.setItemMeta(bmeta);

		bmeta.addPage(mpages_bonus);
		
		manual_bonus = new ItemStack(manual);
		manual_bonus.setItemMeta(bmeta);

		spectating_compass = new ItemStack(Material.COMPASS);
		
		ItemMeta cMeta = spectating_compass.getItemMeta();
		cMeta.setDisplayName(ChatColor.BLUE + "Menu spectateur");
		
		spectating_compass.setItemMeta(cMeta);
	}

	/**
	 * Give an item set to a player.
	 * @param player
	 * @param item
	 */
	public void stuff(Player player, ItemSet item)
	{
		PlayerInventory inv = player.getInventory();

		switch (item)
		{
		case MANUAL:
			if (new Random().nextInt(20) > 0)
				inv.addItem(manual);
			else
				inv.addItem(manual_bonus);
			break;
		case SPECTATOR:
			inv.addItem(spectating_compass);
			break;
		}
	}
}
