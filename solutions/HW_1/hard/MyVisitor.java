
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

//NOTICE: 'MyVisitor' is bad class name!
public class MyVisitor extends SimpleFileVisitor<Path> {
    private final BufferedWriter writer;

    MyVisitor(BufferedWriter writer) {
        this.writer = writer;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        RecursiveWalk.writeCorrectInfo(file, writer);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        RecursiveWalk.writeExceptionInfo(file.toString(), writer);
        return FileVisitResult.CONTINUE;
    }
}
