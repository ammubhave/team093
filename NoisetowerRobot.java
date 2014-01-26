package team093;


import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class NoiseTowerRobot extends BaseRobot{

	//location of pastr Noise Tower is serving...
	MapLocation pastrLoc = null;
	
	//heartbeat and communication channel
	int heartbeatChannel = 0;
	int lastHeartbeat = 0;
	
	
	private MapLocation myLocation=rc.getLocation();
	private int rootRange=17;
	private int range2=12;
	private int rootPastrRange=4;//(int)(Math.pow(GameConstants.PASTR_RANGE, 0.5));
	private int minX=Math.max(myLocation.x-rootRange, 0);
	private int maxX=Math.min(myLocation.x+rootRange,rc.getMapWidth());
	private int minY=Math.max(myLocation.y-rootRange, 0);
	private int maxY=Math.min(myLocation.y+rootRange, rc.getMapHeight());
	private int minX2=Math.max(myLocation.x-range2, 0);
	private int maxX2=Math.min(myLocation.x+range2,rc.getMapWidth());
	private int minY2=Math.max(myLocation.y-range2, 0);
	private int maxY2=Math.min(myLocation.y+range2, rc.getMapHeight());
	private MapLocation attackLocation;
	int q=0;

	public NoiseTowerRobot(RobotController rc) throws GameActionException {
		super(rc);
	}

	public MapLocation getClosestPastr(){
		MapLocation pastrLocations[] = rc.sensePastrLocations(rc.getTeam());
		int closest = -1;
		double minDist = -1;
		double temp=0;
		for(int x=0;x<pastrLocations.length;x++){
			temp=myLocation.distanceSquaredTo(pastrLocations[x]);
			if(temp>minDist){
				minDist=temp;
				closest=x;
			}
		}
		if(closest==-1){
			return null;
		}
		else{
			if (q==0){
				attackLocation=new MapLocation(pastrLocations[closest].x,minY);
				/*for (int i=minY; i<pastrLocations[closest].y-rootPastrRange; i++){
					if(terrainMap[5][5].ordinal()==2){
					//if (terrainMap[pastrLocations[closest].x][i].ordinal()==2){
						minY=i;
					}
				}
				for (int i=maxX; i>pastrLocations[closest].x+rootPastrRange; i--){
					if (terrainMap[i][pastrLocations[closest].y].ordinal()==2){
						maxX=i;
					}
				}
				for (int i=maxY; i>pastrLocations[closest].y+rootPastrRange; i--){
					if (terrainMap[pastrLocations[closest].x][i].ordinal()==2){
						maxY=i;
					}
				}
				for (int i=minX; i<pastrLocations[closest].x-rootPastrRange; i++){
					if (terrainMap[i][pastrLocations[closest].y].ordinal()==2){
						minX=i;
					}
				}*/
				q=1;
			}
			return pastrLocations[closest];
		}
	}

	private void herd(MapLocation closestPastr) throws GameActionException {
		//herding code here
		if (attackLocation.x==closestPastr.x){
			if (attackLocation.y<closestPastr.y){
				if (attackLocation.y+1<closestPastr.y-rootPastrRange){
					attackLocation=new MapLocation(attackLocation.x,attackLocation.y+1);
				}
				else{
					attackLocation=new MapLocation(closestPastr.x+Math.min(maxX2-closestPastr.x, closestPastr.y-minY2),closestPastr.y-Math.min(maxX2-closestPastr.x, closestPastr.y-minY2));
				}
			}
			else{
				if (attackLocation.y-1>closestPastr.y+rootPastrRange){
					attackLocation=new MapLocation(attackLocation.x,attackLocation.y-1);
				}
				else{
					attackLocation=new MapLocation(closestPastr.x-Math.min(closestPastr.x-minX2, maxY2-closestPastr.y),closestPastr.y+Math.min(closestPastr.x-minX2, maxY2-closestPastr.y));
				}
			}
		}
		else if (attackLocation.y==closestPastr.y){
			if (attackLocation.x<closestPastr.x){
				if (attackLocation.x+1<closestPastr.x-rootPastrRange){
					attackLocation=new MapLocation(attackLocation.x+1,attackLocation.y);
				}
				else{
					attackLocation=new MapLocation(closestPastr.x-Math.min(closestPastr.x-minX2, closestPastr.y-minY2),closestPastr.y-Math.min(closestPastr.x-minX2, closestPastr.y-minY2));
				}
			}
			else{
				if (attackLocation.x-1>closestPastr.x+rootPastrRange){
					attackLocation=new MapLocation(attackLocation.x-1,attackLocation.y);
				}
				else{
					attackLocation=new MapLocation(closestPastr.x+Math.min(maxX2-closestPastr.x, maxY2-closestPastr.y),closestPastr.y+Math.min(maxX2-closestPastr.x, maxY2-closestPastr.y));
				}
			}
		}
		else if (attackLocation.y-closestPastr.y==attackLocation.x-closestPastr.x){
			if (attackLocation.x<closestPastr.x){
				if (attackLocation.x+1<closestPastr.x-rootPastrRange){
					attackLocation=new MapLocation(attackLocation.x+1,attackLocation.y+1);
				}
				else{
					attackLocation=new MapLocation(closestPastr.x,minY);
				}
			}
			else{
				if (attackLocation.x-1>closestPastr.x+rootPastrRange){
					attackLocation=new MapLocation(attackLocation.x-1,attackLocation.y-1);
				}
				else{
					attackLocation=new MapLocation(closestPastr.x,maxY);
				}
			}
		}
		else{
			if (attackLocation.x<closestPastr.x){
				if (attackLocation.x+1<closestPastr.x-rootPastrRange){
					attackLocation=new MapLocation(attackLocation.x+1,attackLocation.y-1);
				}
				else{
					attackLocation=new MapLocation(minX,closestPastr.y);
				}
			}
			else{
				if (attackLocation.x-1>closestPastr.x+rootPastrRange){
					attackLocation=new MapLocation(attackLocation.x-1,attackLocation.y+1);
				}
				else{
					attackLocation=new MapLocation(maxX,closestPastr.y);
				}
			}
		}
		//TODO: if you remove this, Noise Tower explodes in blocks map...not sure why...
		if (rc.canAttackSquare(attackLocation)) {
			rc.attackSquare(attackLocation);
		}
	}
	
	//heartbeat function, it may be possible to remove this by making some changes, let's discuss
	public void theBeatOfMyHeart() throws GameActionException {
		if (Clock.getRoundNum() - lastHeartbeat > (declareDeadInterval)) {
			lastHeartbeat = Clock.getRoundNum();
			rc.broadcast(heartbeatChannel, Clock.getRoundNum());
		}
	}
	

	public void run() throws GameActionException {
		
		//before doing anything else, makes sure there is a pastr close to the NoiseTower...
		if (pastrLoc == null) {
			Robot[] surroundingRobots = rc.senseNearbyGameObjects(Robot.class, 3, rc.getTeam());
			
			for (Robot each : surroundingRobots) {
				RobotInfo info = rc.senseRobotInfo(each);
				if (info.type == RobotType.PASTR) {
					pastrLoc = info.location;
				
					rc.setIndicatorString(0, "pastr locatoin has been found");
					
					//now find the pastr's channel so you can heartbeat
					int currentChannel = pastrComStart;
					for (int n = 0; n < 13; n++) {
						int message = rc.readBroadcast(currentChannel);
						MapLocation eachLoc = PastrRobot.channelGetLocation(message);
						if (eachLoc.equals(pastrLoc)) {
							heartbeatChannel = currentChannel + 1;
							theBeatOfMyHeart();
							break;
						}
						currentChannel += 2;
					}
					
					if (heartbeatChannel == 0)
						System.out.println("Couldn't find pastr's channel! something's wrong!");
					
					break;
				}
			}
		//now that the pastr has been identified, start heartbeating and herding...
		} else {
			
			//heartbeat function
			theBeatOfMyHeart();
		
			//herding function
			//TODO: we're 'identifying closest pastr' twice with our integrated code, let's discuss this
			if(rc.isActive()){
				MapLocation closestPastr=getClosestPastr();
				if(closestPastr!=null){
					herd(closestPastr);
				}
			}
		}
	}

}
