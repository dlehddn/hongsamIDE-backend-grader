package ide.run.Controller;

import ide.run.Domain.UserRunRequest;
import ide.run.Domain.UserRunResponse;
import ide.run.Service.JavaCompilerService;
import ide.run.Service.JavaFileIOService;
import ide.run.Service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class LambdaTestCaseRunController implements Function<UserRunRequest, UserRunResponse> {

    private final S3Service s3Service;
    private final JavaCompilerService javaCompilerService;
    private final JavaFileIOService fileIOService;

    @Override
    public UserRunResponse apply(UserRunRequest userRunRequest) {

        File file = null;
        if (userRunRequest.getRequestCode().equals("") || userRunRequest.getRequestCode() == null) {
            return new UserRunResponse("코드를 올바르게 입력하세요.", null, null);
        }
        try {
            // 1. 람다 임시 메모리 공간에 사용자 코드를 파일로 저장
            file = fileIOService.saveFile(userRunRequest.getRequestCode(), userRunRequest.getQuestionId());
        } catch (IOException e) {
            return new UserRunResponse(e.getMessage(), null, null);
        }
        // 2. 사용자 제출 코드 S3에서 관리
        s3Service.putS3(userRunRequest.getUuid(), file);
        try {
            // 3. testCaseInput.txt, testCaseAnswer.txt 파일 가져오기
            javaCompilerService.loadTestInAnsFile(userRunRequest.getQuestionId());

            // 4. Java Compile + Java Reflection
            UserRunResponse userRunResponse = javaCompilerService.compiler(userRunRequest.getQuestionId());

            if (userRunResponse.getResult() == null) {
                // 에러 반환
                return userRunResponse;
            } else {
                // 테스트 케이스 정답 반환
                return javaCompilerService.TestCaseCompareFiles(userRunResponse,"C:\\Users\\Seoyeon\\Desktop\\홍삼\\hongsamIDE-backend-grader\\run\\src\\main\\java\\store\\output.txt",
                        "C:\\Users\\Seoyeon\\Desktop\\홍삼\\hongsamIDE-backend-grader\\run\\src\\main\\java\\store\\testCaseAnswer.txt",
                        "C:\\Users\\Seoyeon\\Desktop\\홍삼\\hongsamIDE-backend-grader\\run\\src\\main\\java\\store\\testCaseInput.txt");
            }

        } catch (Exception e) {
            return new UserRunResponse("알 수 없는 예외 발생.", null, null);
        }
    }
}
