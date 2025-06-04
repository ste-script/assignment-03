/**
 * Authors:
 *  - Babini Stefano (0001125127) stefano.babini5@studio.unibo.it
 *  - Gabos Norbert (0001191794) tiberiunorbert.gabos@studio.unibo.it
 */
package pcd.ass01.Controller.Sequential;

import pcd.ass01.Controller.ParallelController;
import pcd.ass01.Controller.SimulationStateHandler;
import pcd.ass01.Model.BoidsModel;

public class SequentialBoids implements ParallelController, SimulationStateHandler {
    private BoidsModel model;

    public SequentialBoids(BoidsModel model) {
        this.model = model;
    }

    public synchronized void start() {
        model.start();
    }

    public synchronized void update() {
        if (model.isSuspended() || !model.isRunning()) {
            return;
        }
        this.updateVelocity();
        this.updatePosition();
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

    private synchronized void redistributeBoids() {
        model.setBoids(model.getNumberOfBoids());
        start();
    }

    private void checkThreadValidity() {
        if (model.getNumberOfBoids() != model.getBoids().size()) {
            redistributeBoids();
        }
    }

    private void updateVelocity() {
        model.getBoids().forEach(boid -> {
            boid.updateVelocity(model);
        });
    }

    private void updatePosition() {
        model.getBoids().forEach(boid -> {
            boid.updatePosition(model);
        });
    }
}
