/**
 * Authors:
 *  - Babini Stefano (0001125127) stefano.babini5@studio.unibo.it
 *  - Gabos Norbert (0001191794) tiberiunorbert.gabos@studio.unibo.it
 */
package pcd.ass01.Controller;

import pcd.ass01.Controller.DefaultParallelism.PlatformThreadBoids;
import pcd.ass01.Controller.Executor.ExecutorBoids;
import pcd.ass01.Controller.Sequential.SequentialBoids;
import pcd.ass01.Controller.VirtualThreads.VirtualThreadBoids;
import pcd.ass01.Model.BoidsModel;
import pcd.ass01.View.BoidsView;

import java.util.Optional;

public class BoidsSimulator {

    private final BoidsModel model;

    private Optional<BoidsView> view;
    private ParallelController parallelController;

    private static final int FRAMERATE = 25;
    private int framerate;

    public BoidsSimulator(BoidsModel model, Optional<BoidsView> view, BoidsSimulatorType type) {
        this.model = model;
        this.view = view;

        if (type == BoidsSimulatorType.PLATFORM_THREADS) {
            setupBoidsMultithreaded();
        } else if (type == BoidsSimulatorType.EXECUTOR) {
            setupBoidsExecutor();
        } else if (type == BoidsSimulatorType.VIRTUAL_THREADS) {
            setupBoidsVirtualThreads();
        } else if (type == BoidsSimulatorType.SEQUENTIAL) {
            setupBoids(new SequentialBoids(model));
        }
        else {
            throw new IllegalArgumentException("Unknown simulator type: " + type);
        }
    }

    private <T extends SimulationStateHandler & ParallelController> void setupBoids(T executor){
        stopSimulation();
        parallelController = executor;
        view.ifPresent(boidsView -> boidsView.setSimulationStateHandler(executor));
    }

    private void setupBoidsMultithreaded() {
        setupBoids(new PlatformThreadBoids(model));
    }

    private void setupBoidsExecutor() {
        setupBoids(new ExecutorBoids(model));
    }

    private void setupBoidsVirtualThreads() {
        setupBoids(new VirtualThreadBoids(model));
    }

    private void stopSimulation() {
        if (parallelController != null) {
            view.ifPresent(BoidsView::unsetSimulationStateHandler);
            parallelController.stop();
        }
    }

    /**
     * I guess that this is optional cuz in future we might run the sim
     * without the view to measure performances.
     * 
     * @param view
     */
    public void attachView(BoidsView view) {
        this.view = Optional.of(view);
    }

    public void runSimulation() {
        parallelController.start();
        while (true) {
            var t0 = System.currentTimeMillis();
            parallelController.update();

            if (view.isPresent()) {
                view.get().update(framerate);
                var t1 = System.currentTimeMillis();
                var dtElapsed = t1 - t0;
                var frameratePeriod = 1000 / FRAMERATE;

                if (dtElapsed < frameratePeriod) {
                    try {
                        Thread.sleep(frameratePeriod - dtElapsed);
                    } catch (Exception ex) {
                    }
                    framerate = FRAMERATE;
                } else {
                    framerate = (int) (1000 / dtElapsed);
                }
            }
        }
    }
}
