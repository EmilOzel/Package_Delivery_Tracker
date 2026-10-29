package app.exceptions;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {
    private final int code;

    public ApiException(int code, String msg){
        super(msg);
        this.code = code;
    }
}
