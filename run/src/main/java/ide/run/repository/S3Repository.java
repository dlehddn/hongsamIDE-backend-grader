package ide.run.repository;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.File;

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

    public void loadAdminFile(String questionId, String fileName) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(bucket, PATH_ADMIN + questionId + SLASH + fileName);
        File file = new File(PATH_LAMBDA + fileName);
        amazonS3.getObject(getObjectRequest, file);
    }

}
