package ide.run.repository;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import ide.run.exception.s3.S3IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.File;
import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class S3Repository {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;
    private static final String PATH_USER = "user/";
    private static final String PATH_ADMIN = "admin/";
    private static final String PATH_LAMBDA = "/tmp/";
    private static final String SLASH = "/";

    public void saveUserCode(String uuid, File file) {
        amazonS3.putObject(bucket, PATH_USER + uuid + SLASH + file.getName(), file);
    }

    public String getUserCode(String uuid, String questionId, String extension) {
        S3Object findFile = amazonS3.getObject(bucket, PATH_USER + uuid + SLASH + questionId + extension);
        try {
            return ByteToString(findFile);
        } catch (IOException e) {
            log.info("Error by: Transfer ByteStream to String Code", e);
            throw new S3IOException(e);
        }
    }

    public void loadAdminFile(String questionId, String fileName) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(bucket, PATH_ADMIN + questionId + SLASH + fileName);
        File file = new File(PATH_LAMBDA + fileName);
        amazonS3.getObject(getObjectRequest, file);
    }

    private static String ByteToString(S3Object findFile) throws IOException{
        S3ObjectInputStream objectContent = findFile.getObjectContent();
        byte[] bytes = objectContent.readAllBytes();
        String StringCode = new String(bytes, "UTF-8");
        return StringCode;
    }
}
