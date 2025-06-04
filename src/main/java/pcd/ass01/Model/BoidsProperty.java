package pcd.ass01.Model;

import java.util.List;

public interface BoidsProperty {

    void setSeparationWeight(double value);
    void setCohesionWeight(double value);
    void setAlignmentWeight(double value);
    void setNumberOfBoids(int nboids);

    double getWidth();
    List<Boid> getBoids();

    boolean isRunning();
    boolean isSuspended();

}
