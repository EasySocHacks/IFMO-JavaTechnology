package ru.ifmo.rain.elfimov.walk;

import java.io.IOException;

public class RecursiveWalkException extends IOException {
    RecursiveWalkException(String message, Exception exception) {
        super(message, exception);
    }

    RecursiveWalkException(String message) {
        super(message);
    }
}
