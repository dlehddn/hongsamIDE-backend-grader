package ide.get.controller;

import ide.get.domain.UserRequest;
import ide.get.domain.UserResponse;
import ide.get.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
@Slf4j
public class GetRequestController implements Function<UserRequest, UserResponse> {

    private final S3Service s3Service;

    @Override
    public UserResponse apply(UserRequest userRequest) {
        String extension = "." + userRequest.getLanguage();
        return UserResponse.builder()
                .requestCode(s3Service.getCodeFromS3(userRequest.getUuid(), userRequest.getQuestionId(), extension))
                .build();
    }
}
