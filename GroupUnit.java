package team093;

import battlecode.common.*;

public class GroupUnit {

	//constants
	//final int firstChannel = 10000; //open groups channel
	public int membersPerGroup;
	
	//bit masks for first message
	static final int isGroupClosedMask = 	1073741824;
	static final int isGroupClosedEraser = -1073741825;
	
	static final int locationMask = 		1073676288;
	static final int locationMaskEraser =	-1073676289;
	
	static final int groupCountMask = 		63488;
	static final int groupCountEraser =		-63489;
	
	static final int haveLocationMask = 	1024;
	static final int haveLocationEraser =	-1025;
	
	
	//bit masks for second message
	static final int groupRoleMask =		1879048192;
	static final int groupRoleEraser =		-1879048193;
	
	static final int groupStateMask =		251658240;
	static final int groupStateEraser = 	-251658241;
	
	static final int arrivedFlagsMask =	16711680;
	static final int arrivedFlagEraser = 	-16711681;
	
	static final int indiviSlotMask =		32768;
	static final int openSlotMask =		65280;
	static final int openSlotEraser =		65281;
	
	//public fields
	public boolean isFull = false;
	public MapLocation currentTarget;
	public int groupCount;
	
	//the two channels
	//public int destroyerMessage1;
	//public int destroyerMessage2;
	
	
	public SoldierMode groupRole = SoldierMode.UNASSIGNED;
	public int groupChannel = 0;
	
	
	//this should be used to read and write group broadcast information, since it's a bit of an involved process
	public static int[] readGroupInformation(int channel, RobotController rc) throws GameActionException {
		int[] toReturn = new int[2];
		toReturn[0] = rc.readBroadcast(channel);
		toReturn[1] = rc.readBroadcast(channel + 1);
		return toReturn;
	}
	
	public static void writeGroupInformation(int channel, int[] message, RobotController rc) throws GameActionException {
		rc.broadcast(channel, message[0]);
		rc.broadcast(channel + 1, message[1]);
	}
	

	
	public GroupUnit(int membersPerGroup) {
		this.membersPerGroup = membersPerGroup;

		
	}
	
	
	
	//1 if open, 0 if closed
	public static boolean getIsGroupOpen(int[] message) {
		int code = message[0] & -1073741825;
		
		return (code != 0);
	}
	
	public static int[] setIsGroupOpen(int[] message, boolean open) {
		

		
		if (open) {
			message[0] = message[0] | 1073741824;
		} else {
			message[0] = message[0] & -1073741825;
		}
		
		return message;
	}
	
	
	//destroyer message 1 -> 1st bit: NULL, 2nd bit: isGroupClosed, bits 3-16: Group Target Location, bits 17-21: groupCount, bit 22: doesGroupHaveLocation, bit 23: isSpaceAvailable;
	//destroyer message 2 -> bit 1: NULL, bit 2 to 4: robotMode, bit 5 to 8: roleMode, bits 9 to 16: flags whether each member has arrived at location, bits 17 to 24, flags whether each slot is taken
	public static boolean getIsFull(int[] messages) {
		int code = messages[0] & 512;
		return (code != 0);
	}
	
	public static int[] setIsFull(int[] message, GroupUnit unit, boolean state) {
		
		if (state) {
			message[0] = message[0] | 512;
		} else {
			message[0] = message[0] & -513;
		}
		
		unit.isFull = state;
		
		return message;
		
	}
	
	public static SoldierMode getGroupRole(int[] messages) {
		int code = (messages[1] & groupRoleMask) >> 28;

		//System.out.println("getting group role, binary is " + Integer.toBinaryString(destroyerMessage2));
		
		switch(code) {
			case 0:
			return SoldierMode.UNASSIGNED;
			case 2:
				return SoldierMode.DEFENDER;
			case 3:
				return SoldierMode.DESTROYER;
			default:
				return SoldierMode.UNASSIGNED;
		}
	}
	
	public static int[] setGroupRole(SoldierMode mode, int[] messages, GroupUnit unit) {
		
		//System.out.println("setting group mode to: " + mode);
		//System.out.println("before modification, binary was " + Integer.toBinaryString(destroyerMessage2));
		
		messages[1] = messages[1] & groupRoleEraser; //erase previous value
		unit.groupRole = mode;
		

		//System.out.println("after erasure, binary was " + Integer.toBinaryString(destroyerMessage2));
		
		int codeToSet = 0;
		switch(mode) {
		case UNASSIGNED:
			codeToSet = 0;
			break;
		case DEFENDER:
			codeToSet = 2;
			break;
		case DESTROYER:
			codeToSet = 3;
			break;
			default:
				codeToSet = 0;
		}
		
		messages[1] = (messages[1] | (codeToSet << 28)); //set new value
		
		return messages;
		
		//System.out.println("after new value, binary was " + Integer.toBinaryString(destroyerMessage2));
	}
	

	
	public static int[] clearTargetingInformation(RobotController rc, int[] messages) throws GameActionException {
		messages[0] = messages[0] & haveLocationEraser; //set hasLocation flag to 0 (no)
		messages[0] = messages[0] & locationMaskEraser; // erase previous location
		
		messages[1] = messages[1] & arrivedFlagEraser; //wipe out all arrived flags
		
		return messages;
	}
	
	
	public static MapLocation getCurrentTarget(int[] messages) {
		
		//ifGroupDoesn't have location, there is no current target, HQ should assign one momentarily
		if ((messages[0] & haveLocationMask) == 0) return null;
		
		//otherwise, return Location
		return SoldierRobot.intToLoc(((messages[0] & locationMask)>>16));
		
	}
	
