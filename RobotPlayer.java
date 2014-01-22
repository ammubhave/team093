package team093;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class RobotPlayer {
	//static Random rand;
	
	public static void run(RobotController rc) {
		BaseRobot br = null;
		
		try {
			switch (rc.getType()) {
				case HQ:
					br = new HQRobot(rc);
					break;
				case SOLDIER:
					br = new SoldierRobot(rc);
					break;
				case PASTR:
					br = new PastrRobot(rc);
					break;
				case NOISETOWER:
					br = new NoiseTowerRobot(rc);
					break;
			}		
				
			br.loop();
		}	catch (GameActionException ex) {
				System.out.println(ex.getMessage());
		}
	}
}