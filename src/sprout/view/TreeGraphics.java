package sprout.view;

import javax.swing.*;
import java.awt.*;

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

        g2.setColor(new Color(200, 230, 255));
        g2.fillRect(0, 0, width, height);

        int treeHeight = (int) (50 + progress * 300);
        int trunkHeight = treeHeight / 4;
        int canopyHeight = treeHeight - trunkHeight;

        g2.setColor(new Color(101, 67, 33));
        int trunkWidth = 20;
        int trunkX = width / 2 - trunkWidth / 2;
        int trunkY = height - trunkHeight;
        g2.fillRect(trunkX, trunkY, trunkWidth, trunkHeight);

        g2.setColor(new Color(34, 139, 34));
        int canopyWidth = (int) (trunkWidth + progress * 100);
        g2.fillOval(width / 2 - canopyWidth / 2, trunkY - canopyHeight + 10, canopyWidth, canopyHeight);
    }
}
