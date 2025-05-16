package com.contentmunch.foundation.error;

import lombok.Getter;

@Getter
public abstract class BaseException extends RuntimeException {
    private final ErrorMessage errorMessage;

    public BaseException(final ErrorMessage errorMessage) {
        super();
        this.errorMessage = errorMessage;
    }

    public BaseException(final ErrorMessage errorMessage, final Throwable cause) {
        super(cause);
        this.errorMessage = errorMessage;
    }

}
