package team093;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class NoiseTowerRobot extends BaseRobot{

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
		rc.attackSquare(attackLocation);
	}

	public void run() throws GameActionException {
		if(rc.isActive()){
			MapLocation closestPastr=getClosestPastr();
			if(closestPastr!=null){
				herd(closestPastr);
			}
		}
	}

}
