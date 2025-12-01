package sprout.view;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
/**
 * the class for creating the tree graphics used in tree panel
 * 
 */
public class TreeGraphics extends JPanel {
    private double progress = 0.0;
    /**
     * constructor that creates the dimensions of the tree panel
     */
    public TreeGraphics() {
        setPreferredSize(new Dimension(200, 400));
    }
    /**
     * a setter function for the tree progress variable
     * @param progress the current progress value
     */
    public void setProgress(double progress) {
        this.progress = Math.max(0, Math.min(1, progress));
        repaint();
    }
    /**
     * the function for creating the tree graphic on screen
     * @param g the graphics object
     */
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

        //visual stages to the tree growing
        if(progress == 1.0){
            //flower stage
            g2.setColor(new Color(255, 192, 203)); //pink
            
        }else if(progress >= 0.7){
            g2.setColor(new Color(255, 152, 150)); //red peach color
        }else if(progress >= 0.5){
            g2.setColor(new Color(255, 102, 103)); //redish pink
        }else if(progress >= 0.3){
            g2.setColor(new Color(255, 200, 103)); // peach color
        }else if(progress >= 0.1){
            g2.setColor(new Color(209, 139, 34)); // sickly green
        }else{
            g2.setColor(new Color(204, 209, 34)); // brownish color
        }
        
        int canopyWidth = (int) (trunkWidth + progress * 100);
        g2.fillOval(width / 2 - canopyWidth / 2, trunkY - canopyHeight + 10, canopyWidth, canopyHeight);
        
        //creates the flowers
        if(progress == 1.0){
            for(int flowerCount = 1; flowerCount < 8; flowerCount++){
                // Calculate center of the flower
                int centerX = 0;
                int centerY = 0;
                //centerX = getWidth() / (2+flowerCount)
                //centerY = getHeight() / (2+flowerCount)
                if(flowerCount%2 == 0){
                    centerX = (width / 2 - canopyWidth / 2) +50 + (flowerCount*7);
                    centerY = (trunkY - canopyHeight + 10)+30 + (flowerCount*20);
                }else{
                    centerX = (width / 2 - canopyWidth / 2)+18 + (flowerCount*7);
                    centerY = (trunkY - canopyHeight + 10)+100 + (flowerCount*20);
                }
                

                // Draw the flower center
                int centerRadius = 5;
                g2.setColor(Color.YELLOW);
                g2.fillOval(centerX - centerRadius, centerY - centerRadius, centerRadius * 2, centerRadius * 2);

                // Draw the petals
                int numPetals = 10;
                int petalLength = 15;
                int petalWidth = 5;
                g2.setColor(new Color(255, 130, 130));

                for (int i = 0; i < numPetals; i++) {
                    double angle = Math.toRadians(360.0 / numPetals * i);

                    // Translate and rotate for each petal
                    g2.translate(centerX, centerY);
                    g2.rotate(angle);

                    // Draw a petal as an ellipse
                    g2.fill(new Ellipse2D.Double(-petalWidth / 2, centerRadius, petalWidth, petalLength));

                    // Reset transformations
                    g2.rotate(-angle);
                    g2.translate(-centerX, -centerY);
                }
            }
        }
    }
}
