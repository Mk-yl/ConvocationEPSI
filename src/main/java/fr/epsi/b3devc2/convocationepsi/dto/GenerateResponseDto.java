package fr.epsi.b3devc2.convocationepsi.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerateResponseDto {
    private String sessionId;
    private int filesGenerated;
    private String downloadUrl;
    private String message;
}