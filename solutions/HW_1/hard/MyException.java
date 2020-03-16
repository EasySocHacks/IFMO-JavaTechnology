import java.io.IOException;

public class MyException extends IOException {
    MyException(String message, Exception exception) {
        super(message, exception);
    }

    MyException(String message) {
        super(message);
    }
}
