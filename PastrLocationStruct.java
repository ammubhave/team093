package team093;

import battlecode.common.*;

//this is just a struct, all functions responsible for filling it up are in the HQ object

public class PastrLocationStruct {
	
	//statistics
	public int fertilityGrowth;
	public double averageCowGrowth;
	public MapLocation loc;
	public double cowGrowingArea;
	public double distanceToHQ;
	
	//properties
	public boolean hasTooManyEnemies;
	public int tooManyEnemiesTurn;
	public PastrStatus status = PastrStatus.UNASSIGNED;
	public int channel;
	
	private GroupUnit[] defenderUnits = new GroupUnit[16];
	
	
	public boolean addDefender(GroupUnit unit) {
		for (int n = 0; n < defenderUnits.length; n++) {
			if (defenderUnits[n] == null) {
				defenderUnits[n] = unit;
				return true;
			}
		}
		
		return false;
	}
	
	public int getDefenderCount() {
		
		int count = 0;
		for (int n = 0; n < defenderUnits.length; n++) {
			if (defenderUnits[n] != null)
				count++;
		}
		return count;
	}
	
	public boolean removeDefender (GroupUnit unit) {
		for (int n = 0; n < defenderUnits.length; n++) {
			if (defenderUnits[n] == unit) {
				defenderUnits[n] = null;
				return true;
			}
		}
		
		return false;
	}
	
	
	/*public PastrLocationStruct(double fertilityGrowth, double averageCowGrowth, MapLocation loc, int squaredArea, int cowGrowingArea, double distanceToHQ ) {
		this.fertilityGrowth = fertilityGrowth;
		this.averageCowGrowth = averageCowGrowth;
		this.loc = loc;
		this.squaredArea = squaredArea;
		this.cowGrowingArea = cowGrowingArea;
		this.distanceToHQ = distanceToHQ;
	}*/
	
	
	
}
