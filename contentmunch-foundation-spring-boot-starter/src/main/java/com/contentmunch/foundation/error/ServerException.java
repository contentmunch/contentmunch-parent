package com.contentmunch.foundation.error;

import java.io.Serial;

public class ServerException extends BaseException {

    @Serial
    private static final long serialVersionUID = 1L;
    public ServerException(ErrorMessage errorMessage) {
        super(errorMessage);
    }

    public ServerException(ErrorMessage errorMessage, Throwable cause) {
        super(errorMessage, cause);
    }
}
