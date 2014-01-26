package team093;

import battlecode.common.*;

public class RobotCount {
	
	int soldiers;
	int pastrs;
	int noiseTowers;
	
	public RobotCount(Robot[] alliedRobots, RobotController rc) throws GameActionException {
		
		soldiers = 0;
		pastrs = 0;
		noiseTowers = 0;
		
		
		//generate robot counts
		for (Robot each: alliedRobots) {
			RobotType type = rc.senseRobotInfo(each).type;
			
			switch(type) {
			case SOLDIER:
				soldiers++;
				break;
			case PASTR:
				pastrs++;
					break;
			case NOISETOWER:
				noiseTowers++;
				break;
			case HQ:
				break;
				default:
					System.out.println("!!!no match in RobotCount constructor");
			}
			
		}
		
		
	}
	
	public int getTotalRobotCount() {
		return soldiers + pastrs + noiseTowers;
	}
	
	public int getTotalRobotType(RobotType type) {
		switch(type) {
		case SOLDIER:
			return soldiers;
		case PASTR:
			return pastrs;
		case NOISETOWER:
			return noiseTowers;
		default:
			return 1;
		}
	}
	
	

}
