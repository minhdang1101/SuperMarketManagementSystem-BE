package fu.se.smms.service;

import fu.se.smms.dto.AiChatRequestDTO;
import fu.se.smms.dto.AiChatResponseDTO;
import fu.se.smms.dto.UserDetailDTO;

public interface AiChatService {
    AiChatResponseDTO chat(AiChatRequestDTO request, UserDetailDTO principal);
}
