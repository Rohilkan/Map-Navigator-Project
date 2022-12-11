/**
 * Represents an immutable latitude-longitude coordinate point.
 * Uses degrees from -180.0 to 180.0.
 * Point objects are Hashable, Comparable, and printable.
 * @author Brandon Fain
 */
public class Point implements Comparable<Point> {
    private static final double EARTH_RADIUS = 3963.2;
    private double lat;
    private double lon;
    private String myToString;
    
    public Point(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    /**
     * Calculates the straight line distance between two latitude-longitude 
     * representation points in miles.
     * @param p Another point
     * @return The miles (on a straight line) between this and p.
     */
    public double distance(Point p) {
        double deltaLon = Math.toRadians(lon - p.lon);
        double deltaLat = Math.toRadians(lat - p.lat);
        double deltaX = EARTH_RADIUS * Math.cos(Math.toRadians((lat + p.lat)/2)) * deltaLon;
        double deltaY = EARTH_RADIUS * deltaLat;
        return Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Point)) { 
            return false; 
        }
        Point p = (Point) o;
        if ((lat == p.lat) && (lon == p.lon)) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(lat+lon);
    }

    @Override
    public String toString() {
        if (myToString == null) {
            myToString = "(" + lat + ", " + lon + ")";
        }
        return myToString;
    }

    @Override
    public int compareTo(Point p) {
        int latComp = Double.compare(lat, p.lat);
        if (latComp != 0) { return latComp; }
        return Double.compare(lon, p.lon);
    }
}
