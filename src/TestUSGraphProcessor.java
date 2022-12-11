import static org.junit.jupiter.api.Assertions.*;
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
 * Testing GraphProcessor on the family of US data
 * Precursor to autograder, which tests on US data
 * 
 * @author Emily Du
 * @author Havish Malladi
 */

public class TestUSGraphProcessor {
	GraphProcessor2 usDriver = new GraphProcessor2();
	Map<String, Point> usCityLookup;
	String usGraphFile = "data/usa.graph";
	String usCities = "data/uscities.csv";

	// Setup to initialize driver before tests
	@BeforeEach
    public void setup() throws Exception {
		try {
			usDriver.initialize(new FileInputStream(usGraphFile));
			usCityLookup = readCities(usCities);
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
		String[] froms = new String[]{"Durham NC", "Seattle WA"};
		String[] dests = new String[]{"China TX", "Redmond WA"};

		// includes all possible nearest points within 3% of the closest distance

		List<Point[]> pLookedUpRanges = new ArrayList();
		pLookedUpRanges.add(new Point[] {new Point(35.989709, -78.902124)});
		pLookedUpRanges.add(new Point[] {new Point(47.625719, -122.328043)});
		List<Point[]> qLookedUpRanges = new ArrayList();
		qLookedUpRanges.add(new Point[] {new Point(30.047564, -94.33527)});
		qLookedUpRanges.add(new Point[] {new Point(47.679175, -122.184502), new Point(47.669571, -122.186937)}); 
		

		for (int i = 0; i < froms.length; i++) {
			Point p = usCityLookup.get(froms[i]);
			Point q = usCityLookup.get(dests[i]);
			Point nearP = usDriver.nearestPoint(p);
			Point nearQ = usDriver.nearestPoint(q);
			
			assertTrue(pointInRange(nearP, pLookedUpRanges.get(i)), "Your nearest point isn't a valid point whose (distance from the input) is within 3% of the (distance from the input and the true nearest point)!");
			assertTrue(pointInRange(nearQ, qLookedUpRanges.get(i)), "Your nearest point isn't a valid point whose (distance from the input) is within 3% of the (distance from the input and the true nearest point)!");

		}
	}

	/**
     * Tests that driver returns a List<Point> corresponding to the shortest path from start to end
     * Accepts alternate paths that are ultimately within 3% of the distance of the true shortest path
     * @throws InvalidAlgorithmParameterException
     */
	@Test public void testRoute() throws InvalidAlgorithmParameterException {
		// Bellevue WA to Clyde Hill WA
		List<Point> shortRoute = usDriver.route(new Point(47.578813, -122.139773), new Point(47.632292, -122.187898));
		double shortDist = usDriver.routeDistance(shortRoute);
		assertFalse(shortRoute == null, "Your route is null!");
		checkShortPath(shortRoute, shortDist);

		// Los Angeles CA to Sunnyvale CA
		List<Point> medRoute = usDriver.route(new Point(34.154423, -118.396488), new Point(37.398938, -122.02777));
		double medDist = usDriver.routeDistance(medRoute);
		assertFalse(medRoute == null, "Your route is null!");
		checkMedPath(medRoute, medDist);

		// Miami FL to Portland OR
		List<Point> longRoute = usDriver.route(new Point(25.781443, -80.206716), new Point(45.529817, -122.647848));
		double longDist = usDriver.routeDistance(longRoute);
		assertFalse(longRoute == null, "Your route is null!");
		checkLongPath(longRoute, longDist);

		// San Juan PR to Seattle WA (not connected)
		assertThrows(InvalidAlgorithmParameterException.class, ()->usDriver.route(new Point(18.399426, -66.071025), new Point(47.625719, -122.328043)));
	}

	/**
     * Tests that driver returns the distance along a given route represented as a List<Point> input
     * Tests only if .routeDistsance() is correct (i.e. can pass even if .route() is incorect)
     */
	@Test 
	public void testRouteDistance() {
		List<Point> route1 = Arrays.asList(new Point(35.9792, -78.9022),  new Point(35.890653, -78.750076), new Point(35.835315, -78.669448), new Point(35.834585, -78.638592), new Point(35.8324, -78.6429));
		List<Point> route2 = Arrays.asList(new Point(35.665571, -77.398172), new Point(35.629082, -77.433207));
		List<List<Point>> routes = Arrays.asList(route1, route2);
		double[] targetDists = new double[] {18.43, 3.20};

		for (int i = 0; i < routes.size(); i++) {
			double routeDist = usDriver.routeDistance(routes.get(i));
			assertTrue(inRange(routeDist, targetDists[i]), 
				"Your route distance is not within rounding error (+/- 0.03) of the actual route distance! This test is designed so that it passes if your .routeDistance() is correct, even if your .route() is incorrect");
		}
	}

	/**
     * Tests that driver returns true if two inputs are connected in the graph
     */
	@Test
	public void testConnected() {
		// San Juan PR to Seattle WA
		assertFalse(usDriver.connected(new Point(18.399426, -66.071025), new Point(47.625719, -122.328043)), 
			"You mistakenly claim two points representing San Juan PR and Seattle WA's nearest points, respectively, are connected. This test is designed if .connected() is correct, even if .nearestPoint() is faulty"); 
		// Durham NC to Raleigh NC
		assertTrue(usDriver.connected(new Point(35.989709, -78.902124), new Point(35.834585, -78.638592)),
		   "You mistakenly claim two points representing Durham NC and Raleigh NC's nearest points, respectively, are not connected. This test is designed if .connected() is correct, even if .nearestPoint() is faulty"); 
	}
 
	// helper method to check if a point's distance to input is within 3% of the true nearest point's distance to input
	private boolean inRange(double res, double target) {
		return (res > target - 0.05 && res < target + 0.05);
	}

	// helper method to check if a route is among the ranged options 
	// ranged options are the true shortest path or points within 3% of true shortest dist
	private static boolean pointInRange(Point result, Point[] ranged) {
		for (Point p : ranged) {
			if (p.equals(result)) return true;
		}

		return false;
	}

	// checks if a range of points contains a certain result point
	private static boolean checkPaths(List<Point> r1, List<Point> r2, double resPathDist, double truePathDist) {
        if (r1.size() == r2.size()) {
            for (int i = 0; i < r1.size(); i++) {
                if (!(r1.get(i).equals(r2.get(i)))) return false;
            }
        } else {
            if (!(resPathDist > 0.97 * truePathDist && resPathDist < 1.03 * truePathDist)) return false;
			if (!(r1.get(0).equals(r2.get(0)) && r1.get(r1.size() - 1).equals(r2.get(r2.size() - 1)))) return false;
        }
        return true;
    }

	// check short route test case, refactored as separate method for readability
	private static boolean checkShortPath(List<Point>resRoute, double resRouteDist) {
		List<Point> trueRoute = Arrays.asList(new Point(47.578813, -122.139773), new Point(47.580228, -122.174438), new Point(47.603103, -122.184024), new Point(47.61349, -122.188708), new Point(47.617298, -122.188659), new Point(47.632292, -122.187898));
		double trueRouteDist = 5.32;

		return checkPaths(resRoute, trueRoute, resRouteDist, trueRouteDist);
	}

	// check medium length route test case, refactored as separate method for readability
	private static boolean checkMedPath(List<Point>resRoute, double resRouteDist) {
		List<Point> trueRoute = Arrays.asList(
			new Point(34.154423, -118.396488), new Point(34.156727, -118.413778), new Point(34.155630, -118.431244), new Point(34.156984, -118.448711), new Point(34.159474, -118.466145), new Point(34.160522, -118.469514), 
			new Point(34.164495, -118.473504), new Point(34.165431, -118.492355), new Point(34.170443, -118.501105), new Point(34.171201, -118.518555), new Point(34.173234, -118.535985), 
			new Point(34.173554, -118.553467), new Point(34.172213, -118.570933), new Point(34.168076, -118.588164), new Point(34.169461, -118.597433), new Point(34.170718, -118.605807), 
			new Point(34.168960, -118.612679), new Point(34.163344, -118.626862), new Point(34.159461, -118.637394), new Point(34.153357, -118.652108), new Point(34.148522, -118.697845), 
			new Point(34.140882, -118.709614), new Point(34.138138, -118.724538), new Point(34.143058, -118.738115), new Point(34.145997, -118.761182), new Point(34.146907, -118.781080), 
			new Point(34.149060, -118.804070), new Point(34.157596, -118.825089), new Point(34.166066, -118.837823), new Point(34.176050, -118.858520), new Point(34.176168, -118.860601), 
			new Point(34.177362, -118.876550), new Point(34.183481, -118.891565), new Point(34.184355, -118.911134), new Point(34.184427, -118.925720), new Point(34.189769, -118.939297), 
			new Point(34.205245, -118.983586), new Point(34.213674, -119.007747), new Point(34.215744, -119.026154), new Point(34.216762, -119.034049), new Point(34.217667, -119.050523), 
			new Point(34.219175, -119.069835), new Point(34.220390, -119.085445), new Point(34.221743, -119.101973), new Point(34.222058, -119.126837), new Point(34.222582, -119.142341), 
			new Point(34.225136, -119.158751), new Point(34.231838, -119.173234), new Point(34.239360, -119.181007), new Point(34.241590, -119.187256), new Point(34.243537, -119.192723), 
			new Point(34.252552, -119.210989), new Point(34.262289, -119.230918), new Point(34.265043, -119.238846), new Point(34.264258, -119.243411), new Point(34.271753, -119.257112), 
			new Point(34.273593, -119.266392), new Point(34.276510, -119.281139), new Point(34.278266, -119.293064), new Point(34.278235, -119.300709), new Point(34.281058, -119.300741), 
			new Point(34.281630, -119.305606), new Point(34.282731, -119.317028), new Point(34.290029, -119.334365), new Point(34.318062, -119.365082), new Point(34.322386, -119.375596), 
			new Point(34.329915, -119.397826), new Point(34.341097, -119.411216), new Point(34.343237, -119.416693), new Point(34.373509, -119.458895), new Point(34.376259, -119.477542), 
			new Point(34.383764, -119.484595), new Point(34.388328, -119.497980), new Point(34.396818, -119.511557), new Point(34.401156, -119.516530), new Point(34.403302, -119.526604), 
			new Point(34.405529, -119.538503), new Point(34.410795, -119.552976), new Point(34.416584, -119.582604), new Point(34.421208, -119.601615), new Point(34.421616, -119.614200), 
			new Point(34.422426, -119.631527), new Point(34.420823, -119.639971), new Point(34.422667, -119.654511), new Point(34.421390, -119.665779), new Point(34.420350, -119.677184), 
			new Point(34.418261, -119.689699), new Point(34.412212, -119.699161), new Point(34.416385, -119.707294), new Point(34.421811, -119.715212), new Point(34.425385, -119.720329), 
			new Point(34.426669, -119.725684), new Point(34.428018, -119.733708), new Point(34.436814, -119.751626), new Point(34.441229, -119.760011), new Point(34.440902, -119.769264), 
			new Point(34.442327, -119.789305), new Point(34.441920, -119.808414), new Point(34.441623, -119.812147), new Point(34.439535, -119.832376), new Point(34.437668, -119.852916), 
			new Point(34.434478, -119.870817), new Point(34.432332, -119.905907), new Point(34.447681, -119.959506), new Point(34.460934, -120.003762), new Point(34.463285, -120.021440), 
			new Point(34.465585, -120.069070), new Point(34.465753, -120.103430), new Point(34.474656, -120.142166), new Point(34.473435, -120.206856), new Point(34.477861, -120.228494), 
			new Point(34.509200, -120.225894), new Point(34.529301, -120.196647), new Point(34.602742, -120.191856), new Point(34.612133, -120.190151), new Point(34.618367, -120.189303), 
			new Point(34.652820, -120.182790), new Point(34.675833, -120.158029), new Point(34.687847, -120.157675), new Point(34.707994, -120.172577), new Point(34.731403, -120.236049), 
			new Point(34.742573, -120.270832), new Point(34.791691, -120.332930), new Point(34.825168, -120.358948), new Point(34.865414, -120.396187), new Point(34.879977, -120.408810), 
			new Point(34.891185, -120.417677), new Point(34.923603, -120.417903), new Point(34.938345, -120.417774), new Point(34.952997, -120.417280), new Point(34.967658, -120.422977), 
			new Point(34.981302, -120.431426), new Point(34.996262, -120.434725), new Point(35.036866, -120.484604), new Point(35.055237, -120.501132), new Point(35.071888, -120.516286), 
			new Point(35.116188, -120.573310), new Point(35.121370, -120.583732), new Point(35.124279, -120.593303), new Point(35.130119, -120.606638), new Point(35.136155, -120.621928), 
			new Point(35.139318, -120.635612), new Point(35.141432, -120.638656), new Point(35.146968, -120.644935), new Point(35.150929, -120.654602), new Point(35.164156, -120.687159), 
			new Point(35.179754, -120.699449), new Point(35.196063, -120.699411), new Point(35.224079, -120.691515), new Point(35.244664, -120.682153), new Point(35.256860, -120.674842), 
			new Point(35.266102, -120.673464), new Point(35.274415, -120.671356), new Point(35.283327, -120.667552), new Point(35.286698, -120.663528), new Point(35.289903, -120.659355), 
			new Point(35.291878, -120.653170), new Point(35.291822, -120.649608), new Point(35.298735, -120.626793), new Point(35.325529, -120.620259), new Point(35.370628, -120.641106), 
			new Point(35.383181, -120.630323), new Point(35.398526, -120.622501), new Point(35.443532, -120.638278), new Point(35.452586, -120.640901), new Point(35.465922, -120.651293), 
			new Point(35.476503, -120.658363), new Point(35.485283, -120.665095), new Point(35.488608, -120.670872), new Point(35.497028, -120.682991), new Point(35.513199, -120.699416), 
			new Point(35.526560, -120.704513), new Point(35.543707, -120.714437), new Point(35.554365, -120.712055), new Point(35.565829, -120.702002), new Point(35.589368, -120.695565), 
			new Point(35.611395, -120.691144), new Point(35.615303, -120.690480), new Point(35.627721, -120.691456), new Point(35.639153, -120.692454), new Point(35.653518, -120.693870), 
			new Point(35.740678, -120.699824), new Point(35.747879, -120.700436), new Point(35.770594, -120.705789), new Point(35.775186, -120.712253), new Point(35.799237, -120.742681), 
			new Point(35.815429, -120.754659), new Point(35.845996, -120.762405), new Point(35.864682, -120.818040), new Point(35.874038, -120.835678), new Point(35.902460, -120.842743), 
			new Point(35.926482, -120.869179), new Point(35.947026, -120.876029), new Point(35.975493, -120.899461), new Point(36.012962, -120.922238), new Point(36.093838, -121.021561), 
			new Point(36.122026, -121.023400), new Point(36.187559, -121.072335), new Point(36.201321, -121.112595), new Point(36.203330, -121.129552), new Point(36.204265, -121.137636), 
			new Point(36.197559, -121.147898), new Point(36.233811, -121.174393), new Point(36.276092, -121.185465), new Point(36.314295, -121.234254), new Point(36.323761, -121.238186), 
			new Point(36.329371, -121.243862), new Point(36.339388, -121.254913), new Point(36.403405, -121.316603), new Point(36.419431, -121.323679), new Point(36.431642, -121.337322), 
			new Point(36.448999, -121.362888), new Point(36.464945, -121.387719), new Point(36.493379, -121.427341), new Point(36.503893, -121.441777), new Point(36.507351, -121.445752), 
			new Point(36.523542, -121.464136), new Point(36.569364, -121.517951), new Point(36.625146, -121.583805), new Point(36.660803, -121.622890), new Point(36.666552, -121.628120), 
			new Point(36.670863, -121.639144), new Point(36.677717, -121.641333), new Point(36.687100, -121.652813), new Point(36.699316, -121.651912), new Point(36.721523, -121.656182), 
			new Point(36.722026, -121.659835), new Point(36.743729, -121.660500), new Point(36.789491, -121.667560), new Point(36.799354, -121.664223), new Point(36.818355, -121.632836), 
			new Point(36.845195, -121.634633), new Point(36.857948, -121.627954), new Point(36.861927, -121.579568), new Point(36.882539, -121.561548), new Point(36.904680, -121.556667), 
			new Point(36.928282, -121.547370), new Point(36.961213, -121.551060), new Point(36.986927, -121.558415), new Point(37.002990, -121.556468), new Point(37.022385, -121.566650), 
			new Point(37.059359, -121.584545), new Point(37.088858, -121.599405), new Point(37.119255, -121.626276), new Point(37.131522, -121.633544), new Point(37.152963, -121.652502), 
			new Point(37.190715, -121.693228), new Point(37.209738, -121.723114), new Point(37.240271, -121.766088), new Point(37.244080, -121.772745), new Point(37.257228, -121.796300), 
			new Point(37.281791, -121.808359), new Point(37.294677, -121.810028), new Point(37.303126, -121.816707), new Point(37.318435, -121.831534), new Point(37.335864, -121.848388), 
			new Point(37.339626, -121.852020), new Point(37.349887, -121.861660), new Point(37.354168, -121.866124), new Point(37.363029, -121.891487), new Point(37.364176, -121.901840), 
			new Point(37.368614, -121.908717), new Point(37.372177, -121.919393), new Point(37.373756, -121.927718), new Point(37.376730, -121.941623), new Point(37.381979, -121.963938), 
			new Point(37.385691, -121.976845), new Point(37.391193, -121.996007), new Point(37.395660, -122.012808), new Point(37.398938, -122.027770)
		);
		double trueRouteDist = 373.91;

		return checkPaths(resRoute, trueRoute, resRouteDist, trueRouteDist);
	}

	// check short route test case, refactored as separate method for readability
	private static boolean checkLongPath(List<Point> resRoute, double resRouteDist) {
		List<Point> trueRoute = Arrays.asList(
			new Point(25.781443, -80.206716), new Point(25.787857, -80.206912), new Point(25.795235, -80.207019), new Point(25.803600, -80.207306), new Point(25.810050, -80.207534), new Point(25.810427, -80.195327), 
			new Point(25.810615, -80.189456), new Point(25.811414, -80.189378), new Point(25.825515, -80.186918), new Point(25.833337, -80.184075), new Point(25.849033, -80.184700), 
			new Point(25.856455, -80.184915), new Point(25.874807, -80.170342), new Point(25.885248, -80.165002), new Point(25.889393, -80.163749), new Point(25.899927, -80.160550), 
			new Point(25.926100, -80.153796), new Point(25.947440, -80.146994), new Point(25.955094, -80.146827), new Point(25.963625, -80.146927), new Point(25.985662, -80.142367), 
			new Point(25.996737, -80.142689), new Point(26.011512, -80.143054), new Point(26.026245, -80.143257), new Point(26.033593, -80.143306), new Point(26.041023, -80.143359), 
			new Point(26.048754, -80.143536), new Point(26.052320, -80.143680), new Point(26.059260, -80.143847), new Point(26.063332, -80.142372), new Point(26.070565, -80.134578), 
			new Point(26.080575, -80.134527), new Point(26.092786, -80.136546), new Point(26.100277, -80.136780), new Point(26.107667, -80.136973), new Point(26.119214, -80.137204), 
			new Point(26.122440, -80.137270), new Point(26.129925, -80.137507), new Point(26.137048, -80.136324), new Point(26.137400, -80.121167), new Point(26.167193, -80.116867), 
			new Point(26.189600, -80.114888), new Point(26.204147, -80.108633), new Point(26.231761, -80.102815), new Point(26.250069, -80.101013), new Point(26.258854, -80.099773), 
			new Point(26.275479, -80.097440), new Point(26.305063, -80.092890), new Point(26.318000, -80.090761), new Point(26.330210, -80.090823), new Point(26.340944, -80.086193), 
			new Point(26.350500, -80.086400), new Point(26.361772, -80.082369), new Point(26.386060, -80.076521), new Point(26.394633, -80.076570), new Point(26.439754, -80.071243), 
			new Point(26.461660, -80.067541), new Point(26.514554, -80.058990), new Point(26.527117, -80.058092), new Point(26.528900, -80.058132), new Point(26.549081, -80.056000), 
			new Point(26.571976, -80.053596), new Point(26.584418, -80.051936), new Point(26.586745, -80.051982), new Point(26.592161, -80.052419), new Point(26.601349, -80.057266), 
			new Point(26.608587, -80.057604), new Point(26.616265, -80.057346), new Point(26.627581, -80.056947), new Point(26.655051, -80.056008), new Point(26.664637, -80.055544), 
			new Point(26.675700, -80.054739), new Point(26.690317, -80.054380), new Point(26.705931, -80.053728), new Point(26.705976, -80.055166), new Point(26.714415, -80.054927), 
			new Point(26.718134, -80.054814), new Point(26.718447, -80.053400), new Point(26.724437, -80.053213), new Point(26.735900, -80.052845), new Point(26.735995, -80.056847), 
			new Point(26.745845, -80.056544), new Point(26.752868, -80.056161), new Point(26.783404, -80.054800), new Point(26.807389, -80.055876), new Point(26.844134, -80.061280), 
			new Point(26.862807, -80.055469), new Point(26.881589, -80.056442), new Point(26.933955, -80.080869), new Point(26.944237, -80.084555), new Point(26.952300, -80.085190), 
			new Point(26.959300, -80.084745), new Point(27.041917, -80.114654), new Point(27.045637, -80.119922), new Point(27.059458, -80.136638), new Point(27.098860, -80.159310), 
			new Point(27.133924, -80.206777), new Point(27.165927, -80.227071), new Point(27.177925, -80.236773), new Point(27.186839, -80.245592), new Point(27.191100, -80.253056), 
			new Point(27.212532, -80.259979), new Point(27.215893, -80.261116), new Point(27.245518, -80.272317), new Point(27.271076, -80.288360), new Point(27.297984, -80.304080), 
			new Point(27.305398, -80.308380), new Point(27.326954, -80.320962), new Point(27.374235, -80.325876), new Point(27.410712, -80.325924), new Point(27.425314, -80.325945), 
			new Point(27.439631, -80.325916), new Point(27.443253, -80.325910), new Point(27.446996, -80.325983), new Point(27.455150, -80.327241), new Point(27.470128, -80.333847), 
			new Point(27.484393, -80.340805), new Point(27.498543, -80.345424), new Point(27.520700, -80.355608), new Point(27.548966, -80.368665), new Point(27.586721, -80.377892), 
			new Point(27.608839, -80.383787), new Point(27.616062, -80.388009), new Point(27.632552, -80.389184), new Point(27.638424, -80.389152), new Point(27.639833, -80.389109), 
			new Point(27.639752, -80.397378), new Point(27.663200, -80.404657), new Point(27.689575, -80.412498), new Point(27.719010, -80.420987), new Point(27.748522, -80.435575), 
			new Point(27.810398, -80.466946), new Point(27.841071, -80.486886), new Point(27.880337, -80.501244), new Point(27.963843, -80.541700), new Point(28.004391, -80.563747), 
			new Point(28.036434, -80.582362), new Point(28.054352, -80.589126), new Point(28.079913, -80.603524), new Point(28.093832, -80.610200), new Point(28.116100, -80.623134), 
			new Point(28.121617, -80.630945), new Point(28.129004, -80.630285), new Point(28.157548, -80.641341), new Point(28.204732, -80.661744), new Point(28.229470, -80.673000), 
			new Point(28.261593, -80.688009), new Point(28.294096, -80.699784), new Point(28.355670, -80.732533), new Point(28.378143, -80.738684), new Point(28.400331, -80.752792), 
			new Point(28.446777, -80.763068), new Point(28.490971, -80.775500), new Point(28.527772, -80.789493), new Point(28.557506, -80.797837), new Point(28.584286, -80.801313), 
			new Point(28.607985, -80.808000), new Point(28.615381, -80.807909), new Point(28.621036, -80.819831), new Point(28.665600, -80.844963), new Point(28.781076, -80.882083), 
			new Point(28.797410, -80.880935), new Point(28.833668, -80.841769), new Point(28.864113, -80.851422), new Point(28.972857, -80.895966), new Point(29.004270, -80.914049), 
			new Point(29.020907, -80.925231), new Point(29.023569, -80.926387), new Point(29.063553, -80.944779), new Point(29.086500, -80.968648), new Point(29.108055, -80.973213), 
			new Point(29.143222, -80.987359), new Point(29.183754, -81.008511), new Point(29.197452, -81.015654), new Point(29.210835, -81.022800), new Point(29.226535, -81.031200), 
			new Point(29.245856, -81.041695), new Point(29.283068, -81.062027), new Point(29.299038, -81.085045), new Point(29.335920, -81.131115), new Point(29.403773, -81.155200), 
			new Point(29.475827, -81.183724), new Point(29.553939, -81.218732), new Point(29.599544, -81.248043), new Point(29.662410, -81.287557), new Point(29.680205, -81.317636), 
			new Point(29.747989, -81.346561), new Point(29.825541, -81.379158), new Point(29.917452, -81.412020), new Point(29.986818, -81.462231), new Point(30.044718, -81.496600), 
			new Point(30.065076, -81.498165), new Point(30.094541, -81.498190), new Point(30.114617, -81.514156), new Point(30.141416, -81.540624), new Point(30.166494, -81.555080), 
			new Point(30.184773, -81.558131), new Point(30.196500, -81.569865), new Point(30.206247, -81.576965), new Point(30.220740, -81.585752), new Point(30.244185, -81.600252), 
			new Point(30.265065, -81.617579), new Point(30.266172, -81.618352), new Point(30.287700, -81.633620), new Point(30.305792, -81.645402), new Point(30.306397, -81.644812), 
			new Point(30.310037, -81.647730), new Point(30.313668, -81.652177), new Point(30.314050, -81.658429), new Point(30.316307, -81.659035), new Point(30.318546, -81.658939), 
			new Point(30.325136, -81.657973), new Point(30.326073, -81.657348), new Point(30.327226, -81.657018), new Point(30.331375, -81.655810), new Point(30.332495, -81.655492), 
			new Point(30.334183, -81.663461), new Point(30.336361, -81.669311), new Point(30.340790, -81.675536), new Point(30.350831, -81.701371), new Point(30.353178, -81.707245), 
			new Point(30.357464, -81.710805), new Point(30.370681, -81.724192), new Point(30.412137, -81.747468), new Point(30.439815, -81.764481), new Point(30.510737, -81.793208), 
			new Point(30.560173, -81.825461), new Point(30.563680, -81.829769), new Point(30.571046, -81.835468), new Point(30.589524, -81.829779), new Point(30.629641, -81.852066), 
			new Point(30.690944, -81.917737), new Point(30.774141, -81.978545), new Point(30.776427, -81.978993), new Point(30.831613, -82.006078), new Point(30.832700, -82.006384), 
			new Point(30.838398, -82.006829), new Point(30.843649, -82.006953), new Point(30.867724, -82.012768), new Point(30.917620, -82.085048), new Point(30.936897, -82.104038), 
			new Point(31.000492, -82.128376), new Point(31.047833, -82.164989), new Point(31.065640, -82.186881), new Point(31.129944, -82.266709), new Point(31.206777, -82.337052), 
			new Point(31.210127, -82.345308), new Point(31.212095, -82.355849), new Point(31.213923, -82.353475), new Point(31.218637, -82.358558), new Point(31.226640, -82.366731), 
			new Point(31.258949, -82.379190), new Point(31.290885, -82.448283), new Point(31.339609, -82.463797), new Point(31.364979, -82.463347), new Point(31.438335, -82.432721), 
			new Point(31.532878, -82.463889), new Point(31.539225, -82.462698), new Point(31.554260, -82.466158), new Point(31.628455, -82.460536), new Point(31.748984, -82.545100), 
			new Point(31.773637, -82.561644), new Point(31.821555, -82.568296), new Point(31.853070, -82.592780), new Point(31.865307, -82.588558), new Point(31.869649, -82.594668), 
			new Point(31.893810, -82.611147), new Point(31.909348, -82.667560), new Point(31.929410, -82.679463), new Point(31.932679, -82.681121), new Point(31.947497, -82.707685), 
			new Point(31.989455, -82.748514), new Point(32.003582, -82.794106), new Point(32.035888, -82.837778), new Point(32.068470, -82.900750), new Point(32.102358, -82.958423), 
			new Point(32.104602, -83.064875), new Point(32.106443, -83.074118), new Point(32.121846, -83.114898), new Point(32.174364, -83.161628), new Point(32.192830, -83.175275), 
			new Point(32.197810, -83.178558), new Point(32.226779, -83.227615), new Point(32.286389, -83.252474), new Point(32.339270, -83.298796), new Point(32.344900, -83.303297), 
			new Point(32.373370, -83.333200), new Point(32.383980, -83.327324), new Point(32.397240, -83.328837), new Point(32.402130, -83.335987), new Point(32.418864, -83.355605), 
			new Point(32.445582, -83.379037), new Point(32.494334, -83.401793), new Point(32.526661, -83.439778), new Point(32.543050, -83.440856), new Point(32.630412, -83.497188), 
			new Point(32.708960, -83.544600), new Point(32.732320, -83.562795), new Point(32.799946, -83.569500), new Point(32.839816, -83.619497), new Point(32.843034, -83.623574), 
			new Point(32.847469, -83.626739), new Point(32.856248, -83.640558), new Point(32.873938, -83.662262), new Point(32.902966, -83.686595), new Point(32.919124, -83.709072), 
			new Point(32.928130, -83.731452), new Point(32.939935, -83.754358), new Point(32.940232, -83.781245), new Point(32.958581, -83.813013), new Point(32.991405, -83.844556), 
			new Point(33.028131, -83.910087), new Point(33.035372, -83.921857), new Point(33.042900, -83.937982), new Point(33.053664, -83.951104), new Point(33.086589, -83.993944), 
			new Point(33.112258, -84.002860), new Point(33.166349, -84.040282), new Point(33.192264, -84.058177), new Point(33.207220, -84.061000), new Point(33.261477, -84.095289), 
			new Point(33.281499, -84.107627), new Point(33.352909, -84.125040), new Point(33.405749, -84.154726), new Point(33.411266, -84.161249), new Point(33.427394, -84.181613), 
			new Point(33.455613, -84.206686), new Point(33.462729, -84.210269), new Point(33.481934, -84.217200), new Point(33.495329, -84.221406), new Point(33.505985, -84.230944), 
			new Point(33.542951, -84.268774), new Point(33.553207, -84.268742), new Point(33.577782, -84.277925), new Point(33.581456, -84.291033), new Point(33.613100, -84.305488), 
			new Point(33.628534, -84.312940), new Point(33.657746, -84.335160), new Point(33.667917, -84.340357), new Point(33.687684, -84.349160), new Point(33.704423, -84.349532), 
			new Point(33.740204, -84.349256), new Point(33.745405, -84.349235), new Point(33.747610, -84.349224), new Point(33.761975, -84.349220), new Point(33.769968, -84.348964), 
			new Point(33.773840, -84.348945), new Point(33.773797, -84.359970), new Point(33.773654, -84.370934), new Point(33.772537, -84.377700), new Point(33.771897, -84.381824), 
			new Point(33.771866, -84.383342), new Point(33.771264, -84.387403), new Point(33.771273, -84.390238), new Point(33.781528, -84.391050), new Point(33.789968, -84.391238), 
			new Point(33.796076, -84.393539), new Point(33.800476, -84.397820), new Point(33.802865, -84.407820), new Point(33.805263, -84.413259), new Point(33.831719, -84.426477), 
			new Point(33.848828, -84.430929), new Point(33.863075, -84.437914), new Point(33.865000, -84.439610), new Point(33.881051, -84.453127), new Point(33.886453, -84.457902), 
			new Point(33.890189, -84.460831), new Point(33.901838, -84.473469), new Point(33.911791, -84.479365), new Point(33.922771, -84.485443), new Point(33.941090, -84.504519), 
			new Point(33.950863, -84.516599), new Point(33.961613, -84.519861), new Point(33.984479, -84.542112), new Point(33.999424, -84.561725), new Point(34.009803, -84.567733), 
			new Point(34.025695, -84.573473), new Point(34.033555, -84.577056), new Point(34.053931, -84.593611), new Point(34.064526, -84.608191), new Point(34.081400, -84.629680), 
			new Point(34.079331, -84.653757), new Point(34.080815, -84.676486), new Point(34.089210, -84.722000), new Point(34.118684, -84.739442), new Point(34.142534, -84.738922), 
			new Point(34.174548, -84.759210), new Point(34.188722, -84.766914), new Point(34.206558, -84.761571), new Point(34.218589, -84.752580), new Point(34.233856, -84.756153), 
			new Point(34.245342, -84.773512), new Point(34.259407, -84.798950), new Point(34.273123, -84.811288), new Point(34.295365, -84.823487), new Point(34.377189, -84.911104), 
			new Point(34.388248, -84.918298), new Point(34.441973, -84.919987), new Point(34.471122, -84.919081), new Point(34.511755, -84.918609), new Point(34.541021, -84.921516), 
			new Point(34.556866, -84.935980), new Point(34.577009, -84.950387), new Point(34.653470, -84.983894), new Point(34.687671, -85.001988), new Point(34.708157, -85.007272), 
			new Point(34.761015, -85.001972), new Point(34.793524, -85.001080), new Point(34.809110, -85.017270), new Point(34.854550, -85.018290), new Point(34.860651, -85.025564), 
			new Point(34.892443, -85.074504), new Point(34.903214, -85.089476), new Point(34.910210, -85.126790), new Point(34.915260, -85.143925), new Point(34.928190, -85.152410), 
			new Point(34.967800, -85.191196), new Point(34.985916, -85.201591), new Point(34.989939, -85.203840), new Point(34.995814, -85.237427), new Point(35.003452, -85.259850), 
			new Point(35.009392, -85.271630), new Point(35.014498, -85.272532), new Point(35.016479, -85.280406), new Point(35.018566, -85.293533), new Point(35.024079, -85.300126), 
			new Point(35.028780, -85.309862), new Point(35.030778, -85.314964), new Point(35.032272, -85.318682), new Point(35.018715, -85.332103), new Point(35.027747, -85.347751), 
			new Point(35.027598, -85.363576), new Point(35.018676, -85.383280), new Point(34.982968, -85.409356), new Point(34.977470, -85.417065), new Point(34.968600, -85.428915), 
			new Point(34.971650, -85.449976), new Point(34.983140, -85.465178), new Point(34.991481, -85.476723), new Point(34.987675, -85.504500), new Point(35.002924, -85.512310), 
			new Point(35.015196, -85.540554), new Point(35.032645, -85.581897), new Point(35.049396, -85.630767), new Point(35.040125, -85.690543), new Point(35.072243, -85.734783), 
			new Point(35.100310, -85.737691), new Point(35.146034, -85.775059), new Point(35.173633, -85.796099), new Point(35.221713, -85.811677), new Point(35.231038, -85.826933), 
			new Point(35.240270, -85.847780), new Point(35.239122, -85.876259), new Point(35.258801, -85.874285), new Point(35.300153, -85.903087), new Point(35.417554, -86.016533), 
			new Point(35.457528, -86.051928), new Point(35.476416, -86.081239), new Point(35.487729, -86.092032), new Point(35.535478, -86.163631), new Point(35.610278, -86.224544), 
			new Point(35.627024, -86.239747), new Point(35.727292, -86.314495), new Point(35.779873, -86.349363), new Point(35.811706, -86.369652), new Point(35.836663, -86.385101), 
			new Point(35.837272, -86.385391), new Point(35.842273, -86.392010), new Point(35.849796, -86.399263), new Point(35.855579, -86.403351), new Point(35.876550, -86.424562), 
			new Point(35.903504, -86.449463), new Point(35.937884, -86.479906), new Point(35.958208, -86.497250), new Point(35.996784, -86.531367), new Point(36.010658, -86.544119), 
			new Point(36.024651, -86.586363), new Point(36.042853, -86.606544), new Point(36.072039, -86.636177), new Point(36.106225, -86.673031), new Point(36.118273, -86.700271), 
			new Point(36.123863, -86.707470), new Point(36.136943, -86.726949), new Point(36.140314, -86.727206), new Point(36.147141, -86.743653), new Point(36.152586, -86.754447), 
			new Point(36.154617, -86.760337), new Point(36.166580, -86.764886), new Point(36.171720, -86.768298), new Point(36.176748, -86.772546), new Point(36.191967, -86.775813), 
			new Point(36.206468, -86.776371), new Point(36.207048, -86.791606), new Point(36.233906, -86.815242), new Point(36.252502, -86.829876), new Point(36.265686, -86.830873), 
			new Point(36.313197, -86.865152), new Point(36.324223, -86.868435), new Point(36.327720, -86.891845), new Point(36.348341, -86.931295), new Point(36.402175, -87.025141), 
			new Point(36.450203, -87.099502), new Point(36.525415, -87.221317), new Point(36.556309, -87.248976), new Point(36.599909, -87.282552), new Point(36.630114, -87.316922), 
			new Point(36.641460, -87.339592), new Point(36.653457, -87.364145), new Point(36.681877, -87.404480), new Point(36.702782, -87.455029), new Point(36.713190, -87.479619), 
			new Point(36.732827, -87.492810), new Point(36.743901, -87.510974), new Point(36.815894, -87.633959), new Point(36.827840, -87.658614), new Point(36.881942, -87.735127), 
			new Point(36.959297, -87.855418), new Point(36.966768, -87.874682), new Point(36.996143, -87.951822), new Point(37.056821, -88.033726), new Point(37.064347, -88.045775), 
			new Point(37.071384, -88.084973), new Point(37.073977, -88.121735), new Point(37.070857, -88.126643), new Point(37.067745, -88.131568), new Point(37.049228, -88.165517), 
			new Point(37.049068, -88.166903), new Point(37.029683, -88.231630), new Point(37.027653, -88.248421), new Point(37.023585, -88.255330), new Point(37.001795, -88.287504), 
			new Point(37.003024, -88.324708), new Point(37.003842, -88.344876), new Point(37.003898, -88.346214), new Point(37.004148, -88.353990), new Point(37.004470, -88.368759), 
			new Point(37.004950, -88.389801), new Point(37.005220, -88.401385), new Point(37.006435, -88.429665), new Point(37.007001, -88.468476), new Point(37.007893, -88.492739), 
			new Point(37.008889, -88.504663), new Point(37.009171, -88.506766), new Point(37.009390, -88.508365), new Point(37.011844, -88.519131), new Point(37.017387, -88.526217), 
			new Point(37.023452, -88.530423), new Point(37.028021, -88.532392), new Point(37.035884, -88.535600), new Point(37.049235, -88.565302), new Point(37.053918, -88.564938), 
			new Point(37.071153, -88.586701), new Point(37.080838, -88.592779), new Point(37.085982, -88.596781), new Point(37.086979, -88.597414), new Point(37.091643, -88.601410), 
			new Point(37.090283, -88.606539), new Point(37.098274, -88.630383), new Point(37.100060, -88.633420), new Point(37.119640, -88.627439), new Point(37.130761, -88.628769), 
			new Point(37.159940, -88.637009), new Point(37.161343, -88.676791), new Point(37.161343, -88.682542), new Point(37.184835, -88.682756), new Point(37.252843, -88.721981), 
			new Point(37.271287, -88.742065), new Point(37.300821, -88.753138), new Point(37.320039, -88.760605), new Point(37.339285, -88.785067), new Point(37.353136, -88.788071), 
			new Point(37.415027, -88.870897), new Point(37.446482, -88.886905), new Point(37.498457, -88.897333), new Point(37.533416, -88.923898), new Point(37.582745, -88.967113), 
			new Point(37.603114, -88.997068), new Point(37.621574, -88.987498), new Point(37.678148, -88.966255), new Point(37.729860, -88.958745), new Point(37.740483, -88.958316), 
			new Point(37.775091, -88.945827), new Point(37.816633, -88.945913), new Point(37.897750, -88.946300), new Point(37.996839, -88.934498), new Point(38.080359, -88.915830), 
			new Point(38.154672, -88.913813), new Point(38.245966, -88.906260), new Point(38.270802, -88.931966), new Point(38.299864, -88.946385), new Point(38.312770, -88.951879), 
			new Point(38.336472, -88.956385), new Point(38.361681, -89.016809), new Point(38.361984, -89.034662), new Point(38.362287, -89.077320), new Point(38.386376, -89.130878), 
			new Point(38.389471, -89.170446), new Point(38.391623, -89.370947), new Point(38.424109, -89.457464), new Point(38.439842, -89.530377), new Point(38.459134, -89.565954), 
			new Point(38.500322, -89.611874), new Point(38.516240, -89.667234), new Point(38.517448, -89.699678), new Point(38.535410, -89.728689), new Point(38.544440, -89.742851), 
			new Point(38.551757, -89.807010), new Point(38.565684, -89.847952), new Point(38.572931, -89.876060), new Point(38.575984, -89.929061), new Point(38.592388, -89.944253), 
			new Point(38.599063, -89.984894), new Point(38.601075, -90.013990), new Point(38.614255, -90.045877), new Point(38.618782, -90.068493), new Point(38.620961, -90.092912), 
			new Point(38.621971, -90.124094), new Point(38.627231, -90.137973), new Point(38.633198, -90.144153), new Point(38.644456, -90.156373), new Point(38.649379, -90.163878), 
			new Point(38.645851, -90.178368), new Point(38.643255, -90.189000), new Point(38.655069, -90.194992), new Point(38.661929, -90.197299), new Point(38.672879, -90.203977), 
			new Point(38.679547, -90.210714), new Point(38.683667, -90.217881), new Point(38.682930, -90.227280), new Point(38.682662, -90.236721), new Point(38.686213, -90.245390), 
			new Point(38.688497, -90.250974), new Point(38.695366, -90.260469), new Point(38.700482, -90.263543), new Point(38.703195, -90.269337), new Point(38.710864, -90.285301), 
			new Point(38.716825, -90.297403), new Point(38.720675, -90.305343), new Point(38.720608, -90.316801), new Point(38.729313, -90.331478), new Point(38.733029, -90.347013), 
			new Point(38.733398, -90.351520), new Point(38.739289, -90.365853), new Point(38.742871, -90.385251), new Point(38.743582, -90.395508), new Point(38.743808, -90.412073), 
			new Point(38.748427, -90.439239), new Point(38.755455, -90.458336), new Point(38.768667, -90.494476), new Point(38.776770, -90.511680), new Point(38.786606, -90.532622), 
			new Point(38.793564, -90.564680), new Point(38.798380, -90.591416), new Point(38.799684, -90.619698), new Point(38.801658, -90.651369), new Point(38.802962, -90.678062), 
			new Point(38.802327, -90.699778), new Point(38.802995, -90.732780), new Point(38.803898, -90.770416), new Point(38.805437, -90.807409), new Point(38.806340, -90.837193), 
			new Point(38.805665, -90.854680), new Point(38.810099, -90.874758), new Point(38.804058, -90.894215), new Point(38.806891, -90.915038), new Point(38.816526, -90.953696), 
			new Point(38.830000, -91.020566), new Point(38.828108, -91.039281), new Point(38.818340, -91.120394), new Point(38.821086, -91.138759), new Point(38.820025, -91.177648), 
			new Point(38.827307, -91.208744), new Point(38.838738, -91.224031), new Point(38.846164, -91.239857), new Point(38.846047, -91.264757), new Point(38.858224, -91.303811), 
			new Point(38.873661, -91.373034), new Point(38.899450, -91.456075), new Point(38.907398, -91.540489), new Point(38.893866, -91.621415), new Point(38.910485, -91.658964), 
			new Point(38.911272, -91.700306), new Point(38.911781, -91.723810), new Point(38.933508, -91.805304), new Point(38.938353, -91.827074), new Point(38.935725, -91.848624), 
			new Point(38.943479, -91.879808), new Point(38.944332, -91.941136), new Point(38.952055, -91.967492), new Point(38.951996, -92.008665), new Point(38.953406, -92.128662), 
			new Point(38.958712, -92.204044), new Point(38.960059, -92.253191), new Point(38.961717, -92.292105), new Point(38.964681, -92.301335), new Point(38.963847, -92.311249), 
			new Point(38.963914, -92.320862), new Point(38.964014, -92.324381), new Point(38.964614, -92.333565), new Point(38.966318, -92.351845), new Point(38.964486, -92.359669), 
			new Point(38.968239, -92.371145), new Point(38.971555, -92.430425), new Point(38.973590, -92.495270), new Point(38.973054, -92.520850), new Point(38.966927, -92.533756), 
			new Point(38.940490, -92.575878), new Point(38.939584, -92.596593), new Point(38.937600, -92.691087), new Point(38.936313, -92.743192), new Point(38.935612, -92.775893), 
			new Point(38.935111, -92.846231), new Point(38.933396, -92.973645), new Point(38.944558, -92.991096), new Point(38.953045, -93.088475), new Point(38.961945, -93.207268), 
			new Point(38.969031, -93.231700), new Point(38.971354, -93.271608), new Point(38.973390, -93.331347), new Point(38.976993, -93.414688), new Point(38.984632, -93.493781), 
			new Point(38.991804, -93.567381), new Point(38.994217, -93.594098), new Point(38.989464, -93.632470), new Point(38.992155, -93.673273), new Point(38.993625, -93.711015), 
			new Point(39.000810, -93.734922), new Point(39.002546, -93.809813), new Point(39.005562, -93.887896), new Point(39.007680, -93.941860), new Point(39.008513, -93.965077), 
			new Point(39.011282, -94.068716), new Point(39.016272, -94.128649), new Point(39.021310, -94.197989), new Point(39.027419, -94.248662), new Point(39.030153, -94.271407), 
			new Point(39.034179, -94.305375), new Point(39.038889, -94.341044), new Point(39.041838, -94.362532), new Point(39.044852, -94.387430), new Point(39.045990, -94.415174), 
			new Point(39.047124, -94.442696), new Point(39.047337, -94.447828), new Point(39.051796, -94.474412), new Point(39.058950, -94.489717), new Point(39.064082, -94.500618), 
			new Point(39.068798, -94.511113), new Point(39.071368, -94.520640), new Point(39.072939, -94.532822), new Point(39.074304, -94.535597), new Point(39.076558, -94.536838), 
			new Point(39.083923, -94.541229), new Point(39.090421, -94.542295), new Point(39.094452, -94.543568), new Point(39.096476, -94.551489), new Point(39.095873, -94.557006), 
			new Point(39.096099, -94.564209), new Point(39.096957, -94.572325), new Point(39.101241, -94.572562), new Point(39.105954, -94.570999), new Point(39.106487, -94.576664), 
			new Point(39.106620, -94.583187), new Point(39.106853, -94.587994), new Point(39.116768, -94.590837), new Point(39.137392, -94.586974), new Point(39.159940, -94.589399), 
			new Point(39.169248, -94.594860), new Point(39.181275, -94.593390), new Point(39.183670, -94.591953), new Point(39.188859, -94.606018), new Point(39.191021, -94.615116), 
			new Point(39.196210, -94.620352), new Point(39.210476, -94.638290), new Point(39.225039, -94.650178), new Point(39.246878, -94.657989), new Point(39.254389, -94.657903), 
			new Point(39.273361, -94.669018), new Point(39.296148, -94.685154), new Point(39.310959, -94.687600), new Point(39.319425, -94.694724), new Point(39.328787, -94.711204), 
			new Point(39.345549, -94.747982), new Point(39.353580, -94.761543), new Point(39.368213, -94.771843), new Point(39.385363, -94.788923), new Point(39.450974, -94.788322), 
			new Point(39.516655, -94.786520), new Point(39.599803, -94.788408), new Point(39.708210, -94.788406), new Point(39.724155, -94.788580), new Point(39.749392, -94.789631), 
			new Point(39.776880, -94.793301), new Point(39.814989, -94.803686), new Point(39.858035, -94.810295), new Point(39.885800, -94.856858), new Point(39.915892, -94.904108), 
			new Point(39.968175, -94.974060), new Point(39.974621, -95.015473), new Point(40.044996, -95.138083), new Point(40.074525, -95.192821), new Point(40.132691, -95.237474), 
			new Point(40.193725, -95.360298), new Point(40.263448, -95.454884), new Point(40.368651, -95.531659), new Point(40.407337, -95.555509), new Point(40.403709, -95.599068), 
			new Point(40.403987, -95.615129), new Point(40.399216, -95.651768), new Point(40.397275, -95.655760), new Point(40.391966, -95.663259), new Point(40.392748, -95.675474), 
			new Point(40.392777, -95.724918), new Point(40.392805, -95.838804), new Point(40.465307, -95.839249), new Point(40.479786, -95.839295), new Point(40.523020, -95.858771), 
			new Point(40.595672, -95.859725), new Point(40.653120, -95.859071), new Point(40.657865, -95.885402), new Point(40.669474, -95.896654), new Point(40.677102, -95.905280), 
			new Point(40.697022, -95.911637), new Point(40.724982, -95.911793), new Point(40.813237, -95.911642), new Point(40.813241, -95.923004), new Point(40.813160, -95.988278), 
			new Point(40.813135, -96.006893), new Point(40.813107, -96.121160), new Point(40.813091, -96.140059), new Point(40.813022, -96.178490), new Point(40.812579, -96.293653), 
			new Point(40.812949, -96.388561), new Point(40.812989, -96.426900), new Point(40.813009, -96.520922), new Point(40.813050, -96.559535), new Point(40.813070, -96.567711), 
			new Point(40.813436, -96.606023), new Point(40.813330, -96.625067), new Point(40.813432, -96.642963), new Point(40.813472, -96.653708), new Point(40.813480, -96.663299), 
			new Point(40.813476, -96.672832), new Point(40.813525, -96.682332), new Point(40.813553, -96.697444), new Point(40.813627, -96.707743), new Point(40.816927, -96.707797), 
			new Point(40.824703, -96.711928), new Point(40.836765, -96.711992), new Point(40.855224, -96.713070), new Point(40.862183, -96.715946), new Point(40.839386, -96.739313), 
			new Point(40.817561, -96.756404), new Point(40.815888, -96.768265), new Point(40.817362, -96.787657), new Point(40.821870, -96.834907), new Point(40.821336, -96.929702), 
			new Point(40.821377, -97.044650), new Point(40.822156, -97.101848), new Point(40.822412, -97.216135), new Point(40.822018, -97.291950), new Point(40.821515, -97.349049), 
			new Point(40.821925, -97.463853), new Point(40.821202, -97.597657), new Point(40.821381, -97.693058), new Point(40.821121, -97.807117), new Point(40.821092, -97.883120), 
			new Point(40.822059, -97.996915), new Point(40.822197, -98.149812), new Point(40.821084, -98.264036), new Point(40.825757, -98.340361), new Point(40.820171, -98.378389), 
			new Point(40.823483, -98.403157), new Point(40.798599, -98.493639), new Point(40.762804, -98.588127), new Point(40.757372, -98.604602), new Point(40.743928, -98.626403), 
			new Point(40.722238, -98.696537), new Point(40.722726, -98.740692), new Point(40.721905, -98.763968), new Point(40.702086, -98.845512), new Point(40.686707, -98.951411), 
			new Point(40.669873, -99.028911), new Point(40.669287, -99.085972), new Point(40.675850, -99.255681), new Point(40.690338, -99.341120), new Point(40.690035, -99.379916), 
			new Point(40.695046, -99.439895), new Point(40.691125, -99.540709), new Point(40.710363, -99.672738), new Point(40.741819, -99.740791), new Point(40.780026, -99.844737), 
			new Point(40.810557, -99.899958), new Point(40.843672, -99.985907), new Point(40.914415, -100.166854), new Point(40.964981, -100.280253), new Point(40.986786, -100.305873), 
			new Point(41.002334, -100.376319), new Point(41.051541, -100.525455), new Point(41.083544, -100.614359), new Point(41.105238, -100.725537), new Point(41.111321, -100.763882), 
			new Point(41.137207, -101.002454), new Point(41.138809, -101.126763), new Point(41.138553, -101.202214), new Point(41.117484, -101.264055), new Point(41.111030, -101.356382), 
			new Point(41.103864, -101.474576), new Point(41.117961, -101.576800), new Point(41.120447, -101.679132), new Point(41.115126, -101.715122), new Point(41.079383, -101.888092), 
			new Point(41.062078, -102.022101), new Point(41.048636, -102.071362), new Point(41.027839, -102.154784), new Point(41.030874, -102.181413), new Point(41.045764, -102.297585), 
			new Point(41.056787, -102.426685), new Point(41.080702, -102.474225), new Point(41.112384, -102.571353), new Point(41.113661, -102.629723), new Point(41.113047, -102.763469), 
			new Point(41.112946, -102.949131), new Point(41.113679, -103.001482), new Point(41.126181, -103.031298), new Point(41.180653, -103.133554), new Point(41.193571, -103.167586), 
			new Point(41.205809, -103.317270), new Point(41.221757, -103.384899), new Point(41.220950, -103.490133), new Point(41.220438, -103.625718), new Point(41.216401, -103.663275), 
			new Point(41.192136, -103.775917), new Point(41.191936, -103.892990), new Point(41.192441, -103.988256), new Point(41.183010, -104.044572), new Point(41.180981, -104.052930), 
			new Point(41.175064, -104.062753), new Point(41.175779, -104.075771), new Point(41.149900, -104.120865), new Point(41.147897, -104.171247), new Point(41.158689, -104.253902), 
			new Point(41.157526, -104.349518), new Point(41.158172, -104.522638), new Point(41.157203, -104.655762), new Point(41.135098, -104.704685), new Point(41.124739, -104.737109), 
			new Point(41.124043, -104.766998), new Point(41.118935, -104.804935), new Point(41.113849, -104.850869), new Point(41.114667, -104.860468), new Point(41.116696, -104.886463), 
			new Point(41.100520, -105.061709), new Point(41.098305, -105.115900), new Point(41.097270, -105.171862), new Point(41.095589, -105.202074), new Point(41.104708, -105.230484), 
			new Point(41.124948, -105.305157), new Point(41.127276, -105.333996), new Point(41.157849, -105.403433), new Point(41.210624, -105.441284), new Point(41.239980, -105.437979), 
			new Point(41.256259, -105.449781), new Point(41.276516, -105.511751), new Point(41.291028, -105.528316), new Point(41.291415, -105.568914), new Point(41.297413, -105.594320), 
			new Point(41.302204, -105.611132), new Point(41.308987, -105.614233), new Point(41.327939, -105.615864), new Point(41.339700, -105.618353), new Point(41.350784, -105.643330), 
			new Point(41.349302, -105.704269), new Point(41.363346, -105.776196), new Point(41.382090, -105.812845), new Point(41.395025, -105.824228), new Point(41.410291, -105.838165), 
			new Point(41.433589, -105.896015), new Point(41.440539, -105.943823), new Point(41.451412, -105.972147), new Point(41.490578, -106.036263), new Point(41.542891, -106.083212), 
			new Point(41.563059, -106.122694), new Point(41.597922, -106.209898), new Point(41.624682, -106.255560), new Point(41.634177, -106.284142), new Point(41.669450, -106.370230), 
			new Point(41.693104, -106.384048), new Point(41.724052, -106.459751), new Point(41.754154, -106.515112), new Point(41.741795, -106.565237), new Point(41.730586, -106.701622), 
			new Point(41.743076, -106.773806), new Point(41.741362, -106.830411), new Point(41.744997, -106.923494), new Point(41.754154, -106.961603), new Point(41.772336, -107.082281), 
			new Point(41.773996, -107.115906), new Point(41.779057, -107.124853), new Point(41.789137, -107.200470), new Point(41.778321, -107.224524), new Point(41.777785, -107.229593), 
			new Point(41.778425, -107.247838), new Point(41.782177, -107.261329), new Point(41.781817, -107.267268), new Point(41.776689, -107.311277), new Point(41.786865, -107.371960), 
			new Point(41.785329, -107.410583), new Point(41.772976, -107.466373), new Point(41.752425, -107.559242), new Point(41.741282, -107.671337), new Point(41.728408, -107.728672), 
			new Point(41.715916, -107.782917), new Point(41.704319, -107.836132), new Point(41.704575, -107.895355), new Point(41.674611, -107.980628), new Point(41.665474, -108.031611), 
			new Point(41.661082, -108.064656), new Point(41.655824, -108.103065), new Point(41.651655, -108.128343), new Point(41.633760, -108.261852), new Point(41.630392, -108.310003), 
			new Point(41.631675, -108.347940), new Point(41.633407, -108.378754), new Point(41.635460, -108.410511), new Point(41.640078, -108.486471), new Point(41.645338, -108.576593), 
			new Point(41.648801, -108.627405), new Point(41.647326, -108.676586), new Point(41.650918, -108.702078), new Point(41.673296, -108.739843), new Point(41.678457, -108.782512), 
			new Point(41.693681, -108.865156), new Point(41.690989, -108.903694), new Point(41.670940, -108.930194), new Point(41.668616, -108.958797), new Point(41.613004, -109.127369), 
			new Point(41.596767, -109.160070), new Point(41.594367, -109.197316), new Point(41.593691, -109.198511), new Point(41.597408, -109.203528), new Point(41.598086, -109.205392), 
			new Point(41.595457, -109.213907), new Point(41.591398, -109.218736), new Point(41.591278, -109.223671), new Point(41.584992, -109.225656), new Point(41.577126, -109.235521), 
			new Point(41.574304, -109.243471), new Point(41.576142, -109.249990), new Point(41.579369, -109.256115), new Point(41.557408, -109.310532), new Point(41.532162, -109.377222), 
			new Point(41.523784, -109.445667), new Point(41.541044, -109.478379), new Point(41.548761, -109.486752), new Point(41.553361, -109.496784), new Point(41.562096, -109.541674), 
			new Point(41.563726, -109.557005), new Point(41.555481, -109.590769), new Point(41.546039, -109.611111), new Point(41.542442, -109.677328), new Point(41.542891, -109.793329), 
			new Point(41.542329, -109.857381), new Point(41.544240, -109.911175), new Point(41.551065, -109.918921), new Point(41.568548, -109.938717), new Point(41.601131, -109.954369), 
			new Point(41.612391, -109.979735), new Point(41.636663, -110.026343), new Point(41.678734, -110.062606), new Point(41.719204, -110.079644), new Point(41.771648, -110.147233), 
			new Point(41.792233, -110.222340), new Point(41.794909, -110.256441), new Point(41.791031, -110.266286), new Point(41.771694, -110.304836), new Point(41.771440, -110.329481), 
			new Point(41.770879, -110.406645), new Point(41.755808, -110.477370), new Point(41.741511, -110.508161), new Point(41.746227, -110.524704), new Point(41.754148, -110.536372), 
			new Point(41.767529, -110.538092), new Point(41.771261, -110.553156), new Point(41.779898, -110.570377), new Point(41.792486, -110.577001), new Point(41.815338, -110.631123), 
			new Point(41.809605, -110.687626), new Point(41.815932, -110.719598), new Point(41.824597, -110.825568), new Point(41.825566, -110.919352), new Point(41.812075, -110.969381), 
			new Point(41.815586, -110.982696), new Point(42.020031, -110.933847), new Point(42.058087, -110.932131), new Point(42.085468, -110.948471), new Point(42.116744, -110.959350), 
			new Point(42.172564, -110.994186), new Point(42.213143, -111.042638), new Point(42.213477, -111.047090), new Point(42.211705, -111.109715), new Point(42.231028, -111.137191), 
			new Point(42.199408, -111.160974), new Point(42.195055, -111.200716), new Point(42.226424, -111.222308), new Point(42.270176, -111.275268), new Point(42.317654, -111.297920), 
			new Point(42.322112, -111.297898), new Point(42.346492, -111.298065), new Point(42.386317, -111.320504), new Point(42.437327, -111.343936), new Point(42.476513, -111.370790), 
			new Point(42.526043, -111.403301), new Point(42.574811, -111.477555), new Point(42.587066, -111.481659), new Point(42.631772, -111.525296), new Point(42.654691, -111.594926), 
			new Point(42.661535, -111.639690), new Point(42.649025, -111.730356), new Point(42.649459, -111.887459), new Point(42.643313, -111.904593), new Point(42.622884, -111.924352), 
			new Point(42.619963, -111.946478), new Point(42.619763, -112.004521), new Point(42.622663, -112.014617), new Point(42.616049, -112.059482), new Point(42.630604, -112.136628), 
			new Point(42.626949, -112.163501), new Point(42.660788, -112.198732), new Point(42.660947, -112.211523), new Point(42.782705, -112.230824), new Point(42.792976, -112.242937), 
			new Point(42.798833, -112.261734), new Point(42.798990, -112.363057), new Point(42.840007, -112.410865), new Point(42.848565, -112.423525), new Point(42.863289, -112.440648), 
			new Point(42.866686, -112.444468), new Point(42.871528, -112.450106), new Point(42.876813, -112.450862), new Point(42.875830, -112.457943), new Point(42.890689, -112.476118), 
			new Point(42.912435, -112.528625), new Point(42.901810, -112.576261), new Point(42.888668, -112.633338), new Point(42.872851, -112.695093), new Point(42.846054, -112.764187), 
			new Point(42.834657, -112.769895), new Point(42.792740, -112.828603), new Point(42.749281, -112.879457), new Point(42.711264, -112.925291), new Point(42.669058, -112.992754), 
			new Point(42.645695, -113.027808), new Point(42.620981, -113.121028), new Point(42.604388, -113.175667), new Point(42.598808, -113.228874), new Point(42.570465, -113.517308), 
			new Point(42.568759, -113.623309), new Point(42.568790, -113.736606), new Point(42.568285, -113.787761), new Point(42.576470, -113.931570), new Point(42.575996, -114.069071), 
			new Point(42.575996, -114.170823), new Point(42.577165, -114.295964), new Point(42.612022, -114.362097), new Point(42.642167, -114.445267), new Point(42.651951, -114.472711), 
			new Point(42.688902, -114.518137), new Point(42.724128, -114.545817), new Point(42.766107, -114.703445), new Point(42.775369, -114.725289), new Point(42.858004, -114.844680), 
			new Point(42.923498, -114.933815), new Point(42.923678, -114.939823), new Point(42.924126, -114.949179), new Point(42.926581, -114.971428), new Point(42.938014, -114.992781), 
			new Point(42.938556, -115.070535), new Point(42.953753, -115.148306), new Point(42.957155, -115.178714), new Point(42.971628, -115.197535), new Point(42.973915, -115.213709), 
			new Point(42.959932, -115.290329), new Point(42.961330, -115.308444), new Point(42.964604, -115.330001), new Point(42.948961, -115.388128), new Point(42.947070, -115.427502), 
			new Point(42.958715, -115.463637), new Point(42.959220, -115.483826), new Point(43.008750, -115.548280), new Point(43.087633, -115.613894), new Point(43.139340, -115.663826), 
			new Point(43.160232, -115.697459), new Point(43.173667, -115.746739), new Point(43.189119, -115.785929), new Point(43.344281, -115.952539), new Point(43.384872, -115.996313), 
			new Point(43.463074, -116.091628), new Point(43.507414, -116.142526), new Point(43.541954, -116.160636), new Point(43.566275, -116.196342), new Point(43.571250, -116.215138), 
			new Point(43.575323, -116.243291), new Point(43.590090, -116.273804), new Point(43.596586, -116.289876), new Point(43.596368, -116.354570), new Point(43.593415, -116.393194), 
			new Point(43.593446, -116.434017), new Point(43.599297, -116.513910), new Point(43.598917, -116.552882), new Point(43.600595, -116.572881), new Point(43.609952, -116.596664), 
			new Point(43.641972, -116.645097), new Point(43.662594, -116.659656), new Point(43.670759, -116.677723), new Point(43.680086, -116.689106), new Point(43.690156, -116.692700), 
			new Point(43.705763, -116.698966), new Point(43.777413, -116.725048), new Point(43.806040, -116.751280), new Point(43.866558, -116.778402), new Point(43.906952, -116.815181), 
			new Point(43.971260, -116.913671), new Point(44.006863, -116.941270), new Point(44.024713, -116.948862), new Point(44.031920, -116.952381), new Point(44.048743, -116.976472), 
			new Point(44.082591, -117.015724), new Point(44.134605, -117.094259), new Point(44.182450, -117.135887), new Point(44.221491, -117.154491), new Point(44.246306, -117.176743), 
			new Point(44.259132, -117.179908), new Point(44.264072, -117.191162), new Point(44.290373, -117.223520), new Point(44.332014, -117.282401), new Point(44.374944, -117.298601), 
			new Point(44.382628, -117.308064), new Point(44.401625, -117.309008), new Point(44.407021, -117.312913), new Point(44.411896, -117.308235), new Point(44.444902, -117.325959), 
			new Point(44.473420, -117.340539), new Point(44.483555, -117.346859), new Point(44.484719, -117.358918), new Point(44.496590, -117.368467), new Point(44.505657, -117.369390), 
			new Point(44.511503, -117.376428), new Point(44.517929, -117.375612), new Point(44.525212, -117.398186), new Point(44.540186, -117.416124), new Point(44.550096, -117.422175), 
			new Point(44.565951, -117.432690), new Point(44.586693, -117.461743), new Point(44.607579, -117.479210), new Point(44.626824, -117.508736), new Point(44.628642, -117.517255), 
			new Point(44.647528, -117.555084), new Point(44.660931, -117.608707), new Point(44.668149, -117.620187), new Point(44.693339, -117.688851), new Point(44.724296, -117.777386), 
			new Point(44.749080, -117.806267), new Point(44.782795, -117.810710), new Point(44.803276, -117.813177), new Point(44.861649, -117.816524), new Point(44.914690, -117.820193), 
			new Point(44.951801, -117.853432), new Point(44.979402, -117.887764), new Point(45.025601, -117.926130), new Point(45.053393, -117.939949), new Point(45.123959, -117.962694), 
			new Point(45.151719, -117.972136), new Point(45.164701, -117.964497), new Point(45.182808, -117.986963), new Point(45.198067, -118.013248), new Point(45.214909, -118.024750), 
			new Point(45.227967, -118.012004), new Point(45.248244, -118.019127), new Point(45.294573, -118.037624), new Point(45.312141, -118.065691), new Point(45.318629, -118.076313), 
			new Point(45.318750, -118.078566), new Point(45.324593, -118.088012), new Point(45.329771, -118.096440), new Point(45.329796, -118.099337), new Point(45.329808, -118.104787), 
			new Point(45.335616, -118.108091), new Point(45.342795, -118.119121), new Point(45.346174, -118.125558), new Point(45.343926, -118.137875), new Point(45.347380, -118.145449), 
			new Point(45.346068, -118.156135), new Point(45.345148, -118.165426), new Point(45.349703, -118.169675), new Point(45.354799, -118.188300), new Point(45.347561, -118.202977), 
			new Point(45.349612, -118.210273), new Point(45.342283, -118.231301), new Point(45.344590, -118.241687), new Point(45.372047, -118.298357), new Point(45.399475, -118.334513), 
			new Point(45.431046, -118.360391), new Point(45.493021, -118.413648), new Point(45.537617, -118.456328), new Point(45.552014, -118.464460), new Point(45.580857, -118.455362), 
			new Point(45.591339, -118.470898), new Point(45.599762, -118.506925), new Point(45.581217, -118.589967), new Point(45.583680, -118.601618), new Point(45.581758, -118.627625), 
			new Point(45.599356, -118.633118), new Point(45.603275, -118.647580), new Point(45.615568, -118.649426), new Point(45.638385, -118.684123), new Point(45.655418, -118.711095), 
			new Point(45.659617, -118.734205), new Point(45.664851, -118.764890), new Point(45.663367, -118.777947), new Point(45.660937, -118.797741), new Point(45.663022, -118.806388), 
			new Point(45.675639, -118.845184), new Point(45.679125, -118.857651), new Point(45.675797, -118.878035), new Point(45.692345, -118.933396), new Point(45.712710, -118.995881), 
			new Point(45.718680, -119.014292), new Point(45.742940, -119.089222), new Point(45.746651, -119.109629), new Point(45.762970, -119.204064), new Point(45.783507, -119.323905), 
			new Point(45.791916, -119.373450), new Point(45.795297, -119.392999), new Point(45.800204, -119.421987), new Point(45.823177, -119.560089), new Point(45.833935, -119.623808), 
			new Point(45.840595, -119.668321), new Point(45.837949, -119.701323), new Point(45.829368, -119.801187), new Point(45.822071, -119.884186), new Point(45.809299, -119.954417), 
			new Point(45.798798, -120.010700), new Point(45.786979, -120.039132), new Point(45.775456, -120.063915), new Point(45.765697, -120.119104), new Point(45.750666, -120.164852), 
			new Point(45.721529, -120.204367), new Point(45.706838, -120.286303), new Point(45.700800, -120.343262), new Point(45.699915, -120.357671), new Point(45.694910, -120.374622), 
			new Point(45.687325, -120.481825), new Point(45.690338, -120.499656), new Point(45.707558, -120.534439), new Point(45.728891, -120.558944), new Point(45.739525, -120.594821), 
			new Point(45.739840, -120.631063), new Point(45.728712, -120.655117), new Point(45.710622, -120.687614), new Point(45.698267, -120.730605), new Point(45.693366, -120.745067), 
			new Point(45.692781, -120.750561), new Point(45.678901, -120.801544), new Point(45.675932, -120.821114), new Point(45.671749, -120.834611), new Point(45.661807, -120.859137), 
			new Point(45.639218, -120.894670), new Point(45.635542, -120.918317), new Point(45.652598, -120.941062), new Point(45.653933, -120.948529), new Point(45.650363, -120.957777), 
			new Point(45.643463, -120.981231), new Point(45.647078, -121.067190), new Point(45.635407, -121.096029), new Point(45.620551, -121.112015), new Point(45.606923, -121.127272), 
			new Point(45.602824, -121.138473), new Point(45.599491, -121.162419), new Point(45.599642, -121.169114), new Point(45.601233, -121.176538), new Point(45.605947, -121.187439), 
			new Point(45.606967, -121.194756), new Point(45.607628, -121.199541), new Point(45.609632, -121.203060), new Point(45.613347, -121.207867), new Point(45.626779, -121.214218), 
			new Point(45.630590, -121.213338), new Point(45.640238, -121.208811), new Point(45.656003, -121.212673), new Point(45.664776, -121.221857), new Point(45.668420, -121.239753), 
			new Point(45.668960, -121.248679), new Point(45.676367, -121.279278), new Point(45.677416, -121.287174), new Point(45.682768, -121.291637), new Point(45.687026, -121.298418), 
			new Point(45.693306, -121.305456), new Point(45.695194, -121.313524), new Point(45.693126, -121.327171), new Point(45.695299, -121.342621), new Point(45.696843, -121.353951), 
			new Point(45.694355, -121.365066), new Point(45.695644, -121.378884), new Point(45.685811, -121.395493), new Point(45.686111, -121.401243), new Point(45.687745, -121.411543), 
			new Point(45.686246, -121.420813), new Point(45.689064, -121.436691), new Point(45.695667, -121.447732), new Point(45.696259, -121.458600), new Point(45.703534, -121.477697), 
			new Point(45.709970, -121.494155), new Point(45.710540, -121.501794), new Point(45.710951, -121.512276), new Point(45.712877, -121.519947), new Point(45.712517, -121.542950), 
			new Point(45.711281, -121.550235), new Point(45.707093, -121.578312), new Point(45.704991, -121.592174), new Point(45.704374, -121.617923), new Point(45.702763, -121.622537), 
			new Point(45.699391, -121.632514), new Point(45.699840, -121.639938), new Point(45.695876, -121.670515), new Point(45.693606, -121.683412), new Point(45.688697, -121.691459), 
			new Point(45.687370, -121.712980), new Point(45.690248, -121.719418), new Point(45.692377, -121.739459), new Point(45.690458, -121.755466), new Point(45.691178, -121.769135), 
			new Point(45.690653, -121.780357), new Point(45.696663, -121.801686), new Point(45.696079, -121.830826), new Point(45.689679, -121.838626), new Point(45.685422, -121.841769), 
			new Point(45.676966, -121.857648), new Point(45.672529, -121.876295), new Point(45.667880, -121.887689), new Point(45.657427, -121.899705), new Point(45.654113, -121.903052), 
			new Point(45.641805, -121.929080), new Point(45.632233, -121.953993), new Point(45.631131, -121.960301), new Point(45.613024, -122.001050), new Point(45.612762, -122.012744), 
			new Point(45.610915, -122.024481), new Point(45.608439, -122.031884), new Point(45.600768, -122.046991), new Point(45.597269, -122.060552), new Point(45.581465, -122.102931), 
			new Point(45.578829, -122.117608), new Point(45.579032, -122.123809), new Point(45.577786, -122.132478), new Point(45.574586, -122.148979), new Point(45.571537, -122.157841), 
			new Point(45.559579, -122.177217), new Point(45.549475, -122.198224), new Point(45.544493, -122.236054), new Point(45.541540, -122.294698), new Point(45.538309, -122.322807), 
			new Point(45.544546, -122.381537), new Point(45.545042, -122.390871), new Point(45.544681, -122.400570), new Point(45.539887, -122.418380), new Point(45.541946, -122.433143), 
			new Point(45.539271, -122.449096), new Point(45.541450, -122.477345), new Point(45.542397, -122.484190), new Point(45.542247, -122.505638), new Point(45.544628, -122.527288), 
			new Point(45.546507, -122.537363), new Point(45.547506, -122.549283), new Point(45.545710, -122.557801), new Point(45.539571, -122.559904), new Point(45.536535, -122.560537), 
			new Point(45.533905, -122.563047), new Point(45.532868, -122.564721), new Point(45.531560, -122.568626), new Point(45.533432, -122.579033), new Point(45.535648, -122.586973), 
			new Point(45.533642, -122.592971), new Point(45.530365, -122.599332), new Point(45.527591, -122.605244), new Point(45.527712, -122.610211), new Point(45.531681, -122.617335), 
			new Point(45.533101, -122.622957), new Point(45.534296, -122.628965), new Point(45.534018, -122.630660), new Point(45.530771, -122.634673), new Point(45.529817, -122.647848)
		);
		double trueRouteDist =  3231.91;

		return checkPaths(resRoute, trueRoute, resRouteDist, trueRouteDist);
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