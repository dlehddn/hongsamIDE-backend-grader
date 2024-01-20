package ide.run.service;

import ide.run.domain.RequestDto;
import ide.run.domain.ResponseDto;

import java.io.File;

public interface GraderService {
    ResponseDto grader(File codeFile, RequestDto requestDto);
}
