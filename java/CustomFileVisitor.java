import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemLoopException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;

public class CustomFileVisitor extends SimpleFileVisitor<Path> {

    final Path source;
    final Path target;

    public CustomFileVisitor(Path source, Path target) {
        this.source = source;
        this.target = target;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
            throws IOException
    {      
        Path newDirectory= target.resolve(source.relativize(dir));
        try{
            Files.copy(dir,newDirectory);
        }
        catch (FileAlreadyExistsException ioException){
            //log it and move
            return SKIP_SUBTREE; // skip processing
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

       Path newFile = target.resolve(source.relativize(file));

        try{
            Files.copy(file,newFile);
        }
        catch (IOException ioException){
            //log it and move
        }

        return FileVisitResult.CONTINUE;

    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {      
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
        if (exc instanceof FileSystemLoopException) {
            //log error
        } else {
            //log error
        }
        return CONTINUE;
    }
}