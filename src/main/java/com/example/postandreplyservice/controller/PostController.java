package com.example.postandreplyservice.controller;

import com.example.postandreplyservice.domain.Post;
import com.example.postandreplyservice.dto.AllPostsResponse;
import com.example.postandreplyservice.dto.GeneralResponse;
import com.example.postandreplyservice.dto.PostResonse;
import com.example.postandreplyservice.dto.PostUpdateRequest;
import com.example.postandreplyservice.exception.InvalidAuthorityException;
import com.example.postandreplyservice.exception.PostNotFoundException;
import com.example.postandreplyservice.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class PostController {
    private PostService postService;

    @Autowired
    public void setPostService(PostService postService) {
        this.postService = postService;
    }

    @PostMapping("/posts")
    public ResponseEntity<GeneralResponse> createPost(@RequestBody Post post) {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        Long userId = (Long) authentication.getPrincipal();
        postService.savePost(post);
        return ResponseEntity.ok(GeneralResponse.builder().statusCode("200").message("Post created.").build());
    }
    //in normal user home page and admin home page will use this endpoint.
    @GetMapping("/posts")
    public ResponseEntity<AllPostsResponse> getAllPosts() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) authentication.getPrincipal();
        List<GrantedAuthority> authorities = (List<GrantedAuthority>) authentication.getAuthorities();
        //normal user can all published posts and his own posts
        //admin user can see all posts except all the hidden and unpublished posts
        List<Post> posts = postService.getAllPosts(authorities);
        return ResponseEntity.ok(AllPostsResponse.builder().posts(posts).build());
    }
    @GetMapping("/post/{postId}")
    public ResponseEntity<PostResonse> getPostById(@PathVariable String postId) throws PostNotFoundException, InvalidAuthorityException {
        //need to check this user has the authority to see this post
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) authentication.getPrincipal();
        Post post = postService.getPostById(postId);
        if(post == null){
            throw new PostNotFoundException();
        }else{
            if(post.getUserId() != userId){
                throw new InvalidAuthorityException();
            }
            return ResponseEntity.ok(PostResonse.builder().post(post).build());
        }
    }
    //use this endpoint in user profile page
    @GetMapping("/posts/{userId}")
    public ResponseEntity<AllPostsResponse> getUserPosts(@PathVariable Long userId) {
        List<Post> posts = postService.getAllPostsByUserId(userId);
        return ResponseEntity.ok(AllPostsResponse.builder().posts(posts).build());
    }

    @PatchMapping("/posts/{postId}")
    public ResponseEntity<GeneralResponse> updatePost(@PathVariable String postId, @RequestBody PostUpdateRequest request) throws PostNotFoundException, InvalidAuthorityException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) authentication.getPrincipal();
        List<GrantedAuthority> authorities = (List<GrantedAuthority>) authentication.getAuthorities();
        postService.updatePost(postId, request, userId, authorities);
        return ResponseEntity.ok(GeneralResponse.builder().statusCode("200").message("Status updated").build());
    }

//    @PatchMapping("/posts/{postId}/hide")
//    //check owner
//    public ResponseEntity<GeneralResponse> hidePost(@PathVariable String postId) throws PostNotFoundException {
//        postService.hidePost(postId);
//        return ResponseEntity.ok(GeneralResponse.builder().statusCode("200").message("Post hidden.").build());
//    }
//
//    @PatchMapping("/posts/{postId}/publish")
//    @PreAuthorize("hasAuthority('normal') && hasAuthority('admin')")
//    public ResponseEntity<GeneralResponse> publishPost(@PathVariable String postId) throws PostNotFoundException {
//        postService.publishPost(postId);
//        return ResponseEntity.ok(GeneralResponse.builder().statusCode("200").message("Post published.").build());
//    }
//
//    @PatchMapping("/posts/{postId}/ban")
//    @PreAuthorize("hasAuthority('admin')")
//    public ResponseEntity<GeneralResponse> banPost(@PathVariable String postId) throws PostNotFoundException {
//        postService.banPost(postId);
//        return ResponseEntity.ok(GeneralResponse.builder().statusCode("200").message("Post hidden.").build());
//    }
//
//    @PatchMapping("/posts/{postId}/delete")
//    @PreAuthorize("hasAuthority('admin')")
//    public ResponseEntity<GeneralResponse> deletePost(@PathVariable String postId) throws PostNotFoundException {
//        postService.banPost(postId);
//        return ResponseEntity.ok(GeneralResponse.builder().statusCode("200").message("Post hidden.").build());
//    }
}
