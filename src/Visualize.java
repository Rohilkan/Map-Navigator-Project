import java.io.File;
import java.io.FileNotFoundException;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

/**
 * Class for creating visualizations of graphs and routes
 * representing latitude-longitude points using StdDraw.
 * @author Brandon Fain
 */
public class Visualize {
    public static final double RADIUS = 6378137.0; /* in meters on the equator */
    public static final double NODE_SIZE = 0.01;
    public static final double EDGE_SIZE = 0.003;
    private double minLon;
    private double maxLon;
    private double minLat;
    private double maxLat;
    private int width;
    private int height;
    private String imageFile;

    /**
     * Creates a Visualize object for visualizing mapping data using StdDraw.
     * @param visFile Should be a .vis file containing latitude and longitude 
     * bounds, and width/height in pixels, for the imageFile
     * @param imageFile Should be a png image with latitude/longitude and width
     * /height in pixels bounds corresponding to the visFile, onto which routes
     * will be draw.
     * @throws FileNotFoundException If either file not found
     */
    public Visualize(String visFile, String imageFile) throws FileNotFoundException {
        this.imageFile = imageFile;
        readVis(visFile);
        setUp();
    }


    /**
     * Draws the given point on the imageFile
     */
    public void drawPoint(Point p) {
        StdDraw.setPenRadius(NODE_SIZE);
        StdDraw.point(p.getLon(), lat2y(p.getLat()));
        StdDraw.show();
    }


    /**
     * Draws an edge between points u and v on the imageFile
     */
    public void drawEdge(Point u, Point v) {
        StdDraw.setPenRadius(EDGE_SIZE);
        StdDraw.line(u.getLon(), lat2y(u.getLat()), v.getLon(), lat2y(v.getLat()));
        StdDraw.show();
    }

    /**
     * Draws a given graph on the imageFile, drawing each of its
     * vertices and each of its edges. May be slow for large
     * graphs and may be difficult to visualize for graphs that 
     * are dense with respect to the imageFile.
     */
    public void drawGraph(List<Point> vertices, List<Point[]> edges) {
        for (Point p : vertices) {
            drawPoint(p);
        }
        for (Point[] edge : edges) {
            drawEdge(edge[0], edge[1]);
        }
    }


    /**
     * Draws a given route on the imageFile
     */
    public void drawRoute(List<Point> route) {
        if (route == null || route.size() == 0) {
            return;
        }
        Iterator<Point> pointIter = route.iterator();
        Point prev = pointIter.next();
        drawPoint(prev);
        while (pointIter.hasNext()) {
            Point next = pointIter.next();
            drawPoint(next);
            drawEdge(prev, next);
            prev = next;        
        }
        StdDraw.show();
    }

    private void readVis(String visFile) throws FileNotFoundException {
        Scanner reader = new Scanner(new File(visFile));
        String[] lonBounds = reader.nextLine().split(" ");
        minLon = Double.parseDouble(lonBounds[0]);
        maxLon = Double.parseDouble(lonBounds[1]);

        String[] latBounds = reader.nextLine().split(" ");
        minLat = Double.parseDouble(latBounds[0]);
        maxLat = Double.parseDouble(latBounds[1]);

        String[] visDims = reader.nextLine().split(" ");
        width = Integer.parseInt(visDims[0]);
        height = Integer.parseInt(visDims[1]);
        reader.close();
    }

    private void setUp() {
        StdDraw.enableDoubleBuffering();
        StdDraw.setCanvasSize(width, height);
		StdDraw.picture(0.5, 0.5, imageFile, 1.0, 1.0);
        StdDraw.show();
        StdDraw.setXscale(minLon, maxLon);
        StdDraw.setYscale(lat2y(minLat), lat2y(maxLat));
    }

    public void drawPointSet(Set<Point> points) {
        for (Point p : points) {
            drawPoint(p);
        }
    }

    private double lat2y(double aLat) {
        return Math.log(Math.tan(Math.PI / 4 + Math.toRadians(aLat) / 2)) * RADIUS;
    }

}
