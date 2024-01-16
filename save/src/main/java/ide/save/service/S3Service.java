package ide.save.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class S3Service {

    private final AmazonS3 amazonS3;
    private static final String DEFAULT_PATH = "user/";
    private static final String SLASH = "/";
    
    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public void saveCodeToS3(String uuid, File file) {
        amazonS3.putObject(bucket, DEFAULT_PATH + uuid + SLASH + file.getName(), file);
    }

}
