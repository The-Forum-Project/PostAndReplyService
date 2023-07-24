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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
//
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

    public List<Post> getAllPostsByUserId(Long userId, List<GrantedAuthority> authorities) {
        List<Post> posts = postRepository.findByUserId(userId);
        if(authorities.stream().noneMatch(authority -> authority.getAuthority().equals("admin"))) {
            //user could only see un-deleted post
            List<Post> res = new ArrayList<>();
            for (Post post : posts) {
                if (!post.getStatus().equals("deleted")
                        && !post.getStatus().equals("unpublished")) {
                    res.add(post);
                }
            }
            return res;
        }else{
            //admin could see all
            return posts;
        }
    }

    public List<Post> getTop3RepliesPost(Long userId){
        List<Post> all = postRepository.findByUserIdAndStatus(userId, "published");
        System.out.println(all);
        List<Post> sortedPosts = all.stream()
                .sorted(Comparator.comparingInt(post -> {
                    List<PostReply> postReplies = post.getPostReplies();
                    return postReplies != null ? postReplies.size() : 0;
                }))
                .collect(Collectors.toList());
        Collections.reverse(sortedPosts);
        return sortedPosts.subList(0, Math.min(3, sortedPosts.size()));
    }

    public List<Post> getAllDraftsByUserId(Long userId) {
        return postRepository.findByUserIdAndStatus(userId, "unpublished");
    }

    public List<Post> getAllDeletedPosts() throws InvalidAuthorityException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        List<GrantedAuthority> authorities = (List<GrantedAuthority>) authentication.getAuthorities();
        if (authorities.stream().noneMatch(authority -> authority.getAuthority().equals("admin"))) {
            throw new InvalidAuthorityException();
        }
        return postRepository.findByStatus("deleted");
    }

    public void updatePost(String postId, PostUpdateRequest request, Long userId, List<GrantedAuthority> authorities) throws PostNotFoundException, InvalidAuthorityException {
        Optional<Post> optionalPostpost = postRepository.findById(postId);
        String newStatus = request.getStatus();

        if(optionalPostpost.isPresent()){
            Post post = optionalPostpost.get();
            //check whether need to  update the status
            if(newStatus != null){
                String oldStatus = post.getStatus();
                Long userIdOfPost = post.getUserId();
                Predicate<Long> adminCheck = id -> authorities.stream().anyMatch(authority -> authority.getAuthority().equals("admin"));
                Map<String, Predicate<Long>> allowedStatusChanges = new HashMap<>();
                allowedStatusChanges.put("published->hidden", userIdOfPost::equals);
                allowedStatusChanges.put("hidden->published", userIdOfPost::equals);
                allowedStatusChanges.put("unpublished->published", userIdOfPost::equals);

                //publish to delete
                allowedStatusChanges.put("published->deleted", userIdOfPost::equals);
                //banned to delete
                allowedStatusChanges.put("banned->deleted", userIdOfPost::equals);

                allowedStatusChanges.put("published->banned", adminCheck);
                allowedStatusChanges.put("deleted->published", adminCheck);
                allowedStatusChanges.put("banned->published", adminCheck);

                Predicate<Long> statusChangeCheck = allowedStatusChanges.get(oldStatus + "->" + newStatus);
                if (statusChangeCheck != null && statusChangeCheck.test(userId)) {
                    post.setStatus(newStatus);
                } else {
                    throw new InvalidAuthorityException();
                }
            }
            //check whether need to update isArchived only owner can change arcghived
            if(request.getIsArchived() != null){
                if(userId == post.getUserId()){
                    post.setIsArchived(request.getIsArchived());
                }else {
                    throw new InvalidAuthorityException();
                }
            }
            postRepository.save(post);
        }else{
            throw new PostNotFoundException();
        }
    }
    public void replyToPost(String postId, ReplyRequest postReply, Long userId) throws PostNotFoundException, InvalidAuthorityException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        List<GrantedAuthority> authorities = (List<GrantedAuthority>) authentication.getAuthorities();
        if (authorities.stream().noneMatch(authority -> authority.getAuthority().equals("normal"))) {
            throw new InvalidAuthorityException();
        }
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

    public void replyToReply(String postId, int idx, ReplyRequest subReply, Long userId) throws PostNotFoundException, InvalidAuthorityException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        List<GrantedAuthority> authorities = (List<GrantedAuthority>) authentication.getAuthorities();
        if (authorities.stream().noneMatch(authority -> authority.getAuthority().equals("normal"))) {
            throw new InvalidAuthorityException();
        }
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

//    public void modifyPost(String postId, String title, String content, List<String> attachmentUrls, List<String> iamgeUrls, Long userId) throws InvalidAuthorityException, PostNotFoundException {
//
//        Optional<Post> postOptional = postRepository.findById(postId);
//
//        if (postOptional.isPresent()) {
//            Post post = postOptional.get();
//            if(post.getUserId() != userId){
//                throw new InvalidAuthorityException();
//            }
//            if (title != null) {
//                post.setTitle(title);
//            }
//            if (content != null) {
//                post.setContent(content);
//            }
//            if (iamgeUrls != null) {
//
//                post.setImages(iamgeUrls);
//            }
//            if (attachmentUrls != null) {
//                post.setAttachments(attachmentUrls);
//            }
//
//            post.setDateModified(new Date());
//            postRepository.save(post);
//        } else {
//            throw new PostNotFoundException();
//        }
//
//    }


    // Other methods for querying, updating, and deleting posts
}