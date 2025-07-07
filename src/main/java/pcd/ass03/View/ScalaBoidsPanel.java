package pcd.ass03.View;

import pcd.ass01.Model.P2d;
import pcd.ass01.View.BoidPattern.ShapeDrawer;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ScalaBoidsPanel extends JPanel {

    private final ScalaBoidsView view;
    private int framerate;
    private List<P2d> boids;

    public ScalaBoidsPanel(ScalaBoidsView view, List<P2d> boids) {
        this.view = view;
        this.boids = boids;
    }

    public void setBoids(List<P2d> boids) {
        this.boids = boids;
    }

    public void setFrameRate(int framerate) {
        this.framerate = framerate;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        setBackground(Color.WHITE);

        var w = view.getWidth();
        var h = view.getHeight();
        var envWidth = 800;
        var xScale = w / envWidth;
        for (P2d boid : boids) {
            var x = boid.x();
            var y = boid.y();
            int px = (int) ((double) w / 2 + x * xScale);
            int py = (int) ((double) h / 2 - y * xScale);

            drawBoid(g, px, py);
        }

        g.setColor(Color.BLACK);
        g.drawString("Num. Boids: " + boids.size(), 10, 25);
        g.drawString("Framerate: " + framerate, 10, 40);
    }

    private void drawBoid(Graphics g, int px, int py) {
        g.setColor(Color.BLUE);
        ShapeDrawer.drawCircle(g, px, py);
    }
}
