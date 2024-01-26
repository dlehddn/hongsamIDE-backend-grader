package ide.run.Service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public void putS3(String uuid, File file) {
        amazonS3.putObject(bucket, "user/" + uuid + "/" + file.getName(), file);
    }

//    public String getS3(String uuid, String questionId) throws IOException {
//        S3Object file = amazonS3.getObject(bucket, "user/" + uuid + "/" + questionId + ".java");
//        S3ObjectInputStream objectContent = file.getObjectContent();
//        byte[] bytes = objectContent.readAllBytes();
//        String fileContent = new String(bytes, "UTF-8"); // UTF-8 인코딩 사용
//        System.out.println(fileContent);
//        return fileContent;
//    }

}

