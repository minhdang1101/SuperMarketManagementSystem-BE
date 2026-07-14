package fu.se.smms.controller;

import fu.se.smms.dto.AiChatRequestDTO;
import fu.se.smms.dto.AiChatResponseDTO;
import fu.se.smms.dto.UserDetailDTO;
import fu.se.smms.service.AiChatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
public class AiChatController {

    private final AiChatService aiChatService;

    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping("/chat")
    public ResponseEntity<AiChatResponseDTO> chat(@Valid @RequestBody AiChatRequestDTO request,
                                                  @AuthenticationPrincipal UserDetailDTO principal) {
        return ResponseEntity.ok(aiChatService.chat(request, principal));
    }
}
