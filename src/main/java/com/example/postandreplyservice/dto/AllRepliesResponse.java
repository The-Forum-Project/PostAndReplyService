package com.example.postandreplyservice.dto;

import com.example.postandreplyservice.domain.PostReply;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
@Setter
@Getter
@Builder
public class AllRepliesResponse {
    List<PostReply> postReplyList;

}
