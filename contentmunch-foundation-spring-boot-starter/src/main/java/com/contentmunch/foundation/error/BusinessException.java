package com.contentmunch.foundation.error;

import java.io.Serial;

public class BusinessException extends BaseException {
    @Serial
    private static final long serialVersionUID = 1L;

    public BusinessException(ErrorMessage errorMessage) {
        super(errorMessage);
    }

    public BusinessException(ErrorMessage errorMessage, Throwable cause) {
        super(errorMessage, cause);
    }

}
