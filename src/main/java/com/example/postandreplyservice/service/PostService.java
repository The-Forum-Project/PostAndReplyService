package com.example.postandreplyservice.service;

import com.example.postandreplyservice.dao.PostRepository;
import com.example.postandreplyservice.domain.Post;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PostService {
    private final PostRepository postRepository;

    @Autowired
    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    public void savePost(Post post) {
        postRepository.save(post);
    }

    public Post getPostById(String id) {
        return postRepository.findById(id).orElse(null);
    }

    //get all
    public List<Post> getAllPosts() {
        postRepository.save(Post.builder().postId("1").title("title1").content("content1").build());
        return postRepository.findAll();
    }
    // Other methods for querying, updating, and deleting posts
}