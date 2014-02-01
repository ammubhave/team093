package team093;

import team093.BreadthFirst;
import team093.Comms;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.TerrainTile;

public class HQRobot extends BaseRobot{
	
	//Temporary pastr finding variables, will be removed when David and Patrick's method is up
	double[][] cowGrowthMatrix;
	MapLocation hqLoc; //saving so it only needs to be called once (hq won't move).
	MapLocation enemyLoc;
	int mapWidth;
	int mapHeight;
	double maxAverageCowGrowth = 0;
	final int pastrSearchRadius = 4;
	PastrLocationStruct[] bestPastrLocations = new PastrLocationStruct[12];
	//final int MinimumDistanceFromEnemyPastr = 6;
	//PastrLocationStruct[] pastrCandidates;
	//int pastrArrayCount = 0;
	
	
	
	
	
	MapLocation enemyPastr = null;
	public MapLocation enemyHQLocation = rc.senseEnemyHQLocation();
	GroupUnit[] currentUnits = new GroupUnit[16]; //shouldn't have more than 16 units (there are a maximum of 25 soldiers anyways)
	PastrLocationStruct[] currentPastrs = new PastrLocationStruct[25]; // can't have more than 13 pastrs
	
	public HQRobot(RobotController rc) throws GameActionException {
		super(rc);
		
		spawnRobots();//spawn first robot before everything else
		rc.setIndicatorString(0, "Sensing and broacasting terrain map...");
		terrainMap = senseTerrainMap(rc);
		broadcastTerrainMap(rc.getMapWidth(),rc.getMapHeight());
		System.out.println("Terrain map has been broadcast");
		
		

	}
	

	
	//creates 2D TerrainTile array indicating TerrainTile types of all map points
	private TerrainTile[][] senseTerrainMap(RobotController rc) {
		
		int width = rc.getMapWidth();
		int height = rc.getMapHeight();
		
		TerrainTile[][] fieldGrid = new TerrainTile[width][height];
		
		for (int x = 0; x < width ; x++ ){
			for (int y = 0; y < height; y++){				
				fieldGrid[x][y] =  rc.senseTerrainTile(new MapLocation(x,y));
			}
		}
		
		return fieldGrid;
	}
	
	//encode map terrain and broadcast
	private void broadcastTerrainMap(int width, int height) throws GameActionException{
		int buffer=0;
		int channel=1;
		//System.out.println(terrainMap[0][0].ordinal()+" "+terrainMap[0][0].ordinal()*(Math.pow(2, (((0*(rc.getMapHeight()/2)+0)%16)*2))));
		for (int i = 0; i < width; i++){
			for (int j = 0; j < height/2; j++){
				//System.out.print(i*(rc.getMapHeight()/2)+j+",");
				buffer+=terrainMap[i][j].ordinal()<<((i*(height/2)+j)%15*2);
				//System.out.print(terrainMap[i][j].ordinal());
				if((i*(height/2)+j)%15==14){
					rc.broadcast(channel, buffer);
					//System.out.println("channel: "+channel+" buffer: "+buffer+" i: "+i+" j: "+j);
					buffer=0;
					channel+=1;
				}
			}
			//System.out.println(" ");
		}
		//broadcast last piece of map that didn't completely fill the buffer
		if(buffer!=0){
			rc.broadcast(channel, buffer);
		}
		rc.broadcast(0, 1); //flag that the data has been broadcasted
	}
	
