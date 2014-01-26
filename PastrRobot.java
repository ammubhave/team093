package team093;



import battlecode.common.*;

enum PastrStatus {UNASSIGNED, READY, SETTLING, BUILDING, HEALTHY, ATTACKED, EMERGENCY, DOOMED, INVALID};


public class PastrRobot extends BaseRobot{

	
	//STATIC METHODS
	public static int channelGetTurn(int channelInt) {
		return (channelInt & 0b01111111111111000000000000000000) >> 18;
	}
	
	
	public static int channelSetTurn(int turn, int channelInt) {
		if (turn > 8190) System.out.println("at channelSetTurn() turn will take more than 13 bits, overflow error!");
		channelInt = channelInt & 0b10000000000000111111111111111111; // clear previous heartbeat
		channelInt = channelInt | (turn << 18); //set new one
		return channelInt;
	}
	
	public static boolean channelGetIsThereNoiseTower(int channelInt) {
		int code = channelInt & 0b00000000000000000000000000010000;
		return (code != 0);
	}
	
	public static int channelSetIsThereNoiseTower(boolean state, int channelInt) {
		if (state) {
			channelInt = channelInt | 0b00000000000000000000000000010000; //set flag to true
		}
		else {
			channelInt = channelInt & 0b11111111111111111111111111101111;
		}
		return channelInt;
	}
	
	public static MapLocation channelGetLocation(int channelInt) {
		return BaseRobot.intToLoc((channelInt & 0b00000000000000111111111111100000) >> 5);
	}
	
	public static int channelSetLocation(MapLocation loc, int channelInt) {

		channelInt = channelInt & 0b11111111111111000000000000011111; //clear previous target
		channelInt = channelInt | (BaseRobot.locToInt(loc) << 5); //set new target
		return channelInt;
	}
	
	public static PastrStatus channelGetPastrStatus(int channelInt) {
		int code = channelInt & 0b00000000000000000000000000001111;
		
		switch(code) {
		case 0:
			return PastrStatus.UNASSIGNED;
		case 1:
			return PastrStatus.READY;
		case 2:
			return PastrStatus.SETTLING;
		case 3:
			return PastrStatus.HEALTHY;
		case 4:
			return PastrStatus.ATTACKED;
		case 5:
			return PastrStatus.EMERGENCY;
		case 6:
			return PastrStatus.DOOMED;
			default:
				return PastrStatus.INVALID;
		}
	}
	

	
	public static int channelSetPastrStatus(PastrStatus status, int channelInt) {
		
		
		channelInt = channelInt & 0b11111111111111111111111111110000; // clear previous code
		int code = status.ordinal();
		if (code > 15) System.out.println("WARNING: in channelSetPastrStatus(), why is code greater than 15? channel is gonna overflow");
		
		
		channelInt = channelInt | code; //set new code
		
		return channelInt;
	}
	
	
	int pastrChannel = 0;	//Broadcast Channel where Pastr will update with its status
	int lastHeartbeatTurn = 0; //last time Pastr heartbeated
	
	
	//CONSTRUCTOR
	public PastrRobot(RobotController rc) throws GameActionException {
		super(rc);
		
		//When pastr is created, it needs to find the channel it's gonna use to broadcast
		int currentChannel = pastrComStart;
		
		//loop to find the channel
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
		
		//if it didn't find its channel, there's a bug in the code, this SHOULD NOT happen
		if (pastrChannel == 0)
			System.out.println("no channel found!!!! what happened?");
		
	}
	
	
	private int theBeatOfMyHeart(int message) throws GameActionException {
		
	
		if ((Clock.getRoundNum() - lastHeartbeatTurn) > declareDeadInterval) {
			
			//first, beat your heart
			lastHeartbeatTurn = Clock.getRoundNum();
			message = PastrRobot.channelSetTurn(Clock.getRoundNum(), message);
			
			//now, make sure the soldier assigned to become your NoiseTower is still there
			if (PastrRobot.channelGetIsThereNoiseTower(message)) {
				int noiseTowerMessage = rc.readBroadcast(pastrChannel + 1);
				int lastNoiseTowerHeartbeat = noiseTowerMessage & 0b00000000000000001111111111111111;
				int noiseTowerStatus = noiseTowerMessage & 0b11111111111111110000000000000000;
				
				if (noiseTowerStatus == 0) {
					if (isRobotDead(Clock.getRoundNum(),lastNoiseTowerHeartbeat)) {
						message = PastrRobot.channelSetIsThereNoiseTower(false, message);
					}
					
				} else {
					if (isConstructingNoiseTowerDead(Clock.getRoundNum(),lastNoiseTowerHeartbeat)) {
						message = PastrRobot.channelSetIsThereNoiseTower(false, message);
					}
				}
			}
			
			
			rc.broadcast(pastrChannel, message);
		}
		
		return message;
}

	public void run() throws GameActionException {
		
		
		int message = rc.readBroadcast(pastrChannel);
		message = theBeatOfMyHeart(message);
		rc.broadcast(pastrChannel, message);
	}

}
