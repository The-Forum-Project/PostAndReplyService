package com.example.postandreplyservice.controller;

import com.example.postandreplyservice.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PostController {
    private PostService postService;

    @Autowired
    public void setPostService(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/posts")
    public String getAllPosts() {
        return postService.getAllPosts().toString();
    }
}
