package com.example.postandreplyservice.dao;

import com.example.postandreplyservice.domain.Post;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PostRepository extends MongoRepository<Post, String> {
    List<Post> findByUserId(Long userId);
}
