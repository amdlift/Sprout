package sprout.view;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;

public class TreeGraphics extends JPanel {
    private double progress = 0.0;

    public TreeGraphics() {
        setPreferredSize(new Dimension(200, 400));
    }

    public void setProgress(double progress) {
        this.progress = Math.max(0, Math.min(1, progress));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Background
        g2.setColor(new Color(200, 230, 255));
        g2.fillRect(0, 0, width, height);

        // Tree dimensions
        int treeHeight = (int) (50 + progress * 300);
        int trunkHeight = treeHeight / 4;
        int canopyHeight = treeHeight - trunkHeight;

        // Draw trunk
        g2.setColor(new Color(101, 67, 33));
        int trunkWidth = 20;
        int trunkX = width / 2 - trunkWidth / 2;
        int trunkY = height - trunkHeight;
        g2.fillRect(trunkX, trunkY, trunkWidth, trunkHeight);

        // CANOPY COLOR
        if (progress == 1.0) {
            g2.setColor(new Color(255, 192, 203));
        } else if (progress >= 0.7) {
            g2.setColor(new Color(255, 152, 150));
        } else if (progress >= 0.5) {
            g2.setColor(new Color(255, 102, 103));
        } else if (progress >= 0.3) {
            g2.setColor(new Color(255, 200, 103));
        } else if (progress >= 0.1) {
            g2.setColor(new Color(209, 139, 34));
        } else {
            g2.setColor(new Color(204, 209, 34));
        }

        // Draw canopy
        int canopyWidth = (int) (trunkWidth + progress * 100);
        int canopyX = width / 2 - canopyWidth / 2;
        int canopyY = trunkY - canopyHeight + 10;
        g2.fillOval(canopyX, canopyY, canopyWidth, canopyHeight);

        // FLOWERS (only when progress is 1.0)
        if (progress == 1.0) {

            // Random number of flowers: 8–10
            int numFlowers = 8 + (int)(Math.random() * 3); // 8, 9, or 10

            // ellipse center
            double centerX = canopyX + canopyWidth / 2.0;
            double centerY = canopyY + canopyHeight / 2.0;

            // shrink the radius slightly to keep flowers fully inside
            double radiusX = canopyWidth / 2.0 - 10;
            double radiusY = canopyHeight / 2.0 - 10;

            for (int f = 0; f < numFlowers; f++) {

                double rx, ry;

                // Random point inside the unit circle
                do {
                    rx = (Math.random() * 2) - 1;  // -1 to 1
                    ry = (Math.random() * 2) - 1;
                } while (rx * rx + ry * ry > 1);

                // Scale to ellipse size
                int flowerX = (int)(centerX + rx * radiusX);
                int flowerY = (int)(centerY + ry * radiusY);

                // Draw flower center
                int centerRadius = 5;
                g2.setColor(Color.YELLOW);
                g2.fillOval(flowerX - centerRadius, flowerY - centerRadius,
                            centerRadius * 2, centerRadius * 2);

                // Draw petals
                int numPetals = 10;
                int petalLength = 15;
                int petalWidth = 5;
                g2.setColor(new Color(255, 130, 130));

                for (int p = 0; p < numPetals; p++) {
                    double angle = Math.toRadians(360.0 / numPetals * p);

                    g2.translate(flowerX, flowerY);
                    g2.rotate(angle);

                    g2.fill(new Ellipse2D.Double(
                        -petalWidth / 2,
                        centerRadius,
                        petalWidth,
                        petalLength));

                    g2.rotate(-angle);
                    g2.translate(-flowerX, -flowerY);
                }
            }
        }
    }
}
