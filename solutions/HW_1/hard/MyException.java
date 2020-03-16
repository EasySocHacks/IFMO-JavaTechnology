import java.io.IOException;

//NOTICE: 'MyException' is bad class name!
public class MyException extends IOException {
    MyException(String message, Exception exception) {
        super(message, exception);
    }

    MyException(String message) {
        super(message);
    }
}
