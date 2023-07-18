package com.example.postandreplyservice.domain;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class PostReply {
    private Long userId;
    private String comment;
    private Boolean isActive;
    private Date dateCreated;
    private List<SubReply> subReplies;
}
