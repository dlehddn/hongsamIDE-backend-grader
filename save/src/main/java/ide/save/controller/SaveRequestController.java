package ide.save.controller;

import ide.save.domain.UserRequest;
import ide.save.domain.UserResponse;
import ide.save.service.FileIOService;
import ide.save.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
@Slf4j
public class SaveRequestController implements Function<UserRequest, UserResponse> {

    private final S3Service s3Service;
    private final FileIOService fileIOService;
    private static final String DOT = ".";

    @Override
    public UserResponse apply(UserRequest userRequest) {
            File file = fileIOService.makeFileToLambdaMemory(userRequest.getRequestCode(),
                    userRequest.getQuestionId(), DOT + userRequest.getLanguage());
            s3Service.saveCodeToS3(userRequest.getUuid(), file);
            return UserResponse.builder()
                    .message("저장 성공")
                    .build();
    }
}
