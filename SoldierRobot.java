package team093;

import java.util.ArrayList;
import java.util.Random;

import team093.AStarPathFinder;
import team093.BasicPathing;
import team093.BreadthFirst;
import team093.Comms;
import team093.GameMap;
import team093.Path;
import team093.VectorFunctions;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

enum SoldierMode {UNASSIGNED, DEFENDER, DESTROYER, SOLO_SETTLER, SOLO_NOISING, NEED_ROLE, ANY};

public class SoldierRobot extends BaseRobot{
	
	private boolean gotMap = false;
	public static MapLocation enemyHQ;
	public static int directionalLooks[] = new int[]{0,1,-1,2,-2,3,-3,4};
	private MapLocation rallyPoint;
	//private ArrayList<MapLocation> path = new ArrayList<MapLocation>();;
	//private int pathCreatedRound = -1;
	static int bigBoxSize = 5;
	private Path path1=null;
	private GameMap map;
	private AStarPathFinder finder = null;
	public MapLocation destination = null;
	int i=1;
	private double[][] cowGrowth;
	static Random randall = new Random();
	SoldierMode currentRole = SoldierMode.UNASSIGNED;
	int lastHeartbeat = 0;
	
	int lastGroupParameterCheck = 0;
	final int groupParameterTurnCheck = 80;
	int currentTurn = 0;
	
	Robot[] nearbyEnemies = null;
	Robot[] nearbyAllies = null;
	
	GroupUnit group = null;
	int orderInGroup = 0;
	boolean isMemberOfGroup = false;
	
	
	//FOR SETTLERS:
	int pastrChannel = 0;
	
	MapLocation roamingSite = null;
	int robotID;
	
	public SoldierRobot(RobotController rc) throws GameActionException {
		super(rc);
		rc.setIndicatorString(0, "Beginning Constructor...");
		randall.setSeed(rc.getRobot().getID());
		this.cowGrowth = rc.senseCowGrowth();
		//readTerrain(rc.getMapWidth(),rc.getMapHeight());
		
		//if no group or task is available, just roam at HQ, this should never happen for an extended period of time
		roamingSite = rc.senseHQLocation();
		robotID = rc.getRobot().getID();
		rc.setIndicatorString(0, "Constructor has finished.");
	}
	
	
	/*This function determines what role a Soldier takes, it is the HEART of our strategy
	 * In maps where the HQs are far apart, we will prefer to build pastrs and noise towers and give delayed
	 * importance to defense/offense groups
	 * 
	 * In maps where the HQs are close together, we will build defense groups first, then build pastrs
	 * 
	 * 
	 * */
	private void determineSoldierMode () throws GameActionException {
		
		rc.setIndicatorString(0, "determining Soldier Mode...");
		
		int HQdistance = (int)calculateDistance(rc.senseHQLocation(),rc.senseEnemyHQLocation());
		
		//how many robots we want active before we start building pastrs
		int initialSupportingRobots = (120 - HQdistance) / 10;
		if (initialSupportingRobots < 0 || initialSupportingRobots < membersPerGroup) initialSupportingRobots = 0;
		initialSupportingRobots = initialSupportingRobots - (initialSupportingRobots % membersPerGroup);
		/*examples: distance between HQ is 120, so 150-120
		 * */
		//how many pastrs we then want to buid
		int initialPastrs = 2 + ( (int) (HQdistance)/40); 
		
		//how many robots per pastr we then want to exist
		int soldiersPerPastr = membersPerGroup * 2;
		
		

		int effectivePastrs = 0;
		int effectiveNoiseTowers = 0;
		int readyPastrChannel = 0;
		int firstChannelWithoutNoiseTower = 0;
		int currentChannel = pastrComStart;
		
		//BEGIN COUNT ALL ROBOTS ON FIELD
		for (int n = 0; n < 13; n++) {
			
			int message = rc.readBroadcast(currentChannel);
			PastrStatus status = PastrRobot.channelGetPastrStatus(message);
			
			switch (status) {
			//if it's unassigned, keep checking next channel
			case UNASSIGNED:
					break;
				//the good one, if it's ready, become a settler
			case READY:
				
				if (readyPastrChannel == 0) {
					readyPastrChannel = currentChannel;
				}
				
				break;
				//if someone is settling it, check to see if they have a noise tower
			case SETTLING:
			case BUILDING:
			case HEALTHY:
			case EMERGENCY:
			case ATTACKED:
			case DOOMED:
				//in all these cases, increase pastr count. It is the responsiblity of the HQ to make sure
				//this list is updated
				effectivePastrs++;
				
				if (!PastrRobot.channelGetIsThereNoiseTower(message)) {
					
					if (firstChannelWithoutNoiseTower == 0) {
						firstChannelWithoutNoiseTower = currentChannel;
					}
					
				} else {
					effectiveNoiseTowers++;
				}
				break;
			}
				currentChannel += 2;
		}
		
		rc.setIndicatorString(0, "Finished checking Pastr Channels....");
		
		RobotCount robotCount = new RobotCount(rc.senseNearbyGameObjects(Robot.class, 10000, rc.getTeam()),rc);
		
		int effectiveSoldiers = robotCount.getTotalRobotCount() - effectivePastrs - effectiveNoiseTowers;
		//END COUNT ALL ROBOTS ON FIELD
		
		
		
		//if you don't have enough soldiers to protect the first pastrs, create them.
		if (effectiveSoldiers < initialSupportingRobots) {
			rc.setIndicatorString(0, "Going to try to join a group");
			if (!joinAGroup(rc))
				rc.setIndicatorString(0, "Couldn't find group");
		} else {
			
			//make sure there are enough Noise Towers for each pastr out there
			if (effectiveNoiseTowers < effectivePastrs) {
				rc.setIndicatorString(0, "Will begin Noise Tower function...");
				becomeNoiseTower(firstChannelWithoutNoiseTower);
			} else {
				//make sure you have all the pastrs you initially want
				if (effectivePastrs < initialPastrs) {
					rc.setIndicatorString(0, "Will begin Pastr function...");
					becomePastr(readyPastrChannel);
				} else {
					//now that you have all initial pastrs and soldiers, follow a pattern to build the rest
					if ( (effectiveSoldiers - initialSupportingRobots) < (effectivePastrs - initialPastrs) * soldiersPerPastr  ) {
						if (!joinAGroup(rc))
							rc.setIndicatorString(0, "Couldn't find group");
					} else {
						rc.setIndicatorString(0, "Will begin Pastr function...");
						becomePastr(readyPastrChannel);
					}
					
					
				}
			}
			
			
		}
		
		
		//System.out.println("After checking all pastr channels, found " + effectivePastrs + " effective Pastrs and " + effectiveNoiseTowers + " effective Noise Towers and " + effectiveSoldiers + " effective Soldiers");
		

		
			
		
		
	}
	
