package sprout;

import sprout.controller.SproutController;
import sprout.model.TodoListModel;
import javax.swing.SwingUtilities;

public class Sprout {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TodoListModel model = new TodoListModel();
            SproutController controller = new SproutController(model);
            controller.start();
        });
    }
}
