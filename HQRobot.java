package team093;

import battlecode.common.*;

import java.util.*;

public class HQRobot extends BaseRobot {
	public HQRobot(RobotController rc) throws GameActionException {
		super(rc);
		
		System.out.print("Begin init");
		//populate 2D arrays with map information
		//booleanMap = senseBooleanMap(rc);
		terrainMap = senseTerrainMap(rc);
		for (int i = 0; i < rc.getMapWidth(); i++)
			for (int j = 0; j < rc.getMapWidth(); j++)
				rc.broadcast(i + j * rc.getMapWidth(), (terrainMap[i][j]).ordinal());
		System.out.print("Done init");
	}
	
	@Override
	public void run() throws GameActionException {

		//Check if a robot is spawnable and spawn one if it is
		if (rc.isActive()){
			//spawn more robots
			if(rc.senseRobotCount() < 25) {

				Direction toEnemy = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
				if (rc.senseObjectAtLocation(rc.getLocation().add(toEnemy)) == null) {
					rc.spawn(toEnemy);
				}
			}
			Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class,15,rc.getTeam().opponent());
			if (nearbyEnemies.length > 0) {
				RobotInfo robotInfo = rc.senseRobotInfo(nearbyEnemies[0]);
				rc.attackSquare(robotInfo.location);
			}
		}
		
	}
}