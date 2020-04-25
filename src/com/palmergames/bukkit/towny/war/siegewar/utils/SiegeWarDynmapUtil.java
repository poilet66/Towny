package com.palmergames.bukkit.towny.war.siegewar.utils;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.war.siegewar.objects.HeldItemsCombination;
import com.palmergames.bukkit.util.BukkitTools;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

/**
 * This class contains utility functions related to the dynmap
 *
 * @author Goosius
 */
public class SiegeWarDynmapUtil {

	/**
	 * Evaluate players to see if they are 'tactically' invisible
	 * 
	 * Tactical invisibility makes a player invisible on the dynmap
	 * It is triggered if the player sets their main/off hand combinations 
	 * to one of the specified combinations (set in config file).
	 *
	 * Players in banner control sessions cannot go tactically invisible
	 */
	public static void evaluatePlayerTacticalInvisibility() {
		TownyUniverse universe = TownyUniverse.getInstance();
		Towny plugin = Towny.getPlugin();
		boolean invisibleOnDynmap = false;

		for(Player player: BukkitTools.getOnlinePlayers()) {
			try {
				//Check if player is not in banner control session
				if (!universe.getPlayersInBannerControlSessions().contains(player)) {

					//Check item combinations
					for(HeldItemsCombination heldItemsCombination: TownySettings.getWarSiegeTacticalVisibilityItems()) {

						//Off Hand
						if(!heldItemsCombination.isIgnoreOffHand() && player.getInventory().getItemInOffHand().getType() != heldItemsCombination.getOffHandItemType())
							continue;  //off hand does not match. Try next combo

						//Main hand
						if(!heldItemsCombination.isIgnoreMainHand() && player.getInventory().getItemInMainHand().getType() != heldItemsCombination.getMainHandItemType())
							continue; //main hand does not match. Try next combo

						//Player invisible on map
						invisibleOnDynmap = true;
						break;
					}
				}

				if(invisibleOnDynmap) {
					if(!player.hasMetadata("tacticallyInvisible")) {
						player.setMetadata("tacticallyInvisible", new FixedMetadataValue(plugin, true));
					}
				} else {
					if (player.hasMetadata("tacticallyInvisible")) {
						player.removeMetadata("tacticallyInvisible", plugin);
					}
				}

			} catch (Exception e) {
				try {
					System.out.println("Problem evaluating tactical visibility for player " + player.getName());
				} catch (Exception e2) {
					System.out.println("Problem evaluating tactical visibility (could not read player name)");
				}
				e.printStackTrace();
			}
		}
	}
}
