package com.palmergames.bukkit.towny.war.siegewar.timeractions;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.util.BukkitTools;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * This class is responsible for removing pvp immunity
 *
 * @author Goosius
 */
public class RemovePvpImmunity {

	/**
	 * This method cycles through all online players
	 * It determines which players are currently pvp immune, but have reached the time limit - then removes the immunity
	 */
    public static void removePvpImmunity() {
		TownyUniverse universe = TownyUniverse.getInstance();
		List<Player> onlinePlayers = new ArrayList<>(BukkitTools.getOnlinePlayers());
		ListIterator<Player> playerItr = onlinePlayers.listIterator();
		Player player;
		Resident resident;

		while (playerItr.hasNext()) {
			player = playerItr.next();
			/*
			 * We are running in an Async thread so MUST verify all objects.
			 */
			try {
				if(player.isOnline()) {
					resident = universe.getDataSource().getResident(player.getName());
					if(resident.isPvpImmune() && System.currentTimeMillis() > resident.getPvpImmunityEndTime()) {
						resident.setPvpImmune(false);
					}
				}
			} catch (Exception e) {
				TownyMessaging.sendErrorMsg("Problem removing immunity from player " + player.getName());
				e.printStackTrace();
			}
		}
    }

}
