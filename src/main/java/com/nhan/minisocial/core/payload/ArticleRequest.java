package com.nhan.minisocial.core.payload;


import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.Size;

public class ArticleRequest {

    private String description;

    private MultipartFile[] fileDatas;

    private boolean visible = true;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MultipartFile[] getFileDatas() {
        return fileDatas;
    }

    public void setFileDatas(MultipartFile[] fileDatas) {
        this.fileDatas = fileDatas;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
