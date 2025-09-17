package sprout;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import javax.swing.Timer;
import java.util.List;
import java.util.ArrayList;
import java.awt.event.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.IOException;
import java.util.Base64;
import com.google.gson.*;

public class Sprout {
    private static final Path DATA_FILE = Paths.get(System.getProperty("user.home"), ".todo_swing_tasks.json");
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private JFrame frame;
    private DefaultTableModel model;
    private JTable table;
    private JTextField inputField;
    private JButton addButton;
    private JButton removeButton;
    private JComboBox<String> filterCombo;
    private JLabel statusLabel;
    private TableRowSorter<TableModel> sorter;

    public static void main(String[] args) {
        // Start on EDT
        SwingUtilities.invokeLater(() -> {
            // Try system look & feel for nicer appearance
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new Sprout().createAndShowGui();
        });
    }

    private void createAndShowGui() {
        frame = new JFrame("Sprout");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Model with two columns: Done (Boolean), Task (String)
        model = new DefaultTableModel(new Object[]{"Done", "Task"}, 0) {
            @Override public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Boolean.class : String.class;
            }
            @Override public boolean isCellEditable(int row, int column) {
                return true; // allow inline edit and toggling
            }
        };

        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(26);
        table.getColumnModel().getColumn(0).setMaxWidth(70); // checkbox column narrower
        table.setFillsViewportHeight(true);

        // Sorting / filtering support
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        // Input row
        inputField = new JTextField(24);
        addButton = new JButton("Add");
        removeButton = new JButton("Remove");
        filterCombo = new JComboBox<>(new String[]{"All", "Active", "Completed"});
        statusLabel = new JLabel(" ");

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        top.add(inputField);
        top.add(addButton);
        top.add(removeButton);
        top.add(new JLabel("Show:"));
        top.add(filterCombo);

