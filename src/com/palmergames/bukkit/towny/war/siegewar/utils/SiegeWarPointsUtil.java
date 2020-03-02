package com.palmergames.bukkit.towny.war.siegewar.utils;

import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.locations.Siege;
import com.palmergames.bukkit.towny.war.siegewar.locations.SiegeZone;
import com.palmergames.bukkit.util.BukkitTools;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class contains utility functions related to siege points
 * 
 * @author Goosius
 */
public class SiegeWarPointsUtil {

	/**
	 * This method calculates who has won a siege
	 * 
	 * Defending Town - The defending town has won the siege if all of the siege zones are in negative points.
	 * Attacking Nation - an attacking nation has won the siege if its siege points are positive,
	 *                    and higher than the siegepoints of any other attacker.
	 *
	 * @param siege the siege
	 * @return the winner of the siege
	 */
	public static TownyObject calculateSiegeWinner(Siege siege) {
        TownyObject winner = siege.getDefendingTown();
        int winningPoints = 0;

        for(Map.Entry<Nation, SiegeZone> entry: siege.getSiegeZones().entrySet()) {
            if(entry.getValue().getSiegePoints() > winningPoints) {
                winner = entry.getKey();
                winningPoints = entry.getValue().getSiegePoints();
            }
        }

        return winner;
    }

	/**
	 * This method determines if a players is in the 'point scoring zone' of a siegezone
	 * 
	 * - Must be in same world as flag
	 * - Must be in wilderness  (This is important, otherwise the defender could create a 'safe space' 
	 *                           inside a perm-protected town block, and gain points there with no threat.)
	 * - Must be within 1 townblock length of the flag
	 *
	 * @param player the player
	 * @param siegeZone the siege zone
	 * @return true if a player in in the siege point zone
	 */
	public static boolean isPlayerInSiegePointZone(Player player, SiegeZone siegeZone) {

		return player.getLocation().getWorld() == siegeZone.getFlagLocation().getWorld()
				&& !TownyAPI.getInstance().hasTownBlock(player.getLocation())
				&& player.getLocation().distance(siegeZone.getFlagLocation()) < TownySettings.getTownBlockSize();
	}

	/**
	 * This method determines if a siege point penalty should be applied
	 *
	 * These are examples where a penalty is awarded
	 * 
	 * e.g.
	 * - if a military ranked resident is killed within the death zone of a siege
	 * - if a military ranked resident leaves a town/nation which is involved in a siege 
	 * - if a military ranked resident's military rank is removed while a town/nation is involved in a siege
	 * - if an alliance is cancelled while either nation has military ranked players and is involved in a siege 
	 * 
	 * @param resident the player leaving the zone
	 * @param unformattedErrorMessage the error message to be shown if points are deducted
	 */
	public static void evaluateSiegePenaltyPoints(Resident resident,
												 String unformattedErrorMessage) {
		try {
			Player player = TownyAPI.getInstance().getPlayer(resident);

			if(player == null)
				return;  //Player not online, or npc

			if(!resident.hasTown())
				return;

			Town town;
			try {
				town = resident.getTown();
			} catch (NotRegisteredException e) {
				return;
			}

			if(town.isOccupied() )
				return;  ///Residents of occupied towns cannot affect siege points.

			//Is the resident a guard of a town under siege, and in the death zone ?
			TownyUniverse universe = TownyUniverse.getInstance();
			if(town.hasSiege() 
				&& town.getSiege().getStatus() == SiegeStatus.IN_PROGRESS 
				&& universe.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_TOWN_SIEGE_POINTS.getNode())) {

				for(SiegeZone siegeZone: town.getSiege().getSiegeZones().values()) {
					if (player.getLocation().distance(siegeZone.getFlagLocation()) < TownySettings.getWarSiegeZoneDeathRadiusBlocks()) {
						awardSiegePenaltyPoints(false, siegeZone.getAttackingNation(), resident, siegeZone, unformattedErrorMessage);
					}
				}
			} 

			if (town.hasNation()
				&& universe.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_NATION_SIEGE_POINTS.getNode())) {

				//Is the resident a soldier attacking a town, and in the death zone?
				Nation nation = town.getNation();
				for (SiegeZone siegeZone : universe.getDataSource().getSiegeZones()) {
					if (siegeZone.getSiege().getStatus() == SiegeStatus.IN_PROGRESS
						&& (nation == siegeZone.getAttackingNation() || nation.hasMutualAlly(siegeZone.getAttackingNation()))
						&& player.getLocation().distance(siegeZone.getFlagLocation()) < TownySettings.getWarSiegeZoneDeathRadiusBlocks()) {

						awardSiegePenaltyPoints(true, siegeZone.getDefendingTown(), resident, siegeZone, unformattedErrorMessage);
					}
				}

				//Is the resident a soldier defending a town, and in the death zone?
				for (SiegeZone siegeZone : universe.getDataSource().getSiegeZones()) {
					if (siegeZone.getSiege().getStatus() == SiegeStatus.IN_PROGRESS
						&& siegeZone.getDefendingTown().hasNation()
						&& (nation == siegeZone.getDefendingTown().getNation() || nation.hasMutualAlly(siegeZone.getDefendingTown().getNation()))
						&& player.getLocation().distance(siegeZone.getFlagLocation()) < TownySettings.getWarSiegeZoneDeathRadiusBlocks()) {

						awardSiegePenaltyPoints(false, siegeZone.getAttackingNation(), resident, siegeZone, unformattedErrorMessage);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error evaluating siege point penalty");
		}
	}

	public static void awardSiegePenaltyPoints(boolean attackerDeath,
											   TownyObject pointsRecipient,
											   Resident deadResident,
											   SiegeZone siegeZone,
											   String unformattedErrorMessage) throws NotRegisteredException {

		//Give siege points to opposing side
		int siegePoints;
		if (attackerDeath) {
			siegePoints = TownySettings.getWarSiegePointsForAttackerDeath();
			siegeZone.adjustSiegePoints(-siegePoints);
		} else {
			siegePoints = TownySettings.getWarSiegePointsForDefenderDeath();
			siegeZone.adjustSiegePoints(siegePoints);
		}

		TownyUniverse.getInstance().getDataSource().saveSiegeZone(siegeZone);

		//Send messages to siege participants
		String message = String.format(
			unformattedErrorMessage,
			siegeZone.getDefendingTown().getFormattedName(),
			siegePoints);

		//Inform attacker nation
		TownyMessaging.sendPrefixedNationMessage(siegeZone.getAttackingNation(), message);
		Set<Nation> alliesToInform = new HashSet<>();
		alliesToInform.addAll(siegeZone.getAttackingNation().getMutualAllies());

		//Inform defending town, and nation if there is one
		if (siegeZone.getDefendingTown().hasNation()) {
			TownyMessaging.sendPrefixedNationMessage(siegeZone.getDefendingTown().getNation(), message);
			alliesToInform.addAll(siegeZone.getDefendingTown().getNation().getMutualAllies());
		} else {
			TownyMessaging.sendPrefixedTownMessage(siegeZone.getDefendingTown(), message);
		}

		//Inform allies
		for(Nation alliedNation: alliesToInform) {
			TownyMessaging.sendPrefixedNationMessage(alliedNation, message);
		}
	}
	
	private Player getPlayerWhetherOnlineOrOffline(Resident resident) {
		for (Player player : BukkitTools.getServer().getOfflinePlayers()) {
			if (player != null) {
				if (player.getName().equals(resident.getName())) {
					return player;
				}
			}
		}
	}
}
