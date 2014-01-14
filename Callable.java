package team093;

import battlecode.common.*;

public interface Callable <I, O> {
	public O call (I input);
}

class GoalTestIfDestination implements Callable<MapLocation, Boolean> {
	MapLocation destination;
	public GoalTestIfDestination(MapLocation destination) {
	    this.destination = destination;
	  }
	  public Boolean call(MapLocation input) {
	    return this.destination.equals(input);
	  }
	};
class HeuristicManhattanDistance implements Callable<MapLocation, Double> {
	  MapLocation destination;
	  public HeuristicManhattanDistance(MapLocation destination) {
	    this.destination = destination;
	  }
	  public Double call(MapLocation input) {
	    return 10 * Math.sqrt(input.distanceSquaredTo(this.destination));
	  }
};

class HeuristicZero implements Callable<MapLocation, Double> {
	public Double call(MapLocation input) { return 0d; }
}