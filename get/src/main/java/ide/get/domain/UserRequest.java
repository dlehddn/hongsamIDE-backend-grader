package ide.get.domain;

import lombok.Data;

@Data
public class UserRequest {
    private String questionId;
    private String uuid;
    private String language;
}
