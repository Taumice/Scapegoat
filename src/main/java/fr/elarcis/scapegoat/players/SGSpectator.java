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

package fr.elarcis.scapegoat.players;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Team;

import fr.elarcis.scapegoat.ItemSet;

public class SGSpectator extends SGOnline
{

	public SGSpectator(Player player)
	{
		super(player);

		SGOnline.sgSpectators.put(id, this);
		plugin.getScoreboard().getTeam("Spectators").addPlayer(player);
		join();
	}

	public PlayerType getType()
	{
		return PlayerType.SPECTATOR;
	}

	public void join()
	{
		super.join();
		respawn();

		for (Entry<UUID, SGSpectator> e : sgSpectators.entrySet())
		{
			if (e.getValue().isOnline())
				getPlayer().showPlayer(e.getValue().getPlayer());
		}

		for (Entry<UUID, SGPlayer> e : sgPlayers.entrySet())
		{
			e.getValue().getPlayer().hidePlayer(getPlayer());
		}
	}

	public void openInventory()
	{
		int cells = Math.min(
				((Bukkit.getOnlinePlayers().length - 2) / 9) * 9 + 9, 54);

		Inventory panel = Bukkit.createInventory(null, cells,
				"§9Menu spectateur");

		List<ItemStack> heads = new ArrayList<ItemStack>();

		for (Player p : Bukkit.getOnlinePlayers())
		{
			if (equals(p))
				continue;

			byte damage = 0;
			String typeLore = null;
			PlayerType type = SGOnline.getType(p.getUniqueId());

			switch(type)
			{
			case SCAPEGOAT:
				damage = 1;
				typeLore = "§7Bouc-émissaire";
				break;
			case PLAYER:
				damage = 3;
				typeLore = "§7Joueur";
				break;
			case SPECTATOR:
				damage = 0;
				typeLore = "§7Spectateur";
				break;
			default:
				continue;
			}

			ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, damage);

			SkullMeta hMeta = (SkullMeta) head.getItemMeta();
			hMeta.setOwner(p.getName());
		
			Team t = plugin.getScoreboard().getPlayerTeam(p);
			hMeta.setDisplayName(((t != null) ? t.getSuffix() : "") + p.getName());

			List<String> lore = new ArrayList<String>();
			lore.add(typeLore);
			lore.add("§dClic-droit pour se téléporter !");
			hMeta.setLore(lore);

			head.setItemMeta(hMeta);

			if (type == PlayerType.SPECTATOR)
			{
				heads.add(0, head);
			} else
			{
				heads.add(head);
			}
		}

		for (ItemStack h : heads)
		{
			panel.addItem(h);
		}

		getPlayer().openInventory(panel);
	}

	public void remove()
	{		
		if (sgSpectators.remove(id) == null || !isOnline())
			return;
		
		Player p = getPlayer();
		
		plugin.getScoreboard().getTeam("Spectators").removePlayer(p);
		p.removePotionEffect(PotionEffectType.INVISIBILITY);

		// Player is no longer spectating, so we restore their visibility.
		for (Entry<UUID, SGSpectator> e : sgSpectators.entrySet())
			p.hidePlayer(e.getValue().getPlayer());

		// We also hide spectators to that pesky traitor.
		for (Entry<UUID, SGPlayer> e : sgPlayers.entrySet())
			e.getValue().getPlayer().showPlayer(p);
		
		super.remove();
	}

	public void respawn()
	{
		Player p = getPlayer();
		
		p.getInventory().clear();
		plugin.getStuffer().stuff(p, ItemSet.MANUAL);
		plugin.getStuffer().stuff(p, ItemSet.SPECTATOR);
		giveTrophees();

		p.setGameMode(GameMode.CREATIVE);
		p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0));
	}

	public void teleport(String player)
	{
		Player p = Bukkit.getPlayer(plugin.getUuid(player));

		if (p != null)
			getPlayer().teleport(p);
	}
}
