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
	
	
	//constant variables
	final public int declareDeadInterval = 15; //interval at which soldier heartbeats
	final public int pastrComStart = 50001; //broadcast channel where pastr communication starts
	final public int pastrLocationChannel=50000; //broadcast channel for whether pastrs locations have been calcualated
	final public int membersPerGroup = 6; //number of soldiers per group, 8 is max
	final public int firstGroupChannel = 40002; //broadcast channel where group communication starts
	
	public static RobotController rc;
	public BaseRobot(RobotController rc) throws GameActionException {
		this.rc = rc;
		//this.cowGrowth = rc.senseCowGrowth();
	}
	
	//IS ROBOT DEAD METHODS:
	//returns whether a robot appears to be dead based on its last heartbeat
	public boolean isRobotDead(int currentTurn, int lastTurn) {
		return (currentTurn - lastTurn > declareDeadInterval * 2 );
	}
	
	//it takes 50 turns to build a pastr, during which time the robot can't heartbeat
	public boolean isConstructingPastrRobotDead(int currentTurn, int lastTurn) {
		return (currentTurn - lastTurn) > ( (declareDeadInterval * 2) + 100) ;
	}
	
	//it takes 100 turns to build a noise tower, during which time the robot can't heartbeat
	public boolean isConstructingNoiseTowerDead(int currentTurn, int lastTurn) {
		return (currentTurn - lastTurn) > ( (declareDeadInterval * 2) + 150);
	}
	
	//INTEGER to LOCATION METHODS:
	public static int locToInt(MapLocation m){
		return (m.x*100 + m.y);
	}
	
	public static MapLocation intToLoc(int i){
		return new MapLocation(i/100,i%100);
	}
	
	
	
	//Improved method that accuratly detects number of robots on field
	int senseActualRobotCount(RobotController rc, Robot[] alliedRobots) throws GameActionException {
		
		RobotCount counter = new RobotCount(alliedRobots,rc);
		
		
		
		return counter.getTotalRobotCount();
		
		
		
	}
	
	//I'm honestly not sure what this does or what it was meant for
	int getIntFromBitFlag(int flag) {
		switch (flag) {
		case 1:
			return 1;
		case 2:
			return 2;
		case 3:
			return 4;
		case 4:
			return 8;
		case 5:
			return 16;
		case 6:
			return 32;
		case 7:
			return 64;
		case 8:
			return 128;
			default:
				System.out.println("No flag specified in BaseRobot.getIntFromBitFlag");
				return 0;
		}
	}
	
	//calcualates distance between two points, we may not need this anymore though
	public double calculateDistance(MapLocation a, MapLocation b) {
		return Math.sqrt((double) ( Math.pow((b.x - a.x),2) + Math.pow((b.y - a.y),2) ));
	}
	
	public abstract void run() throws GameActionException;
	
	public void loop() throws GameActionException {
		while(true) {
				run();
			rc.yield();
		}
	}
}