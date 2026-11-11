package org.example.aispringboot.service;

import org.example.aispringboot.entity.SysFileInfo;
import org.example.aispringboot.mapper.SysFileInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 文件上传Service
 */
@Service
public class FileService {
    @Value("${file.upload-dir}")
    private String uploadDir;

    @Autowired
    private SysFileInfoMapper fileInfoMapper;

    // 保存上传文件并写入文件记录
    public SysFileInfo upload(MultipartFile file, String businessType, String businessId,
                              String businessField, Long userId) throws IOException {
        // 1. 生成文件名（时间戳+原扩展名）
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf("."));
        }
        String fileName = System.currentTimeMillis() + ext;

        // 数据库与URL使用的相对路径（沿用bussiness目录命名）
        String relativePath = "/files/bussiness/article/" + fileName;

        // 2. 准备本地磁盘目录并保存文件
        File dir = new File(uploadDir + "/bussiness/article");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("创建文件存储目录失败：" + dir.getAbsolutePath());
        }
        File dest = new File(dir, fileName);
        file.transferTo(dest);

        // 3. 写入文件信息记录
        SysFileInfo info = SysFileInfo.builder()
                .originalName(original)
                .filePath(relativePath)
                .fileSize(file.getSize())
                .fileType("IMG")
                .businessType(businessType)
                .businessId(businessId)
                .businessField(businessField)
                .uploadUserId(userId)
                .isTemp(0)
                .status(1)
                .createTime(LocalDateTime.now())
                .build();
        fileInfoMapper.insert(info);
        return info;
    }
}
