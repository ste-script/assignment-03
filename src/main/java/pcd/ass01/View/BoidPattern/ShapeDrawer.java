package pcd.ass01.View.BoidPattern;

import java.awt.*;

public class ShapeDrawer {
    private final static double SCALE = 0.6;
    private final static int BORDER_SIZE = 2;   // best values = 1 or 2

    private final static Color BORDER_COLOR = Color.BLACK;
    private final static double SCALE_FACTOR = 10;

    @FunctionalInterface
    private interface ShapeDrawerFunction {
        void draw(Graphics g, int px, int py, int size);
    }

    private static void drawWithBorder(Graphics g, int px, int py, ShapeDrawerFunction shapeFunction) {
        int size = (int) (SCALE_FACTOR * SCALE);
        int totalBorder = BORDER_SIZE * 2;

        Color oldColor = g.getColor();

        g.setColor(BORDER_COLOR);
        shapeFunction.draw(g, px - BORDER_SIZE, py - BORDER_SIZE, size + totalBorder);
        g.setColor(oldColor);
        shapeFunction.draw(g, px, py, size);
    }

    public static void drawCircle(Graphics g, int px, int py) {
        drawWithBorder(g, px, py, (gr, x, y, s) -> gr.fillOval(x, y, s, s));
    }

    public static void drawSquare(Graphics g, int px, int py) {
        drawWithBorder(g, px, py, (gr, x, y, s) -> gr.fillRect(x, y, s, s));
    }

    public static void drawTriangle(Graphics g, int px, int py) {
        drawWithBorder(g, px, py, (gr, x, y, s) -> {
            int[] xPoints = {x, x + s, x - s};
            int[] yPoints = {y - s, y + s, y + s};
            gr.fillPolygon(xPoints, yPoints, 3);
        });
    }

    public static void drawStar(Graphics g, int px, int py) {
        drawWithBorder(g, px, py, (gr, x, y, s) -> {
            int[] starX = {
                    x, x + s / 2, x + s, x + (s * 7 / 10), x + (s * 3 / 10),
                    x, x - (s * 3 / 10), x - (s * 7 / 10), x - s, x - s / 2
            };
            int[] starY = {
                    y - s, y - (s * 3 / 10), y - (s * 3 / 10), y + (s * 2 / 10), y + (s * 7 / 10),
                    y + (s * 3 / 10), y + (s * 7 / 10), y + (s * 2 / 10), y - (s * 3 / 10), y - (s * 3 / 10)
            };
            gr.fillPolygon(starX, starY, 10);
        });
    }

    public static void drawDiamond(Graphics g, int px, int py) {
        drawWithBorder(g, px, py, (gr, x, y, s) -> {
            int[] diamondX = {x, x + s, x, x - s};
            int[] diamondY = {y - s, y, y + s, y};
            gr.fillPolygon(diamondX, diamondY, 4);
        });
    }
}