	void reTargetDefenderGroup(int[] messages, GroupUnit unit) {
		
		MapLocation target = null;
		
		int cumX = 0;
		int cumY = 0;
		
		int counter = 0;
		int firstReadyPastr = 0;
		boolean foundFirstReadyPastr = false;
		
		StringBuilder sb = new StringBuilder();
		
		for (int f = 0; f < currentPastrs.length; f++) {
			
			if (currentPastrs[f] != null) {
				PastrStatus status = currentPastrs[f].status;

				sb.append(" " + status + "-" + currentPastrs[f].loc + " ");
				if (!foundFirstReadyPastr) {
					if (status == PastrStatus.READY) {
						foundFirstReadyPastr = true;
						firstReadyPastr = f;
						
					}
				}
				if (status != PastrStatus.UNASSIGNED && status != PastrStatus.READY) {
				cumX += currentPastrs[f].loc.x;
				cumY += currentPastrs[f].loc.y;
				counter++;

				}
			} else {
				sb.append(" null ");
			}
		}
		
		rc.setIndicatorString(2, sb.toString());
		
		if (counter > 0) {
			target = new MapLocation(cumX/counter, cumY/counter);
		} else if (foundFirstReadyPastr) {
			target = currentPastrs[firstReadyPastr].loc;
		} else {
			target = rc.senseHQLocation();
		}
		

		
		
		if (rc.senseTerrainTile(target) == TerrainTile.VOID) {
			target = rc.senseHQLocation();
		}
		
		GroupUnit.setCurrentTarget(target, messages, unit);
		
	}
	
	MapLocation getClosestPastr(MapLocation destroyerLocation) {
		double closest = 10000;
		MapLocation toReturn = null;
		MapLocation[] pastrLocs = rc.sensePastrLocations(rc.getTeam().opponent());
		
		for (int n = 0; n < pastrLocs.length; n++) {
			double distance = calculateDistance(pastrLocs[n],destroyerLocation);
			if (distance < closest) {
				closest = distance;
				toReturn = pastrLocs[n];
			}
		}
		return toReturn;
	}
	
