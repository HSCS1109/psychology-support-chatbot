package org.example.aispringboot.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.example.aispringboot.common.Result;
import org.example.aispringboot.entity.SysFileInfo;
import org.example.aispringboot.service.FileService;
import org.example.aispringboot.utils.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 文件上传Controller
 */
@RestController
@RequestMapping("/api/file")
public class FileController {
    @Autowired
    private FileService fileService;

    // 上传文件（文章封面）
    @PostMapping("/upload")
    public Result<SysFileInfo> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("businessType") String businessType,
            @RequestParam(value = "businessId", required = false) String businessId,
            @RequestParam(value = "businessField", required = false) String businessField) throws IOException {
        Long userId = getCurrentUserId();
        SysFileInfo info = fileService.upload(file, businessType, businessId, businessField, userId);
        return Result.ok(info);
    }

    // 从token解析当前用户ID
    private Long getCurrentUserId() {
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT decodedJWT = JwtTokenUtil.verifyToken(token);
        return decodedJWT.getClaim("userId").asLong();
    }
}
