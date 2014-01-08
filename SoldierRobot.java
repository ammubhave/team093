package team093;

import battlecode.common.*;

import java.util.*;

public class SoldierRobot extends BaseRobot {
	public SoldierRobot(RobotController rc) throws GameActionException {
		super(rc);
	}
	
	@Override
	public void run() throws GameActionException {

		Random rand = new Random();
		Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
		
		try {
			if (rc.isActive()) {
				int action = (rc.getRobot().getID()*rand.nextInt(101) + 50)%101;
				//Construct a PASTR
				if (action < 1 && rc.getLocation().distanceSquaredTo(rc.senseHQLocation()) > 2) {
					rc.construct(RobotType.PASTR);
				//Attack a random nearby enemy
				} else if (action < 30) {
					Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class,10,rc.getTeam().opponent());
					if (nearbyEnemies.length > 0) {
						RobotInfo robotInfo = rc.senseRobotInfo(nearbyEnemies[0]);
						rc.attackSquare(robotInfo.location);
					}
				//Move in a random direction
				} else if (action < 80) {
					Direction moveDirection = directions[rand.nextInt(8)];
					if (rc.canMove(moveDirection)) {
						rc.move(moveDirection);
					}
				//Sneak towards the enemy
				} else {
					Direction toEnemy = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
					if (rc.canMove(toEnemy)) {
						rc.sneak(toEnemy);
					}
				}
			}
		} catch (Exception e) {
			System.out.println("Soldier Exception");
		}
	}
	
	public MapLocation getNearestCowLocation() {
		
		return null;
	}
}