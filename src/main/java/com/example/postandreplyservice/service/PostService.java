package com.example.postandreplyservice.service;

import com.example.postandreplyservice.dao.PostRepository;
import com.example.postandreplyservice.domain.Post;
import com.example.postandreplyservice.domain.PostReply;
import com.example.postandreplyservice.domain.SubReply;
import com.example.postandreplyservice.dto.PostUpdateRequest;
import com.example.postandreplyservice.dto.ReplyRequest;
import com.example.postandreplyservice.dto.UpdatePostRequest;
import com.example.postandreplyservice.exception.InvalidAuthorityException;
import com.example.postandreplyservice.exception.PostNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

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
    public List<Post> getAllPosts(List<GrantedAuthority> authorities) {
        List<Post> posts = postRepository.findAll();
        List<Post> res = new ArrayList<>();
        //normal user can all published posts
        if(authorities.stream().noneMatch(authority -> authority.getAuthority().equals("admin"))){
            for(Post post: posts){
                if(post.getStatus().equals("published")){
                    res.add(post);
                }
            }
        }else{//admin user can see all posts except all the hidden and unpublished posts
            for(Post post: posts){
                if(!post.getStatus().equals("hidden") && !post.getStatus().equals("unpublished")){
                    res.add(post);
                }
            }
        }
//        Collections.sort(res, new Comparator<Post>() {
//            @Override
//            public int compare(Post o1, Post o2) {
//                return o2.getDateCreated().compareTo(o1.getDateCreated());
//            }
//        });
        return res;
    }

    public List<Post> getAllPostsByUserId(Long userId) {
        return postRepository.findByUserId(userId);
    }

    public void updatePost(String postId, PostUpdateRequest request, Long userId, List<GrantedAuthority> authorities) throws PostNotFoundException, InvalidAuthorityException {
        Optional<Post> optionalPostpost = postRepository.findById(postId);
        String newStatus = request.getStatus();
        if(optionalPostpost.isPresent()){
            Post post = optionalPostpost.get();
            String oldStatus = post.getStatus();
            Long userIdOfPost = post.getUserId();
//            if (oldStatus.equals("Published") && newStatus.equals("Hidden") && userId.equals(userIdOfPost)) {
//
//            } else if (oldStatus.equals("Hidden") && newStatus.equals("Published") && userId.equals(userIdOfPost)) {
//
//            } else if (oldStatus.equals("Unpublished") && newStatus.equals("Published") && userId.equals(userIdOfPost)) {
//
//            } else if (oldStatus.equals("Published") && newStatus.equals("Banned") && (authorities.stream().anyMatch(authority -> authority.getAuthority().equals("admin")))) {
//
//            } else if (oldStatus.equals("Deleted") && newStatus.equals("Published") && (authorities.stream().anyMatch(authority -> authority.getAuthority().equals("admin")))) {
//
//            } else if (oldStatus.equals("Banned") && newStatus.equals("Published") && (authorities.stream().anyMatch(authority -> authority.getAuthority().equals("admin")))) {
//
//            } else {
//                throw new InvalidAuthorityException();
//            }
            Predicate<Long> adminCheck = id -> authorities.stream().anyMatch(authority -> authority.getAuthority().equals("admin"));
            Map<String, Predicate<Long>> allowedStatusChanges = new HashMap<>();
            allowedStatusChanges.put("Published->Hidden", userIdOfPost::equals);
            allowedStatusChanges.put("Hidden->Published", userIdOfPost::equals);
            allowedStatusChanges.put("Unpublished->Published", userIdOfPost::equals);
            allowedStatusChanges.put("Published->Banned", adminCheck);
            allowedStatusChanges.put("Deleted->Published", adminCheck);
            allowedStatusChanges.put("Banned->Published", adminCheck);

            Predicate<Long> statusChangeCheck = allowedStatusChanges.get(oldStatus + "->" + newStatus);
            if (statusChangeCheck != null && statusChangeCheck.test(userId)) {
                // Logic to change the status
                post.setStatus(newStatus);
                post.setIsArchived(request.getIsArchived());
                postRepository.save(post);
            } else {
                throw new InvalidAuthorityException();
            }

        }else{
            throw new PostNotFoundException();
        }
    }

    public void modifyPost(String postId, UpdatePostRequest updatePostRequest, Long userId) throws PostNotFoundException, InvalidAuthorityException {

        Optional<Post> postOptional = postRepository.findById(postId);

        if (postOptional.isPresent()) {
            Post post = postOptional.get();
            if(post.getUserId() != userId){
                throw new InvalidAuthorityException();
            }
            if (updatePostRequest.getTitle() != null) {
                post.setTitle(updatePostRequest.getTitle());
            }
            if (updatePostRequest.getContent() != null) {
                post.setContent(updatePostRequest.getContent());
            }
            if (updatePostRequest.getImages() != null) {
                post.setImages(updatePostRequest.getImages());
            }
            if (updatePostRequest.getAttachments() != null) {
                post.setAttachments(updatePostRequest.getAttachments());
            }

            post.setDateModified(new Date());
            postRepository.save(post);
        } else {
            throw new PostNotFoundException();
        }

    }

    public void replyToPost(String postId, ReplyRequest postReply, Long userId) throws PostNotFoundException {

        Optional<Post> postOptional = postRepository.findById(postId);

        if (postOptional.isPresent()) {
            Post post = postOptional.get();
            PostReply newReply = new PostReply();

            newReply.setUserId(userId);
            newReply.setComment(postReply.getComment());
            newReply.setIsActive(true);
            newReply.setDateCreated(new Date());
            newReply.setSubReplies(new ArrayList<>());

            post.getPostReplies().add(newReply);
            postRepository.save(post);
        } else {
            throw new PostNotFoundException();
        }

    }

    public void replyToReply(String postId, int idx, ReplyRequest subReply, Long userId) throws PostNotFoundException {

        Optional<Post> postOptional = postRepository.findById(postId);

        if (postOptional.isPresent()) {
            Post post = postOptional.get();
            PostReply reply = post.getPostReplies().get(idx);
            SubReply newReply = new SubReply();

            newReply.setUserId(userId);
            newReply.setComment(subReply.getComment());
            newReply.setIsActive(true);
            newReply.setDateCreated(new Date());

            reply.getSubReplies().add(newReply);
            postRepository.save(post);
        } else {
            throw new PostNotFoundException();
        }
    }


    public void hidePost(String postId) throws PostNotFoundException {
        Optional<Post> optionalPost = postRepository.findById(postId);
        if(optionalPost.isPresent()){
            Post post = optionalPost.get();
            post.setStatus("hidden");
            postRepository.save(post);
        }else{
            throw new PostNotFoundException();
        }
    }

    public void publishPost(String postId) throws PostNotFoundException {
        Optional<Post> optionalPost = postRepository.findById(postId);
        if(optionalPost.isPresent()){
            Post post = optionalPost.get();
            post.setStatus("published");
            postRepository.save(post);
        }else{
            throw new PostNotFoundException();
        }
    }

    public void banPost(String postId) throws PostNotFoundException {
        Optional<Post> optionalPost = postRepository.findById(postId);
        if(optionalPost.isPresent()){
            Post post = optionalPost.get();
            post.setStatus("ban");
            postRepository.save(post);
        }else{
            throw new PostNotFoundException();
        }
    }

    public List<PostReply> getAllReplies(String postId) throws PostNotFoundException {
        Optional<Post> optionalPost = postRepository.findById(postId);
        if(optionalPost.isPresent()){
            Post post = optionalPost.get();
            return post.getPostReplies();
        }else{
            throw new PostNotFoundException();
        }

    }


    // Other methods for querying, updating, and deleting posts
}