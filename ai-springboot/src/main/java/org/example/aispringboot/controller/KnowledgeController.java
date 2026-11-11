package org.example.aispringboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.validation.Valid;
import org.example.aispringboot.DTO.command.ArticleStatusCommandDTO;
import org.example.aispringboot.DTO.command.KnowledgeArticleCommandDTO;
import org.example.aispringboot.DTO.response.KnowledgeArticlePageDTO;
import org.example.aispringboot.common.Result;
import org.example.aispringboot.entity.KnowledgeArticle;
import org.example.aispringboot.entity.KnowledgeCategory;
import org.example.aispringboot.service.KnowledgeService;
import org.example.aispringboot.utils.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 知识库管理Controller，提供分类与文章接口
 */
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {
    @Autowired
    private KnowledgeService knowledgeService;

    // 分类树（启用的分类列表）
    @GetMapping("/category/tree")
    public Result<List<KnowledgeCategory>> categoryTree() {
        return Result.ok(knowledgeService.categoryTree());
    }

    // 文章分页。frontend=true时前台只看已发布
    @GetMapping("/article/page")
    public Result<Page<KnowledgeArticlePageDTO>> articlePage(
            @RequestParam(value = "currentPage", defaultValue = "1") Integer currentPage,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "frontend", required = false, defaultValue = "false") Boolean frontend) {
        Page<KnowledgeArticlePageDTO> page = knowledgeService.articlePage(
                currentPage, size, title, categoryId, status, frontend);
        return Result.ok(page);
    }

    // 文章详情
    @GetMapping("/article/{id}")
    public Result<KnowledgeArticle> articleDetail(@PathVariable("id") String id) {
        return Result.ok(knowledgeService.getArticleDetail(id));
    }

    // 新建文章
    @PostMapping("/article")
    public Result<KnowledgeArticle> createArticle(@Valid @RequestBody KnowledgeArticleCommandDTO dto) {
        Long currentUserId = getCurrentUserId();
        KnowledgeArticle article = knowledgeService.createArticle(currentUserId, dto);
        return Result.ok(article);
    }

    // 更新文章
    @PutMapping("/article/{id}")
    public Result<KnowledgeArticle> updateArticle(@PathVariable("id") String id,
                                                  @Valid @RequestBody KnowledgeArticleCommandDTO dto) {
        KnowledgeArticle article = knowledgeService.updateArticle(id, dto);
        return Result.ok(article);
    }

    // 修改文章状态（发布/下线）
    @PutMapping("/article/{id}/status")
    public Result<Boolean> changeStatus(@PathVariable("id") String id,
                                        @Valid @RequestBody ArticleStatusCommandDTO dto) {
        return Result.ok(knowledgeService.changeStatus(id, dto.getStatus()));
    }

    // 删除文章
    @DeleteMapping("/article/{id}")
    public Result<Boolean> deleteArticle(@PathVariable("id") String id) {
        return Result.ok(knowledgeService.deleteArticle(id));
    }

    // 从token中解析当前登录用户ID
    private Long getCurrentUserId() {
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT decodedJWT = JwtTokenUtil.verifyToken(token);
        return decodedJWT.getClaim("userId").asLong();
    }
}
