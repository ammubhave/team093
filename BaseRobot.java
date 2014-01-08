package team093;

import battlecode.common.GameConstants;
import battlecode.common.RobotController;
import battlecode.common.*;
import java.util.*;

public abstract class BaseRobot {
	public final RobotController rc;
	public BaseRobot(RobotController rc) throws GameActionException {
		this.rc = rc;
	}
	
	public abstract void run() throws GameActionException;
	
	public void loop() {
		while(true) {
			try {				
				run();	
			} catch (Exception e) {
				e.printStackTrace();
			}
	
			rc.yield();
		}
	}
	
	
}