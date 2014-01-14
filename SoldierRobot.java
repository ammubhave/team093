package team093;

import battlecode.common.*;

import java.util.*;

public class SoldierRobot extends BaseRobot {
	
	//definitions
	enum SoldierMode {SETTLER, HERDER, DEFENDER, DESTROYER};
	enum TravelMode {IDLE, MOVING, SNEAKING, WAITING};
	enum HerdMode {NOT_HERDING, HERDING, SNEAKOUT};
	enum DestroyerMode {GROUPING, CHARGING, DESTROYING};
	
	//Path Detection and finding:
	private int moveCount=0;
	private MapLocation destination = new MapLocation(0,29);
	private MapLocation[] ls = null;
	
	//FOR ALL:
	TravelMode currentMode = TravelMode.IDLE; //currentMode tracks whether robot is idle, sneaking, or running
	SoldierMode currentRole;
	
	//FOR SETTLERS:
	boolean shouldBuildPastrAtDestination = false;
	
	
	//FOR HERDERS: 
	HerdMode currentHerdMode = HerdMode.NOT_HERDING;  //currentHerdMode tracks what phase of herding robot is in.
	
	//FOR DESTROYERS:
	short destroyerGroup = 0;
	
	
	
	
	
	public SoldierRobot(RobotController rc) throws GameActionException {
		super(rc);
		readTerrain(rc.getMapWidth(),rc.getMapHeight());
		determineInitialSoldierMode(rc);
		
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
	
	//INCOMPLETE: This method is a placeholder; we need to program it with the real algorithm we'll use
	private void determineInitialSoldierMode (RobotController rc) {
		//temporary method, but assuming we have no noise towers:
		
		
		int actualRobotCount = this.senseActualRobotCount(rc);
		
		switch(actualRobotCount)
		{
		//for now, first four robots will be settlers/herders for David to work with
		case 1:
		case 2:
		case 3:
		case 4:
			currentRole = SoldierMode.SETTLER;
			break;
		//next 3 robots will be destroyers; Alan is programming them
		case 5:
		case 6:
		case 7:
			currentRole = SoldierMode.DESTROYER;
			break;
			
		}
		

		rc.setIndicatorString(0, "" + currentRole);
		
	}
	
	private static int locToInt(MapLocation m){
		return (m.x*100 + m.y);
	}
	
	private static MapLocation intToLoc(int i){
		return new MapLocation(i/100,i%100);
	}
	
	//returns ideal location to place a new pasture
	private MapLocation newPastureLocation(){
		
		class GoalTestIdealPastureLocation implements Callable<MapLocation, Boolean> {
			double cowGrowth[][];
			TerrainTile terrainMap[][];
			public GoalTestIdealPastureLocation(double cowGrowth[][], TerrainTile terrainMap[][]) {
				this.cowGrowth = cowGrowth;
				this.terrainMap = terrainMap;
			}
			public Boolean call(MapLocation input) {
				for (int i = input.x > 0 ? input.x - 1 : input.x; i <= ((input.x < this.cowGrowth.length - 1) ? input.x + 1 : input.x); i++)
					for (int j = input.y > 0 ? input.y - 1 : input.y; j <= ((input.y < this.cowGrowth[0].length - 1) ? input.y + 1 : input.y); j++) {
						if (input.x == 24 && input.y == 31) {
							//System.out.println(this.cowGrowth[i][j]);							
						}
						if (this.cowGrowth[i][j] == 0 || terrainMap[i][j] != TerrainTile.NORMAL)
							return false;
					}
				System.out.println("$$$$$$$$$$$$$");
				return true;
			}
		};
		MapLocation locs[] = (new MapPathSearchNode(terrainMap, rc.getLocation(), null, 0, new GoalTestIdealPastureLocation(this.cowGrowth, this.terrainMap))).getPathTo(destination);
		System.out.print(locs[locs.length - 1]);
		return locs[locs.length - 1];
		
		//return new MapLocation(20,10);
	}
	
	private MapLocation shouldIAttack(Robot[] nearbyEnemies){
		return new MapLocation(-1,-1);
	}
	
	private void herd() throws GameActionException{
		if(currentHerdMode== HerdMode.NOT_HERDING){
			currentHerdMode = HerdMode.HERDING;
		}
		//herd in
		if(currentHerdMode==HerdMode.HERDING){
			destination=intToLoc(rc.readBroadcast(1000));
			destination=new MapLocation(destination.x-1,destination.y+1);
			ls = (new MapPathSearchNode(terrainMap, rc.getLocation(), null, 0, destination)).getPathTo(destination);
			currentMode= TravelMode.MOVING;
		}
		//sneak out
		else if(currentHerdMode==HerdMode.SNEAKOUT){
			System.out.println("heard out");
			destination=intToLoc(rc.readBroadcast(1000));
			destination=new MapLocation(destination.x-1,destination.y+29);
			ls = (new MapPathSearchNode(terrainMap, rc.getLocation(), null, 0, destination)).getPathTo(destination);
			currentMode = TravelMode.SNEAKING;
		}
	}
	
	@Override
	public void run() throws GameActionException {

		//SELF-DEFENSE FUNCTION: scan for nearbyEnemies, decide whether or not to attack
		Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class,10,rc.getTeam().opponent());
		if (nearbyEnemies.length > 0) {
			MapLocation attack = shouldIAttack(nearbyEnemies);
			if(!attack.equals(new MapLocation(-1,-1))){
				rc.attackSquare(attack);
			}
		}
		
		
		//ROLE-SPECIFIC ACTION LOOP, separated by if-else statements for each Role
		if (currentRole == SoldierMode.SETTLER || currentRole == SoldierMode.HERDER) {
			
			
			//SETTLERS and HERDER LOOP (we'll have to divide this into two later)
			MapLocation[] myPastrLocations = rc.sensePastrLocations(rc.getTeam());
			if(myPastrLocations.length==0&& currentMode== TravelMode.IDLE &&rc.readBroadcast(1000)==0){
				destination=newPastureLocation();
				shouldBuildPastrAtDestination = true;
				rc.broadcast(1000, locToInt(destination));
			}
			if(myPastrLocations.length>0 && currentMode== TravelMode.WAITING){
				herd();
			}
			if(currentMode==TravelMode.IDLE){
				ls = (new MapPathSearchNode(terrainMap, rc.getLocation(), null, 0, destination)).getPathTo(destination);
				currentMode=TravelMode.MOVING;
			}
			if (rc.isActive()&& (currentMode == TravelMode.MOVING || currentMode == TravelMode.SNEAKING)) {
				//System.out.print(ls.length);System.out.flush();
				if(moveCount<ls.length-1){
					//System.out.print(ls[i]);
					Direction toGoal = ls[moveCount].directionTo(ls[moveCount+1]);
					if (rc.canMove(toGoal)) {
						//System.out.print(ls[i]);
						//System.out.print(toGoal);
						if(currentMode == TravelMode.MOVING){
							rc.move(toGoal);
						}
						else if(currentMode == TravelMode.SNEAKING){
							rc.sneak(toGoal);
						}
						moveCount++;
					}
				}
				else{
					System.out.println("arrived");
					if(shouldBuildPastrAtDestination){
						rc.construct(RobotType.PASTR);
						shouldBuildPastrAtDestination = false;
					}
					if(currentHerdMode==HerdMode.HERDING){
						currentHerdMode= HerdMode.SNEAKOUT;
					}
					else if(currentHerdMode==HerdMode.SNEAKOUT){
						currentHerdMode=HerdMode.HERDING;
					}
					moveCount=0;
					currentMode = TravelMode.WAITING;
				}
			}
			
			
			
		}
		// DESTROYER LOOP
		else if (currentRole == SoldierMode.DESTROYER) {
			
		}

	}
	
}