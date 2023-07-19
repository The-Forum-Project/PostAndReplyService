package com.example.postandreplyservice.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePostRequest {
    private String title;
    private String content;
    private List<String> images;
    private List<String> attachments;

    // Getters and Setters...
}
