package fu.se.smms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiChatRequestDTO {
    @NotBlank(message = "Tin nhan khong duoc de trong")
    private String message;

    private List<AiChatMessageDTO> history;
    private String pagePath;
}
