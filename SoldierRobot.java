package team093;

import battlecode.common.*;

import java.util.*;

public class SoldierRobot extends BaseRobot {
	public SoldierRobot(RobotController rc) throws GameActionException {
		super(rc);
		readTerrain(rc.getMapWidth(),rc.getMapHeight());
	}
	private void readTerrain(int width, int height) throws GameActionException{
		
		//read and decode half the map
		terrainMap = new TerrainTile[width][height];

		int buffer=0;
		int channel=0;
		for (int i = 0; i < width; i++){
			for (int j = 0; j < height/2; j++){
				if((i*(height/2)+j)%15==0){
					buffer=rc.readBroadcast(channel);
					//System.out.println(channel+": "+buffer);
					channel+=1;
				}
				//bitshift buffer down until the first two bits are the ones being extracted, then extract by & with 3
				terrainMap[i][j]= TerrainTile.values()[(buffer>>((i*(rc.getMapHeight()/2)+j)%15*2))&3];
				terrainMap[width-1-i][height-1-j] = terrainMap[i][j]; //rotate 180 to get the other half of the map
			}
		}
		/*
		for (int i = 0; i < width; i++){
			for (int j = 0; j < height; j++){
				System.out.print(terrainMap[i][j].ordinal());
			}
			System.out.println(" ");
		}
		*/
		
	}
	
	//returns ideal location to place a new pasture
	private MapLocation newPastureLocation(){
		return new MapLocation(10,10);
	}
	
	private MapLocation shouldIAttack(Robot[] nearbyEnemies){
		return new MapLocation(-1,-1);
	}
	
	@Override
	public void run() throws GameActionException {
		MapLocation destination;
		
		//scan for nearbyEnemies, decide whether or not to attack
		Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class,10,rc.getTeam().opponent());
		if (nearbyEnemies.length > 0) {
			MapLocation attack = shouldIAttack(nearbyEnemies);
			if(!attack.equals(new MapLocation(-1,-1))){
				rc.attackSquare(attack);
			}
		}
		
		//sense nearby pastrs, build one if there aren't any
		MapLocation[] myPastrLocations = rc.sensePastrLocations(rc.getTeam());
		if(myPastrLocations.length==0){
			destination=newPastureLocation();
		}
		else{
			//heard heard(pasture, location) 
		}
		
		rc.yield();
		MapLocation[] ls = (new MapPathSearchNode(terrainMap, rc.getLocation(), null, 0, rc.getLocation().add(-10, -23))).getPathTo(rc.getLocation().add(-10, -23));
		rc.yield();
		//System.out.print("RUNNING SOLDIER!\n");
		try {
			//
			if (rc.isActive()) {			
				//rc.construct(RobotType.PASTR);
				rc.getHealth();
				//System.out.print(ls.length);System.out.flush();
				for(int i= 0;i<ls.length-1;i++) {
					while(!rc.isActive());
					//System.out.print(ls[i]);
					Direction toGoal = ls[i].directionTo(ls[i+1]);
					if (rc.canMove(toGoal)) {
						//System.out.print(ls[i]);
						//System.out.print(toGoal);
						rc.move(toGoal);
						rc.yield();
					}
					
				}
				
				ls = (new MapPathSearchNode(terrainMap, rc.getLocation(), null, 0, rc.getLocation().add(10, 23))).getPathTo(rc.getLocation().add(10, 23));
				rc.yield();
				
				for(int i= 0;i<ls.length-1;i++) {
					while(!rc.isActive());
					//System.out.print(ls[i]);
					Direction toGoal = ls[i].directionTo(ls[i+1]);
					if (rc.canMove(toGoal)) {
						//System.out.print(ls[i]);
						//System.out.print(toGoal);
						rc.move(toGoal);
						rc.yield();
					}
					
				}
				
			}
		}
		catch (Exception ex) {
			System.out.print(ex);
		}
	}
	
}