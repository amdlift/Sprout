package sprout.controller;

import sprout.model.TodoListModel;
import sprout.view.SproutView;

import javax.swing.*;

public class SproutController {
    private final TodoListModel model;
    private final SproutView view;

    public SproutController(TodoListModel model) {
        this.model = model;
        this.view = new SproutView(this, model);

        model.getTableModel().addTableModelListener(e -> {
            model.saveTasks();
            updateProgress();
        });
    }

    public void start() {
        model.loadTasks();
        updateProgress();
        view.show();
    }

    public void handleAddTask() {
        String task = JOptionPane.showInputDialog(null, "Enter a new task:");
        if (task != null && !task.trim().isEmpty()) {
            model.getTableModel().addRow(new Object[]{false, task});
        }
    }

    public void handleRemoveTask() {
        JTable table = view.getTable();
        int[] selected = table.getSelectedRows();
        for (int i = selected.length - 1; i >= 0; i--) {
            model.getTableModel().removeRow(selected[i]);
        }
    }

    private void updateProgress() {
        double progress = model.getProgress();
        view.getTreePanel().setProgress(progress);
    }
}
