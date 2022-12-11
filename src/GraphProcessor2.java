import java.security.InvalidAlgorithmParameterException;
import java.util.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Models a weighted graph of latitude-longitude points
 * and supports various distance and routing operations.
 * To do: Add your name(s) as additional authors
 * @author Brandon Fain
 *
 */
public class GraphProcessor2 {
    public LinkedHashMap<Point, List<Point>> aList;
    public static List<Point> points;
    Map<Point, HashSet<Point>>myMap = new HashMap<>();
    /**
     * Creates and initializes a graph from a source data
     * file in the .graph format. Should be called
     * before any other methods work.
     * @param file a FileInputStream of the .graph file
     * @throws Exception if file not found or error reading
     */
    public void initialize(FileInputStream file) throws Exception {
        readGraph(file);
    }

    public void readGraph(FileInputStream file) throws IOException{
        Scanner reader = new Scanner(file);
        String [] info = reader.nextLine().split(" ");
        int vertices = Integer.parseInt(info[0]);
        int edges = Integer.parseInt(info[1]);
        Point[] points = new Point[vertices];
        for (int i = 0; i < vertices; i++) {
            String [] coords = reader.nextLine().split(" ");
            String name = coords[0];
            double lat = Double.parseDouble(coords[1]);
            double lon = Double.parseDouble(coords[2]);
            points[i] = new Point(lat, lon);
        }

        myMap = new HashMap<>();
        for (int i = 0; i < edges; i++) {
            String [] connections = reader.nextLine().split(" ");
            int aIndex = Integer.parseInt(connections[0]);
            int bIndex = Integer.parseInt(connections[1]);
            Point a = points[aIndex];
            Point b = points[bIndex];
            myMap.putIfAbsent(a, new HashSet<Point>());
            myMap.get(a).add(b);
            myMap.putIfAbsent(b, new HashSet<Point>());
            myMap.get(b).add(a);
        }

        reader.close();
        return;
	}
    public void realAddEdge(int a, int b){
        addEdge(a, b);
        addEdge(b, a);
    }
    public void addEdge(int a, int b){
        ArrayList<Point >points = new ArrayList<>();
        points.addAll(myMap.keySet());
        List<Point> edgeList = aList.get(points.get(a));
        if (edgeList == null){
            edgeList = new ArrayList<>();
            aList.put(points.get(a), edgeList);
        }
        edgeList.add(points.get(b));
    }

    /**
     * Searches for the point in the graph that is closest in
     * straight-line distance to the parameter point p
     * @param p A point, not necessarily in the graph
     * @return The closest point in the graph to p
     */
    public Point nearestPoint(Point p) {
        ArrayList<Point >points = new ArrayList<>();
        points.addAll(myMap.keySet());
        double d = 90071992547409.0;
        Point ret = new Point(0, 0);
        for (Point other:points){
            if (other.equals(p)) {continue;}
            if (p.distance(other) < d){
                d = p.distance(other);
                ret = other;
            }
        }
        return ret;
    }


    /**
     * Calculates the total distance along the route, summing
     * the distance between the first and the second Points, 
     * the second and the third, ..., the second to last and
     * the last. Distance returned in miles.
     * @param start Beginning point. May or may not be in the graph.
     * @param end Destination point May or may not be in the graph.
     * @return The distance to get from start to end
     */
    public double routeDistance(List<Point> route) {
        double cumDistance = 0.0;
        for(int i=0; i<route.size()-1; i++){
            double length = route.get(i).distance(route.get(i+1));
            cumDistance += length;
        }
        return cumDistance;
    }
    
    /**
     * Checks if input points are part of a connected component
     * in the graph, that is, can one get from one to the other
     * only traversing edges in the graph
     * @param p1 one point
     * @param p2 another point
     * @return true if p2 is reachable from p1 (and vice versa)
     */
    public boolean connected(Point p1, Point p2) {
        HashSet<Point> visited = new HashSet<>();
        HashMap<Point, Point> previous = new HashMap<>();

        Stack<Point> toExplore = new Stack<>();
        toExplore.add(p1);
        visited.add(p1);

        while (!toExplore.isEmpty()){
            Point current = toExplore.pop();
            for (Point neighbor: aList.get(current)){
                if(!visited.contains(neighbor)){
                    if(neighbor.equals(p2)){return true;}
                    toExplore.add(neighbor);
                    visited.add(neighbor);
                    previous.put(neighbor, current);
                }
            }
        }
        return false;
    }

    /**
     * Returns the shortest path, traversing the graph, that begins at start
     * and terminates at end, including start and end as the first and last
     * points in the returned list. If there is no such route, either because
     * start is not connected to end or because start equals end, throws an
     * exception.
     * @param start Beginning point.
     * @param end Destination point.
     * @return The shortest path [start, ..., end].
     * @throws InvalidAlgorithmParameterException if there is no such route, 
     * either because start is not connected to end or because start equals end.
     */
    public List<Point> route(Point start, Point end) throws InvalidAlgorithmParameterException {
        if (!connected(start, end)){
            throw new InvalidAlgorithmParameterException
                ("No path between start and end");
        }
        Map<Point, Point> previous = new HashMap<>();
        Map<Point, Double> dist = new HashMap<>();

        Comparator<Point> comp = (a,b) -> 
            Double.compare(dist.get(a), dist.get(b));
        PriorityQueue<Point> toExplore = new PriorityQueue<>(comp);
        Point current = start;
        dist.put(current, 0.0);
        toExplore.add(current);

        while (!toExplore.isEmpty()){
            current = toExplore.remove();
            for (Point neighbor: aList.get(current)){
                double weight = getWeight(current, neighbor);
                if(!dist.containsKey(neighbor)||
                    dist.get(neighbor) > dist.get(current) + weight){
                        dist.put(neighbor, dist.get(current) + weight);
                        previous.put(neighbor, current);
                        toExplore.add(neighbor);
                    }
            }
        }
        ArrayList<Point>shortList = new ArrayList<>();
        ArrayList<Point> store = getShortList(end, start, previous, shortList);
        Collections.reverse(store);
        return store;
    }
    
    public ArrayList<Point> getShortList(Point end, Point start,
        Map<Point, Point> previous, ArrayList<Point> shortList){
            shortList.add(end);
            Point current = previous.get(end);
            if (current == start){return shortList;}
            shortList.add(current);
            if(current.equals(start)){return shortList;}
            return getShortList(current, start, previous, shortList);
        }

    public double shortest(Point end, Point start, 
        Map <Point, Point> previous, Map <Point, Double> dist, double soFar){
            Point current = previous.get(end);
            soFar = soFar + getWeight(end, current);
            if(current.equals(start)){return soFar;}
            return shortest(previous.get(current), start, previous, dist, soFar);
        
    }
    public double getWeight(Point a, Point b){
        return a.distance(b);
    }
    
}
