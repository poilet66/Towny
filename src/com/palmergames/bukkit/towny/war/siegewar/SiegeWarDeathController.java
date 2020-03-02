package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.locations.SiegeZone;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarPointsUtil;
import org.bukkit.entity.Player;

/**
 * This class intercepts 'player death' events coming from the towny entity monitor listener class.
 *
 * This class evaluates the death, and determines if the player is involved in any nearby sieges.
 * If so, their opponents gain siege points.
 * 
 * @author Goosius
 */
public class SiegeWarDeathController {

	/**
	 * Evaluates a siege death event.
	 * 
	 * If the dead player is officially involved in a nearby siege, their side loses siege points:
	 * 
	 * NOTE: 
	 * This mechanic allows for a wide range of siege-kill-tactics.
	 * Examples:
     * - Players from non-nation towns can contribute to siege points
	 * - Players from secretly-allied nations can contribute to siege points
	 * - Players without official military rank can contribute to siege points
	 * - Devices (cannons, traps, bombs etc.) can be used to gain siege points
	 * - Note that players from Neutral towns can use devices, but are technically prevented from non-device kills.
	 * 
	 * @param deadPlayer The player who died
	 * @param deadResident The resident who died
	 *  
	 */
	public static void evaluateSiegePlayerDeath(Player deadPlayer, Resident deadResident)  {
		
		System.out.println("Now evaluating player death");
		try {
			if (!deadResident.hasTown())
				return;

			Town deadResidentTown = deadResident.getTown();

			//Residents of occupied towns do not give siege points if killed
			if (deadResidentTown.isOccupied())
				return;

			//If resident was a defending guard, award siege points
			TownyUniverse universe = TownyUniverse.getInstance();
			if(deadResidentTown.hasSiege()
				&& deadResidentTown.getSiege().getStatus() == SiegeStatus.IN_PROGRESS
				&& universe.getPermissionSource().testPermission(deadPlayer, PermissionNodes.TOWNY_TOWN_SIEGE_POINTS.getNode())
			) {
				for (SiegeZone siegeZone : deadResidentTown.getSiege().getSiegeZones().values()) {
					if (deadPlayer.getLocation().distance(siegeZone.getFlagLocation()) < TownySettings.getWarSiegeZoneDeathRadiusBlocks()) {
						awardSiegeDeathPenaltyPoints(false, siegeZone.getAttackingNation(), deadResident, siegeZone);
						return;
					}
				}
			}
			
			//If resident was a defending soldier, award siege points
			if (deadResidentTown.hasNation()
				&& universe.getPermissionSource().testPermission(deadPlayer, PermissionNodes.TOWNY_NATION_SIEGE_POINTS.getNode())
			) {
				for (SiegeZone siegeZone : universe.getDataSource().getSiegeZones()) {
					if (siegeZone.getSiege().getStatus() == SiegeStatus.IN_PROGRESS
						&& (deadResidentTown.getNation() == siegeZone.getDefendingTown().getNation() || deadResidentTown.getNation().hasMutualAlly(siegeZone.getDefendingTown().getNation())
						&& deadPlayer.getLocation().distance(siegeZone.getFlagLocation()) < TownySettings.getWarSiegeZoneDeathRadiusBlocks())
					) {
						awardSiegeDeathPenaltyPoints(false, siegeZone.getAttackingNation(), deadResident, siegeZone);
						return;
					}
				}
			}

			//If resident was an attacking soldier, award siege points
			if (deadResidentTown.hasNation()
				&& universe.getPermissionSource().testPermission(deadPlayer, PermissionNodes.TOWNY_NATION_SIEGE_POINTS.getNode())
			) {
				for (SiegeZone siegeZone : universe.getDataSource().getSiegeZones()) {
					if (siegeZone.getSiege().getStatus() == SiegeStatus.IN_PROGRESS
						&& (deadResidentTown.getNation() == siegeZone.getAttackingNation() || deadResidentTown.getNation().hasMutualAlly(siegeZone.getAttackingNation())
						&& deadPlayer.getLocation().distance(siegeZone.getFlagLocation()) < TownySettings.getWarSiegeZoneDeathRadiusBlocks())
					) {
						awardSiegeDeathPenaltyPoints(true, siegeZone.getDefendingTown(), deadResident, siegeZone);
						return;
					}
				}
			}
			
		} catch (NotRegisteredException e) {
			System.out.println("Error evaluating siege player death");
			e.printStackTrace();
		}

	}

	private static void awardSiegeDeathPenaltyPoints(boolean attackerDeath,
													 TownyObject pointsRecipient,
													 Resident deadResident,
													 SiegeZone siegeZone) throws NotRegisteredException {
		/*
		 * If a defender was killed, reward all attackers.
		 * This prevents the exploit of a siege being joined by a 'pretend enemy' nation,
		 * and the defenders repeatedly dying to these soldiers,
		 * something which could be used if a siege is going badly, so that the town would fall to an actually friendly nation.
		 */
		if(attackerDeath) {
			SiegeWarPointsUtil.awardSiegePenaltyPoints(
				attackerDeath,
				pointsRecipient,
				deadResident,
				siegeZone,
				TownySettings.getLangString("msg_siege_war_attacker_death"));
		} else {
			for(SiegeZone siegeZoneInCollection: siegeZone.getSiege().getSiegeZones().values()) {
				SiegeWarPointsUtil.awardSiegePenaltyPoints(
					attackerDeath,
					siegeZoneInCollection.getAttackingNation(),
					deadResident,
					siegeZoneInCollection,
					TownySettings.getLangString("msg_siege_war_defender_death"));
			}
		}
	}
}
