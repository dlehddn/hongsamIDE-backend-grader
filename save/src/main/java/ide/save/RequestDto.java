package ide.save;

import lombok.Data;

@Data
public class RequestDto {
    private String questionId;
    private String uuid;
    private String requestCode;
    private String language;
}
