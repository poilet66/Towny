package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
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
	 * Evaluates a PVP death event.
	 * 
	 * If both players are directly involved in a nearby siege, the killer's side gains siege points:
	 * 
	 * NOTE: 
	 * Allied nations or friendly towns can still be involved in sieges,
	 * (e.g. via resource support, scouting, spying, diversions, or attacking enemy combatants),
	 * but they cannot directly affect the siege points totals. 
	 *
	 * @param deadPlayer The player who died
	 * @param killerPlayer The player who did the killing
	 * @param deadResident The resident who died
	 * @param killerResident The resident who did the killing
	 *  
	 */
	public static void evaluateSiegePvPDeath(Player deadPlayer, Player killerPlayer, Resident deadResident, Resident killerResident)  {
		
		/*
		
		TESTING
Siege attack - Ally of attacker gains timed points PASS
Siege defence - Ally of defender gains timed points PASS

Defending guard killed by attacking soldier
defending soldier killed by attacking soldier
attacking soldier killed by defending guard
attacking soldier killed by defending soldier

defending guard killed by attacking ally soldier
defending soldier killed by attacking ally soldier
attacking soldier killed by defending ally soldier

??????
defending ally soldier killed by attacking soldier
attacking ally soldier killed by defending soldier
attacking ally soldier killed by defending guard

Exploration testing....

		
		 */
		System.out.println("Now evaluating pvp death");
		try {
			if (!deadResident.hasTown())
				return;

			if (!killerResident.hasTown())
				return;

			Town deadResidentTown = deadResident.getTown();
			Town killerResidentTown = killerResident.getTown();

			//Residents of occupied towns cannot affect siege points.
			if (deadResidentTown.isOccupied() || killerResidentTown.isOccupied())
				return;

			SiegeZone siegeZone;
			
			if ((siegeZone = getSiegeZoneForDefendingGuardKilledByAttackingSoldier(deadPlayer, killerPlayer, deadResidentTown, killerResidentTown)) != null) {
				awardSiegePvpPenaltyPoints(false, siegeZone.getAttackingNation(), deadResident, siegeZone);

			} else if ((siegeZone = getSiegeZoneForDefendingSoldierKilledByAttackingSoldier(deadPlayer, killerPlayer, deadResidentTown, killerResidentTown)) != null) {
				awardSiegePvpPenaltyPoints(false, siegeZone.getAttackingNation(), deadResident, siegeZone);

			} else if ((siegeZone = getSiegeZoneForAttackingSoldierKilledByDefendingGuard(deadPlayer, killerPlayer, deadResidentTown, killerResidentTown)) != null) {
				awardSiegePvpPenaltyPoints(false, siegeZone.getAttackingNation(), deadResident, siegeZone);

			} else if ((siegeZone = getSiegeZoneForAttackingSoldierKilledByDefendingSoldier(deadPlayer, killerPlayer, deadResidentTown, killerResidentTown)) != null) {
				awardSiegePvpPenaltyPoints(false, siegeZone.getAttackingNation(), deadResident, siegeZone);

			//Allies
			} else if ((siegeZone = getSiegeZoneForDefendingGuardKilledByAttackingAllySoldier(deadPlayer, killerPlayer, deadResidentTown, killerResidentTown)) != null) {
				awardSiegePvpPenaltyPoints(false, siegeZone.getAttackingNation(), deadResident, siegeZone);

			} else if ((siegeZone = getSiegeZoneForDefendingSoldierKilledByAttackingAllySoldier(deadPlayer, killerPlayer, deadResidentTown, killerResidentTown)) != null) {
				awardSiegePvpPenaltyPoints(false, siegeZone.getAttackingNation(), deadResident, siegeZone);

			} else if ((siegeZone = getSiegeZoneForAttackingSoldierKilledByDefendingAllySoldier(deadPlayer, killerPlayer, deadResidentTown, killerResidentTown)) != null) {
				awardSiegePvpPenaltyPoints(false, siegeZone.getAttackingNation(), deadResident, siegeZone);
			}

		} catch (NotRegisteredException e) {
			e.printStackTrace();
			System.out.println("Error evaluating siege pvp death");
		}
		
				 
	}

	private static SiegeZone getSiegeZoneForDefendingGuardKilledByAttackingSoldier(
		Player deadPlayer, Player killerPlayer, Town deadResidentTown, Town killerResidentTown) throws NotRegisteredException 
	{
		TownyUniverse universe = TownyUniverse.getInstance();
		if (deadResidentTown.hasSiege()
			&& killerResidentTown.hasNation()
			&& deadResidentTown.getSiege().getStatus() == SiegeStatus.IN_PROGRESS
			&& deadResidentTown.getSiege().getSiegeZones().containsKey(killerResidentTown.getNation())
			&& universe.getPermissionSource().testPermission(deadPlayer, PermissionNodes.TOWNY_TOWN_SIEGE_POINTS.getNode())
			&& universe.getPermissionSource().testPermission(killerPlayer, PermissionNodes.TOWNY_NATION_SIEGE_POINTS.getNode())) {

			SiegeZone siegeZone = deadResidentTown.getSiege().getSiegeZones().get(killerResidentTown.getNation());

			if (deadPlayer.getLocation().distance(siegeZone.getFlagLocation()) < TownySettings.getWarSiegeZoneDeathRadiusBlocks()) {
				return siegeZone;
			}
		}
		return null;
	}
	
	private static SiegeZone getSiegeZoneForDefendingGuardKilledByAttackingAllySoldier(
		Player deadPlayer, Player killerPlayer, Town deadResidentTown, Town killerResidentTown) throws NotRegisteredException
	{
		TownyUniverse universe = TownyUniverse.getInstance();
		if (deadResidentTown.hasSiege()
			&& killerResidentTown.hasNation()
			&& deadResidentTown.getSiege().getStatus() == SiegeStatus.IN_PROGRESS
			&& universe.getPermissionSource().testPermission(deadPlayer, PermissionNodes.TOWNY_TOWN_SIEGE_POINTS.getNode())
			&& universe.getPermissionSource().testPermission(killerPlayer, PermissionNodes.TOWNY_NATION_SIEGE_POINTS.getNode())) {

			for (SiegeZone siegeZone : deadResidentTown.getSiege().getSiegeZones().values()) {
				if (siegeZone.getAttackingNation().getAllies().contains(killerResidentTown.getNation())
					&& deadPlayer.getLocation().distance(siegeZone.getFlagLocation()) < TownySettings.getWarSiegeZoneDeathRadiusBlocks()) {
					
					return siegeZone;
				}
			}
		}
		return null;
	}
	
	private static SiegeZone getSiegeZoneForDefendingSoldierKilledByAttackingSoldier(
		Player deadPlayer, Player killerPlayer, Town deadResidentTown, Town killerResidentTown) throws NotRegisteredException 
	{
		TownyUniverse universe = TownyUniverse.getInstance();
		if (killerResidentTown.hasNation()
			&& deadResidentTown.hasNation()
			&& universe.getPermissionSource().testPermission(deadPlayer, PermissionNodes.TOWNY_NATION_SIEGE_POINTS.getNode())
			&& universe.getPermissionSource().testPermission(killerPlayer, PermissionNodes.TOWNY_NATION_SIEGE_POINTS.getNode())) {

			for (SiegeZone siegeZone : killerResidentTown.getNation().getSiegeZones()) {
				if (siegeZone.getSiege().getStatus() == SiegeStatus.IN_PROGRESS
					&& siegeZone.getDefendingTown().hasNation()
					&& siegeZone.getDefendingTown().getNation() == deadResidentTown.getNation()
					&& deadPlayer.getLocation().distance(siegeZone.getFlagLocation()) < TownySettings.getWarSiegeZoneDeathRadiusBlocks()) {

					return siegeZone;
				}
			}
		}
		return null;
	}
	
	private static SiegeZone getSiegeZoneForAttackingSoldierKilledByDefendingGuard(
		Player deadPlayer, Player killerPlayer, Town deadResidentTown, Town killerResidentTown) throws NotRegisteredException
	{
		TownyUniverse universe = TownyUniverse.getInstance();
		if (deadResidentTown.hasNation()
			&& killerResidentTown.hasSiege()
			&& killerResidentTown.getSiege().getStatus() == SiegeStatus.IN_PROGRESS
			&& deadResidentTown.getSiege().getSiegeZones().containsKey(killerResidentTown.getNation())
			&& universe.getPermissionSource().testPermission(deadPlayer, PermissionNodes.TOWNY_NATION_SIEGE_POINTS.getNode())
			&& universe.getPermissionSource().testPermission(killerPlayer, PermissionNodes.TOWNY_NATION_TOWN_POINTS.getNode())) {
			
			SiegeZone siegeZone = deadResidentTown.getSiege().getSiegeZones().get(killerResidentTown.getNation());

			if (deadPlayer.getLocation().distance(siegeZone.getFlagLocation()) < TownySettings.getWarSiegeZoneDeathRadiusBlocks()) {
				return siegeZone;
			}
		}
		return null;
	}

	private static SiegeZone getSiegeZoneForAttackingSoldierKilledByDefendingSoldier(
		Player deadPlayer, Player killerPlayer, Town deadResidentTown, Town killerResidentTown) throws NotRegisteredException
	{
		TownyUniverse universe = TownyUniverse.getInstance();
		if (killerResidentTown.hasNation()
			&& deadResidentTown.hasNation()
			&& universe.getPermissionSource().testPermission(deadPlayer, PermissionNodes.TOWNY_NATION_SIEGE_POINTS.getNode())
			&& universe.getPermissionSource().testPermission(killerPlayer, PermissionNodes.TOWNY_NATION_SIEGE_POINTS.getNode())) {

			for (SiegeZone siegeZone : deadResidentTown.getNation().getSiegeZones()) {
				if (siegeZone.getSiege().getStatus() == SiegeStatus.IN_PROGRESS
					&& siegeZone.getDefendingTown().hasNation()
					&& siegeZone.getDefendingTown().getNation() == killerResidentTown.getNation()
					&& deadPlayer.getLocation().distance(siegeZone.getFlagLocation()) < TownySettings.getWarSiegeZoneDeathRadiusBlocks()) {

					return siegeZone;
				}
			}
		}
		return null;
	}

	private static SiegeZone getSiegeZoneForDefendingSoldierKilledByAttackingAllySoldier(
		Player deadPlayer, Player killerPlayer, Town deadResidentTown, Town killerResidentTown) throws NotRegisteredException {
		TownyUniverse universe = TownyUniverse.getInstance();
		if (killerResidentTown.hasNation()
			&& deadResidentTown.hasNation()
			&& universe.getPermissionSource().testPermission(deadPlayer, PermissionNodes.TOWNY_NATION_SIEGE_POINTS.getNode())
			&& universe.getPermissionSource().testPermission(killerPlayer, PermissionNodes.TOWNY_NATION_SIEGE_POINTS.getNode())) {

			for (SiegeZone siegeZone : killerResidentTown.getNation().getSiegeZones()) {
				if (siegeZone.getSiege().getStatus() == SiegeStatus.IN_PROGRESS
					&& siegeZone.getDefendingTown().hasNation()
					&& deadResidentTown.getNation().getAllies().contains(siegeZone.getDefendingTown().getNation())
					&& deadPlayer.getLocation().distance(siegeZone.getFlagLocation()) < TownySettings.getWarSiegeZoneDeathRadiusBlocks()) {

					return siegeZone;
				}
			}
		}
		return null;
	}

	private static SiegeZone getSiegeZoneForAttackingSoldierKilledByDefendingAllySoldier(
		Player deadPlayer, Player killerPlayer, Town deadResidentTown, Town killerResidentTown) throws NotRegisteredException
	{
		TownyUniverse universe = TownyUniverse.getInstance();
		if (killerResidentTown.hasNation()
			&& deadResidentTown.hasNation()
			&& universe.getPermissionSource().testPermission(deadPlayer, PermissionNodes.TOWNY_NATION_SIEGE_POINTS.getNode())
			&& universe.getPermissionSource().testPermission(killerPlayer, PermissionNodes.TOWNY_NATION_SIEGE_POINTS.getNode())) {

			for (SiegeZone siegeZone : deadResidentTown.getNation().getSiegeZones()) {
				if (siegeZone.getSiege().getStatus() == SiegeStatus.IN_PROGRESS
					&& siegeZone.getDefendingTown().hasNation()
					&& killerResidentTown.getNation().getAllies().contains(siegeZone.getDefendingTown().getNation())
					&& deadPlayer.getLocation().distance(siegeZone.getFlagLocation()) < TownySettings.getWarSiegeZoneDeathRadiusBlocks()) {

					return siegeZone;
				}
			}
		}
		return null;
	}
	
	private static void awardSiegePvpPenaltyPoints(boolean attackerDeath,
											   TownyObject pointsRecipient,
											   Resident deadResident,
											   SiegeZone siegeZone) throws NotRegisteredException {
		SiegeWarPointsUtil.awardSiegePenaltyPoints(
			attackerDeath,
			pointsRecipient, 
			deadResident, 
			siegeZone, 
			TownySettings.getLangString("msg_siege_war_participant_death"));
	}
}
