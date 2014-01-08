package team093;

import battlecode.common.*;
import java.util.ArrayList;

public class MapPathSearchNode {
	TerrainTile map[][] = null;
	MapLocation location;
	
	public MapPathSearchNode(TerrainTile map[][], MapLocation location) {
		this.map = map;
		this.location = location;
	}
	
	public MapPathSearchNode[] getChildren() {
		ArrayList<MapPathSearchNode> children = new ArrayList<MapPathSearchNode>();
		
		for (int i = location.x > 0 ? location.x - 1 : location.x; i < ((location.x < (map.length - 1)) ? location.x + 1 : location.x) ; i++) {
			for (int j = location.y > 0 ? location.y - 1 : location.y; j < ((location.y < (map[0].length - 1)) ? location.y + 1 : location.y) ; j++) {
				if (map[i][j] == TerrainTile.NORMAL || map[i][j] == TerrainTile.ROAD) 
					children.add(new MapPathSearchNode(map, new MapLocation(i, j)));
			}
		}
		
		return (MapPathSearchNode[]) children.toArray();
	}
}
