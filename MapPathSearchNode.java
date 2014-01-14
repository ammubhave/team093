package team093;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.PriorityQueue;

import team093.BaseRobot;

public class MapPathSearchNode implements Comparable<MapPathSearchNode> {
	//BaseRobot rbt;
	BaseRobot rbt;
	//TerrainTile map[][] = null;
	MapLocation location;
	MapLocation goalLocation;
	MapPathSearchNode parent;
	Callable<MapLocation, Boolean> goalTest;
	Callable<MapLocation, Double> heuristic;
	int cost;
	
	public MapPathSearchNode(BaseRobot rbt, MapLocation location, MapPathSearchNode parent, int cost, MapLocation goalLocation) {
		this(rbt, location, parent, cost, new GoalTestIfDestination(goalLocation), new HeuristicManhattanDistance(goalLocation));
		/*this.map = map;
		this.location = location;
		this.parent = parent;
		this.cost = cost;
		this.goalLocation = goalLocation;
		this.goalTest = new GoalTestIfDestination(goalLocation);*/
	}
	public MapPathSearchNode(BaseRobot rbt, MapLocation location, MapPathSearchNode parent, int cost, Callable<MapLocation, Boolean> goalTest) {
		this(rbt, location, parent, cost, goalTest, new HeuristicZero());
	}
	public MapPathSearchNode(BaseRobot rbt, MapLocation location, MapPathSearchNode parent, int cost, Callable<MapLocation, Boolean> goalTest, Callable<MapLocation, Double> heuristic) {
		this.rbt = rbt;
		this.location = location;
		this.parent = parent;
		this.cost = cost;
		this.goalTest = goalTest;
		this.heuristic = heuristic;
		
	}
	
	public MapPathSearchNode[] getChildren() {
		ArrayList<MapPathSearchNode> children = new ArrayList<MapPathSearchNode>(9);

		for (int i = location.x > 0 ? location.x - 1 : location.x; i <= ((location.x < (rbt.terrainMap.length - 1)) ? location.x + 1 : location.x) ; i++) {
			for (int j = location.y > 0 ? location.y - 1 : location.y; j <= ((location.y < (rbt.terrainMap[0].length - 1)) ? location.y + 1 : location.y) ; j++) {
				if (rbt.terrainMap[i][j] == TerrainTile.NORMAL || rbt.terrainMap[i][j] == TerrainTile.ROAD || rbt.terrainMap[i][j] == TerrainTile.OFF_MAP) {
					children.add(new MapPathSearchNode(rbt, new MapLocation(i, j), this, this.cost + (rbt.terrainMap[i][j] == TerrainTile.ROAD && rbt.terrainMap[this.location.x][this.location.y] == TerrainTile.ROAD ? 10 : 10), goalTest, heuristic));
				}
			}
		}
		
		return (MapPathSearchNode[]) children.toArray(new MapPathSearchNode[children.size()]);
	}

	@Override
	public int compareTo(MapPathSearchNode node) {	
		//return this.cost-node.cost;
		return (int)((this.cost + this.heuristic.call(this.location)) - (node.cost + node.heuristic.call(node.location)));
		/*return (this.cost + 10 * (Math.abs(this.location.x - this.goalLocation.x) +  Math.abs(this.location.y - this.goalLocation.y))
				- (node.cost  + 10 * (Math.abs(node.location.x - node.goalLocation.x) +  Math.abs(node.location.y - node.goalLocation.y))));*/
	}
	
	public MapLocation[] getPathTo(MapLocation goal) {
		PriorityQueue<MapPathSearchNode> nodes = new PriorityQueue<MapPathSearchNode>();
		ArrayList<MapLocation> locationsVisited = new ArrayList<MapLocation>();
		nodes.add(this);		
		locationsVisited.add(this.location);
		//int nodesExpanded = 0;
		while (!nodes.isEmpty()) {
			MapPathSearchNode node = nodes.remove();
			//nodesExpanded += 1;
			
			MapPathSearchNode nodeChildren[] = node.getChildren();
			for (MapPathSearchNode nodeChild : nodeChildren) {
				
				if (this.goalTest.call(nodeChild.location)/*  nodeChild.location.equals(goal)*/) {
					ArrayList<MapLocation> path = new ArrayList<MapLocation>();
					MapPathSearchNode curNode = nodeChild;
					while (curNode != null) {
						path.add(0, curNode.location);
						curNode = curNode.parent;
					}					
					//System.out.print("COUNT: "); System.out.println(nodesExpanded);
					return (MapLocation[]) path.toArray(new MapLocation[path.size()]);
				} else if(!locationsVisited.contains(nodeChild.location)) {
					if (rbt.terrainMap[nodeChild.location.x][nodeChild.location.y] != TerrainTile.OFF_MAP)
						nodes.add(nodeChild);
					locationsVisited.add(nodeChild.location);
				}
			}
		}
		
		return null;
	}
}
