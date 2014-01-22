package team093;

import battlecode.common.*;

import java.util.*;

public abstract class BaseRobot {

	public boolean[][] booleanMap;
	public TerrainTile[][] terrainMap;
	static int myBand = 2000;
	
	static Direction[] directions=Direction.values();
	/*public TerrainTile getTerrainMap(int x, int y) {
		if (terrainMap[x][y] == null) terrainMap[x][y] = senseTerrainMap(rc);
		return terrainMap[x][y];
	}*/
	
	//public double cowGrowth[][];
	public static RobotController rc;
	public BaseRobot(RobotController rc) throws GameActionException {
		this.rc = rc;
		//this.cowGrowth = rc.senseCowGrowth();
	}
	
	//returns actual number of robots
	int senseActualRobotCount(RobotController rc) {
		int weightedRobotCount = rc.senseRobotCount();
		int pastrCount = rc.sensePastrLocations(rc.getTeam()).length;
		
	//	System.out.println("Robot Count: Weighted - " + weightedRobotCount + " pastr - " + pastrCount + "actual - " + (weightedRobotCount - pastrCount));
		
		return weightedRobotCount - pastrCount;
		
	}
	
	public abstract void run() throws GameActionException;
	
	public void loop() throws GameActionException {
		while(true) {
				run();
			rc.yield();
		}
	}
}