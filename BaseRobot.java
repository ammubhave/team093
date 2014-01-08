package team093;

import battlecode.common.*;

import java.util.*;

public abstract class BaseRobot {
	
	public boolean[][] booleanMap;
	public TerrainTile[][] terrainMap;
	
	public final RobotController rc;
	public BaseRobot(RobotController rc) throws GameActionException {
		this.rc = rc;
		
		//populate 2D arrays with map information
		booleanMap = senseBooleanMap(rc);
		terrainMap = senseTerrainMap(rc);
	}
	
	//creates 2D boolean array indicating whether each map point can be moved into
	public boolean[][] senseBooleanMap(RobotController rc) {
		
		int width = rc.getMapWidth();
		int height = rc.getMapHeight();
		
		boolean[][] fieldGrid = new boolean[width][height];

		for (int x = 0; x < width ; x++ ) {
			
			for (int y = 0; y < height; y++) {
				TerrainTile tile = rc.senseTerrainTile(new MapLocation(x,y));
				fieldGrid[x][y] = (tile == TerrainTile.NORMAL || tile == TerrainTile.ROAD );
			}
		}
		
		return fieldGrid;
	}
	
	
	//creates 2D TerrainTile array indicating TerrainTile types of all map points
	public TerrainTile[][] senseTerrainMap(RobotController rc) {
		
		int width = rc.getMapWidth();
		int height = rc.getMapHeight();
		
		TerrainTile[][] fieldGrid = new TerrainTile[width][height];
		
		for (int x = 0; x < width ; x++ )
		{
			for (int y = 0; y < height; y++)
			{
				fieldGrid[x][y] =  rc.senseTerrainTile(new MapLocation(x,y));
			}
		}
		
		return fieldGrid;
	}
	
	public abstract void run() throws GameActionException;
	
	public void loop() throws GameActionException {
		while(true) {			
				run();
			rc.yield();
		}
	}
}