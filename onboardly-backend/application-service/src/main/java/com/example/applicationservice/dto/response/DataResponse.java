package com.example.applicationservice.dto.response;

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