package sprout.view;

import sprout.controller.SproutController;
import sprout.model.Persistence;
import javax.swing.*;
import java.awt.*;

public class SproutView {
    private final JFrame frame;
    private final JTable table;
    private final TreeGraphics treePanel;
    private final JPanel controlPanel;

    public SproutView(SproutController controller, Persistence model) {
        frame = new JFrame("Sprout To-Do List");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        table = new JTable(model.getTableModel());
        table.setFillsViewportHeight(true);
        treePanel = new TreeGraphics();

        JButton addButton = new JButton("Add Task");
        JButton removeButton = new JButton("Remove Selected");

        controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.add(addButton);
        controlPanel.add(removeButton);

        addButton.addActionListener(e -> controller.handleAddTask());
        removeButton.addActionListener(e -> controller.handleRemoveTask());

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(new JScrollPane(table), BorderLayout.CENTER);
        mainPanel.add(treePanel, BorderLayout.EAST);
        mainPanel.add(controlPanel, BorderLayout.SOUTH);

        frame.setContentPane(mainPanel);
        frame.setSize(600, 400);
        frame.setLocationRelativeTo(null);
    }

    public void show() { frame.setVisible(true); }
    public JTable getTable() { return table; }
    public TreeGraphics getTreePanel() { return treePanel; }
}
