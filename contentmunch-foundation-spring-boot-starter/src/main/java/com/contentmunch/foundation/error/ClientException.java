package com.contentmunch.foundation.error;

import java.io.Serial;

public class ClientException extends BaseException {
    @Serial
    private static final long serialVersionUID = 1L;
    public ClientException(ErrorMessage errorMessage) {
        super(errorMessage);
    }

    public ClientException(ErrorMessage errorMessage, Throwable cause) {
        super(errorMessage, cause);
    }
}
