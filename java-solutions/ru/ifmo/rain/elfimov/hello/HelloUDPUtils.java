package ru.ifmo.rain.elfimov.hello;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class HelloUDPUtils {
    protected final static int AWAIT_REQUESTS_TIME = 10;
    protected final static TimeUnit AWAIT_REQUEST_TIME_UNIT = TimeUnit.SECONDS;
    protected final static Charset SERVER_AND_CLIENT_CHARSET = StandardCharsets.UTF_8;

    protected static int tryParseInt(String parsingString, String argumentName) throws NumberFormatException {
        try {
            return Integer.parseInt(parsingString);
        } catch (NumberFormatException e) {
            throw new RuntimeException(String.format("Argument '%s' must be an integer", argumentName), e);
        }
    }
}
