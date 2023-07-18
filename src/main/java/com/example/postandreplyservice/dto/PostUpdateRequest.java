package com.example.postandreplyservice.dto;

import lombok.*;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class PostUpdateRequest {
    private String status;
    private Boolean isArchived;

    // Getters and setters...
}
