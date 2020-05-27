package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.db.TownyDataSource;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.util.BukkitTools;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class HealingPotionLimiterUtil {
	/**
	 * Remove healing potion limits from all online players
	 */
	public static void clearAllNumRecentHealingPotions() {
		Resident resident;
		TownyDataSource dataSource = TownyUniverse.getInstance().getDataSource();

		for (Player player : new ArrayList<>(BukkitTools.getOnlinePlayers())) {
			/*
			 * We are running in an Async thread so MUST verify all objects.
			 */
			try {
				if (player.isOnline()) {
					try {
						resident = dataSource.getResident(player.getName());
					} catch (NotRegisteredException e) {
						continue; //Next player pls
					}
					resident.clearNumRecentHealingPotions();
				}
			} catch (Exception e) {
				try {
					TownyMessaging.sendErrorMsg("Problem removing potion limit player " + player.getName());
				} catch (Exception e2) {
					TownyMessaging.sendErrorMsg("Problem removing potion limit (could not read player name");
				}
				e.printStackTrace();
			}
		}
	}
}
