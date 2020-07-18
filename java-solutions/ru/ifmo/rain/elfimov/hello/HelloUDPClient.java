package ru.ifmo.rain.elfimov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloUDPClient extends HelloUDPUtils implements HelloClient {
    private final static String CONSOLE_INPUT_USAGE = "HelloUDPClient host port prefix threads requests";
    private final static int DATAGRAM_SOCKET_SO_TIMEOUT = 100;

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        final ExecutorService parallelExecutor = Executors.newFixedThreadPool(threads);

        for (int threadNumber = 0; threadNumber < threads; threadNumber++) {
            final int currentFinalThreadNumber = threadNumber;

            parallelExecutor.submit(() -> {
                try (DatagramSocket udpConnection = new DatagramSocket()) {
                    udpConnection.setSoTimeout(DATAGRAM_SOCKET_SO_TIMEOUT);

                    final DatagramPacket request;

                    try {
                        request = new DatagramPacket(
                                new byte[udpConnection.getSendBufferSize()],
                                udpConnection.getSendBufferSize(),
                                new InetSocketAddress(InetAddress.getByName(host), port));
                    } catch (UnknownHostException e) {
                        throw new IllegalArgumentException(String.format("host name '%s' is not correct", host));
                    }

                    final DatagramPacket response = new DatagramPacket(
                            new byte[udpConnection.getReceiveBufferSize()],
                            udpConnection.getReceiveBufferSize());

                    for (int currentThreadRequestNumber = 0; currentThreadRequestNumber < requests; currentThreadRequestNumber++) {
                        final String sendingMessage = prefix + currentFinalThreadNumber + "_" + currentThreadRequestNumber;

                        while (!udpConnection.isClosed()) {
                            try {
                                request.setData(sendingMessage.getBytes(SERVER_AND_CLIENT_CHARSET));

                                System.out.println("Sending message: " + sendingMessage);
                                udpConnection.send(request);

                                udpConnection.receive(response);
                                final String receivedMessage = new String(response.getData(), response.getOffset(), response.getLength(), SERVER_AND_CLIENT_CHARSET);

                                if (receivedMessage.contains(sendingMessage)) {
                                    System.out.println("Received message: " + receivedMessage);

                                    break;
                                }
                            } catch (IOException e) {
                                System.out.println("Something goes wrong while sending or receiving message");
                            }
                        }
                    }
                } catch (SocketException e) {
                    System.out.println("Something goes wrong while connecting to the server");
                }
            });
        }

        parallelExecutor.shutdown();

        try {
            parallelExecutor.awaitTermination(AWAIT_REQUESTS_TIME, AWAIT_REQUEST_TIME_UNIT);
        } catch (InterruptedException ignored) {
        }
    }

    public static void main(String[] args) {
        if (args.length != 5) {
            throw new RuntimeException("Usage: " + CONSOLE_INPUT_USAGE);
        }

        String host = args[0];
        int port = tryParseInt(args[1], "port");
        String prefix = args[2];
        int threads = tryParseInt(args[3], "threads");
        int requests = tryParseInt(args[4], "requests");

        HelloUDPClient helloUDPClient = new HelloUDPClient();
        helloUDPClient.run(host, port, prefix, threads, requests);
    }
}