	//GROUP SECTION: joining/leaving
	private boolean joinAGroup(RobotController rc) throws GameActionException {
		

		

		
		//This searches for each open group, then checks to see if there's space available
		int maxGroupsToCheck = (25 / membersPerGroup) + 1;
		
		int[] messages = new int[2];
		int currentChannel = firstGroupChannel;
		
		//System.out.println("In join a group function, maxGroupsToCheck is " + maxGroupsToCheck);
		
		for (int n = 0; n < maxGroupsToCheck; n++) {
			messages[0] = rc.readBroadcast(currentChannel);
			messages[1] = rc.readBroadcast(currentChannel + 1);
			
			//System.out.println("Checking channel " + currentChannel + "...");
			
			if (GroupUnit.getIsGroupOpen(messages)) {
				
				//System.out.println("found open group with message " + Integer.toBinaryString(messages[0]));
				
				if (!GroupUnit.getIsFull(messages)) {
					
					System.out.println("found group with space available...");
					
					group = new GroupUnit(membersPerGroup);
					group.groupChannel = currentChannel;
					
					int[] groupAssignmentNumbers = GroupUnit.addRobotToGroupTakesThreeArray(messages, group, rc);
					messages[0] = groupAssignmentNumbers[0];
					messages[1] = groupAssignmentNumbers[1];
					orderInGroup = groupAssignmentNumbers[2];
					
					if (groupAssignmentNumbers[2] == -1) System.out.println("addRobotToGroup() reported group was already closed! this is gonna mess everything up!!!!!!!!!!!!");
					
					isMemberOfGroup = true;
					
					
					
					//start initial heartbeat
					rc.broadcast(currentChannel + 2 + orderInGroup, Clock.getRoundNum());
					

					System.out.println("Reading group " + group.groupChannel + " and getting binary of " + Integer.toBinaryString(messages[0])     );
					
					updateGroupInformation(group, messages);
					if (path1 == null) {
						System.out.println("path1 is null!");
					}
					System.out.println("Before broadcast");
					rc.broadcast(currentChannel, messages[0]);
					rc.broadcast(currentChannel + 1, messages[1]);
					
					
					System.out.println("Is member " + orderInGroup + " of group " + group.groupChannel);
					
					return true;
					
				}
			}
			
			currentChannel += (2 + membersPerGroup);
		}
			
			
		return false;	
		
		

		

		
	}
	
