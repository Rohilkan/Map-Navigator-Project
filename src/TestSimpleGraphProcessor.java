import static org.junit.jupiter.api.Assertions.*;

import org.junit.Assert;
import org.junit.jupiter.api.*;

import java.util.Arrays;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.security.InvalidAlgorithmParameterException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.*;


/**
 * Testing GraphProcessor on the family of simple data
 * Intended to help debug with simple tests
 * 
 */

public class TestSimpleGraphProcessor {
	GraphProcessor2 simpleDriver = new GraphProcessor2();
	Map<String, Point> simpleCityLookup;
	String simpleGraphFile = "data/simple.graph";
	String simpleCities = "data/simplecities.csv";

    // Setup to initialize driver before tests
	@BeforeEach
    public void setup() throws Exception {
		try {
			simpleDriver.initialize(new FileInputStream(simpleGraphFile));
			simpleCityLookup = readCities(simpleCities);
		} catch(FileNotFoundException filenotFound) {
			assertTrue(false, "File not found; please follow the project description for instructions on either" +
						"a) changing your settings.json or b) replacing usGraphFile and usCities with their absolute paths!");
		}

    }



    /**
     * Tests that driver returns closest point in graph to a given query point
     */
	@Test
	public void testNearestPoint() {
		String[] froms = new String[]{"A A", "B B", "H H", "K K", "L L"};

		// includes all possible nearest points within 3% of the closest distance
        List<Point[]> pLookedUpRanges = new ArrayList();
		pLookedUpRanges.add(new Point[] {new Point(2, -1)});
        pLookedUpRanges.add(new Point[] {new Point(2, 0)});
        pLookedUpRanges.add(new Point[] {new Point(-1, -1)});
        pLookedUpRanges.add(new Point[] {new Point(2, -1), new Point(1, -1)});
        pLookedUpRanges.add(new Point[] {new Point(-1, -1), new Point(0, 0)});
		

		for (int i = 0; i < froms.length; i++) {
			Point p = simpleCityLookup.get(froms[i]);
			Point nearP = simpleDriver.nearestPoint(p);
            
			assertTrue(pointInRange(nearP, pLookedUpRanges.get(i)), "Your nearest point isn't a valid point whose (distance from the input) is within 3% of the (distance from the input and the true nearest point)!");
		}
	}

    /**
     * Tests that driver returns a List<Point> corresponding to the shortest path from start to end
     * Accepts alternate paths that are ultimately within 3% of the distance of the true shortest path
     * @throws InvalidAlgorithmParameterException
     */
	@Test public void testRoute() throws InvalidAlgorithmParameterException {
		// A to F
		List<Point> resRoute1 = simpleDriver.route(new Point(2, -1), new Point(1, 1));
		List<Point> trueRoute1 = Arrays.asList(new Point(2, -1), new Point(2, 0), new Point(1, 1));
		double trueRouteDist1 = 166.93;
        assertFalse(resRoute1 == null, "Your route is null!");
		assertTrue(checkPaths(resRoute1, trueRoute1, simpleDriver.routeDistance(resRoute1), trueRouteDist1), "Your route was not close to the true shortest path between the start and ending points!");

		// A to B
		List<Point>resRoute2 = simpleDriver.route(new Point(2, -1), new Point(2, 0));
		List<Point> trueRoute2 = Arrays.asList(new Point(2, -1), new Point(2, 0));
		assertFalse(resRoute2 == null, "Your route is null!");
		assertTrue(resRoute2.size() == trueRoute2.size() && 
			resRoute2.get(0).equals(trueRoute2.get(0)) && resRoute2.get(1).equals(trueRoute2.get(1))); // no ambiguity

        // D to J (1, -1) to (-1, 1)
        assertThrows(InvalidAlgorithmParameterException.class, ()->simpleDriver.route(new Point(1, -1), new Point(-1, 1)));
	
	}

    /**
     * Tests that driver returns the distance along a given route represented as a List<Point> input
     * Tests only if .routeDistsance() is correct (i.e. can pass even if .route() is incorect)
     */
	@Test 
	public void testRouteDistance() {
		List<Point> route1 = Arrays.asList(new Point(1, 0), new Point(1, 1), new Point(2, 1)); // E to C
        List<Point> route2 = Arrays.asList(new Point(2, 0), new Point(1, 0)); // B to E
        List<Point> route3 = Arrays.asList(new Point(0, 0), new Point(-1, 1));
		List<List<Point>> routes = Arrays.asList(route1, route2, route3);
		double[] targetDists = new double[] {138.331, 69.17, 97.82};

		for (int i = 0; i < routes.size(); i++) {
			double routeDist = simpleDriver.routeDistance(routes.get(i));
			assertTrue(inRange(routeDist, targetDists[i]),
				"Your route distance is not within rounding error (+/- 0.03) of the actual route distance! This test is designed so that it passes if your .routeDistance() is correct, even if your .route() is incorrect");
		}
	}

    /**
     * Tests that driver returns true if two inputs are connected in the graph
     */
	@Test
	public void testConnected() {
		// A to J
		assertFalse(simpleDriver.connected(new Point(2, -1), new Point(-1, 1)), 
			"You mistakenly claim two points representing San Juan PR and Seattle WA's nearest points, respectively, are connected. This test is designed if .connected() is correct, even if .nearestPoint() is faulty"); 
		// G to H
		assertTrue(simpleDriver.connected(new Point(0, 0), new Point(-1, -1)),
		   "You mistakenly claim two points representing Durham NC and Raleigh NC's nearest points, respectively, are not connected. This test is designed if .connected() is correct, even if .nearestPoint() is faulty"); 
	}
 
    // helper method to check if a point's distance to input is within 3% of the true nearest point's distance to input
	private static boolean inRange(double resPathDist, double truePathDist) {
		return (resPathDist > 0.97 * truePathDist && resPathDist < 1.03 * truePathDist);
	}

    // helper method to check if a route is the true shortest path, or within 3% of the overall distance of the true shortest path
    private static boolean checkPaths(List<Point> r1, List<Point> r2, double resPathDist, double truePathDist) {
        if (r1.size() == r2.size()) {
            for (int i = 0; i < r1.size(); i++) {
                if (!(r1.get(i).equals(r2.get(i)))) return false;
            }
        } else {
            if (!inRange(resPathDist, truePathDist)) return false;
			if (!(r1.get(0).equals(r2.get(0)) && r1.get(r1.size() - 1).equals(r2.get(r2.size() - 1)))) return false;
        }
        return true;
    }

    // checks if a range of points contains a certain result point
    private static boolean pointInRange(Point result, Point[] ranged) {
		for (Point p : ranged) {
			if (p.equals(result)) return true;
		}

		return false;
	}

    // reads cities from a file
	private static Map<String, Point> readCities(String fileName) throws FileNotFoundException {
		Scanner reader = new Scanner(new File(fileName));
		Map<String, Point> cityLookup = new HashMap<>(); 
		while (reader.hasNextLine()) {
			try {
				String[] info = reader.nextLine().split(",");
				cityLookup.put(info[0] + " " + info[1], 
				new Point(Double.parseDouble(info[2]),
				Double.parseDouble(info[3])));
			} catch(Exception e) {
				continue;    
			}
		}
		return cityLookup;
	}
}