package sprout.model;

public class TodoItem {
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
