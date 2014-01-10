package team093;

import battlecode.common.*;

import java.util.*;

public class HQRobot extends BaseRobot {
	public HQRobot(RobotController rc) throws GameActionException {
		super(rc);
		
		//System.out.print("Begin init");
		//populate 2D arrays with map information
		//booleanMap = senseBooleanMap(rc);
		
		terrainMap = senseTerrainMap(rc);
		broadcastTerrainMap(rc.getMapWidth(),rc.getMapHeight());
	}
	
	private void broadcastTerrainMap(int width, int height) throws GameActionException{
		//encode map terrain and broadcast
		int buffer=0;
		int channel=0;
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
	}
	
	@Override
	public void run() throws GameActionException {

		//Check if a robot is spawnable and spawn one if it is
		if (rc.isActive()){
			//spawn more robots
			if(rc.senseRobotCount() < 2) {

				Direction toEnemy = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
				if (rc.senseObjectAtLocation(rc.getLocation().add(toEnemy)) == null) {
					rc.spawn(toEnemy);
				}
			}
			Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class,15,rc.getTeam().opponent());
			if (nearbyEnemies.length > 0) {
				RobotInfo robotInfo = rc.senseRobotInfo(nearbyEnemies[0]);
				rc.attackSquare(robotInfo.location);
			}
		}
		
	}
}