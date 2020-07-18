package ru.ifmo.rain.elfimov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloUDPServer extends HelloUDPUtils implements HelloServer {
    private final static String CONSOLE_INPUT_USAGE = "HelloUDPServer port threads";

    private ExecutorService parallelExecutor;
    private DatagramSocket datagramSocket;

    @Override
    public void start(int port, int threads) {
        parallelExecutor = Executors.newFixedThreadPool(threads);

        try {
            datagramSocket = new DatagramSocket(port);
        } catch (SocketException e) {
            throw new IllegalArgumentException(String.format("Unable to open socket on port '%d'", port));
        }

        for (int threadNumber = 0; threadNumber < threads; threadNumber++) {

            int currentFinalThreadNumber = threadNumber;

            parallelExecutor.submit(() -> {
                try {
                    while (!datagramSocket.isClosed()) {
                        DatagramPacket requestAndResponse = new DatagramPacket(
                                new byte[datagramSocket.getSendBufferSize()],
                                datagramSocket.getSendBufferSize());

                        try {
                            datagramSocket.receive(requestAndResponse);

                            requestAndResponse.setData(("Hello, " +
                                    new String(
                                            requestAndResponse.getData(),
                                            requestAndResponse.getOffset(),
                                            requestAndResponse.getLength(),
                                            SERVER_AND_CLIENT_CHARSET)).
                                    getBytes(SERVER_AND_CLIENT_CHARSET));

                            datagramSocket.send(requestAndResponse);
                        } catch (IOException ignored) {
                        }
                    }
                } catch (SocketException e) {
                    System.out.println("Something goes wrong while creating server listener on thread " + currentFinalThreadNumber);
                }
            });
        }
    }

    @Override
    public void close() {
        datagramSocket.close();

        parallelExecutor.shutdown();

        try {
            parallelExecutor.awaitTermination(AWAIT_REQUESTS_TIME, AWAIT_REQUEST_TIME_UNIT);
        } catch (InterruptedException ignored) {
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            throw new RuntimeException("Usage: " + CONSOLE_INPUT_USAGE);
        }

        int port = tryParseInt(args[0], "port");
        int threads = tryParseInt(args[1], "threads");

        try (HelloUDPServer helloUDPServer = new HelloUDPServer()) {
            helloUDPServer.start(port, threads);
        }
    }
}
