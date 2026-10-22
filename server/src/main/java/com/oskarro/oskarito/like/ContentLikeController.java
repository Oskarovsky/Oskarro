package com.oskarro.oskarito.like;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/likes")
public class ContentLikeController {

    private final ContentLikeService likes;

    public ContentLikeController(ContentLikeService likes) {
        this.likes = likes;
    }

    @GetMapping("/{kind}/{key}")
    LikeView state(@PathVariable String kind, @PathVariable String key) {
        return likes.state(kind, key);
    }

    @PostMapping("/{kind}/{key}")
    LikeView toggle(@PathVariable String kind, @PathVariable String key) {
        return likes.toggle(kind, key);
    }
}
