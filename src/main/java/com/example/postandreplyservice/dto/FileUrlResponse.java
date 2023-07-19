package com.example.postandreplyservice.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
@Builder
public class FileUrlResponse {

    private String statusCode;
    private List<String> urls;
}
