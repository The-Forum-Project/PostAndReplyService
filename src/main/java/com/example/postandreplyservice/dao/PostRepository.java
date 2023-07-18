package com.example.postandreplyservice.dao;

import com.example.postandreplyservice.domain.Post;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PostRepository extends MongoRepository<Post, String> {
    // Custom query methods, if needed
}
