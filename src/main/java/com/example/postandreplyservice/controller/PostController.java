package com.example.postandreplyservice.controller;

import com.example.postandreplyservice.domain.Post;
import com.example.postandreplyservice.domain.PostReply;
import com.example.postandreplyservice.dto.*;
import com.example.postandreplyservice.exception.InvalidAuthorityException;
import com.example.postandreplyservice.exception.PostNotFoundException;
import com.example.postandreplyservice.service.PostService;
import com.example.postandreplyservice.service.remote.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RestController
public class PostController {
    private PostService postService;
    private FileService fileService;
    @Autowired
    public void setPostService(PostService postService) {
        this.postService = postService;
    }

    @Autowired
    public void setFileService(FileService fileService) {
        this.fileService = fileService;
    }
    @PostMapping(value = "/posts")
    public ResponseEntity<GeneralResponse> createPost(
            @RequestParam(value = "title") String title,
            @RequestParam(value = "content") String content,
            @RequestParam(value = "images") MultipartFile[] images,
            @RequestParam(value = "attachments") MultipartFile[] attachments) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) authentication.getPrincipal();

        Post post = Post.builder()
                .userId(userId)
                .title(title)
                .content(content)
                .isArchived(false)

                .status("published")
                .dateCreated(new Date())
                .dateModified(new Date())
                .postReplies(new ArrayList<>())
                .build();

        //upload any images to S3
        //how to change it to correct way?
        System.out.println("Build post: " + post);
        //MultipartFile[] images = req.getFiles("images").toArray(new MultipartFile[0]);
        ResponseEntity<FileUrlResponse> response = fileService.uploadFiles(images);
        System.out.println("image urls: " + response.getBody().getUrls());
        post.setImages(response.getBody().getUrls());

        //upload any attachments to S3
        //MultipartFile[] attachments = req.getFiles("attachments").toArray(new MultipartFile[0]);
        ResponseEntity<FileUrlResponse> attachmentResponse = fileService.uploadFiles(attachments);
        System.out.println("attachment urls: " + attachmentResponse.getBody().getUrls());
        post.setAttachments(attachmentResponse.getBody().getUrls());

        postService.savePost(post);
        return ResponseEntity.ok(GeneralResponse.builder().statusCode("200").message("Post created.").build());
    }

    //in normal user home page and admin home page will use this endpoint.
    @GetMapping("/posts")
    public ResponseEntity<AllPostsResponse> getAllPosts() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        Long userId = (Long) authentication.getPrincipal();
//        System.out.println("userId" + userId);
//        System.out.println("authorities: " + authentication.getAuthorities());

        List<GrantedAuthority> authorities = (List<GrantedAuthority>) authentication.getAuthorities();
        //normal user can all published posts and his own posts
        //admin user can see all posts except all the hidden and unpublished posts
        List<Post> posts = postService.getAllPosts(authorities);
        return ResponseEntity.ok(AllPostsResponse.builder().posts(posts).build());
    }

    //TODO: do we need check authority here? Everybody can see the post if this post not hidden, unpublished or deleted.
    //true does not need to check whether this user is the owner of the post
    @GetMapping("/post/{postId}")
    public ResponseEntity<PostResonse> getPostById(@PathVariable String postId) throws PostNotFoundException, InvalidAuthorityException {
        //need to check this user has the authority to see this post
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) authentication.getPrincipal();
        Post post = postService.getPostById(postId);
        return ResponseEntity.ok(PostResonse.builder().post(post).build());
//        if(post == null){
//            throw new PostNotFoundException();
//        }else{
//            if(post.getUserId() != userId){
//                throw new InvalidAuthorityException();
//            }
//        }
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

    //can only be modified by post onwer
    //attachments 如果要修改attachment 增加或删除attachment时
    @PatchMapping("/{postId}")
    public ResponseEntity<GeneralResponse> modifyPost(@PathVariable String postId, @RequestParam(value = "title") String title,
                                                      @RequestParam(value = "content") String content,
                                                      @RequestParam(value = "images") MultipartFile[] images,
                                                      @RequestParam(value = "attachments") MultipartFile[] attachments) throws PostNotFoundException, InvalidAuthorityException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) authentication.getPrincipal();
        List<String> iamgeUrls = null;
        if(images != null && images.length > 0){
            ResponseEntity<FileUrlResponse> imagesresponse  = fileService.uploadFiles(images);
            iamgeUrls = imagesresponse.getBody().getUrls();
        }


        List<String> attachmentUrls = null;
        if(attachments != null && attachments.length > 0){
            ResponseEntity<FileUrlResponse> attachmentResponse = fileService.uploadFiles(attachments);
            attachmentUrls = attachmentResponse.getBody().getUrls();
        }


        postService.modifyPost(postId,title
                , content, attachmentUrls, iamgeUrls, userId);
        return ResponseEntity.ok(GeneralResponse.builder().statusCode("200").message("Post modified").build());
    }

    //reply to a post, only normal user or admin can reply
    @PatchMapping("/{postId}/replies")
    public ResponseEntity<GeneralResponse> replyToPost(@PathVariable String postId, @RequestBody ReplyRequest replyRequest) throws InvalidAuthorityException, PostNotFoundException {
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
    public ResponseEntity<GeneralResponse> replyToReply(@PathVariable String postId, @PathVariable int idx, @RequestBody ReplyRequest subReply) throws InvalidAuthorityException, PostNotFoundException {

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
}