	//GROUPS SECTION: two methods below, manageGroups() and setupNewGroup()
	//make sure there is at least one DEFENDER group
	public void manageGroups() throws GameActionException {
		
		//System.out.println("Now managing groups...");
		int currentTurn = Clock.getRoundNum();
		
		int maxGroupsToCheck = (25 / membersPerGroup) + 1;
		

		int currentChannel = firstGroupChannel;
		boolean isAtLeastOneGroupOpen = false;
		
		for (int n = 0; n < maxGroupsToCheck; n++) {
			
			int[] messages = GroupUnit.readGroupInformation(currentChannel, rc);
			
			if (!isAtLeastOneGroupOpen) {
				isAtLeastOneGroupOpen = GroupUnit.getIsGroupOpen(messages);
			}
			
			
			
			
			SoldierMode status = GroupUnit.getGroupRole(messages);
			
			switch(status) {
			case UNASSIGNED:
				if (!isAtLeastOneGroupOpen) {
					setupNewGroup(rc,n, currentChannel,messages);
				}
				break;
			case GROUPING:
				//turn a grouping group into something else if all members have arrived, or if a certain amount of time has passed
				boolean haveAllMembersArrived = true;
				if (Clock.getRoundNum() >= (currentUnits[n].turnCreated + 300)) {
					System.out.println("Assigned role to group because of group creation timeout");
				}
				
				//300 hardcoded, give a group 300 turns to populate
				if (Clock.getRoundNum() < (currentUnits[n].turnCreated + 300)) {
				
					for (int i = 0; i < membersPerGroup; i++) {
						if(!GroupUnit.getHasArrived(i, messages)){
							haveAllMembersArrived = false;
							//System.out.println("Member " + i + " has not arrived yet");
							break;
						}
					}
					if (haveAllMembersArrived) {

						System.out.println("Assigned role to group because entire group arrived");
					}
				
				}
				
				//assign as a DEFENDER or DESTROYER
				//for now, make sure there are enough DEFENDER soldiers for the given number of pastrs
				if (haveAllMembersArrived) {
					int defenders = 0;
					int destroyers = 0;
					for (int y = 0; y < currentUnits.length; y++) {
						if (currentUnits[y] != null) {
							if (currentUnits[y].groupRole == SoldierMode.DEFENDER) {
								defenders++;
							}
							if (currentUnits[y].groupRole == SoldierMode.DESTROYER) {
								destroyers++;
							}
							
							
						}
						
					}
					
					if (defenders < getLeastNumberGroups((rc.sensePastrLocations(rc.getTeam()).length) * 2) + 5 ) {
						GroupUnit.setGroupRole(SoldierMode.DEFENDER, messages, currentUnits[n]);
						//TODO: fix this so it roams around areas that are not being roamed by other DEFENDER groups
						//basically you're just sending group to roam around area between all pastrs
						reTargetDefenderGroup(messages, currentUnits[n]);
						
					} else {
						
						
						
						GroupUnit.setGroupRole(SoldierMode.DESTROYER,messages,currentUnits[n]);
						//TODO: fix this so it doesn't just target the first pastr in the array
						MapLocation target = getClosestPastr(currentUnits[n].currentTarget);
						if (target == null)
							target = rc.senseHQLocation();
						
						GroupUnit.setCurrentTarget(target, messages, currentUnits[n]);
						
					
					}
					
					GroupUnit.writeGroupInformation(currentChannel, messages, rc);
				}
				
				
				break;
			case DEFENDER:
				//if Defender group is not in a mission, reposition it to where it should be
				if (!currentUnits[n].inMission) {
					reTargetDefenderGroup(messages,currentUnits[n]);
				}
				
				GroupUnit.writeGroupInformation(currentChannel, messages, rc);
				
				
				break;
			case DESTROYER:
				//TODO: 
				//check to see if there is an enemy pastr at target location. If not, assign it to the next best enemy pastr.
				
				boolean isTargetAnEnemyPastr = false;
				MapLocation[] enemyPastrs = rc.sensePastrLocations(rc.getTeam().opponent());
				MapLocation currentTarget = currentUnits[n].currentTarget;
				double closest = 10000;
				MapLocation targetToSet = null;
				//if (currentTarget != null) {
					
					for (int i = 0; i < enemyPastrs.length; i++) {
						double distance = calculateDistance(currentTarget,enemyPastrs[i]);
						if (distance < closest) {
							closest = distance;
							targetToSet = enemyPastrs[i];
						}
						
						
						if (enemyPastrs[i].equals(currentTarget)  ) {
							isTargetAnEnemyPastr = true;
							break;
						}
					}
					
					
				//}
				
				
				
				if (!isTargetAnEnemyPastr) {
					//MapLocation targetToSet = null;
					//if (enemyPastrs.length > 0)
						//targetToSet = enemyPastrs[0];
					if (targetToSet == null) {
						targetToSet = rc.senseHQLocation();
					}
					
					
					rc.setIndicatorString(2, "target to set will be " + targetToSet);
					//GroupUnit.setCurrentTarget(targetToSet, messages, currentUnits[n]);
					System.out.println("LOOK HERE!!!!!!!! Target to set is " + targetToSet.x + ", " + targetToSet.y);
					GroupUnit.setCurrentTarget(targetToSet, messages, currentUnits[n]);
					GroupUnit.writeGroupInformation(currentChannel, messages, rc);
				}
				
				
				
				break;
				default:
					System.out.println("Um...group had no recognizable role in HQRobot.manageGroups()");
			
			}
			

			
			//iterate to next group channel
			currentChannel += (3 + membersPerGroup);
		}
		
	}
	
