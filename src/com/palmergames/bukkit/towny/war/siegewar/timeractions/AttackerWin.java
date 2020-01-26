package com.palmergames.bukkit.towny.war.siegewar.timeractions;

import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.war.siegewar.locations.SiegeZone;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarSiegeCompletionUtil;
import com.palmergames.bukkit.towny.war.siegewar.locations.Siege;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.util.ChatTools;

/**
 * This class is responsible for processing siege attacker wins
 *
 * @author Goosius
 */
public class AttackerWin {

	/**
	 * This method triggers siege values to be updated for an attacker win
	 * 
	 * @param siege the siege
	 * @param winnerNation the winning nation
	 */
	public static void attackerWin(Siege siege, Nation winnerNation) {
        SiegeWarSiegeCompletionUtil.updateSiegeValuesToComplete(siege, SiegeStatus.ATTACKER_WIN, winnerNation);

		TownyMessaging.sendGlobalMessage(String.format(
			TownySettings.getLangString("msg_siege_war_attacker_win"),
			TownyFormatter.getFormattedNationName(winnerNation),
			TownyFormatter.getFormattedTownName(siege.getDefendingTown())
		));

		//The winning nation receives all the warchests
		if(TownySettings.isUsingEconomy()) {
			try {
				for (SiegeZone siegeZone : siege.getSiegeZones().values()) {
					winnerNation.collect(siegeZone.getWarChestAmount(), "War Chest Captured/Returned");
					String message =
						String.format(
							TownySettings.getLangString("msg_siege_war_attack_recover_war_chest"),
							TownyFormatter.getFormattedNationName(winnerNation),
							TownyEconomyHandler.getFormattedBalance(siegeZone.getWarChestAmount()));

					//Send message to nation(s)
					TownyMessaging.sendPrefixedNationMessage(winnerNation, message);
					if(winnerNation != siegeZone.getAttackingNation())
						TownyMessaging.sendPrefixedNationMessage(siegeZone.getAttackingNation(), message);
					//Send message to town
					TownyMessaging.sendPrefixedTownMessage(siege.getDefendingTown(), message);
				}
			} catch (EconomyException e) {
				System.out.println("Problem paying war chest(s) to winner nation");
				e.printStackTrace();
			}
		}
    }
}