	private void leaveCurrentGroup(RobotController rc) throws GameActionException {
		
		int[] messages = new int[2];
		
		messages[0] = rc.readBroadcast(group.groupChannel);
		messages[1] = rc.readBroadcast(group.groupChannel + 1);
		
		
		isMemberOfGroup = false;
		GroupUnit.removeRobot(orderInGroup, messages, group);
		
		rc.broadcast(group.groupChannel, messages[0]);
		rc.broadcast(group.groupChannel, messages[1]);
		group = null;
		
		

		
	}
	
	private void updateGroupInformation(GroupUnit newUnit, int[] messages) {
		
		currentRole = GroupUnit.getGroupRole(messages); 
		
		//find out group's destination
		MapLocation newTarget = GroupUnit.getCurrentTarget(messages);
		if (newTarget != null && !newTarget.equals(destination)) {
			
			destination = newTarget;
			//System.out.println("got target");
			//rc.setIndicatorString(0, "Is part of group, now searching for path from " + rc.getLocation() + " to " + destination);
			setPath(rc.getLocation(),destination);
			//System.out.println("move list is " + ls.length + " long");
		} else {
			//System.out.println("didnt get target ");
		}
		
	}
	
	private void announceArrivalAtGroupSite(RobotController rc) throws GameActionException {
		
		int[] messages = new int[2];
		
		messages[0] = rc.readBroadcast(group.groupChannel);
		messages[1] = rc.readBroadcast(group.groupChannel + 1);
		
		messages = GroupUnit.setHasArrived(orderInGroup, true, messages);

		roamingSite = destination;
		destination = null;
		
		
		System.out.println("robot has arrived at location.");
		//System.out.println("after robot# " + orderInGroup + " added his arrival, message 2 is " + Integer.toBinaryString(group.destroyerMessage2));
		
		if (GroupUnit.hasEntireGroupArrived(messages, group)) {
			System.out.println("Entire group has arrived at location.");
			GroupUnit.clearTargetingInformation(rc, messages);
		}
		
		
	}
	
	//THIS METHOD SETS UP NOISE TOWERS
	private void becomeNoiseTower(int firstChannelWithoutNoiseTower) throws GameActionException {
		rc.setIndicatorString(0, "In becomeNoiseTower(), getting messages...");
		
		int message1 = rc.readBroadcast(firstChannelWithoutNoiseTower);
		message1 = PastrRobot.channelSetIsThereNoiseTower(true, message1);
		rc.broadcast(firstChannelWithoutNoiseTower, message1);
		
		//first heartbeat
		rc.broadcast(firstChannelWithoutNoiseTower + 1, Clock.getRoundNum());
		
		currentRole = SoldierMode.SOLO_NOISING;
		pastrChannel = firstChannelWithoutNoiseTower;
		
		rc.setIndicatorString(0, "Now searching for a good spot around the pastr...");
		
		//search all spots around Pastr to find one that is empty
		MapLocation pastrLoc = PastrRobot.channelGetLocation(message1);
		MapLocation newLoc = null;
		MapLocation hq = rc.senseHQLocation();
		MapLocation enemyHQ = rc.senseEnemyHQLocation();
		Direction[] dirList = Direction.values();
		
		for (Direction each : dirList) {
			if (each != Direction.NONE && each != Direction.OMNI) {
				
				newLoc = pastrLoc.add(each);
				
				rc.setIndicatorString(0, "Checking point " + newLoc.x + "," + newLoc.y + "...");
				
				if (terrainMap[newLoc.x][newLoc.y] != TerrainTile.OFF_MAP && terrainMap[newLoc.x][newLoc.y] != TerrainTile.VOID ) {
					rc.setIndicatorString(0, "Checking point " + newLoc.x + "," + newLoc.y + ", it is clear of obstacles");
					
					if (!newLoc.equals(hq) && !newLoc.equals(enemyHQ)) {
						rc.setIndicatorString(0, "Checking point " + newLoc.x + "," + newLoc.y + ", it is clear of HQ's, good to go...");
						break;
					}
				}
				
			}
		}
		
		destination = newLoc;
		
		
		rc.setIndicatorString(0, "Getting path to location for Noise tower, from " + rc.getLocation().toString() + " to " + destination);
		setPath(rc.getLocation(),destination  );
		
		
		rc.setIndicatorString(1, "Will create Noise Tower at channel " + pastrChannel);
		
	}
	
	
	private void becomePastr(int readyPastrChannel) throws GameActionException {
		
		int message = rc.readBroadcast(readyPastrChannel);
		
		message = PastrRobot.channelSetPastrStatus(PastrStatus.SETTLING, message);
		pastrChannel = readyPastrChannel;
		
		//first heartbeat
		message = PastrRobot.channelSetTurn(Clock.getRoundNum(), message);
		rc.broadcast(readyPastrChannel, message);
		
		//setup destination and path
		destination = PastrRobot.channelGetLocation(message);
		setPath(rc.getLocation(),destination);

		
		

		
		currentRole = SoldierMode.SOLO_SETTLER;

		rc.setIndicatorString(1, "Will create Pastr at channel " + pastrChannel);
		
	}
	
	
	
