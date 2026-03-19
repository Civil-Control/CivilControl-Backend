package PSG.backEnd.exception.handler;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ResponseMessage {

    private int status;

    private String message;

    private String exception;

    private LocalDateTime timestamp;
}