	//This will set up the new group, assigning it the role of 'GROUPING' with a rally point
	//it is reponsiblity of 'manageGroups()' to determine what to do with group once it has rallied
	private boolean setupNewGroup(RobotController rc, int slot, int groupChannel, int[] messages) throws GameActionException {

		
		GroupUnit newUnit = new GroupUnit(membersPerGroup);
		newUnit.groupChannel = groupChannel;
		currentUnits[slot] = newUnit;
		
		messages[0] = 0;
		messages[1] = 0;
		messages[2] = 0;
		
		//rally robots around first pastr location. If pastr location has not been determined, rally it
		//around the HQ.
		int pastrLoc = rc.readBroadcast(pastrComStart);
		MapLocation loc = PastrRobot.channelGetLocation(pastrLoc);
		if (loc.x == 0 && loc.y == 0) {
			
			loc = rc.senseHQLocation();
		}
		
		//loc = new MapLocation(31,5);
		
		messages = GroupUnit.setGroupRole(SoldierMode.GROUPING, messages, newUnit);
		System.out.println("Created new group " + GroupUnit.getGroupRole(messages));
		messages = GroupUnit.setCurrentTarget(loc, messages, newUnit);
		messages = GroupUnit.setIsGroupOpen(messages, true);
		newUnit.turnCreated = Clock.getRoundNum();
		GroupUnit.writeGroupInformation(groupChannel, messages, rc);
		
		

		
		
		
		return true;
	}
	

	
	

	
	
	//TODO: add functionatlity, such as cases when there are too many enemies around the destroyed pastr
	public void dealWithPastrDeath(int channel, int message) throws GameActionException {
		
		message = PastrRobot.channelSetPastrStatus(PastrStatus.READY, message);
		rc.broadcast(channel, message);
		
	}
	
	
	public void managePastrs(RobotController rc, int currentTurn) throws GameActionException {
		
		System.out.println("Now Managing Pastrs...");
		
		//check for first Pastr that is unassigned
		int currentChannel = pastrComStart;
		
		//here, 12 is hard-coded because there can never be more than 13 pastrs in the game
		for (int m = 0; m < 12; m++) {
			

			
			
			int message = rc.readBroadcast(currentChannel);
			//System.out.println("Checking channel " + currentChannel + " where code is " + Integer.toBinaryString(channelInt));
			//get code
			PastrStatus status = PastrRobot.channelGetPastrStatus(message);
			
			//make sure currentPastrs array is filled up with pastrs found by first soldier
			if (currentPastrs[m] == null) {
				if (status != PastrStatus.UNASSIGNED) {
				currentPastrs[m] = new PastrLocationStruct();
				currentPastrs[m].loc = PastrRobot.channelGetLocation(message);
				System.out.println("setting location of null pastr: " + PastrRobot.channelGetLocation(message) + " at channel " + currentChannel);
				currentPastrs[m].status = status;
				}
			}
			
			
			switch (status) {
			case UNASSIGNED:
				
				//System.out.println("Channel " + currentChannel + " has not been assigned by robot yet.");
				
				
				break;
			case READY:
				
				//nothing needs to be done
				
				break;
				
			case BUILDING:
				int lastHeartbeat = PastrRobot.channelGetTurn(message);
				
				//if robot is dead, set up that spot to be ready for a new pastr. Note: the function readyNewPastr()
				//calls rc.broadcast().
				if (isConstructingPastrRobotDead(Clock.getRoundNum(),lastHeartbeat)) {
					
					dealWithPastrDeath(currentChannel,message);
					
				}
				
				
				break;
			case ATTACKED:
			case EMERGENCY:
				
				//TODO: HQ is not fast enough for this, implement this behavior in each group
				//System.out.println("Identified Pastr in state of emergency!!! in channel " + currentChannel + " message is " + Integer.toBinaryString(message));
				//declare pastr dead if there has been no recent heartbeat
				int lastTurn = PastrRobot.channelGetTurn(message);
				if (isRobotDead(Clock.getRoundNum(),lastTurn)) {
					dealWithPastrDeath(currentChannel,message);
				}
				
				
				//if pastr is under attack, get a defense unit to aid it
				if (currentPastrs[m].getDefenderCount() < 0) {
					
					for (int n = 0; n < currentUnits.length; n++) {
						if (currentUnits[n] != null) {
							if (currentUnits[n].groupRole == SoldierMode.DEFENDER) {
								//only if it's a assign a group to aid if the group was doing nothing
								if (currentUnits[n].inMission == false) {
									
									int[] messages = GroupUnit.readGroupInformation(currentUnits[n].groupChannel, rc);
									GroupUnit.setCurrentTarget(currentPastrs[m].loc, messages, currentUnits[n]);
									GroupUnit.writeGroupInformation(currentUnits[n].groupChannel, messages, rc);
									
									currentUnits[n].inMission = true;
									currentPastrs[m].addDefender(currentUnits[n]);
									break;
								}
							}
						}
					}
					
					
				}
				
				
				break;
			case SETTLING:
			case HEALTHY:
				//declare pastr dead if there has been no recent heartbeat
				int lastTurnHealthy = PastrRobot.channelGetTurn(message);
				if (isRobotDead(Clock.getRoundNum(),lastTurnHealthy)) {
					dealWithPastrDeath(currentChannel,message);
				}
				
				//if pastr is no longer in danger, free its defender group
				if (currentPastrs[m].getDefenderCount() > 0) {
					for (int n = 0; n < currentPastrs[m].defenderUnits.length; n++) {
						if (currentPastrs[m].defenderUnits[n] != null) {
							
							//release each unit
							GroupUnit eachUnit = currentPastrs[m].defenderUnits[n];
							int[] messages = GroupUnit.readGroupInformation(eachUnit.groupChannel, rc);
							GroupUnit.clearTargetingInformation(rc, messages, eachUnit);
							eachUnit.inMission = false;
							reTargetDefenderGroup(messages,eachUnit);
							GroupUnit.writeGroupInformation(eachUnit.groupChannel, messages, rc);
							
							//declare that the unit is no longer defending the pastr
							currentPastrs[m].defenderUnits[n] = null;

						}
					}
					
				}
				
				
				
				break;
				
				
			}
			currentChannel += 2;
		}
		
	}
	
	
	
	
	//Check if a robot is spawnable and spawn one if it is
	public void spawnRobots() throws GameActionException{
		if(rc.senseRobotCount() < 25) {
			Direction spawnDirection = rc.getLocation().directionTo(enemyHQLocation);
			for(int i=0;i<8;i++){
				spawnDirection=directions[(spawnDirection.ordinal()+1)%8];
				//senseObject uses less byte code than canMove
				if(rc.senseObjectAtLocation(rc.getLocation().add(spawnDirection)) == null&&rc.senseTerrainTile((rc.getLocation().add(spawnDirection))).ordinal()<=1){
					rc.spawn(spawnDirection);
					break;
				}
			}
		}
	}
	
