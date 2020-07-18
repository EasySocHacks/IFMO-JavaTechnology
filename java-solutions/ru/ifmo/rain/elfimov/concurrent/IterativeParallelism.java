package ru.ifmo.rain.elfimov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.AdvancedIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IterativeParallelism implements AdvancedIP {
    private final ParallelMapper parallelMapper;

    /**
     * Creating instance of {@link IterativeParallelism}, defining {@link ParallelMapper} for
     * current instance as <strong>null</strong>.
     */
    public IterativeParallelism() {
        parallelMapper = null;
    }

    /**
     * Creating instance of {@link IterativeParallelism}, defining {@link ParallelMapper} for
     * current instance as <strong>parallelMapper</strong>.
     *
     * @param parallelMapper a {@link ParallelMapper} to mapping {@link List} of {@link Stream}.
     */
    public IterativeParallelism(ParallelMapper parallelMapper) {
        this.parallelMapper = parallelMapper;
    }

    /**
     * Performs a task for a {@link List} in parallel.
     * <br>
     * If this {@link IterativeParallelism} contains not null {@link ParallelMapper}
     * then split task not more then <strong>threadCount</strong> blocks.
     * Apply <strong>subListMapper</strong> for each block using {@link ParallelMapper}.
     * Then apply <strong>subListReducer</strong> between threads.
     * <br>
     * Otherwise if this {@link IterativeParallelism} contains nullable {@link ParallelMapper}:
     * <br>
     * Split task not more then <strong>threadCount</strong> threads.
     * Apply <strong>subListMapper</strong> for each threads.
     * Then apply <strong>subListReducer</strong> between threads.
     *
     * @param threadCount number of threads to parallel required task.
     * @param list a {@link List} to compute required task.
     * @param subListMapper a mapper to apply it for eah thread.
     * @param subListReducer a reducer to apply it between after mapper.
     * @param <R> return value of required task.
     * @param <T> type (subtype) of {@link List} elements.
     * @return solution for the required task.
     * @throws InterruptedException when an error occurred while a thread sleeping.
     */
    private <R, T> R computeFunctionFromListInThreads(int threadCount, List<T> list,
                                                      Function<Stream<T>, R> subListMapper,
                                                      Function<Stream<R>, R> subListReducer) throws InterruptedException {
        final int creatingThreadCount = Math.min(threadCount, list.size());
        final int creatingThreadsBlockSize = (creatingThreadCount == 0 ? 0 : list.size() / creatingThreadCount);

        final List<Thread> threadList = new ArrayList<>();
        final List<R> results;
        final List<Stream<T>> valuesStreamsSlicesList = new ArrayList<>();

        for (int i = 0; i < creatingThreadCount; i++) {
            final int leftBorder = i * creatingThreadsBlockSize;
            final int rightBorder = (i != creatingThreadCount - 1 ? leftBorder + creatingThreadsBlockSize : list.size());

            valuesStreamsSlicesList.add(list.subList(leftBorder, rightBorder).stream());
        }

        if (parallelMapper == null) {
            List<R> temporaryResults = new ArrayList<>(Collections.nCopies(creatingThreadCount, null));

            for (int i = 0; i < valuesStreamsSlicesList.size(); i++) {

                final int resultPositionInList = i;

                threadList.add(new Thread(() -> temporaryResults.set(resultPositionInList, subListMapper.apply(valuesStreamsSlicesList.get(resultPositionInList)))));
                threadList.get(i).start();
            }

            results = temporaryResults;

            InterruptedException interruptedException = null;

            for (int i = 0; i < creatingThreadCount; i++) {
                try {
                    threadList.get(i).join();
                } catch (InterruptedException e) {
                    if (interruptedException == null) {
                        interruptedException = e;
                    } else {
                        interruptedException.addSuppressed(e);
                    }
                }
            }

            if (interruptedException != null) {
                throw interruptedException;
            }
        } else {
            results = parallelMapper.map(subListMapper, valuesStreamsSlicesList);
        }

        return subListReducer.apply(results.stream());
    }

    @Override
    public String join(int threads, List<?> list) throws InterruptedException {
        return computeFunctionFromListInThreads(threads,
                list,
                stream -> stream.map(Object::toString).collect(Collectors.joining()),
                stream -> stream.collect(Collectors.joining()));
    }

    @Override
    public <T> List<T> filter(int threads, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        return computeFunctionFromListInThreads(threads,
                list,
                stream -> stream.filter(predicate).collect(Collectors.toList()),
                stream -> stream.flatMap(List::stream).collect(Collectors.toList()));
    }

    @Override
    public <T, U> List<U> map(int threads, List<? extends T> list, Function<? super T, ? extends U> function) throws InterruptedException {
        return computeFunctionFromListInThreads(threads,
                list,
                stream -> stream.map(function).collect(Collectors.toList()),
                stream -> stream.flatMap(List::stream).collect(Collectors.toList()));
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Override
    public <T> T maximum(int threads, List<? extends T> list, Comparator<? super T> comparator) throws InterruptedException {
        return computeFunctionFromListInThreads(threads,
                list,
                stream -> stream.max(comparator).get(),
                stream -> stream.max(comparator).get());
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> list, Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, list, Collections.reverseOrder(comparator));
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        return computeFunctionFromListInThreads(threads,
                list,
                stream -> stream.allMatch(predicate),
                stream -> stream.allMatch(Boolean::booleanValue));
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        return computeFunctionFromListInThreads(threads,
                list,
                stream -> stream.anyMatch(predicate),
                stream -> stream.anyMatch(Boolean::booleanValue));
    }

    @Override
    public <T> T reduce(int threads, List<T> values, Monoid<T> monoid) throws InterruptedException {
        return computeFunctionFromListInThreads(threads,
                values,
                stream -> stream.reduce(monoid.getIdentity(), monoid.getOperator()),
                stream -> stream.reduce(monoid.getIdentity(), monoid.getOperator()));
    }

    @Override
    public <T, R> R mapReduce(int threads, List<T> values, Function<T, R> lift, Monoid<R> monoid) throws InterruptedException {
        return computeFunctionFromListInThreads(threads,
                values,
                stream -> stream.map(lift).reduce(monoid.getIdentity(), monoid.getOperator()),
                stream -> stream.reduce(monoid.getIdentity(), monoid.getOperator()));
    }
}
