package ru.ifmo.rain.elfimov.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

public class ParallelMapperImpl implements ParallelMapper {
    private final List<Thread> threadList;
    private final Queue<Runnable> taskList;

    /**
     * Creating an instance of {@link ParallelMapperImpl} with
     * <strong>threads</strong> count of threads.
     * <br>
     * Start <strong>threads</strong> {@link Thread}, awaiting for
     * tasks to execute, until {@link #close()} is called.
     * <br>
     * @param threads threads count to create.
     */
    public ParallelMapperImpl(int threads) {
        threadList = new ArrayList<>();
        taskList = new LinkedList<>();

        for (int i = 0; i < threads; i++) {
            Thread thread = createTaskThread();

            threadList.add(thread);
            thread.start();
        }
    }

    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> function, List<? extends T> list) throws InterruptedException {
        final List<R> resultList = new ArrayList<>(Collections.nCopies(list.size(), null));
        final SynchronizedIntegerWrapper completedTasksCounter = new SynchronizedIntegerWrapper(0);

        for (int i = 0; i < list.size(); i++) {
            final int resultPositionInList = i;
            synchronized (taskList) {
                taskList.add(() -> {
                    resultList.set(resultPositionInList, function.apply(list.get(resultPositionInList)));

                    synchronized (completedTasksCounter) {
                        if (completedTasksCounter.incrementAndGet() == list.size()) {
                            completedTasksCounter.notify();
                        }
                    }
                });

                taskList.notifyAll();
            }
        }

        synchronized (completedTasksCounter) {
            while (completedTasksCounter.getValue() < list.size()) {
                completedTasksCounter.wait();
            }

            return resultList;
        }
    }

    @Override
    public void close() {
        for (Thread thread : threadList) {
            thread.interrupt();
        }

        for (Thread thread : threadList) {
            try {
                thread.join();
            } catch (InterruptedException ignored) { }
        }
    }

    /**
     * Create a thread, waiting for a task.
     *
     * Waiting a {@link Runnable} task from {@link #taskList} to
     * execute it.
     * <br>
     * Ignore {@link InterruptedException} if happened.
     * @return described thread.
     */
    private Thread createTaskThread() {
        return new Thread(() -> {
            try {
                while (!Thread.interrupted()) {
                    Runnable takenTask;

                    synchronized (taskList) {
                        while (taskList.isEmpty()) {
                            taskList.wait();
                        }

                        takenTask = taskList.poll();
                        taskList.notifyAll();
                    }

                    takenTask.run();
                }
            } catch (InterruptedException ignored) {}
        });
    }

    /**
     * A class to increment and get an integer synchronized.
     */
    private static class SynchronizedIntegerWrapper {
        private int value;

        private SynchronizedIntegerWrapper(int value) {
            this.value = value;
        }

        private int getValue() {
            return value;
        }

        private int incrementAndGet() {
            return ++value;
        }
    }
}
