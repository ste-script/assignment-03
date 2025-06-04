package pcd.ass01.View.BoidPattern;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class BoidPatterns {
    private final List<Color> colors = List.of(Color.RED, Color.GREEN, Color.BLUE, Color.MAGENTA, Color.YELLOW);
    private final List<Pattern> patterns;
    private int currentIndex = 0;

    public BoidPatterns() {
        patterns = generatePatterns();
    }

    private List<Pattern> generatePatterns() {
        List<Pattern> patternList = new ArrayList<>();
        for (ShapeType shape : ShapeType.values()) {
            for (Color color : colors) {
                patternList.add(new Pattern(color, shape));
            }
        }
        return patternList;
    }

    public int getPatternsCount() {
        return patterns.size();
    }

    public Pattern getNextPattern() {
        if (currentIndex >= patterns.size()) {
            throw new IllegalStateException("There are no more available patterns!");
        }
        return patterns.get(currentIndex++);
    }

    public void resetPatterns() {
        this.currentIndex = 0;
    }

    public static class Pattern {
        private final Color color;
        private final ShapeType shape;

        public Pattern(Color color, ShapeType shape) {
            this.color = color;
            this.shape = shape;
        }

        public Color getColor() {
            return color;
        }

        public ShapeType getShape() {
            return shape;
        }
    }
}