	private boolean isMapReady() throws GameActionException {
		if(rc.readBroadcast(0)==1){
			return true;
		}
		return false;
	}
	
	//Each time this is called, it finds some random direction to move in, but always within border around roamingSite
	//created by roam Radius
	private void roam(RobotController rc, int roamRadius, MapLocation pivot) throws GameActionException {
		int leftX = pivot.x - ((roamRadius -1)/2);
		int topY = pivot.y - ((roamRadius -1)/2);
		int randX = randall.nextInt(roamRadius);
		int randY = randall.nextInt(roamRadius);

		MapLocation randomLocation = new MapLocation(leftX + randX, topY + randY ); 
		
		Direction toGoal = rc.getLocation().directionTo(randomLocation);
		if (toGoal != Direction.NONE && toGoal != Direction.OMNI) {
			if (rc.canMove(toGoal)) {
				rc.sneak(toGoal);
			}
		}
	}
	
	private MapLocation findAverageAllyLocation(Robot[] alliedRobots) throws GameActionException {
		//find average soldier location
		MapLocation[] alliedRobotLocations = VectorFunctions.robotsToLocations(alliedRobots, rc, true);
		MapLocation startPoint;
		if(alliedRobotLocations.length>0){
			startPoint = VectorFunctions.meanLocation(alliedRobotLocations);
			if(Clock.getRoundNum()%100==0)//update rally point from time to time
				rallyPoint=startPoint;
		}else{
			startPoint = rc.senseHQLocation();
		}
		return startPoint;
	}
/*	
	private void navigateByPath(Robot[] alliedRobots) throws GameActionException{
		if(path.size()<=1){//
			//check if a new path is available
			int broadcastCreatedRound = rc.readBroadcast(myBand);
			if(pathCreatedRound <broadcastCreatedRound){//download new place to go
				pathCreatedRound = broadcastCreatedRound;
				path = Comms.downloadPath();
			}else{//just waiting around. Consider building a pastr
				//considerBuildingPastr(alliedRobots);
			}
		}
		if(path.size()>0){
			//follow breadthFirst path...
			Direction bdir = BreadthFirst.getNextDirection(path, bigBoxSize);
			//...except if you are getting too far from your allies
			MapLocation[] alliedRobotLocations = VectorFunctions.robotsToLocations(alliedRobots, rc, true);
			if(alliedRobotLocations.length>0){
				MapLocation allyCenter = VectorFunctions.meanLocation(alliedRobotLocations);
				if(rc.getLocation().distanceSquaredTo(allyCenter)>16){
					bdir = rc.getLocation().directionTo(allyCenter);
				}
			}
			BasicPathing.tryToMove(bdir, true,true, false);
		}
	}
*/	
	
//	public MapLocation goodPastrLocation(){
//		
//		return new MapLocation(rc.senseHQLocation().x+2,rc.senseHQLocation().y+2);
//	}
	
