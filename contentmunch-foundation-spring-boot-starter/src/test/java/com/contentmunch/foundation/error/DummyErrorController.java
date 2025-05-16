package com.contentmunch.foundation.error;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dummy/error")
class DummyErrorController {

    @GetMapping("/server")
    public void server(){
        throw new ServerException(ErrorMessage.builder().code("SERVER_ERR").message("Internal server error").build());
    }

    @GetMapping("/client")
    public void client(){
        throw new ClientException(ErrorMessage.builder().code("CLIENT_ERR").message("Invalid request").build());
    }

    @GetMapping("/business")
    public void business(){
        throw new BusinessException(ErrorMessage.builder().code("BUSINESS_ERR").message("Violation").build());
    }

    @GetMapping("/unhandled")
    public void unhandled(){
        throw new RuntimeException("Unexpected error");
    }
}
