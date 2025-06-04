/**
 * Authors:
 *  - Babini Stefano (0001125127) stefano.babini5@studio.unibo.it
 *  - Gabos Norbert (0001191794) tiberiunorbert.gabos@studio.unibo.it
 */
package pcd.ass01.Controller.VirtualThreads;

import java.util.ArrayList;
import java.util.List;
import pcd.ass01.BoidsSimulation;
import pcd.ass01.Controller.ParallelController;
import pcd.ass01.Controller.SimulationStateHandler;
import pcd.ass01.Model.BoidsModel;
import pcd.ass01.View.BoidPattern.BoidPatterns;
import java.util.concurrent.CyclicBarrier;

public class VirtualThreadBoids implements ParallelController, SimulationStateHandler {
    private List<BoidRunner> boidRunners;
    private CyclicBarrier barrier;
    private BoidsModel model;
    private BoidPatterns boidPatterns = new BoidPatterns();

    public VirtualThreadBoids(BoidsModel model) {
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

    private void createAndAssignBoidRunners() {
        this.barrier = new CyclicBarrier(model.getBoids().size() + 1);
        // assigning patterns to each BoidRunner
        this.boidPatterns.resetPatterns();
        model.getBoids().forEach((boid) -> {
            BoidPatterns.Pattern assignedPattern = BoidsSimulation.DEFAULT_PATTERN;
            boidRunners.add(new BoidRunner(boid, model, barrier, assignedPattern));
        });
        boidRunners.forEach(boidRunner -> Thread.ofVirtual().start(boidRunner));
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
        try {
            barrier.await();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
