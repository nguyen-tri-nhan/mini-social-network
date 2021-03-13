package com.nhan.minisocial.core.service;

import com.nhan.minisocial.core.entity.Comment;
import com.nhan.minisocial.core.repository.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    public Comment save(Comment comment) {
        return commentRepository.save(comment);
    }

    public List<Comment> getComments(long articleId){
        return commentRepository.findCommentsByArticle(articleId);
    }
}
