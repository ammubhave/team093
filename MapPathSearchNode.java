package team093;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.PriorityQueue;

public class MapPathSearchNode implements Comparable<MapPathSearchNode> {
	TerrainTile map[][] = null;
	MapLocation location;
	Callable<MapLocation, Double> heuristic;
	MapPathSearchNode parent;
	int cost;
	
	public MapPathSearchNode(TerrainTile map[][], MapLocation location, MapPathSearchNode parent, int cost, Callable<MapLocation, Double> heuristic) {
		this.map = map;
		this.location = location;
		this.parent = parent;
		this.cost = cost;
		this.heuristic = heuristic;
	}
	public MapPathSearchNode(TerrainTile map[][], MapLocation location, MapPathSearchNode parent, int cost) {
		this(map, location, parent, cost, null);
	}
	public MapPathSearchNode(TerrainTile map[][], MapLocation location, MapPathSearchNode parent) {
		this(map, location, parent, 0);
	}
	
	public MapPathSearchNode[] getChildren() {
		ArrayList<MapPathSearchNode> children = new ArrayList<MapPathSearchNode>(9);
		
		for (int i = location.x > 0 ? location.x - 1 : location.x; i <= ((location.x < (map.length - 1)) ? location.x + 1 : location.x) ; i++) {
			for (int j = location.y > 0 ? location.y - 1 : location.y; j <= ((location.y < (map[0].length - 1)) ? location.y + 1 : location.y) ; j++) {
				if (map[i][j] == TerrainTile.NORMAL || map[i][j] == TerrainTile.ROAD)
					children.add(new MapPathSearchNode(map, new MapLocation(i, j), this, this.cost + (map[i][j] == TerrainTile.ROAD && map[this.location.x][this.location.y] == TerrainTile.ROAD ? 10 : 10), goalLocation));
			}
		}
		
		return (MapPathSearchNode[]) children.toArray(new MapPathSearchNode[children.size()]);
	}

	@Override
	public int compareTo(MapPathSearchNode node) {		
		return (this.cost + this.heuristic(this.location))
				- (node.cost  + node.heuristic(node.location));
	}
	
	public MapLocation[] getPathTo(Callable<MapLocation, Boolean> goal) {
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
				
				if (goal.call(nodeChild.location)) {
					ArrayList<MapLocation> path = new ArrayList<MapLocation>();
					MapPathSearchNode curNode = nodeChild;
					while (curNode != null) {
						path.add(0, curNode.location);
						curNode = curNode.parent;
					}					
					//System.out.print("COUNT: "); System.out.println(nodesExpanded);
					return (MapLocation[]) path.toArray(new MapLocation[path.size()]);
				} else if(!locationsVisited.contains(nodeChild.location)) {
					nodes.add(nodeChild);
					locationsVisited.add(nodeChild.location);
				}
			}
		}
		
		return null;
	}
}
