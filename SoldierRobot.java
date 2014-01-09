package team093;

import battlecode.common.*;

import java.util.*;

public class SoldierRobot extends BaseRobot {
	public SoldierRobot(RobotController rc) throws GameActionException {
		super(rc);
		readTerrain(rc.getMapWidth(),rc.getMapHeight());
	}
	public void readTerrain(int width, int height) throws GameActionException{
		
		//read and decode half the map
		terrainMap = new TerrainTile[width][height];
		/*
		for (int i = 0; i < width; i++)
			for (int j = 0; j < height/2; j++)
				terrainMap[i][j] = TerrainTile.values()[rc.readBroadcast(i + j * height)];
		*/
		int buffer=0;
		int channel=0;
		for (int i = 0; i < width; i++){
			for (int j = 0; j < height/2; j++){
				if((i*(rc.getMapHeight()/2)+j)%16==0){
					buffer=rc.readBroadcast(channel);
					channel+=1;
				}
				//System.out.println("channel: " +(channel-1)+" #"+((i*(rc.getMapHeight()/2)+j)%16)+" buffer: "+((buffer&(3<<((i*(rc.getMapHeight()/2)+j)%16*2)))>>((i*(rc.getMapHeight()/2)+j)%16*2)));
				terrainMap[i][j]= TerrainTile.values()[(buffer&(3<<((i*(rc.getMapHeight()/2)+j)%16*2)))>>((i*(rc.getMapHeight()/2)+j)%16*2)];
			}
		}

		//rotate 180 to get the other half
		for (int i = 0; i < width; i++){
			for (int j = 0; j < height/2; j++){
				terrainMap[width-1-i][height-1-j] = terrainMap[i][j];
			}
		}
		
		for (int i = 0; i < width; i++){
			for (int j = 0; j < height; j++){
				System.out.print(terrainMap[i][j].ordinal()+", ");
			}
			System.out.println(" ");
		}
		
	}
	@Override
	public void run() throws GameActionException {

		Random rand = new Random();
		Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
		rc.yield();
		MapLocation[] ls = (new MapPathSearchNode(terrainMap, rc.getLocation(), null, 0, rc.getLocation().add(-10, -23))).getPathTo(rc.getLocation().add(-10, -23));
		rc.yield();
		//System.out.print("RUNNING SOLDIER!\n");
		try {

			if (rc.isActive()) {
				
				/*
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
				}*/
				//rc.yield();rc.yield();rc.yield();rc.yield();rc.yield();rc.yield();rc.yield();rc.yield();
				
			//	rc.move(Direction.NORTH_EAST);
				//rc.yield();
				//System.out.print(ls.length);System.out.flush();
				for(int i= 0;i<ls.length-1;i++) {
					while(!rc.isActive());
					Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class,10,rc.getTeam().opponent());
					if (nearbyEnemies.length > 0) {
						RobotInfo robotInfo = rc.senseRobotInfo(nearbyEnemies[0]);
						rc.attackSquare(robotInfo.location);
					}
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
				while(true)
					rc.yield();
			}
		}
		catch (Exception ex) {
			System.out.print(ex);
		}
	}
	
	public MapLocation getNearestCowLocation() {
		
		return null;
	}
}