package ide.get.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class S3Service {

    private final AmazonS3 amazonS3;
    private static final String DEFAULT_PATH = "user/";
    private static final String SLASH = "/";

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public String getCodeFromS3(String uuid, String questionId, String extension) throws IOException {
        S3Object findFile = amazonS3.getObject(bucket, DEFAULT_PATH + uuid + SLASH + questionId + extension);
        return getStringCode(findFile);
    }

    private static String getStringCode(S3Object findFile) throws IOException {
        S3ObjectInputStream objectContent = findFile.getObjectContent();
        byte[] bytes = objectContent.readAllBytes();
        String StringCode = new String(bytes, "UTF-8");
        return StringCode;
    }
}