        frame.getContentPane().add(top, BorderLayout.NORTH);
        frame.getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);
        frame.getContentPane().add(statusLabel, BorderLayout.SOUTH);

        // Action: add task on button press or Enter in text field
        addButton.addActionListener(e -> addTaskFromInput());
        inputField.addActionListener(e -> addTaskFromInput());

        // Remove selected
        removeButton.addActionListener(e -> removeSelectedTask());

        // Keyboard: Delete to remove
        table.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
        table.getActionMap().put("delete", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { removeSelectedTask(); }
        });

        // Keyboard: Ctrl+S to save
        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
             .put(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK), "save");
        frame.getRootPane().getActionMap().put("save", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { saveTasks(); status("Saved."); }
        });

        // Context menu on table
        JPopupMenu popup = new JPopupMenu();
        JMenuItem editItem = new JMenuItem("Edit task");
        JMenuItem toggleItem = new JMenuItem("Toggle Done");
        JMenuItem deleteItem = new JMenuItem("Delete");
        popup.add(editItem); popup.add(toggleItem); popup.add(deleteItem);

        editItem.addActionListener(e -> {
            int viewRow = table.getSelectedRow();
            if (viewRow >= 0) {
                int modelRow = table.convertRowIndexToModel(viewRow);
                table.editCellAt(viewRow, 1);
                table.requestFocusInWindow();
            }
        });

        toggleItem.addActionListener(e -> {
            int viewRow = table.getSelectedRow();
            if (viewRow >= 0) {
                int modelRow = table.convertRowIndexToModel(viewRow);
                Boolean cur = (Boolean) model.getValueAt(modelRow, 0);
                model.setValueAt(!cur, modelRow, 0);
            }
        });

        deleteItem.addActionListener(e -> removeSelectedTask());

        table.setComponentPopupMenu(popup);

        // Double-click to edit the task cell
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int viewRow = table.rowAtPoint(e.getPoint());
                    int viewCol = table.columnAtPoint(e.getPoint());
                    if (viewRow >= 0 && viewCol == 1) {
                        table.editCellAt(viewRow, viewCol);
                    }
                }
            }
        });

        // Filter combo
        filterCombo.addActionListener(e -> applyFilter());

        // Save on model change (add/edit/toggle), simple approach for small lists
        model.addTableModelListener(e -> {
            // ignore structure changes, only data changes
            if (e.getType() == TableModelEvent.UPDATE || e.getType() == TableModelEvent.INSERT || e.getType() == TableModelEvent.DELETE) {
                saveTasks();
                updateStatusCount();
            }
        });

        // Save on window close
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                saveTasks();
            }
        });

        // populate data
        loadTasks();
        updateStatusCount();

        frame.setSize(700, 420);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Focus the input field
        inputField.requestFocusInWindow();
        
        table.setDragEnabled(true);
        table.setDropMode(DropMode.INSERT_ROWS);
        table.setTransferHandler(new TableRowTransferHandler(table));


    }

    private void addTaskFromInput() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        model.addRow(new Object[]{false, text});
        inputField.setText("");
        status("Added: " + text);
    }

    private void removeSelectedTask() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        String task = (String) model.getValueAt(modelRow, 1);
        model.removeRow(modelRow);
        status("Removed: " + task);
    }

    private void applyFilter() {
        String sel = (String) filterCombo.getSelectedItem();
        if ("All".equals(sel)) {
            sorter.setRowFilter(null);
        } else if ("Active".equals(sel)) {
            sorter.setRowFilter(new RowFilter<TableModel, Integer>() {
                public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                    Boolean done = (Boolean) entry.getValue(0);
                    return !done;
                }
            });
        } else { // Completed
            sorter.setRowFilter(new RowFilter<TableModel, Integer>() {
                public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                    Boolean done = (Boolean) entry.getValue(0);
                    return done;
                }
            });
        }
    }

    private void loadTasks() {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        if (!Files.exists(DATA_FILE)) return;

        try {
            String json = new String(Files.readAllBytes(DATA_FILE), StandardCharsets.UTF_8);
            TodoItem[] items = gson.fromJson(json, TodoItem[].class);
            for (TodoItem item : items) {
                model.addRow(new Object[]{item.isDone(), item.getTask()});
            }
            updateStatusCount();
            status("Loaded " + items.length + " tasks.");
        } catch (IOException e) {
            e.printStackTrace();
            status("Failed to load tasks: " + e.getMessage());
        }
    }


    private void saveTasks() {
        List<TodoItem> items = new ArrayList<>();
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            boolean done = Boolean.TRUE.equals(model.getValueAt(i, 0));
            String task = model.getValueAt(i, 1).toString();
            items.add(new TodoItem(task, done));
        }

        try {
            Files.write(DATA_FILE, gson.toJson(items).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
            status("Failed to save tasks: " + e.getMessage());
        }
    }


    private void status(String s) {
        statusLabel.setText(s);
        // clear message after a short delay
        Timer t = new Timer(3000, evt -> statusLabel.setText(" "));
        t.setRepeats(false);
        t.start();
    }

    private void updateStatusCount() {
        int total = model.getRowCount();
        int done = 0;
        for (int i = 0; i < total; i++) {
            if (Boolean.TRUE.equals(model.getValueAt(i, 0))) done++;
        }
        statusLabel.setText(String.format("Tasks: %d — Done: %d", total, done));
    }
    // Robust row drag-and-drop handler for the JTable
    private static class TableRowTransferHandler extends TransferHandler {
        private final JTable table;
        private List<Object[]> draggedRows;
        private int[] sourceModelRows;      // original model indices of dragged rows
        private int insertIndex = -1;       // model index where we inserted
        private int rowsInserted = 0;       // how many rows we inserted
        private static final DataFlavor FLAVOR = new DataFlavor(List.class, "List of rows");

        public TableRowTransferHandler(JTable table) {
            this.table = table;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            int[] selectedViewRows = table.getSelectedRows();
            sourceModelRows = new int[selectedViewRows.length];
            draggedRows = new ArrayList<>();
            DefaultTableModel model = (DefaultTableModel) table.getModel();

            for (int i = 0; i < selectedViewRows.length; i++) {
                int modelRow = table.convertRowIndexToModel(selectedViewRows[i]);
                sourceModelRows[i] = modelRow;
                int colCount = model.getColumnCount();
                Object[] values = new Object[colCount];
                for (int col = 0; col < colCount; col++) {
                    values[col] = model.getValueAt(modelRow, col);
                }
                draggedRows.add(values);
            }

            return new Transferable() {
                @Override public DataFlavor[] getTransferDataFlavors() { return new DataFlavor[]{FLAVOR}; }
                @Override public boolean isDataFlavorSupported(DataFlavor d) { return FLAVOR.equals(d); }
                @Override public Object getTransferData(DataFlavor d) { return draggedRows; }
            };
        }

        @Override
        public int getSourceActions(JComponent c) { return MOVE; }

        @Override
        public boolean canImport(TransferSupport support) {
            return support.isDrop() && support.isDataFlavorSupported(FLAVOR);
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) return false;

            JTable.DropLocation dl = (JTable.DropLocation) support.getDropLocation();
            int viewRow = dl.getRow();
            DefaultTableModel model = (DefaultTableModel) table.getModel();

            // Convert view row to model insert index safely (handle drop-at-end)
            final int modelInsert;
            if (viewRow < 0) {
                modelInsert = model.getRowCount();
            } else if (viewRow >= table.getRowCount()) {
                // dropped after the last visible row -> append at end of model
                modelInsert = model.getRowCount();
            } else {
                modelInsert = table.convertRowIndexToModel(viewRow);
            }

            try {
                List<Object[]> rows = (List<Object[]>) support.getTransferable().getTransferData(FLAVOR);

                // Insert rows at modelInsert (remember original insert position)
                int insertPos = modelInsert;
                for (Object[] rowData : rows) {
                    model.insertRow(insertPos++, rowData);
                }

                // record for exportDone removal
                insertIndex = modelInsert;
                rowsInserted = rows.size();

                // select the newly inserted rows (convert to view indices)
                if (rowsInserted > 0) {
                    int firstView = table.convertRowIndexToView(insertIndex);
                    int lastView  = table.convertRowIndexToView(insertIndex + rowsInserted - 1);
                    if (firstView >= 0 && lastView >= firstView) {
                        table.getSelectionModel().setSelectionInterval(firstView, lastView);
                    }
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void exportDone(JComponent c, Transferable t, int action) {
            if (action == MOVE && sourceModelRows != null) {
                DefaultTableModel model = (DefaultTableModel) table.getModel();

                // remove original rows in reverse order. If we inserted rows before
                // some original rows, those original indices have been shifted by rowsInserted.
                Integer[] idxs = Arrays.stream(sourceModelRows).boxed().toArray(Integer[]::new);
                Arrays.sort(idxs, Collections.reverseOrder());

                for (int idx : idxs) {
                    int removeIndex = idx;
                    if (insertIndex >= 0 && idx >= insertIndex) {
                        // insertion happened before this original index, so adjust
                        removeIndex = idx + rowsInserted;
                    }
                    // safety check
                    if (removeIndex >= 0 && removeIndex < model.getRowCount()) {
                        model.removeRow(removeIndex);
                    } else if (idx >= 0 && idx < model.getRowCount()) {
                        // fallback: try original index if in bounds
                        model.removeRow(idx);
                    }
                }
            }

            // reset state
            draggedRows = null;
            sourceModelRows = null;
            insertIndex = -1;
            rowsInserted = 0;
        }
    }
    class TodoItem {
        private boolean done;
        private String task;

        public TodoItem(String task, boolean done) {
            this.task = task;
            this.done = done;
        }

        // getters & setters
        public boolean isDone() { return done; }
        public void setDone(boolean done) { this.done = done; }
        public String getTask() { return task; }
        public void setTask(String task) { this.task = task; }
    }

    
}
