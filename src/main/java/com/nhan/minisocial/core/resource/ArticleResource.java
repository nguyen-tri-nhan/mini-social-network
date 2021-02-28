package com.nhan.minisocial.core.resource;

import com.nhan.minisocial.core.entity.User;
import com.nhan.minisocial.core.resource.BaseResource;

public class ArticleResource extends BaseResource {
    private long id;
    private String description;
    private String image;
    private User user;
    private boolean visible;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
