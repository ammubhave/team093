package team093;

import battlecode.common.*;

import java.util.*;

enum PastrStatus {UNASSIGNED, READY, SETTLING, BUILDING, HEALTHY, ATTACKED, EMERGENCY, DOOMED, INVALID};

public class PastrRobot extends BaseRobot {
	
	public static int channelGetTurn(int channelInt) {
		return (channelInt & 2147221504) >> 18;
	}
	
	
	public static int channelSetTurn(int turn, int channelInt) {
		if (turn > 8190) System.out.println("at channelSetTurn() turn will take more than 13 bits, overflow error!");
		channelInt = channelInt & -2147221505; // clear previous heartbeat
		channelInt = channelInt | (turn << 18); //set new one
		return channelInt;
	}
	
	public static boolean channelGetIsThereNoiseTower(int channelInt) {
		int code = channelInt & 16;
		return (code != 0);
	}
	
	public static int channelSetIsThereNoiseTower(boolean state, int channelInt) {
		if (state) {
			channelInt = channelInt | 16; //set flag to true
		}
		else {
			channelInt = channelInt & -17;
		}
		return channelInt;
	}
	
	public static MapLocation channelGetLocation(int channelInt) {
		return SoldierRobot.intToLoc((channelInt & 262112) >> 5);
	}
	
	public static int channelSetLocation(MapLocation loc, int channelInt) {

		channelInt = channelInt & -262113; //clear previous target
		channelInt = channelInt | (SoldierRobot.locToInt(loc) << 5); //set new target
		return channelInt;
	}
	
	public static PastrStatus channelGetPastrStatus(int channelInt) {
		int code = channelInt & 15;
		
		switch(code) {
		case 0:
			return PastrStatus.UNASSIGNED;
		case 1:
			return PastrStatus.READY;
		case 2:
			return PastrStatus.SETTLING;
		case 3:
			return PastrStatus.BUILDING;
		case 4:
			return PastrStatus.HEALTHY;
		case 5:
			return PastrStatus.ATTACKED;
		case 6:
			return PastrStatus.EMERGENCY;
		case 7:
			return PastrStatus.DOOMED;
			default:
				return PastrStatus.INVALID;
		}
	}
	

	
	public static int channelSetPastrStatus(PastrStatus status, int channelInt) {
		
		
		channelInt = channelInt & -16; // clear previous code
		int code = status.ordinal();
		if (code > 15) System.out.println("WARNING: in channelSetPastrStatus(), why is code greater than 15? channel is gonna overflow");
		
		
		channelInt = channelInt | code; //set new code
		
		return channelInt;
	}
	
	
	int pastrChannel = -1;
	int lastHeartbeatTurn = 0;
	
	double[] healthHistory = new double[20];
	PastrStatus lastStatus = PastrStatus.HEALTHY;
	
	public PastrRobot(RobotController rc) throws GameActionException {
		super(rc);
		
		int currentChannel = pastrComStart;
		System.out.println("Upon creation, Pastr health is " + rc.getHealth());
		
		//fill out initial healthHistory
		for (int n = 0; n < healthHistory.length; n++) {
			healthHistory[n] = 200;
			
		}
		
		//find your channel
		for (int n = 0; n < 13; n++) {
			int message = rc.readBroadcast(currentChannel);

			//System.out.println("location of this pastr is " + rc.getLocation().toString() + " and location of channel its checking is  " + PastrRobot.channelGetLocation(message));
			
			if (rc.getLocation().equals(PastrRobot.channelGetLocation(message))) {
				pastrChannel = currentChannel;
				message = PastrRobot.channelSetPastrStatus(PastrStatus.HEALTHY, message);
				rc.broadcast(currentChannel, message);
				break;
			}
			currentChannel += 2;
		}
		
		if (pastrChannel == -1)
			System.out.println("no channel found!!!! what happened?");
		
	}
	
	void theBeatOfMyHeart(PastrStatus status) throws GameActionException {
		
				int message = 0; //may not be used if neither if condition evaluates to true
				boolean mustBroadcast = false;
				
				if (status != lastStatus) {
					lastStatus = status;
					message = rc.readBroadcast(pastrChannel);
					PastrRobot.channelSetPastrStatus(status, message);
					mustBroadcast = true;
				}
		
				//constant heartbeat + make sure noise tower is still around
				if ((Clock.getRoundNum() - lastHeartbeatTurn) > declareDeadInterval ) {
					lastHeartbeatTurn = Clock.getRoundNum();
					if (message == 0) message = rc.readBroadcast(pastrChannel);
					message = PastrRobot.channelSetTurn(Clock.getRoundNum(), message);
					
					if (PastrRobot.channelGetIsThereNoiseTower(message)) {
						int noiseTowerMessage = rc.readBroadcast(pastrChannel + 1);
						int lastNoiseTowerHeartbeat = noiseTowerMessage & 65535;
						int noiseTowerStatus = noiseTowerMessage & -65535;
						
						if (noiseTowerStatus == 0) {
							if (isRobotDead(Clock.getRoundNum(),lastNoiseTowerHeartbeat)) {
								PastrRobot.channelSetIsThereNoiseTower(false, message);
							}
							
						} else {
							if (isConstructingNoiseTowerDead(Clock.getRoundNum(),lastNoiseTowerHeartbeat)) {
								PastrRobot.channelSetIsThereNoiseTower(false, message);
							}
						}
					}
					
					
					
				}
				
				if (mustBroadcast)
					rc.broadcast(pastrChannel, message);;
	}
	
	@Override
	public void run() throws GameActionException {
		
		double max = 0;
		
		//update health history
		for (int n = 0; n < (healthHistory.length -1); n++) {
			
			
			
			healthHistory[n] = healthHistory[n+1];
			if (healthHistory[n] > max) max = healthHistory[n];
		}
		healthHistory[healthHistory.length-1] = rc.getHealth();
		
		//default pastr status is healthy
		PastrStatus status = PastrStatus.HEALTHY;
		
		//if you've been taking damage, update pastr status
		//if you've lost a lot in the last 20 turns ,declare emergency
		if (healthHistory[healthHistory.length-1] < (max - 40)    ) {
			status = PastrStatus.EMERGENCY;
		//if not, at least if you've been just attacked, declare attack	
		} else if (healthHistory[healthHistory.length-1] < max) {
			status = PastrStatus.ATTACKED;
		}
		//otherwise, you haven't been attacked in last 20 turns. Good for you!
		
		
		//System.out.println("Max is " + max + " and current health is " + healthHistory[healthHistory.length-1] + ", so status is " + status);
		//System.out.println("Five turns ago, health was " + healthHistory[15]);
		
		
		/*This function:
		 * 1. beats heart
		 * 2. checks to see if Noise Tower has beat heart
		 * 3. checks to see if it is necessary to update pastr status
		 * 
		 * */
		
		theBeatOfMyHeart(status);
		

		
		
		
	}
}