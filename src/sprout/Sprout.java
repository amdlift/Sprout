package sprout;

import sprout.controller.SproutController;
import sprout.model.Persistence;
import javax.swing.SwingUtilities;

/**
 * main class of the project that initializes the other classes
 * 
 */

public class Sprout {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Persistence model = new Persistence();
            SproutController controller = new SproutController(model);
            controller.start();
        });
    }
}
