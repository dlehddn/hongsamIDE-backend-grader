package ide.save;

import com.amazonaws.services.s3.AmazonS3;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class LambdaSave implements Function<RequestDto, String> {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Override
    public String apply(RequestDto requestDto) {
        File file = null;
        if (requestDto.getRequestCode().equals("") || requestDto.getRequestCode() == null) {
            return "코드를 올바르게 입력하세요.";
        }
        try {
            file = saveFile(requestDto.getRequestCode(), requestDto.getQuestionId());
        } catch (IOException e) {
            return e.getMessage();
        }
        putS3(requestDto.getUuid(), file);
        return "저장 성공";
    }

    public File saveFile(String newCode, String title) throws IOException {
        File javaFile = new File("/tmp/" + title + ".java");
        FileWriter writer = new FileWriter(javaFile);
        writer.write(newCode);
        writer.close();
        return javaFile;
    }

    public void putS3(String uuid, File file) {
        amazonS3.putObject(bucket, "user/" + uuid + "/" + file.getName(), file);
    }
}
