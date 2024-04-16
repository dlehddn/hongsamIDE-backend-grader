package ide.run.exception.s3;

public class S3IOException extends RuntimeException {
    public S3IOException(Throwable cause) {
        super(cause);
    }
}
