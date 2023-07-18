package com.example.postandreplyservice.dto;

import com.example.postandreplyservice.domain.Post;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class PostResonse {

    Post post;
}