	//attack nearby robots, use splash damage to attack robots that are slightly out of range
	public void attack(Robot[] nearbyEnemies) throws GameActionException{
		if (nearbyEnemies.length > 0) {
			rc.setIndicatorString(0,"yes");
			RobotInfo robotInfo = rc.senseRobotInfo(nearbyEnemies[0]);
			MapLocation attackLocation = robotInfo.location;
			if(Math.abs(attackLocation.x-rc.getLocation().x)!=5&&Math.abs(attackLocation.y-rc.getLocation().y)!=5){
				//splash damage attack (probably an easier way, but this works)
				if(Math.abs(attackLocation.x-rc.getLocation().x)==3&&Math.abs(attackLocation.y-rc.getLocation().y)==3){
					attackLocation=new MapLocation(attackLocation.x-(attackLocation.x-rc.getLocation().x)/3,attackLocation.y-(attackLocation.y-rc.getLocation().y)/3);
				}
				if(Math.abs(attackLocation.x-rc.getLocation().x)>3&&Math.abs(attackLocation.y-rc.getLocation().y)==3){
					attackLocation=new MapLocation(attackLocation.x-(attackLocation.x-rc.getLocation().x)/3,attackLocation.y-(attackLocation.y-rc.getLocation().y)/3);
				}
				else if(Math.abs(attackLocation.x-rc.getLocation().x)>3&&Math.abs(attackLocation.y-rc.getLocation().y)!=3){
					attackLocation=new MapLocation(attackLocation.x-(attackLocation.x-rc.getLocation().x)/3,attackLocation.y);
				}
				if(Math.abs(attackLocation.y-rc.getLocation().y)>3&&Math.abs(attackLocation.x-rc.getLocation().x)==3){
					attackLocation=new MapLocation(attackLocation.x-(attackLocation.x-rc.getLocation().x)/3,attackLocation.y-(attackLocation.y-rc.getLocation().y)/3);
				}
				else if(Math.abs(attackLocation.y-rc.getLocation().y)>3&&Math.abs(attackLocation.x-rc.getLocation().x)!=3){
					attackLocation=new MapLocation(attackLocation.x,attackLocation.y-(attackLocation.y-rc.getLocation().y)/3);
				}
				rc.attackSquare(attackLocation);
			}
		}
	}
	
