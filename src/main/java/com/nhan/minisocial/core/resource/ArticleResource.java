package com.nhan.minisocial.core.resource;

import com.nhan.minisocial.core.entity.User;
import com.nhan.minisocial.core.resource.BaseResource;

public class ArticleResource extends BaseResource {
    private long id;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    private String description;
    private String image;

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

}
