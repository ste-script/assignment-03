package pcd.ass01.View;

import pcd.ass01.Model.BoidsProperty;
import pcd.ass01.View.BoidPattern.ShapeDrawer;
import pcd.ass01.Model.Boid;

import javax.swing.*;
import java.awt.*;

public class BoidsPanel extends JPanel {

	private BoidsView view;
	private BoidsProperty boidsProperty;
    private int framerate;

    public BoidsPanel(BoidsView view, BoidsProperty boidsProperty) {
    	this.boidsProperty = boidsProperty;
    	this.view = view;
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
        var envWidth = boidsProperty.getWidth();
        var xScale = w/envWidth;
        // var envHeight = model.getHeight();
        // var yScale = h/envHeight;

        var boids = boidsProperty.getBoids();

        for (Boid boid : boids) {
            var x = boid.getPos().x();
            var y = boid.getPos().y();
            int px = (int)(w/2 + x*xScale);
            int py = (int)(h/2 - y*xScale);

            drawBoid(boid, g, px, py);
        }
        
        g.setColor(Color.BLACK);
        g.drawString("Num. Boids: " + boids.size(), 10, 25);
        g.drawString("Framerate: " + framerate, 10, 40);
   }

   private void drawBoid(Boid boid, Graphics g, int px, int py) {
       g.setColor(boid.getPattern().getColor());
       switch (boid.getPattern().getShape()) {
           case CIRCLE:
               ShapeDrawer.drawCircle(g, px, py);
               break;
           case SQUARE:
               ShapeDrawer.drawSquare(g, px, py);
               break;
           case TRIANGLE:
               ShapeDrawer.drawTriangle(g, px, py);
               break;
           case STAR:
               ShapeDrawer.drawStar(g, px, py);
               break;
           case DIAMOND:
               ShapeDrawer.drawDiamond(g, px, py);
               break;
           default:
               break;
       }
   }
}
