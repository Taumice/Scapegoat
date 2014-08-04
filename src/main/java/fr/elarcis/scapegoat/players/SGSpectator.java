package fr.elarcis.scapegoat.players;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import fr.elarcis.scapegoat.ItemSet;
import fr.elarcis.scapegoat.ScapegoatPlugin;

public class SGSpectator extends SGOnline
{

	public SGSpectator(ScapegoatPlugin plugin, OfflinePlayer player)
	{
		super(plugin, player);

		join();
		SGOnline.sgSpectators.put(id, this);
		plugin.getScoreboard().getTeam("Spectators").addPlayer(player);
	}

	public PlayerType getType()
	{
		return PlayerType.SPECTATOR;
	}

	public void join()
	{
		welcome();
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
			}

			ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, damage);

			SkullMeta hMeta = (SkullMeta) head.getItemMeta();
			hMeta.setOwner(p.getName());
			hMeta.setDisplayName(plugin.getScoreboard().getPlayerTeam(p).getSuffix() + p.getName());

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
		if (sgSpectators.remove(id) != null && isOnline())
		{
			plugin.getScoreboard().getTeam("Spectators")
					.removePlayer(getPlayer());
			getPlayer().removePotionEffect(PotionEffectType.INVISIBILITY);

			// Le joueur n'est plus spectateur, on le rend donc visible à tout
			// le monde
			// tout en lui masquant les spectateurs. Il n'y a pas besoin de la
			// logique inverse
			// du côté des joueurs vu que seuls les spectateurs sont invisibles.
			for (Entry<UUID, SGSpectator> e : sgSpectators.entrySet())
			{
				getPlayer().hidePlayer(e.getValue().getPlayer());
			}

			for (Entry<UUID, SGPlayer> e : sgPlayers.entrySet())
			{
				e.getValue().getPlayer().showPlayer(getPlayer());
			}
		}
	}

	public void respawn()
	{
		getPlayer().getInventory().clear();
		plugin.getStuffer().stuff(getPlayer(), ItemSet.MANUAL);
		plugin.getStuffer().stuff(getPlayer(), ItemSet.SPECTATOR);
		giveTrophees();

		getPlayer().setGameMode(GameMode.CREATIVE);
		getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,
				Integer.MAX_VALUE, 0));
	}

	public void teleport(String player)
	{
		Player p = Bukkit.getPlayer(plugin.getUuid(player));

		if (p != null)
		{
			getPlayer().teleport((Player) p);
		}
	}
}
