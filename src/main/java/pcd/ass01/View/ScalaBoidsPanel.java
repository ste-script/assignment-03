package pcd.ass01.View;

import pcd.ass01.Model.P2d;
import pcd.ass01.View.BoidPattern.ShapeDrawer;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class ScalaBoidsPanel extends JPanel {

    private final ScalaBoidsView view;
    private int framerate;
    private final Map<String, P2d> boidMap;

    public ScalaBoidsPanel(ScalaBoidsView view, Map<String, P2d> boidMap) {
        this.view = view;
        this.boidMap = boidMap;
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
        var copyBoidMap = new HashMap<>(boidMap);
        var boids = copyBoidMap.values();

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
