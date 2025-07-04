/**
 * Authors:
 *  - Babini Stefano (0001125127) stefano.babini5@studio.unibo.it
 *  - Gabos Norbert (0001191794) tiberiunorbert.gabos@studio.unibo.it
 */
package pcd.ass01;

import pcd.ass01.Controller.BoidsSimulator;
import pcd.ass01.Controller.BoidsSimulatorType;
import pcd.ass01.View.BoidPattern.BoidPatterns;
import pcd.ass01.View.BoidPattern.ShapeType;
import pcd.ass01.Model.BoidsModel;
import pcd.ass01.View.BoidsView;

import java.awt.*;
import java.util.Optional;

public class BoidsSimulation {

	final static int N_BOIDS = 2000;

	final static double SEPARATION_WEIGHT = 1.0;
	final static double ALIGNMENT_WEIGHT = 1.0;
	final static double COHESION_WEIGHT = 1.0;

	final static int ENVIRONMENT_WIDTH = 1000;
	final static int ENVIRONMENT_HEIGHT = 1000;
	static final double MAX_SPEED = 4.0;
	static final double PERCEPTION_RADIUS = 50.0;
	static final double AVOID_RADIUS = 20.0;

	final static int SCREEN_WIDTH = 800;
	final static int SCREEN_HEIGHT = 800;

	public final static int SEED = 1234;

	public final static BoidPatterns.Pattern DEFAULT_PATTERN = new BoidPatterns.Pattern(Color.BLUE, ShapeType.CIRCLE);
	/**
	 * These two variables handle the test mode:
	 * - THREAD_COUNT works only if PATTERN_BASED is set to true
	 * - THREAD_COUNT must a number smaller than the total amount of patterns
	 */
	final static int THREAD_COUNT = 10;
	final static BoidsSimulatorType SIMULATOR_TYPE = BoidsSimulatorType.EXECUTOR;

	public static void main(String[] args) {
		var model = new BoidsModel(
				N_BOIDS,
				SEPARATION_WEIGHT, ALIGNMENT_WEIGHT, COHESION_WEIGHT,
				ENVIRONMENT_WIDTH, ENVIRONMENT_HEIGHT,
				MAX_SPEED,
				PERCEPTION_RADIUS,
				AVOID_RADIUS);

		var view = new BoidsView(model, SCREEN_WIDTH, SCREEN_HEIGHT);
		var sim = new BoidsSimulator(model, Optional.of(view), SIMULATOR_TYPE);
		sim.runSimulation();
	}
}
