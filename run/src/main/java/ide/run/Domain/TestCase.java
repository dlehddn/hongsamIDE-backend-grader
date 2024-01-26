package ide.run.Domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TestCase {

    private String input;
    private String answer;
    private String result;
    private String output;

}
