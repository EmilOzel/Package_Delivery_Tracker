package app.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor

public class CreateParcelDTO {

    private String senderName;
    private String recipientName;
    private String destinationAddress;
}
