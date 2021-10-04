package com.nhan.minisocial.core.service;

import com.nhan.minisocial.core.entity.Article;
import com.nhan.minisocial.core.entity.User;
import com.nhan.minisocial.core.payload.ArticleRequest;
import com.nhan.minisocial.core.resource.ArticleResource;
import com.nhan.minisocial.core.resource.UserResource;
import com.nhan.minisocial.core.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ArticleResourceService {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserResourceService userResourceService;

    @Autowired
    private VoteService voteService;

    public ArticleResource createOrUpdate(ArticleRequest articleRequest, UserPrincipal userPrincipal) {
        Article article = toEntity(articleRequest, userPrincipal);
        Article savingArticle = articleService.save(article);
        return toResource(savingArticle);
    }

    public Article toEntity(ArticleRequest articleRequest, UserPrincipal userPrincipal) {
        Article article = new Article();
        User user = userService.getUser(userPrincipal.getId());
        article.setId(articleRequest.getId());
        article.setDescription(articleRequest.getDescription());
        article.setImage(articleRequest.getImage());
        article.setVisible(true);
        article.setUser(user);
        return article;
    }

    public Stream<ArticleResource> getAll() {
        return getAllEntity().map(this::toResource);
    }

    public ArticleResource getArticleResource(long id){
        Article article = articleService.getOne(id);
        return toResource(article);
    }

    private Stream<Article> getAllEntity() {
        return articleService.getAll();
    }

    private ArticleResource toResource(Article article) {
        ArticleResource articleResource = new ArticleResource();
        UserResource userResource = userResourceService.toResource(article.getUser());
        long vote = voteService.countVoteByArticleId(article.getId());
        articleResource.setId(article.getId());
        articleResource.setDescription(article.getDescription());
        articleResource.setImage(article.getImage());
        articleResource.setUser(userResource);
        articleResource.setVotes(vote);
        articleResource.setCreateAt(article.getCreateAt());
        return articleResource;
    }
}
