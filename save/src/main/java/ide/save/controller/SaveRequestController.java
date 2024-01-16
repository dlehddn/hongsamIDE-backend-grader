package ide.save.controller;

import ide.save.domain.UserRequest;
import ide.save.service.FileIOService;
import ide.save.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
@Slf4j
public class SaveRequestController implements Function<UserRequest, String> {

    private final S3Service s3Service;
    private final FileIOService fileIOService;
    private static final String DOT = ".";

    @Override
    public String apply(UserRequest userRequest) {
        try {
            File file = fileIOService.makeFileToLambdaMemory(userRequest.getRequestCode(),
                    userRequest.getQuestionId(), DOT + userRequest.getLanguage());
            s3Service.saveCodeToS3(userRequest.getUuid(), file);
            return "저장 성공";
        } catch (IOException e) {
            log.info("파일 저장 에러", e);
            return "오류 발생, 저장 실패";
        }
    }
}