	public static int[] setCurrentTarget(MapLocation loc, int[] messages, GroupUnit unit) {
		//set in object
		unit.currentTarget = loc;
		
		
		messages[0] = messages[0] & locationMaskEraser; // erase previous location
		messages[0] = messages[0] | (SoldierRobot.locToInt(loc) << 16); //write new location
		
		//set mask that indicates group has Location
		messages[0] = messages[0] | haveLocationMask;
		
		return messages;
		

		
	}
	
	public static int getGroupCount(int[] messages) {
		return (messages[0] & groupCountMask) >> 11;
	}
	
	public static int[] setGroupCount(int count, GroupUnit unit, int[] messages) {
		unit.groupCount = count;
		messages[0] = messages[0] & groupCountEraser; //erase previous number
		messages[0] = messages[0] | (count << 11); //set new number
		
		return messages;
	}
	
	public static int[] setSlotTaken(int slot, int[] messages) {
		messages[1] = messages[1] | (32768 >> (slot) );
		return messages;
	}
	
	public static int[] setSlotEmpty(int slot, int[] messages) {
		messages[1] = messages[1] & (-32769 >> (slot));
		return messages;
	}
	
	
	
	public static int[] removeRobot(int slot, int[] messages, GroupUnit unit) {
		
		
		setSlotEmpty(slot, messages);
		setGroupCount(getGroupCount(messages) - 1, unit, messages);
		setIsFull(messages,unit, false);
		
		return messages;
		
		
		
	}
	

	

	
	public static boolean isSlotTaken(int slot, int[] messages) {
		if ( (messages[1] & (32768 >> (slot) )  ) == 0)
			return false;
		else return true;
	}
	

	
	//returns numberInGroup, which starts at 0, will return -1 if there is actually no space
	public static int[] addRobotToGroupTakesThreeArray(int[] messages, GroupUnit unit, RobotController rc) throws GameActionException {
		
		int currentRobotCount = GroupUnit.getGroupCount(messages);
		
		
		
		//System.out.println("at line 353 getGroupCount() gave " + currentRobotCount);
		
		//if this is the case, group should have been closed
		if (currentRobotCount >= unit.membersPerGroup) {
			GroupUnit.setIsFull(messages, unit, true);
			int[] toReturn = new int[3];
			toReturn[0] = messages[0];
			toReturn[1] = messages[1];
			toReturn[2] = -1;
			return toReturn;
			
		} else {
			currentRobotCount++;
			//update group count
			GroupUnit.setGroupCount(currentRobotCount, unit, messages);
			
			//System.out.println("at line 365, new group count is " + getGroupCount());
			
			//close group if full
			if (currentRobotCount >= unit.membersPerGroup) {
				//System.out.println("calling setStatusParameters to close group number " + groupNumber);
				GroupUnit.setIsFull(messages, unit, true);
			}
			
			
			
			//find first empty slot, close it, and return it
			int firstSlot = -1;
			
			for (int n = 0; n < 8; n++) {
				if (!isSlotTaken(n, messages)) {
					firstSlot = n;
					break;
				}
			}
			
			//System.out.println("first empty slot is" + firstSlot);
			setSlotTaken(firstSlot, messages);
			
			GroupUnit.setSlotTaken(firstSlot, messages);
			
			int[] toReturn = new int[3];
			toReturn[0] = messages[0];
			toReturn[1] = messages[1];
			toReturn[2] = firstSlot;
			
			return toReturn;
			
			
			
		}
		
	}
	

	

	

	

	
	public static boolean getHasArrived(int memberNumber, int[] messages) {
		return (messages[1] & (8388608 >> memberNumber )) != 0;
	}
	
	public static int[] setHasArrived(int memberNumber, boolean hasArrived, int[] messages) {
		if (hasArrived)
			messages[1] = messages[1] | (8388608 >> memberNumber  );
		else {
			messages[1] = messages[1] & (-8388609 >> memberNumber);
		}
		
		return messages;
			
	}
	
	public static boolean hasEntireGroupArrived(int[] messages, GroupUnit unit) {
		
		for (int n = 0; n < unit.membersPerGroup; n++) {
			if (!getHasArrived(n, messages)) return false;
		}
		
		return true;
	}
	

	

	

	

	

	
}
