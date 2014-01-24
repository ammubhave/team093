package team093;

import team093.BreadthFirst;
import team093.Comms;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.TerrainTile;

public class HQRobot extends BaseRobot{
	
	public MapLocation enemyHQLocation = rc.senseEnemyHQLocation();

	public HQRobot(RobotController rc) throws GameActionException {
		super(rc);
		spawnRobots();//spawn first robot before everything else
		terrainMap = senseTerrainMap(rc);
		broadcastTerrainMap(rc.getMapWidth(),rc.getMapHeight());
	}
	
	//creates 2D TerrainTile array indicating TerrainTile types of all map points
	private TerrainTile[][] senseTerrainMap(RobotController rc) {
		
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
	
	//Check if a robot is spawnable and spawn one if it is
	public void spawnRobots() throws GameActionException{
		if(senseActualRobotCount(rc) < 25) {
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
		
		if (rc.isActive()){
			Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class,25,rc.getTeam().opponent());
			attack(nearbyEnemies);
			if(rc.isActive()){
				spawnRobots();
			}
			//pathing
//			MapLocation startPoint = rc.getLocation();
//			System.out.println("x: "+startPoint.x+" y: "+startPoint.y);
//			int bigBoxSize = 5;
//			BreadthFirst.init(rc, bigBoxSize);
//			Comms.findPathAndBroadcast(2,startPoint,new MapLocation(10,10),bigBoxSize ,2);
		}
	}
}
