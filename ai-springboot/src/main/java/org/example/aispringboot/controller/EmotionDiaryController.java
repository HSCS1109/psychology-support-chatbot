package org.example.aispringboot.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.validation.Valid;
import org.example.aispringboot.DTO.command.EmotionDiaryCommandDTO;
import org.example.aispringboot.common.Result;
import org.example.aispringboot.entity.EmotionDiary;
import org.example.aispringboot.service.EmotionDiaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.example.aispringboot.utils.JwtTokenUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 情绪日记Controller
 */
@RestController
@RequestMapping("/api/emotion-diary")
public class EmotionDiaryController {
    @Autowired
    private EmotionDiaryService emotionDiaryService;

    // 提交情绪日记
    @PostMapping
    public Result<EmotionDiary> save(@Valid @RequestBody EmotionDiaryCommandDTO dto) {
        // 从JWT解析当前用户ID
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();

        EmotionDiary diary = emotionDiaryService.saveDiary(userId, dto);
        return Result.ok(diary);
    }

    // 管理端分页查询
    @GetMapping("/admin/page")
    public Result<Page<EmotionDiary>> adminPage(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "moodScreRange", required = false) String moodScreRange) {
        Page<EmotionDiary> page = emotionDiaryService.adminPage(current, size, userId, moodScreRange);
        return Result.ok(page);
    }

    // 管理端删除
    @DeleteMapping("/admin/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        emotionDiaryService.deleteById(id);
        return Result.ok();
    }
}
