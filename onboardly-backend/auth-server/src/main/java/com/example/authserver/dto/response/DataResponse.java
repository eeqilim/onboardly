package com.example.authserver.dto.response;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class DataResponse {
    private String message;
    private Object data;
}