	public void run() throws GameActionException {		
		
		//if rc is active, spawn and attack
		if (rc.isActive()){
			
			rc.setIndicatorString(0, "Attacking and spawning...");
			Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class,25,rc.getTeam().opponent());
			attack(nearbyEnemies);
			if(rc.isActive()){
				spawnRobots();
			}
			
			//if these functions start to malfunction (pun intended), we'll have to split up these three roles
			//of the hq via the 'management counter'
			rc.setIndicatorString(0, "managing Pastrs...");
			managePastrs(rc,Clock.getRoundNum());
			
			rc.setIndicatorString(0, "managing Groups...");
			manageGroups();
			
			
			
			
			/*
			MapLocation[] locs =	rc.sensePastrLocations(rc.getTeam().opponent());
			if (locs.length > 0) {
			
			if (enemyPastr == null) {
				
				
					
				enemyPastr = locs[0];
				
				for (GroupUnit group : currentUnits) {
					if (group != null) {
					if (group.groupRole == SoldierMode.DESTROYER) {
						int[] messages = new int[2];
						messages[0] = rc.readBroadcast(group.groupChannel);
						messages[1] = rc.readBroadcast(group.groupChannel + 1);
						
						messages = GroupUnit.setCurrentTarget(locs[0], messages, group);
						
						rc.broadcast(group.groupChannel, messages[0]);
						rc.broadcast(group.groupChannel + 1, messages[1]);
						
					}
					}
					
					
					
				}
				
				
				
				} else {
					boolean containsEnemyPastr = false;
					
					for (MapLocation eachLoc: locs) {
						if (eachLoc.equals(enemyPastr)) {
							containsEnemyPastr = true;
							break;
						}
					}
					
					if (!containsEnemyPastr) {
						enemyPastr = locs[0];
						
						for (GroupUnit group : currentUnits) {
							if (group != null) {
							if (group.groupRole == SoldierMode.DESTROYER) {
								int[] messages = new int[2];
								messages[0] = rc.readBroadcast(group.groupChannel);
								messages[1] = rc.readBroadcast(group.groupChannel + 1);
								
								messages = GroupUnit.setCurrentTarget(locs[0], messages, group);
								
								rc.broadcast(group.groupChannel, messages[0]);
								rc.broadcast(group.groupChannel + 1, messages[1]);
								
							}
							}
							
							
							
						}
						
					}
					
					
				}
			
			
		}*/
			
			
			
			//pathing
//			MapLocation startPoint = rc.getLocation();
//			System.out.println("x: "+startPoint.x+" y: "+startPoint.y);
//			int bigBoxSize = 5;
//			BreadthFirst.init(rc, bigBoxSize);
//			Comms.findPathAndBroadcast(2,startPoint,new MapLocation(10,10),bigBoxSize ,2);
		
		}
		
		//regardless of whether it is active or not, manage groups and pastrs
		
		
		
		
	}
}
