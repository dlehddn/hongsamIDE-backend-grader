package ide.get.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import ide.get.exception.EncodingException;
import ide.get.exception.FileIOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

@Component
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final AmazonS3 amazonS3;
    private static final String DEFAULT_PATH = "user/";
    private static final String SLASH = "/";

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public String getCodeFromS3(String uuid, String questionId, String extension) {
        S3Object findFile = amazonS3.getObject(bucket, DEFAULT_PATH + uuid + SLASH + questionId + extension);
        return ByteToString(findFile);
    }

    private static String ByteToString(S3Object findFile)  {
        S3ObjectInputStream objectContent = findFile.getObjectContent();
        byte[] bytes = new byte[0];
        try {
            bytes = objectContent.readAllBytes();
        } catch (IOException e) {
            log.info("Error By: Occur during byte to string", e);
            throw new FileIOException(e);
        }

        String StringCode = null;
        try {
            StringCode = new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.info("Error By: encoding exception", e);
            throw new EncodingException(e);
        }
        return StringCode;
    }
}
