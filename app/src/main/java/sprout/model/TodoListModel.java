package sprout.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javax.swing.table.DefaultTableModel;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class TodoListModel {
    private final DefaultTableModel tableModel;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DATA_FILE = Paths.get(System.getProperty("user.home"), ".sprout_tasks.json");

    public TodoListModel() {
        this.tableModel = new DefaultTableModel(new Object[]{"Done", "Task"}, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Boolean.class : String.class;
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }
        };
    }

    public DefaultTableModel getTableModel() {
        return tableModel;
    }

    public void loadTasks() {
        if (!Files.exists(DATA_FILE)) return;
        try {
            String json = Files.readString(DATA_FILE, StandardCharsets.UTF_8);
            TodoItem[] items = gson.fromJson(json, TodoItem[].class);
            for (TodoItem item : items) {
                tableModel.addRow(new Object[]{item.isDone(), item.getTask()});
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveTasks() {
        List<TodoItem> items = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            boolean done = Boolean.TRUE.equals(tableModel.getValueAt(i, 0));
            String task = tableModel.getValueAt(i, 1).toString();
            items.add(new TodoItem(task, done));
        }
        try {
            Files.writeString(DATA_FILE, gson.toJson(items), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public double getProgress() {
        int total = tableModel.getRowCount();
        int done = 0;
        for (int i = 0; i < total; i++) {
            if (Boolean.TRUE.equals(tableModel.getValueAt(i, 0))) done++;
        }
        return total == 0 ? 0 : (double) done / total;
    }
}
