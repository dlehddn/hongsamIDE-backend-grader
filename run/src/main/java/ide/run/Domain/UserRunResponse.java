package ide.run.Domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class UserRunResponse {

    private String message;
    private List<String> result;
    private List<TestCase> testCaseResult;

}
