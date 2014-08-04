package fr.elarcis.scapegoat;

import java.util.Random;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import fr.elarcis.scapegoat.players.SGOnline;

public class ItemStuffer {	
	protected ItemStack manual;
	protected ItemStack manual_bonus;
	protected ItemStack spectating_compass;
	
	public ItemStuffer() {
		String[] mpages = new String[]{
			"\n\n\n\n\n\n�c = BOUC-EMISSAIRE =\n   -R�gles du jeu-\n           -o-",
			"�cBut du jeu:�0\nEtre le dernier joueur vivant.\n\n" +
			"�cBouc-�missaire:�0\nChaque meurtrier devient bouc-�missaire.\n\n" +
			"�cTP:�0\nUn joueur tir� au sort est TP au bouc-�missaire.",
			"�c/!\\ TRICHE�0\n\nToute prise en flagrant d�lit de triche quelconque :" +
			"\n\n- X-Ray\n- FlyHack\n- etc.\n\nest punie d'un ban �4permanent�0 et �4irr�versible�0.",
			"�cCOMMANDES:�0\n\n�c/votemap�0\nDemander un changement de map au lieu de commencer la partie.",
			"\n\n\n\n\n\n   �5 Bonne chance !",
		};
			
		String[] mpages_bonus = new String[]{
			"\n\n\n\n\n\n       Ne pas lire\n        plus loin\n             ...",
			"\n\n\n\n\n\n       The game.\n\n (On t'avait pr�venu)",
		};

		manual = new ItemStack(Material.WRITTEN_BOOK);
		BookMeta bmeta = (BookMeta)manual.getItemMeta();
		bmeta.setAuthor("�6E�flarcis");
		bmeta.setTitle("�aR�gles du jeu");
		bmeta.addPage(mpages);
		manual.setItemMeta(bmeta);
		
		bmeta.addPage(mpages_bonus);
		manual_bonus = new ItemStack(manual);
		manual_bonus.setItemMeta(bmeta);
		
		spectating_compass = new ItemStack(Material.COMPASS);
		ItemMeta cMeta = spectating_compass.getItemMeta();
		cMeta.setDisplayName("�9Menu spectateur");
		spectating_compass.setItemMeta(cMeta);
	}
	
	public void stuff(SGOnline player, ItemSet item) {
		if (player.getOfflinePlayer().isOnline()) {
			stuff(player.getPlayer(), item);
		}
	}
	
	public void stuff(Player player, ItemSet item) {
		PlayerInventory inv = player.getInventory();
		
		switch(item) {
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