	private void readTerrain(int width, int height) throws GameActionException{
		//read and decode half the map
		terrainMap = new TerrainTile[width][height];
	
		int buffer=0;
		int channel=1;
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


	public MapLocation goodPastrLocation(){
		//width, then height
		//I'm afraid to ask what BS stands for here
		int BS=5;
		int width=rc.getMapWidth();
		int height=rc.getMapHeight();
		double[][] avgGrowth=new double[width][height];
		//first block, top left
		for (int a=0; a<BS; a++){
			for (int b=0; b<BS; b++){
				avgGrowth[2][2]+=cowGrowth[a][b];
			}
		}
		avgGrowth[2][2]/=(BS*BS);
		//horizontal blocks
		for (int i=3; i<width-2; i++){
			avgGrowth[i][2]=avgGrowth[i-1][2];
			for (int a=0; a<5; a++){
				avgGrowth[i][2]+=cowGrowth[i+2][a]/BS/BS-cowGrowth[i-3][a]/BS/BS;
			}
		}
		for (int j=3; j<height/2; j++){
			for (int i=2; i<width-2; i++){
				avgGrowth[i][j]=avgGrowth[i][j-1];
				for (int a=0; a<5; a++){
					avgGrowth[i][j]+=cowGrowth[i+a-2][j+2]/BS/BS-cowGrowth[i+a-2][j-3]/BS/BS;
				}
			}
		}
		for (int i=width-3; i>=2; i--){
			for (int j=height-3; j>=height/2; j--){
				avgGrowth[i][j]=avgGrowth[width-i-1][height-j-1];
			}
		}
		MapLocation HQ=rc.senseHQLocation();
		for (int i=2; i<width-2; i++){
			for (int j=2; j<height-2; j++){
				avgGrowth[i][j]/=(Math.abs(HQ.x-i)+Math.abs(HQ.y-j)+5);
			}
		}
		/*
		System.out.println("-------");
		for (int i=0; i<width; i++){
			for (int j=0; j<height; j++){
				System.out.print((int)(100*avgGrowth[i][j]+.5)/100.0+"\t");
			}
			System.out.println();
		}
		System.out.println("-------");
		*/
//		for (int j=0; j<height; j++){
//			for (int i=0; i<width; i++){
//				System.out.print((int)cowGrowth[i][j]+"\t");
//			}
//			System.out.println();
//		}
//		System.out.println("-------");
//		System.out.println(terrainMap.length);
//		System.out.println(terrainMap[0].length);
//		
		//System.out.println(i+"");
		//System.out.println(j+"");
		for (int x = 0; x < width; x++){
			for (int j = 0; j < height; j++){
				//System.out.println(terrainMap[x][j]);
				if(terrainMap[x][j] != null){
					if (terrainMap[x][j].ordinal() >= 1){
						avgGrowth[x][j]=0;
					}
				}
			}
		}

		double maxValue = 0;
		int maxX=0;
		int maxY=0;
		
		//System.out.println("\nMax values in 2D array: ");
		for (int i = 0; i < avgGrowth.length; i++)
		    for (int j = 0; j < avgGrowth[i].length; j++)
		        if (avgGrowth[i][j] > maxValue){
		           maxValue = avgGrowth[i][j];
		           maxX=i;
		           maxY=j;
		        }
					
		//System.out.println("Maximum value: " + maxValue);
		return new MapLocation(maxX,maxY);
		
	}
	
	
	private void theBeatOfMyHeart(int currentTurn) throws GameActionException {
		//heartbeat subroutine. I subtract 5 to be sure robot isn't declared dead
		if (currentTurn - lastHeartbeat > declareDeadInterval - 5) {
			lastHeartbeat = currentTurn;
			
			if (isMemberOfGroup) {
				rc.broadcast(group.groupChannel + 2 + orderInGroup, Clock.getRoundNum());
			}
			else {
				if (currentRole == SoldierMode.SOLO_SETTLER) {
					
					int message = rc.readBroadcast(pastrChannel);
					message = PastrRobot.channelSetTurn(Clock.getRoundNum(), message);
					rc.broadcast(pastrChannel, message);
					
				} else if (currentRole == SoldierMode.SOLO_NOISING) {
					rc.broadcast(pastrChannel + 1, Clock.getRoundNum());
				}
			}
		}
	}
	
	
	//this returns TRUE if pathing is successful, FALSE if it is not
	private boolean setPath(MapLocation start, MapLocation end) {
		//set i to 1. i is like what moveCount used to be?

			i =1;
			//DISCUSS: was destination saved elsewhere?
			
			if (finder == null) System.out.println("Inside setPath(), finder is null!");
			if (path1 == null) System.out.println("Before findPath(), path1 is null inside setPath()");
			//System.out.println("Right before findPath, start point is " + start.toString() + " and endPoint is "+ end.toString() );
			path1 = finder.findPath(start.x,start.y, end.x,end.y);
			//System.out.println("Right after endpath");

			//if (path1 == null) System.out.println("After findPath(), path1 is null inside setPath()");
			
			
			return path1 != null;

		
	}
	
	//returns TRUE if there is a next spot to move to, FALSE if there is no next spot (because destination was already reached)
	private boolean moveToNextSpot(boolean sneak) throws GameActionException {
		
		if (path1 != null) {

			Robot[] nearbyRobot2 = rc.senseNearbyGameObjects(Robot.class,5,rc.getTeam());
			
			//TODO: is the second condition ever gonna be called here?
			if(i<path1.getLength()){
				//System.out.println("x: "+path1.getStep(i).getX()+" y: "+path1.getStep(i).getY());
				Direction direction = rc.getLocation().directionTo(new MapLocation(path1.getStep(i).getX(),path1.getStep(i).getY()));
				BasicPathing.tryToMove(direction, true,true,sneak);
				i++;
				return true;
			}
			else if(i<path1.getLength()&&nearbyRobot2.length<1&&i>=3){
				i--;
				Direction direction = rc.getLocation().directionTo(new MapLocation(path1.getStep(i).getX(),path1.getStep(i).getY()));
				BasicPathing.tryToMove(direction, true,true,sneak);
				return true;
			}
			else if(i>=path1.getLength()){
				
				MapLocation whereDidYouEndUp = rc.getLocation();
				if (whereDidYouEndUp.equals(destination)) {
					destination = null;
					return false;
				} else {
					setPath(whereDidYouEndUp, destination);
					i= 1;
					return true;
				}
				
				

			}
		}
		
		//this point should never be reached
		System.out.println("path 1 was null!");
		return false;
		
	}

	
	public void run() throws GameActionException {
		//if the map is ready download it
		if(gotMap==false&&isMapReady()==true){
			rc.setIndicatorString(0, "Now getting map...");
			readTerrain(rc.getMapWidth(),rc.getMapHeight());
			gotMap=true;
			map = new GameMap(terrainMap,rc.getMapWidth(),rc.getMapHeight());
			/** The path finder we'll use to search our map */
			finder = new AStarPathFinder(map, 500, true);
			rc.setIndicatorString(0, "Finished initliazing map and finder");
		}
		

		if (gotMap == true) {
			
			currentTurn = Clock.getRoundNum();
			
			//If you don't have a Role, stop being lazy, find one:
			if (currentRole == SoldierMode.UNASSIGNED) {
				determineSoldierMode();
			} else { 
				
				//if you have a role, beat your heart
				theBeatOfMyHeart(Clock.getRoundNum());
		
				if(rc.isActive()){
					
					
					
					nearbyEnemies = rc.senseNearbyGameObjects(Robot.class,10,rc.getTeam().opponent());
					nearbyAllies = rc.senseNearbyGameObjects(Robot.class,10,rc.getTeam());
					//Decide whether to attack or commit suicide
					if (nearbyEnemies.length > 0) {
						
						
							if (nearbyEnemies.length > 0) {
									
									MapLocation currentLoc = rc.getLocation();
									double lowestHealth = 1000;
									MapLocation targetLocation = null; //I'm pretty sure that the below loop guarantees this will never be null
									int adjacentEnemies = 0;
									int adjacentAllies = 0;
									
									
									//search for enemy with lowest health, and determine how many enemies are next to you
									for (int n = 0; n < nearbyEnemies.length; n++) {
										
										RobotInfo info = rc.senseRobotInfo(nearbyEnemies[n]);
										
										if ( info.health < lowestHealth  ) {
											lowestHealth = info.health;
											targetLocation = info.location;
										}
										
										if ( Math.abs(info.location.x - currentLoc.x) <= 1 && Math.abs(info.location.y - currentLoc.y) <= 1 ) {
											adjacentEnemies++;
										}
										
									}
									
									//determines how many allies are next to you
									for (int n = 0; n < nearbyAllies.length; n++) {
										
										RobotInfo info = rc.senseRobotInfo(nearbyAllies[n]);
										if ( Math.abs(info.location.x - currentLoc.x) <= 1 && Math.abs(info.location.y - currentLoc.y) <= 1 ) {
											adjacentAllies++;
										}
										
									}
									
									//suicide algorithm
									int shouldIKillMyself = adjacentAllies - adjacentEnemies;
									if (shouldIKillMyself < 0) {
										double health = rc.senseRobotInfo(rc.getRobot()).health;
										if ((int) health / (shouldIKillMyself * -30) < 1) {
											if (isMemberOfGroup)
												leaveCurrentGroup(rc);
											rc.selfDestruct();
										}
									} else {
										if (targetLocation != null) {
										if (rc.canAttackSquare(targetLocation))
											rc.attackSquare(targetLocation);
										}
									
									}
								}
							
						
					}
					//At this point, robot has decided not to attack and will move instead
					else{
						
						//debugging purposes:
						StringBuilder stringb = new StringBuilder();
						for (int n = 1; n < path1.getLength(); n++ ) {
							stringb.append(" (" + path1.getStep(n).getX() + "," + path1.getStep(n).getY() + ") "  );
						}
						rc.setIndicatorString(1, "i is " + i + " and path is " + stringb.toString());
						
						//Loop for memebers of a Group
						if (isMemberOfGroup) {
							
							rc.setIndicatorString(2, "Is member " + orderInGroup + " of group " + group.groupChannel + " with the role " + currentRole);
							
							if (rc.isActive()) {
								
							//update Group Data is sufficient time has passed since last check
							if ((currentTurn - lastGroupParameterCheck) > groupParameterTurnCheck) {
								lastGroupParameterCheck = currentTurn;
								
								int[] messages = GroupUnit.readGroupInformation(group.groupChannel, rc);
								updateGroupInformation(group, messages);
							}
							
							/*Check if robot has destination. moveToNextSpot() sets destination to null
							 * when there are no more locations to travel
							 *
							 * */
							if (destination != null) {
								
									
									if (currentRole == SoldierMode.DEFENDER || currentRole == SoldierMode.DESTROYER) {
												if(calculateDistance(rc.getLocation(),destination) > 4){
													moveToNextSpot(true);
												}
												else {
													roam(rc,4,destination);
												}
									} 
							//If Destination is null, check group information to see if a new target has been
							//assigned...
							} else {
								
								int[] messages = GroupUnit.readGroupInformation(group.groupChannel, rc);   
								MapLocation groupTarget = GroupUnit.getCurrentTarget(messages);
								
								if (!groupTarget.equals(roamingSite)) {
								 destination = groupTarget;
									setPath(rc.getLocation(),destination);
								}
								 
								 //if robot still has no destination, roam
								 if (destination == null) {
									 roam(rc, 5, roamingSite);
								 } 
							}
							}
							
				//This is the loop for robots who are not part of a group	
				} else {
						if (rc.isActive()) {
							
							rc.setIndicatorString(2, "Current role is " + currentRole);
							
							if (currentRole == SoldierMode.SOLO_SETTLER) {
								
									/*move a space. If function returns false, you are at the end of the path
									But because of a current but, you may not be in the exact destination you
									were looking for...*/
									if (!moveToNextSpot(false)) {
										//set in your pastr Channel that you're going to start 
										//construction on a pastr
										int channelInt = rc.readBroadcast(pastrChannel);
										channelInt = PastrRobot.channelSetPastrStatus(PastrStatus.BUILDING, channelInt);
										channelInt = PastrRobot.channelSetTurn(currentTurn, channelInt);
										rc.broadcast(pastrChannel, channelInt);
										rc.construct(RobotType.PASTR);
									}
								

							} else if (currentRole == SoldierMode.SOLO_NOISING) {
								//move if a space, if you're there, build a noise tower...
								if (!moveToNextSpot(false)) {
									//don't even ask about this, it's not worth it...
									int constructingNoiseTowerFlag = 65536;
									int message = constructingNoiseTowerFlag + Clock.getRoundNum();
									rc.broadcast(pastrChannel + 1, message);
									rc.construct(RobotType.NOISETOWER);
								}
								

								
								
							}
							
							
							
							}
					}
						
					}
				}
		
		
		
		
		}
		
		
		}
		
		
	}
	
}
