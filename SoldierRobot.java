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
	private AStarPathFinder finder;
	int i=1;
	private double[][] cowGrowth;
	static Random randall = new Random();
	
	public SoldierRobot(RobotController rc) throws GameActionException {
		super(rc);
		randall.setSeed(rc.getRobot().getID());
		this.cowGrowth = rc.senseCowGrowth();
		//readTerrain(rc.getMapWidth(),rc.getMapHeight());
	}
	
	private boolean isMapReady() throws GameActionException {
		if(rc.readBroadcast(0)==1){
			return true;
		}
		return false;
	}
	
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
	
	public MapLocation goodPastrLocation(){
		//width, then height
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

	
	public void run() throws GameActionException {
		//if the map is ready download it
		if(gotMap==false&&isMapReady()==true){
			readTerrain(rc.getMapWidth(),rc.getMapHeight());
			gotMap=true;
			map = new GameMap(terrainMap,rc.getMapWidth(),rc.getMapHeight());
			/** The path finder we'll use to search our map */
			finder = new AStarPathFinder(map, 500, true);
		}
		if(rc.isActive() && gotMap==true){
//			enemyHQ = rc.senseEnemyHQLocation();
//			Robot[] alliedRobots = rc.senseNearbyGameObjects(Robot.class,100000000,rc.getTeam());
//			navigateByPath(alliedRobots);
			Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class,10,rc.getTeam().opponent());
			if (nearbyEnemies.length > 0) {
				RobotInfo robotInfo = rc.senseRobotInfo(nearbyEnemies[0]);
				rc.attackSquare(robotInfo.location);
			}
			else{
				if(i==1){
					MapLocation start = rc.getLocation();
					MapLocation end = goodPastrLocation();
					path1 = finder.findPath(start.x,start.y, end.x,end.y);
				}
				Robot[] nearbyRobot2 = rc.senseNearbyGameObjects(Robot.class,5,rc.getTeam());
				if(i<path1.getLength()){
					//System.out.println("x: "+path1.getStep(i).getX()+" y: "+path1.getStep(i).getY());
					Direction direction = rc.getLocation().directionTo(new MapLocation(path1.getStep(i).getX(),path1.getStep(i).getY()));
					BasicPathing.tryToMove(direction, true,true,false);
					i++;
				}
				else if(i<path1.getLength()&&nearbyRobot2.length<1&&i>=3){
					i--;
					Direction direction = rc.getLocation().directionTo(new MapLocation(path1.getStep(i).getX(),path1.getStep(i).getY()));
					BasicPathing.tryToMove(direction, true,true,false);
				}
				else if(i>=path1.getLength()){
					roam(rc, 4, new MapLocation(path1.getStep(i-1).getX(),path1.getStep(i-1).getY()));
				}
				
				Robot[] nearbyRobot = rc.senseNearbyGameObjects(Robot.class,2,rc.getTeam());
				Robot[] senseAllRobots = rc.senseNearbyGameObjects(Robot.class,1000,rc.getTeam());
				MapLocation[] nearbyPastr = rc.sensePastrLocations(rc.getTeam());
				if(i==path1.getLength()&&rc.isActive()&&nearbyRobot.length<3&&nearbyPastr.length<2){
					rc.construct(RobotType.PASTR);
				}
				else if(i==path1.getLength()&&rc.isActive()&&nearbyRobot.length==1){
					rc.construct(RobotType.NOISETOWER);
				}
			}
		}
	}
	
}
