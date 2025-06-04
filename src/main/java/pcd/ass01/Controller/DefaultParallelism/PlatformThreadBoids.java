/**
 * Authors:
 *  - Babini Stefano (0001125127) stefano.babini5@studio.unibo.it
 *  - Gabos Norbert (0001191794) tiberiunorbert.gabos@studio.unibo.it
 */
package pcd.ass01.Controller.DefaultParallelism;

import java.util.ArrayList;
import java.util.List;
import pcd.ass01.BoidsSimulation;
import pcd.ass01.Controller.ParallelController;
import pcd.ass01.Controller.SimulationStateHandler;
import pcd.ass01.Model.Boid;
import pcd.ass01.Model.BoidsModel;
import pcd.ass01.View.BoidPattern.BoidPatterns;

public class PlatformThreadBoids implements ParallelController, SimulationStateHandler {
    private List<BoidRunner> boidRunners;
    private BoidsMonitor barrier;
    private BoidsModel model;
    private int numberOfThreads;
    private BoidPatterns boidPatterns = new BoidPatterns();

    public PlatformThreadBoids(BoidsModel model) {
        this.model = model;
        boidRunners = new ArrayList<>();
        createAndAssignBoidRunners();
    }

    public synchronized void start() {
        model.setBoids(model.getNumberOfBoids());
        model.start();
    }

    public synchronized void update() {
        if (model.isSuspended()) {
            return;
        }
        if (!boidRunners.isEmpty()) {
            this.updateVelocity();
            this.updatePosition();
        }
        this.checkThreadValidity();
    }

    public synchronized void stop() {
        model.setBoids(0);
        model.stop();
    }

    @Override
    public void resume() {
        model.resume();
    }

    @Override
    public void suspend() {
        model.suspend();
    }

    private void calculateNumberOfThreads() {
        var numberOfAvailableProcessors = Runtime.getRuntime().availableProcessors() + 1;
        numberOfThreads = Math.max(1, Math.min(numberOfAvailableProcessors, model.getBoids().size()));
    }

    private void createAndAssignBoidRunners() {
        calculateNumberOfThreads();
        this.barrier = new BoidsMonitor(numberOfThreads + 1);
        final var boids = model.getBoids();
        var chunkSize = Math.max(1, boids.size() / numberOfThreads);
        var boidsGroupedInChunks = getBoidsGroupedInChunks(boids, numberOfThreads, chunkSize);

        // assigning patterns to each BoidRunner
        this.boidPatterns.resetPatterns();
        boidsGroupedInChunks.forEach((boidChunk) -> {
            BoidPatterns.Pattern assignedPattern = BoidsSimulation.DEFAULT_PATTERN;
            boidRunners.add(new BoidRunner(boidChunk, model, barrier, assignedPattern));
        });
        boidRunners.forEach(boidRunner -> Thread.ofPlatform().start(boidRunner));
    }

    private void checkThreadValidity() {
        try {
            if (model.getNumberOfBoids() != model.getBoids().size()) {
                redistributeBoids();
            } else if (model.isRunning()) {
                barrier.await();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteThreads() {
        boidRunners.forEach(BoidRunner::stop);
        barrier.await();
        boidRunners.clear();
    }

    private synchronized void redistributeBoids() {
        if (!boidRunners.isEmpty()) {
            deleteThreads();
        }
        if (!model.isRunning()) {
            return;
        }
        model.setBoids(model.getNumberOfBoids());
        createAndAssignBoidRunners();
        start();
    }

    private ArrayList<List<Boid>> getBoidsGroupedInChunks(final List<Boid> boids, final int numberOfThreads,
            int chunkSize) {
        var boidsGroupedInChunks = new ArrayList<List<Boid>>();
        for (int i = 0; i < numberOfThreads; i++) {
            var start = i * chunkSize;
            var end = Math.min((i + 1) * chunkSize, boids.size());
            if (i == numberOfThreads - 1) {
                end = boids.size();
            }
            boidsGroupedInChunks.add(boids.subList(start, end));
        }
        return boidsGroupedInChunks;
    }

    private void updateVelocity() {
        try {
            barrier.await();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updatePosition() {
        try {
            barrier.await();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
