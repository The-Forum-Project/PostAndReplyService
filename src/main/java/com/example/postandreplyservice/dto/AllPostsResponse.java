package com.example.postandreplyservice.dto;

import com.example.postandreplyservice.domain.Post;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Setter
@Getter
@Builder
public class AllPostsResponse {

    List<Post> posts;
}
