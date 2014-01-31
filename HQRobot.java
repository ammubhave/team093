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
	
	//GROUPS SECTION: two methods below, manageGroups() and setupNewGroup()
	public void manageGroups() throws GameActionException {
		
		//System.out.println("Now managing groups...");
		
		int maxGroupsToCheck = (25 / membersPerGroup) + 1;
		
		int[] messages = new int[2];
		int currentChannel = firstGroupChannel;
		boolean isAtLeastOneGroupReady = false;
		
		for (int n = 0; n < maxGroupsToCheck; n++) {
			
			messages[0] = rc.readBroadcast(currentChannel);
			messages[1] = rc.readBroadcast(currentChannel + 1);
			
			//System.out.println("HQ is checking group channel " + currentChannel + " and found a message: " + Integer.toBinaryString(messages[0]));
			
			//make sure at least one group is ready to accept members
			if (!isAtLeastOneGroupReady) {
			
				if (!GroupUnit.getIsGroupOpen(messages) ) {
						isAtLeastOneGroupReady = true;
						setupNewGroup(rc,currentChannel,messages);
					
				} else {
					if (!GroupUnit.getIsFull(messages)) {
						isAtLeastOneGroupReady = true;
					}
				}
			
			}
			
			//TODO: do make sure all members of each group are alive
			
			//TODO: do manage groups
			
			//iterate to next group channel
			currentChannel += (2 + membersPerGroup);
		}
		
	}
	
	//This functions opens up a new group. It returns true if a group was formed, false if it wasn't
	private boolean setupNewGroup(RobotController rc, int groupChannel, int[] messages) throws GameActionException {

		
		GroupUnit newUnit = new GroupUnit(membersPerGroup);
		newUnit.groupChannel = groupChannel;
		
		int maxGroupsToCheck = (25 / membersPerGroup) + 1;
		
		//find first empty slot in currentUnits and assign Unit to it
		for (int n = 0; n < maxGroupsToCheck; n++) {
			if (currentUnits[n] == null) {
				currentUnits[n] = newUnit;
				//System.out.println("setting this new group to be number " + n + " in the currentUnits array.");
				break;
			}
		}
		
		
		//gets data for that group and clear it, in case last owner left anything
		messages[0] = 0;
		messages[1] = 0;
		
		//STRATEGY: we will iterate through each pastr, as saved variables, to ensure each has at least one defender unit
		
		int pass = 1;
		
		while(pass < 2) {
		
			for (int n = 0; n < currentPastrs.length; n++) {
				//System.out.println("Assigning mission to group " + groupChannel + ", checking currentPastrs[n] " + n + " at pass " + pass);
				
				if (currentPastrs[n] != null) {
					
					System.out.println("found a pastr, it has " + currentPastrs[n].getDefenderCount() + " defenders ");
					
					//for each pass, try to find a pastr that has less than that amount of defenders
					if (currentPastrs[n].getDefenderCount() < pass) {
						
						//add the unit as the defender
						currentPastrs[n].addDefender(newUnit);
						
						
						messages = GroupUnit.setGroupRole(SoldierMode.DEFENDER, messages, newUnit);
						
						messages = GroupUnit.setCurrentTarget(currentPastrs[n].loc, messages, newUnit);
						
						messages = GroupUnit.setIsGroupOpen(messages, true);
						rc.setIndicatorString(2, "Setting Group " + groupChannel + " to location " + GroupUnit.getCurrentTarget(messages).toString());
						GroupUnit.writeGroupInformation(groupChannel, messages, rc);
						
						
						return true;
						
					}
				}
			}
		
			pass++;
		}
		
		
		if (enemyPastr == null) {
		MapLocation[] locs =	rc.sensePastrLocations(rc.getTeam().opponent());
		
		if (locs.length > 0) {
			
		//just as a test
		messages = GroupUnit.setGroupRole(SoldierMode.DESTROYER, messages, newUnit);
		messages = GroupUnit.setCurrentTarget(locs[0], messages, newUnit);
		messages = GroupUnit.setIsGroupOpen(messages, true);
		rc.broadcast(groupChannel, messages[0]);
		rc.broadcast(groupChannel + 1, messages[1]);
		enemyPastr = locs[0];
		return true;
		}
		} else {
			messages = GroupUnit.setGroupRole(SoldierMode.DESTROYER, messages, newUnit);
			messages = GroupUnit.setCurrentTarget(enemyPastr, messages, newUnit);
			messages = GroupUnit.setIsGroupOpen(messages, true);
			rc.broadcast(groupChannel, messages[0]);
			rc.broadcast(groupChannel + 1, messages[1]);
			return true;
		}
		
		
		//as code is designed, you really shouldn't get to this point
		
		//System.out.println("This should not have happened");
		
		
		
		return false;
	}
	

	
	
	/*PastrLocationStruct getPotentialPastrLocation(double maxDistance, int minImmediate, RobotController rc) {
		
		MapLocation[] currentFriendlyPastrLocations = rc.sensePastrLocations(rc.getTeam());
		MapLocation[] currentEnemyPastrLocations = rc.sensePastrLocations(rc.getTeam().opponent());
		
		PastrLocationStruct toReturn = null;
		double maxAverage = 0;
		
		for (int n = 0; n < pastrArrayCount; n ++) {
			
			//first, make sure location is within parameters
			if (pastrCandidates[n].distanceToHQ < maxDistance && pastrCandidates[n].fertilityGrowth >= minImmediate  ) {
				if (pastrCandidates[n].averageCowGrowth > maxAverage) {
					
					
					//Now, check if location is too close to other Pastrs
					MapLocation potentialLoc = pastrCandidates[n].loc;
					boolean isTooCloseToAnotherPastr = false;
					
					//CONSTANT: setting distance from friendly pastrs at 5
					for (MapLocation eachLoc: currentFriendlyPastrLocations) {
						if ( Math.abs(potentialLoc.x - eachLoc.x) < 5 || Math.abs(potentialLoc.y - eachLoc.y) <5   ) {
							isTooCloseToAnotherPastr = true;
							break;
						}
					}
					
					//CONSTANT: setting distance from enemy pastrs at 6
					for (MapLocation eachLoc: currentEnemyPastrLocations) {
						if ( Math.abs(potentialLoc.x - eachLoc.x) < 6 || Math.abs(potentialLoc.y - eachLoc.y) < 6   ) {
							isTooCloseToAnotherPastr = true;
							break;
						}
					}
					
						
						if (!isTooCloseToAnotherPastr) {
							
							boolean alreadyBeingSettled = false;
							
							//check to see if any other SETTLER groups are already on their way
							/*for (int m = 0; m < 16; m++) {
								//System.out.println("checking to see if index space " + m + " is on its way to settle current spot");
								if (currentUnits[m] != null) {
									if (currentUnits[m].groupRole == SoldierMode.SETTLER) {
										//System.out.println("found that group " + m + " was on its way to settle something");
										if (currentUnits[m].currentTarget == null) {
											//System.out.println("What happened? why does a team with SETTLER have no target?");
										}
										else {
										if (calculateDistance(currentUnits[m].currentTarget,pastrCandidates[n].loc ) < 10  ) {
											alreadyBeingSettled = true;
											break;
										}
										}
									}
									
								}
							}*/
							/*
							//check to see if any other pastrs structs have already been saved with this location
							for (int m = 0; m < 25; m++) {
								if (currentPastrs[m] != null) {
									if (calculateDistance(currentPastrs[m].loc,pastrCandidates[n].loc  ) < 10) {
										alreadyBeingSettled = true;
										break;
									}
								}
							}
							
							if (!alreadyBeingSettled) {
								toReturn = pastrCandidates[n];
								maxAverage = pastrCandidates[n].averageCowGrowth;
							}
						
						
						}
				}
			}
			
		}
		
		//Remember, if no pastr is found within parameters, null MapLocation is returned
		return toReturn;
	}*/
	
	/*
	public void readyNewPastr(RobotController rc, int currentChannel) throws GameActionException {
		
		//first, search through pastr channels to nullify any pastr that already had currentChannel
		//it is the reponsiblity of the calling function to make sure that if a pastr already had currentChannel, it
		//is actually dead!
		int arrayIndexToReplace = -1;
		
		for (int n = 0; n < currentPastrs.length; n++) {
			if (currentPastrs[n] != null && currentPastrs[n].channel == currentChannel) {
				currentPastrs[n] = null;
				arrayIndexToReplace = n;
				break;
			}
		}
		
		
		int message = 0;
		
		
		//get target
		PastrLocationStruct target = getPotentialPastrLocation(20,20,rc);
		if (target == null)
			target = getPotentialPastrLocation(40,15,rc);
		if (target==null)
			target = getPotentialPastrLocation(120,0,rc);
		//TODO: think about what happens if you can't get any more pastr locations
		if (target == null) {
			target = new PastrLocationStruct();
			target.loc = new MapLocation (0,0);
					System.out.println("!!!!!!!!WARNING: could not find pastr location!");
		}
			
		
		//set target
		message = PastrRobot.channelSetLocation(target.loc, message);
		
		//set status
		message = PastrRobot.channelSetPastrStatus(PastrStatus.READY, message);
		target.status = PastrStatus.READY;
		target.channel = currentChannel;
		
		rc.broadcast(currentChannel, message); //broadcast
		
		if (arrayIndexToReplace == -1) {
			
			//if you're not replacing anything, find the first available slot
			for (int n = 0; n < 25; n++) {
				if (currentPastrs[n] == null) {
					currentPastrs[n] = target;
					break;
				}
			}
			
			
		} else {
			currentPastrs[arrayIndexToReplace] = target;
		}
		
		
		
	}*/
	
	
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
				currentPastrs[m] = new PastrLocationStruct();
				currentPastrs[m].loc = PastrRobot.channelGetLocation(message);
				
			}
			
			currentPastrs[m].status = status;
			
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
			case SETTLING:
			case HEALTHY:
			case ATTACKED:
			case EMERGENCY:
			case DOOMED:

				int lastTurn = PastrRobot.channelGetTurn(message);
				
				//if robot hasn't beat his heart in a while, remove him
				if (isRobotDead(Clock.getRoundNum(),lastTurn)) {
					
					
					//TODO: declare it as Doomed?
					dealWithPastrDeath(currentChannel,message);
					
					
					
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
