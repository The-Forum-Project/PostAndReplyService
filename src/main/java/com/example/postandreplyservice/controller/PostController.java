package com.example.postandreplyservice.controller;

import com.example.postandreplyservice.domain.Post;
import com.example.postandreplyservice.domain.PostReply;
import com.example.postandreplyservice.dto.*;
import com.example.postandreplyservice.exception.InvalidAuthorityException;
import com.example.postandreplyservice.exception.PostNotFoundException;
import com.example.postandreplyservice.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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

    //user for microservice
    @PostMapping(value = "/posts")
    public ResponseEntity<GeneralResponse> createPost(@RequestBody Post post){
        postService.savePost(post);
        return ResponseEntity.ok(GeneralResponse.builder().message("Post created successfully").statusCode("200").build());
    }


    //in normal user home page and admin home page will use this endpoint.
    @GetMapping("/posts")
    public ResponseEntity<AllPostsResponse> getAllPosts() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        List<GrantedAuthority> authorities = (List<GrantedAuthority>) authentication.getAuthorities();
        //normal user can all published posts and his own posts
        //admin user can see all posts except all the hidden and unpublished posts
        List<Post> posts = postService.getAllPosts(authorities);
        return ResponseEntity.ok(AllPostsResponse.builder().posts(posts).build());
    }

    //user for microservice
    @GetMapping("/post/{postId}")
    public ResponseEntity<PostResonse> getPostById(@PathVariable String postId) throws PostNotFoundException {
        Post post = postService.getPostById(postId);
        return ResponseEntity.ok(PostResonse.builder().post(post).build());
    }

    //use this endpoint in user profile page
    @GetMapping("/posts/{userId}")
    public ResponseEntity<AllPostsResponse> getUserPosts(@PathVariable Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        List<GrantedAuthority> authorities = (List<GrantedAuthority>) authentication.getAuthorities();
        List<Post> posts = postService.getAllPostsByUserId(userId, authorities);
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

    //reply to a post, only normal user or admin can reply
    @PatchMapping("/{postId}/replies")
    public ResponseEntity<GeneralResponse> replyToPost(@PathVariable String postId, @RequestBody ReplyRequest replyRequest) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) authentication.getPrincipal();
        List<GrantedAuthority> authorities = (List<GrantedAuthority>) authentication.getAuthorities();
        if(authorities.stream().anyMatch(authority -> authority.getAuthority().equals("normal"))){
            postService.replyToPost(postId, replyRequest, userId);
            return ResponseEntity.ok(GeneralResponse.builder().statusCode("200").message("Replied a post").build());
        }else{
            throw new InvalidAuthorityException();
        }
    }

    //reply to a reply, only normal user or admin can reply
    @PatchMapping("/{postId}/replies/{idx}/subreplies")
    public ResponseEntity<GeneralResponse> replyToReply(@PathVariable String postId, @PathVariable int idx, @RequestBody ReplyRequest subReply) throws Exception {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) authentication.getPrincipal();
        List<GrantedAuthority> authorities = (List<GrantedAuthority>) authentication.getAuthorities();
        if(authorities.stream().anyMatch(authority -> authority.getAuthority().equals("normal"))){
            postService.replyToReply(postId, idx, subReply,userId);
            return ResponseEntity.ok(GeneralResponse.builder().statusCode("200").message("Replied a reply").build());
        }else{
            throw new InvalidAuthorityException();
        }
    }

    //do we need this controller? We could get the all replies by post. Post reply is a list in post.
    @GetMapping("/{postId}")
    public ResponseEntity<AllRepliesResponse> getAllReplies(@PathVariable String postId) throws PostNotFoundException {
        List<PostReply> postReplies = postService.getAllReplies(postId);
        return ResponseEntity.ok(AllRepliesResponse.builder().postReplyList(postReplies).build());
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

    //top 3 posts
    @GetMapping("/posts/{id}/top")
    public ResponseEntity<AllPostsResponse> getUserPostsTop3RepliesCount(@PathVariable Long id) {
        List<Post> posts = postService.getTop3RepliesPost(id);
        return ResponseEntity.ok(AllPostsResponse.builder().posts(posts).build());
    }

    //list all drafts
    @GetMapping("/posts/{id}/drafts")
    public ResponseEntity<AllPostsResponse> getUserDrafts(@PathVariable Long id) {
        List<Post> posts = postService.getAllDraftsByUserId(id);
        return ResponseEntity.ok(AllPostsResponse.builder().posts(posts).build());
    }

    @GetMapping("posts/deleted")
    public ResponseEntity<AllPostsResponse> getAllDeletedPosts() throws InvalidAuthorityException {
        List<Post> posts = postService.getAllDeletedPosts();
        return ResponseEntity.ok(AllPostsResponse.builder().posts(posts).build());
    }

    @GetMapping("posts/banned")
    public ResponseEntity<AllPostsResponse> getAllBannedPosts() throws InvalidAuthorityException {
        List<Post> posts = postService.getAllBannedPosts();
        return ResponseEntity.ok(AllPostsResponse.builder().posts(posts).build());
    }
}
