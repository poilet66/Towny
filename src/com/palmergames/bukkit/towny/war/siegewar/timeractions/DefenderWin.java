package com.palmergames.bukkit.towny.war.siegewar.timeractions;

import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.war.siegewar.locations.SiegeZone;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarSiegeCompletionUtil;
import com.palmergames.bukkit.towny.war.siegewar.locations.Siege;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.util.ChatTools;

/**
 * This class is responsible for processing siege defender wins
 *
 * @author Goosius
 */
public class DefenderWin
{
	/**
	 * This method triggers siege values to be updated for a defender win
	 *
	 * @param siege the siege
	 * @param winnerTown the winning town
	 */
    public static void defenderWin(Siege siege, Town winnerTown) {
        SiegeWarSiegeCompletionUtil.updateSiegeValuesToComplete(siege, SiegeStatus.DEFENDER_WIN, null);

		TownyMessaging.sendGlobalMessage(String.format(
			TownySettings.getLangString("msg_siege_war_defender_win"),
			TownyFormatter.getFormattedTownName(winnerTown)));

		//The town receives all the warchests
		if(TownySettings.isUsingEconomy()) {
			try {
				for (SiegeZone siegeZone : siege.getSiegeZones().values()) {
					winnerTown.collect(siegeZone.getWarChestAmount(), "War Chest Captured");
					String message =
						String.format(
							TownySettings.getLangString("msg_siege_war_attack_recover_war_chest"),
							TownyFormatter.getFormattedTownName(winnerTown),
							TownyEconomyHandler.getFormattedBalance(siegeZone.getWarChestAmount()));

					//Send message to nation
					TownyMessaging.sendPrefixedNationMessage(siegeZone.getAttackingNation(), message);
					//Send message to town
					TownyMessaging.sendPrefixedTownMessage(siege.getDefendingTown(), message);
				}
			} catch (EconomyException e) {
				System.out.println("Problem paying war chest(s) to winner town");
				e.printStackTrace();
			}
		}
    }

}
