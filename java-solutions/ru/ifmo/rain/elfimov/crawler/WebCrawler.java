package ru.ifmo.rain.elfimov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;

@SuppressWarnings("WeakerAccess")
public class WebCrawler implements Crawler {
    private final Downloader downloader;
    private final ExecutorService executorServiceDownloader;
    private final ExecutorService executorServiceExtractor;
    private final int maxPerHostDownloads;

    private final static String INCORRECT_ARGUMENT_VALUE_MESSAGE_FORMAT =
            "Argument '%s' must be a correct value from 1 " + " to " + Integer.MAX_VALUE;
    private final static int DEFAULT_WEB_CRAWLER_DEPTH = 1;
    private final static int DEFAULT_WEB_CRAWLER_DOWNLOADERS = 1;
    private final static int DEFAULT_WEB_CRAWLER_EXTRACTORS = 1;
    private final static int DEFAULT_WEB_CRAWLER_MAX_PER_HOST_DOWNLOADS = 1;

    private final Map<String, HostTaskQueueWrapper> hostsTaskQueueWrapperMap;

    /**
     * Creating a new {@link WebCrawler} instance.
     *
     * Downloading pages will be compute by {@link Downloader} <strong>downloader</strong>.
     * Pages will be download in <strong>downloaders</strong> threads and
     * links gonna extracts in <strong>extractors</strong> threads.
     * Maximum number of downloads perHost is <strong>perHost</strong>.
     *
     * @param downloader a web pages downloader.
     * @param downloaders threads to download pages.
     * @param extractors threads to extract links.
     * @param perHost maximum number of downloads per host.
     */
    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;

        executorServiceDownloader = Executors.newFixedThreadPool(downloaders);
        executorServiceExtractor = Executors.newFixedThreadPool(extractors);

        maxPerHostDownloads = perHost;

