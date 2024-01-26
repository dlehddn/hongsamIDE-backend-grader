package ide.get;


import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class LambdaGet implements Function<RequestDto, String> {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Override
    public String apply(RequestDto requestDto) {

        try {
            return getS3(requestDto.getUuid(), requestDto.getQuestionId());
        } catch (IOException e) {
            return "기본 파일 제공";
        }

    }

    public String getS3(String uuid, String questionId) throws IOException {
        S3Object file = amazonS3.getObject(bucket, "user/" + uuid + "/" + questionId + ".java");
        S3ObjectInputStream objectContent = file.getObjectContent();
        byte[] bytes = objectContent.readAllBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }

}
