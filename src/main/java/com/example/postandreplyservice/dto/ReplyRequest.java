package com.example.postandreplyservice.dto;

import lombok.*;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplyRequest {
    private String comment;

}