        hostsTaskQueueWrapperMap = new ConcurrentHashMap<>();
    }

    @Override
    public Result download(String url, int depth) {
        Set<String> downloadedLinks = ConcurrentHashMap.newKeySet();
        Set<String> extractedLinks = ConcurrentHashMap.newKeySet();

        ConcurrentMap<String, IOException> exceptionMap = new ConcurrentHashMap<>();

        extractedLinks.add(url);

        Phaser phaser = new Phaser(1);
        downloadUniqueLinks(url, depth, downloadedLinks, extractedLinks, exceptionMap, phaser);
        phaser.arriveAndAwaitAdvance();

        return new Result(new ArrayList<>(downloadedLinks), exceptionMap);
    }

    @Override
    public void close() {
        executorServiceDownloader.shutdown();
        executorServiceExtractor.shutdown();
    }

    @SuppressWarnings("ConstantConditions")
    private void downloadUniqueLinks(String url,
                                     int depth,
                                     Set<String> downloadedLinks,
                                     Set<String> extractedLinks,
                                     Map<String, IOException> exceptionMap,
                                     Phaser phaser) {

        Queue<WaitingDownloadingLinks> waitingDownloadingLinksQueue = new ConcurrentLinkedQueue<>();
        waitingDownloadingLinksQueue.add(new WaitingDownloadingLinks(url, depth));

        int processingDepth = depth;

        while (!waitingDownloadingLinksQueue.isEmpty()) {
            if (waitingDownloadingLinksQueue.peek().getDepth() != processingDepth) {
                processingDepth--;
                phaser.arriveAndAwaitAdvance();

                continue;
            }

            WaitingDownloadingLinks processingLink = waitingDownloadingLinksQueue.poll();
            String currentUrl = processingLink.getUrl();
            int currentDepth = processingLink.getDepth();

            try {
                String currentUrlHost = URLUtils.getHost(currentUrl);
                phaser.register();

                HostTaskQueueWrapper currentUrlHostQueueWrapper =
                        hostsTaskQueueWrapperMap.computeIfAbsent(currentUrlHost, ignored -> new HostTaskQueueWrapper());

                currentUrlHostQueueWrapper.trySubmit(() -> {
                    try {
                        final Document downloadedDocument = downloader.download(currentUrl);

                        downloadedLinks.add(currentUrl);

                        if (currentDepth > 1) {
                            phaser.register();

                            executorServiceExtractor.submit(() -> {
                                try {
                                    for (String extractingLink : downloadedDocument.extractLinks()) {
                                        if (!extractedLinks.contains(extractingLink)) {
                                            extractedLinks.add(extractingLink);

                                            waitingDownloadingLinksQueue.add(new WaitingDownloadingLinks(extractingLink,
                                                    currentDepth - 1));
                                        }
                                    }
                                } catch (IOException ignored) {
                                } finally {
                                    phaser.arriveAndDeregister();
                                }
                            });
                        }
                        
                    } catch (IOException e) {
                        exceptionMap.put(currentUrl, e);
                    } finally {
                        phaser.arriveAndDeregister();
                        currentUrlHostQueueWrapper.trySubmit();
                    }
                });
            } catch (MalformedURLException e) {
                exceptionMap.put(currentUrl, e);
            }

            if (waitingDownloadingLinksQueue.isEmpty()) {
                phaser.arriveAndAwaitAdvance();
            }
        }
    }

    private class HostTaskQueueWrapper {
        private final Queue<Runnable> queue;
        private int processingPages;

        public HostTaskQueueWrapper() {
            queue = new LinkedList<>();
            processingPages = 0;
        }

        public synchronized void trySubmit(Runnable task) {
            if (processingPages >= maxPerHostDownloads) {
                queue.add(task);
            } else {
                executorServiceDownloader.submit(task);

                processingPages++;

            }
        }

        public synchronized void trySubmit() {
            if (!queue.isEmpty()) {
                executorServiceDownloader.submit(queue.poll());
            } else {
                processingPages--;
            }
        }
    }

    private static class ArgumentOutOfBoundsException extends Exception {
        ArgumentOutOfBoundsException() {
            super();
        }
    }

    private static class WaitingDownloadingLinks {
        private final String url;
        private final int depth;

        public WaitingDownloadingLinks(String url, int depth) {
            this.url = url;
            this.depth = depth;
        }

        public String getUrl() {
            return url;
        }

        public int getDepth() {
            return depth;
        }
    }

    /**
     * A main method to run a WebCrawler, downloading pages.
     * <br>
     * Usage: java WebCrawler url [depth [downloaders [extractors [perHost]]]]
     * <br>
     * Downloading pages starting with <strong>url</strong> and
     * going down no deeper then <strong>depth</strong>.
     * Using <string>downloaders</string> threads to
     * download pages and <strong>extractors</strong> threads to extract links from pages.
     * Maximum number of downloads per one host is <strong>perHost</strong>.
     * <br>
     * Print downloaded pages list and pages downloading exception during
     * {@link WebCrawler} work to {@link System#out}.
     * <br>
     * If optional argument is not specified then use a default value.
     * @see #DEFAULT_WEB_CRAWLER_DEPTH
     * @see #DEFAULT_WEB_CRAWLER_DOWNLOADERS
     * @see #DEFAULT_WEB_CRAWLER_EXTRACTORS
     * @see #DEFAULT_WEB_CRAWLER_MAX_PER_HOST_DOWNLOADS
     *
     * @param args program arguments.
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java WebCrawler url [depth [downloaders [extractors [perHost]]]]");
            return;
        }

        String url;
        int depth = DEFAULT_WEB_CRAWLER_DEPTH;
        int downloaders = DEFAULT_WEB_CRAWLER_DOWNLOADERS;
        int extractors = DEFAULT_WEB_CRAWLER_EXTRACTORS;
        int maxPerHostDownloads = DEFAULT_WEB_CRAWLER_MAX_PER_HOST_DOWNLOADS;

        url = args[0];

        try {
            if (args.length > 1) {
                depth = tryConvertArgumentToInteger(args[1], "depth");
                checkArgumentNumberBounds(depth, "depth");
            }

            if (args.length > 2) {
                downloaders = tryConvertArgumentToInteger(args[2], "downloaders");
                checkArgumentNumberBounds(downloaders, "downloaders");
            }

            if (args.length > 3) {
                extractors = tryConvertArgumentToInteger(args[3], "extractors");
                checkArgumentNumberBounds(extractors, "extractors");
            }

            if (args.length > 4) {
                maxPerHostDownloads = tryConvertArgumentToInteger(args[3], "perHost");
                checkArgumentNumberBounds(maxPerHostDownloads, "perHost");
            }

            try(WebCrawler webCrawler = new WebCrawler(new CachingDownloader(), downloaders, extractors, maxPerHostDownloads)) {
                Result downloadResult = webCrawler.download(url, depth);

                System.out.println("Downloaded:");
                for (String downloadedLink : downloadResult.getDownloaded()) {
                    System.out.println(" * " + downloadedLink);
                }

                System.out.println("---------------------------------");

                System.out.println("Errors:");
                for (Map.Entry<String, IOException> linkExceptionEntry : downloadResult.getErrors().entrySet()) {
                    System.out.println(" * " + linkExceptionEntry.getKey());

                    linkExceptionEntry.getValue().printStackTrace();
                }
            }
        } catch (NumberFormatException | ArgumentOutOfBoundsException ignored) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int tryConvertArgumentToInteger(String argumentValue,
                                                   String argumentName) throws NumberFormatException{
        try {
            return Integer.parseInt(argumentValue);
        } catch (NumberFormatException e) {
            System.out.println(String.format(INCORRECT_ARGUMENT_VALUE_MESSAGE_FORMAT, argumentName));
            throw e;
        }
    }

    private static void checkArgumentNumberBounds(int argumentValue,
                                                  String argumentName) throws ArgumentOutOfBoundsException {
        if (argumentValue < 1) {
            System.out.println(String.format(INCORRECT_ARGUMENT_VALUE_MESSAGE_FORMAT, argumentName));
            throw new ArgumentOutOfBoundsException();
        }
    }
}
