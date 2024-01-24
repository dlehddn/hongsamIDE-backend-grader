package ide.run.controller;

import ide.run.domain.RequestDto;
import ide.run.domain.ResponseDto;
import ide.run.service.fileIO.FileIOService;
import ide.run.service.grader.GraderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class RunRequestController implements Function<RequestDto, ResponseDto> {

    private final FileIOService fileIOService;
    private final GraderService graderService;

    @Override
    public ResponseDto apply(RequestDto requestDto) {
        String extension = "." + requestDto.getLanguage();
        File file = fileIOService.makeFileToLambdaMemory(requestDto.getRequestCode(), requestDto.getQuestionId(), extension);


        return graderService.grader(file, requestDto);
    }
}
