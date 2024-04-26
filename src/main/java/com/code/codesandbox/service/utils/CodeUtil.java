package com.code.codesandbox.service.utils;

import com.github.dockerjava.api.model.Image;

import java.util.List;

/**
 * Created by Lxy on 2024/4/26 10:35
 */
public class CodeUtil {

    /**
     * get image from images by image name
     * @param images  get images by docker api
     * @param imageName user input image name
     * @return Docker's Image
     */
    public static Image findImage(List<Image> images, String imageName) {
        return images
                .stream()
                .filter(image -> imageName.equals(image.getRepoTags()[0]))
                .findFirst()
                .orElse(null);
    }
}
