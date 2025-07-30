package fr.epsi.b3devc2.convocationepsi.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportResponseDto {
    private List<CandidatDto> candidats;
    private String sessionId;
    private int candidatsCount;
    private List<String> errors;
    private String message;
}