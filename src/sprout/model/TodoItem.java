package sprout.model;
/**
 * the class for making tasks
 * 
 */
public class TodoItem {
    private boolean done;
    private String task;
    /**
     * the constructor for creating new tasks
     * @param task  the string variable for what the tasks is
     * @param done the current state of if a task is done
     */
    public TodoItem(String task, boolean done) {
        this.task = task;
        this.done = done;
    }
    /**
     * a getter function for the tasks done variable
     * @return the state of if a task is completed
     */
    public boolean isDone() { return done; }
    /**
     * a setter function for changing the done variable
     * @param done the state of if a task is completed
     */
    public void setDone(boolean done) { this.done = done; }
    /**
     * a getter function for returning the task details
     * @return the string variable or description of the task
     */
    public String getTask() { return task; }
    /**
     * a setter function for changing the task description
     * @param task the string variable or description of the task
     */
    public void setTask(String task) { this.task = task; }
}
