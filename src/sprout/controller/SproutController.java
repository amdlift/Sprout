package sprout.controller;

import sprout.model.Persistence;
import sprout.view.SproutView;

import javax.swing.*;

/**
 * the controller part of MVC and interacts with the model and view to modify the tasks and tree graphic
 * 
 */

public class SproutController {
    private final Persistence model;
    private final SproutView view;
    
    /**
     * takes in the persistence class object from model so it can call the saving and loading of tasks
     * @param model object of the persistence class used for saving and loading tasks
     */
    public SproutController(Persistence model) {
        this.model = model;
        this.view = new SproutView(this, model);

        model.getTableModel().addTableModelListener(e -> {
            model.saveTasks();
            updateProgress();
        });
    }
    /**
     * loads in the currently saved tasks and tells the view to display them along with the associated tree growth
     */
    public void start() {
        model.loadTasks();
        updateProgress();
        view.show();
    }
    
    /**
     * is associated with the add task button to add new tasks to the list
     */

    public void handleAddTask() {
        String task = JOptionPane.showInputDialog(null, "Enter a new task:");
        if (task != null && !task.trim().isEmpty()) {
            model.getTableModel().addRow(new Object[]{false, task});
        }
    }

    /**
     * is associated with the remove task button to remove tasks from the list
     */
    public void handleRemoveTask() {
        JTable table = view.getTable();
        int[] selected = table.getSelectedRows();
        for (int i = selected.length - 1; i >= 0; i--) {
            model.getTableModel().removeRow(selected[i]);
        }
    }

    /**
     * talks with the view and model to change the current state of the tree graphic based on task progress
     */
    private void updateProgress() {
        double progress = model.getProgress();
        view.getTreePanel().setProgress(progress);
    }
}
