import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;
import java.util.stream.Collectors;

enum Priority {
    LOW,
    MEDIUM,
    HIGH
}

interface Identifiable {
    String getId();
}

class Task implements Identifiable {
    private final String id;
    private String title;
    private String description;
    private Priority priority;
    private LocalDate dueDate;
    private boolean completed;

    public Task(String title, String description, Priority priority, LocalDate dueDate) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.dueDate = dueDate;
        this.completed = false;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Priority getPriority() {
        return priority;
    }

    public boolean isCompleted() {
        return completed;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void markAsCompleted() {
        this.completed = true;
    }

    public void update(String title, String description, Priority priority, LocalDate dueDate) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.dueDate = dueDate;
    }

    @Override
    public String toString() {
        return String.format(
                "ID: %s\nTitle: %s\nDescription: %s\nPriority: %s\nDue Date: %s\nCompleted: %s\n",
                id,
                title,
                description,
                priority,
                dueDate,
                completed ? "Yes" : "No"
        );
    }
}

class TaskManager {

    private final List<Task> tasks = new ArrayList<>();

    public void addTask(Task task) {
        tasks.add(task);
    }

    public Optional<Task> findById(String id) {
        return tasks.stream()
                .filter(t -> t.getId().equals(id))
                .findFirst();
    }

    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks);
    }

    public List<Task> getCompletedTasks() {
        return tasks.stream()
                .filter(Task::isCompleted)
                .collect(Collectors.toList());
    }

    public List<Task> getPendingTasks() {
        return tasks.stream()
                .filter(t -> !t.isCompleted())
                .collect(Collectors.toList());
    }

    public List<Task> sortByPriority() {
        return tasks.stream()
                .sorted(Comparator.comparing(Task::getPriority))
                .collect(Collectors.toList());
    }

    public List<Task> sortByDueDate() {
        return tasks.stream()
                .sorted(Comparator.comparing(Task::getDueDate))
                .collect(Collectors.toList());
    }

    public boolean deleteTask(String id) {
        return tasks.removeIf(t -> t.getId().equals(id));
    }
}

public class Main {

    private static final Scanner scanner = new Scanner(System.in);
    private static final TaskManager manager = new TaskManager();

    public static void main(String[] args) {
        boolean running = true;

        while (running) {
            printMenu();
            String choice = scanner.nextLine();

            switch (choice) {
                case "1" -> createTask();
                case "2" -> listTasks(manager.getAllTasks());
                case "3" -> markTaskCompleted();
                case "4" -> deleteTask();
                case "5" -> listTasks(manager.sortByPriority());
                case "6" -> listTasks(manager.sortByDueDate());
                case "7" -> running = false;
                default -> System.out.println("Invalid option.\n");
            }
        }

        System.out.println("Application closed.");
    }

    private static void printMenu() {
        System.out.println("===== TASK MANAGER =====");
        System.out.println("1. Add Task");
        System.out.println("2. List All Tasks");
        System.out.println("3. Mark Task as Completed");
        System.out.println("4. Delete Task");
        System.out.println("5. Sort by Priority");
        System.out.println("6. Sort by Due Date");
        System.out.println("7. Exit");
        System.out.print("Choose: ");
    }

    private static void createTask() {
        try {
            System.out.print("Title: ");
            String title = scanner.nextLine();

            System.out.print("Description: ");
            String description = scanner.nextLine();

            System.out.print("Priority (LOW, MEDIUM, HIGH): ");
            Priority priority = Priority.valueOf(scanner.nextLine().toUpperCase());

            System.out.print("Due date (YYYY-MM-DD): ");
            LocalDate dueDate = LocalDate.parse(scanner.nextLine());

            Task task = new Task(title, description, priority, dueDate);
            manager.addTask(task);

            System.out.println("Task added successfully.\n");

        } catch (Exception e) {
            System.out.println("Invalid input. Task not created.\n");
        }
    }

    private static void listTasks(List<Task> tasks) {
        if (tasks.isEmpty()) {
            System.out.println("No tasks found.\n");
            return;
        }

        for (Task task : tasks) {
            System.out.println(task);
        }
    }

    private static void markTaskCompleted() {
        System.out.print("Enter Task ID: ");
        String id = scanner.nextLine();

        Optional<Task> task = manager.findById(id);

        if (task.isPresent()) {
            task.get().markAsCompleted();
            System.out.println("Task marked as completed.\n");
        } else {
            System.out.println("Task not found.\n");
        }
    }

    private static void deleteTask() {
        System.out.print("Enter Task ID: ");
        String id = scanner.nextLine();

        if (manager.deleteTask(id)) {
            System.out.println("Task deleted successfully.\n");
        } else {
            System.out.println("Task not found.\n");
        }
    }
}
