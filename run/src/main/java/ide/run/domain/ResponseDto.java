package ide.run.domain;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class ResponseDto {
    private List<Boolean> correctResult;
    private List<String> printResult;
    private boolean isError;
    private String errorMessage;
}
