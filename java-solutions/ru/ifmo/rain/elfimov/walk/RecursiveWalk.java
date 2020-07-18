package ru.ifmo.rain.elfimov.walk;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

@SuppressWarnings("WeakerAccess")
public class RecursiveWalk {

    private static final String OUTPUT_FORMAT = "%08x %s%n";
    private static final Charset CHARSET = StandardCharsets.UTF_8;

    public void run(String[] args) throws RecursiveWalkException {
        if (args == null || args.length != 2) {
            throw new RecursiveWalkException("Usage: java RecursiveWalk <input file> <output file>");
        } else {
            try (BufferedReader inputReader = Files.newBufferedReader(Paths.get(args[0]), CHARSET)) {
                try (BufferedWriter outputWriter = Files.newBufferedWriter(Paths.get(args[1]), CHARSET)) {
                    String line;

                    try {
                        while ((line = inputReader.readLine()) != null) {
                            try {
                                Path path = Paths.get(line);
                                Files.walkFileTree(path, new RecursiveFileVisitor(outputWriter));
                            } catch (NullPointerException | InvalidPathException e) {
                                writeExceptionInfo(line, outputWriter);
                            }
                        }
                    } catch (IOException e) {
                        throw new RecursiveWalkException("Cannot read input file because of some I/O error", e);
                    }
                } catch (IOException e) {
                    throw new RecursiveWalkException("Cannot create writer to write output file", e);
                } catch (NullPointerException | InvalidPathException e) {
                    throw new RecursiveWalkException("Output file '" + args[1] + "' must be a correct file path", e);
                }

            } catch (IOException e) {
                throw new RecursiveWalkException("Cannot create reader to read input file", e);
            } catch (NullPointerException | InvalidPathException e) {
                throw new RecursiveWalkException("Input file '" + args[0] + "' must be a correct existing file path", e);
            }
        }
    }

    public static void writeExceptionInfo(String file, BufferedWriter writer) throws RecursiveWalkException {
        try {
            writer.write(String.format(OUTPUT_FORMAT, 0, file));
        } catch (IOException e) {
            throw new RecursiveWalkException("Cannot write to output file because of some I/O error", e);
        }
    }

    public static void writeCorrectInfo(Path file, BufferedWriter writer) throws RecursiveWalkException {
        InputStream stream;
        try {
            stream = new BufferedInputStream(Files.newInputStream(file));
        } catch (IOException e) {
            throw new RecursiveWalkException("Cannot create reader to file '" + file + "'", e);
        }

        int hash = 0x811c9dc5;
        byte[] readArray = new byte[1024];

        try {
            int readBytes;

            while ((readBytes = stream.read(readArray)) != -1) {
                for (int i = 0; i < readBytes; i++) {
                    hash = (hash * 0x01000193) ^ (readArray[i] & 0xff);
                }
            }
        } catch (IOException e) {
            throw new RecursiveWalkException("Cannot read file '" + file, e);
        } catch (NullPointerException e) {
            throw new RecursiveWalkException("Something goes wrong while reading", e);
        }

        try {
            writer.write(String.format(OUTPUT_FORMAT, hash, file));
        } catch (IOException e) {
            throw new RecursiveWalkException("Cannot write to output file because of some I/O error", e);
        }
    }

    public static void main(String[] args) {
        RecursiveWalk walk = new RecursiveWalk();
        try {
            walk.run(args);
        } catch (RecursiveWalkException e) {
            e.getMessage();
        }
    }
}
