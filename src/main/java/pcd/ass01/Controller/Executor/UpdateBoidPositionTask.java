/**
 * Authors:
 *  - Babini Stefano (0001125127) stefano.babini5@studio.unibo.it
 *  - Gabos Norbert (0001191794) tiberiunorbert.gabos@studio.unibo.it
 */
package pcd.ass01.Controller.Executor;


import pcd.ass01.Model.Boid;
import pcd.ass01.Model.BoidsModel;

public class UpdateBoidPositionTask extends AbstractBoidTask {

    public UpdateBoidPositionTask(Boid boid, BoidsModel model) {
        super(boid, model);
    }

    @Override
    public Void call() {
        boid.updatePosition(model);
        return null;
    }
}
