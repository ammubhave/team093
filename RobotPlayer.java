package team093;

import battlecode.common.Direction;
import battlecode.common.GameConstants;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.*;
import java.util.*;

public class RobotPlayer {
	static Random rand;
	
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
					br = new NoisetowerRobot(rc);
					break;
			}		
				
			br.loop();
		}	catch (GameActionException ex) {
			
		}
	}
}