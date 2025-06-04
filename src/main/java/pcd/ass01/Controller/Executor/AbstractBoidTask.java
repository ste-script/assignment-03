/**
 * Authors:
 *  - Babini Stefano (0001125127) stefano.babini5@studio.unibo.it
 *  - Gabos Norbert (0001191794) tiberiunorbert.gabos@studio.unibo.it
 */
package pcd.ass01.Controller.Executor;

import pcd.ass01.Model.Boid;
import pcd.ass01.Model.BoidsModel;
import java.util.concurrent.Callable;

public abstract class AbstractBoidTask implements Callable<Void> {

    protected Boid boid;
    protected BoidsModel model;

    public AbstractBoidTask(Boid boid, BoidsModel model) {
        this.boid = boid;
        this.model = model;
    }
}
