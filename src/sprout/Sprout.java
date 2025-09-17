package sprout;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.TransferHandler;
import javax.swing.DropMode;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;

public class Sprout {
    private JTable table;
    private TreePanel treePanel;
    private JPanel controlPanel;
    private DefaultTableModel model;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DATA_FILE = Paths.get(System.getProperty("user.home"), ".sprout_tasks.json");

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Sprout().createAndShowGui());
    }

    public void createAndShowGui() {
        JFrame frame = new JFrame("Sprout To-Do List");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create table model
        model = new DefaultTableModel(new Object[]{"Done", "Task"}, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Boolean.class : String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }
        };

        table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setDragEnabled(true);
        table.setDropMode(DropMode.INSERT_ROWS);
        table.setTransferHandler(new TableRowTransferHandler(table));

        // Table model listener for updates
        model.addTableModelListener(e -> {
            saveTasks();
            updateTreeProgress();
        });

        // Tree panel on the right
        treePanel = new TreePanel();

        // Control panel with buttons
        controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addButton = new JButton("Add Task");
        JButton removeButton = new JButton("Remove Selected");
        controlPanel.add(addButton);
        controlPanel.add(removeButton);

        addButton.addActionListener(e -> {
            String task = JOptionPane.showInputDialog(null, "Enter a new task:");
            if (task != null && !task.trim().isEmpty()) {
                model.addRow(new Object[]{false, task});
            }
        });

        removeButton.addActionListener(e -> {
            int[] selectedRows = table.getSelectedRows();
            Arrays.sort(selectedRows);
            for (int i = selectedRows.length - 1; i >= 0; i--) {
                model.removeRow(selectedRows[i]);
            }
        });

        // Main panel layout
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(new JScrollPane(table), BorderLayout.CENTER);
        mainPanel.add(treePanel, BorderLayout.EAST);
        mainPanel.add(controlPanel, BorderLayout.SOUTH);

        frame.setContentPane(mainPanel);
        frame.setSize(600, 400);
        frame.setLocationRelativeTo(null);
        frame.setResizable(true);
        frame.setVisible(true);

        loadTasks();
        updateTreeProgress();
    }

    // -------------------------------
    // Tree progress computation
    // -------------------------------
    private void updateTreeProgress() {
        int total = model.getRowCount();
        int done = 0;
        for (int i = 0; i < total; i++) {
            if (Boolean.TRUE.equals(model.getValueAt(i, 0))) done++;
        }
        double progress = total == 0 ? 0 : (double) done / total;
        treePanel.setProgress(progress);
    }

    // -------------------------------
    // JSON persistence
    // -------------------------------
    private void saveTasks() {
        List<TodoItem> items = new ArrayList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            boolean done = Boolean.TRUE.equals(model.getValueAt(i, 0));
            String task = model.getValueAt(i, 1).toString();
            items.add(new TodoItem(task, done));
        }

        try {
            Files.write(DATA_FILE, gson.toJson(items).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadTasks() {
        if (!Files.exists(DATA_FILE)) return;

        try {
            String json = new String(Files.readAllBytes(DATA_FILE), StandardCharsets.UTF_8);
            TodoItem[] items = gson.fromJson(json, TodoItem[].class);
            for (TodoItem item : items) {
                model.addRow(new Object[]{item.isDone(), item.getTask()});
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // -------------------------------
    // TodoItem class for JSON
    // -------------------------------
    public static class TodoItem {
        private boolean done;
        private String task;

        public TodoItem(String task, boolean done) {
            this.task = task;
            this.done = done;
        }

        public boolean isDone() { return done; }
        public void setDone(boolean done) { this.done = done; }
        public String getTask() { return task; }
        public void setTask(String task) { this.task = task; }
    }

    // -------------------------------
    // TreePanel class
    // -------------------------------
    public static class TreePanel extends JPanel {
        private double progress = 0.0;

        public TreePanel() {
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

            // background
            g2.setColor(new Color(200, 230, 255));
            g2.fillRect(0, 0, width, height);

            // compute tree size
            int treeHeight = (int) (50 + progress * 300);
            int trunkHeight = treeHeight / 4;
            int canopyHeight = treeHeight - trunkHeight;

            // trunk
            g2.setColor(new Color(101, 67, 33));
            int trunkWidth = 20;
            int trunkX = width / 2 - trunkWidth / 2;
            int trunkY = height - trunkHeight;
            g2.fillRect(trunkX, trunkY, trunkWidth, trunkHeight);

            // canopy
            g2.setColor(new Color(34, 139, 34));
            int canopyWidth = (int) (trunkWidth + progress * 100);
            g2.fillOval(width / 2 - canopyWidth / 2, trunkY - canopyHeight + 10, canopyWidth, canopyHeight);
        }
    }

    // -------------------------------
    // Drag-and-drop row reordering
    // -------------------------------
    private static class TableRowTransferHandler extends TransferHandler {
        private final JTable table;
        private List<Object[]> draggedRows;
        private int[] sourceModelRows;
        private int insertIndex = -1;
        private int rowsInserted = 0;
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

            int modelInsert;
            if (viewRow < 0 || viewRow >= table.getRowCount()) {
                modelInsert = model.getRowCount();
            } else {
                modelInsert = table.convertRowIndexToModel(viewRow);
            }

            try {
                List<Object[]> rows = (List<Object[]>) support.getTransferable().getTransferData(FLAVOR);
                int insertPos = modelInsert;
                for (Object[] rowData : rows) {
                    model.insertRow(insertPos++, rowData);
                }

                insertIndex = modelInsert;
                rowsInserted = rows.size();

                if (rowsInserted > 0) {
                    int firstView = table.convertRowIndexToView(insertIndex);
                    int lastView = table.convertRowIndexToView(insertIndex + rowsInserted - 1);
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
                Integer[] idxs = Arrays.stream(sourceModelRows).boxed().toArray(Integer[]::new);
                Arrays.sort(idxs, Collections.reverseOrder());

                for (int idx : idxs) {
                    int removeIndex = idx;
                    if (insertIndex >= 0 && idx >= insertIndex) {
                        removeIndex = idx + rowsInserted;
                    }
                    if (removeIndex >= 0 && removeIndex < model.getRowCount()) {
                        model.removeRow(removeIndex);
                    } else if (idx >= 0 && idx < model.getRowCount()) {
                        model.removeRow(idx);
                    }
                }
            }
            draggedRows = null;
            sourceModelRows = null;
            insertIndex = -1;
            rowsInserted = 0;
        }
    }
}
