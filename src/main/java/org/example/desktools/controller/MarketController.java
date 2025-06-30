package org.example.desktools.controller;

import org.example.desktools.entity.Plugin;
import org.example.desktools.repository.MarketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class MarketController {

    private static final Logger logger = LoggerFactory.getLogger(MarketController.class);
    private static final String UPLOAD_DIR = "uploads";

    @Autowired
    private MarketRepository marketRepository;

    @PostMapping("/upload-plugin")
    public ResponseEntity<Map<String, Object>> uploadPlugin(@RequestBody Map<String, Object> pluginData) {
        try {
            String name = (String) pluginData.get("name");
            String jsonData = pluginData.toString();

            Plugin plugin = new Plugin();
            plugin.setName(name);
            plugin.setJsonData(jsonData);
            marketRepository.save(plugin);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "插件信息上传成功");
            response.put("plugin", plugin);
            logger.info("插件信息已保存: {}", name);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("上传插件信息失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "插件信息上传失败");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/upload-zip")
    public ResponseEntity<Map<String, Object>> uploadZip(@RequestParam("zip") MultipartFile zipFile) {
        try {
            // 获取上传目录的绝对路径
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                boolean created = uploadDir.mkdirs();
                logger.info("创建上传目录: {}, 结果: {}", uploadDir.getAbsolutePath(), created);
            } else {
                logger.info("使用现有上传目录: {}", uploadDir.getAbsolutePath());
            }

            // 保存文件
            String fileName = zipFile.getOriginalFilename();
            Path filePath = Paths.get(UPLOAD_DIR, fileName);
            Files.write(filePath, zipFile.getBytes());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "插件压缩包上传成功");
            response.put("fileName", fileName);
            response.put("filePath", filePath.toAbsolutePath().toString()); // 返回绝对路径
            logger.info("插件压缩包已保存: {}", filePath.toAbsolutePath());
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IOException e) {
            logger.error("上传插件压缩包失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "插件压缩包上传失败");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/install-plugin")
    public ResponseEntity<byte[]> installPlugin(@RequestBody Map<String, Object> requestData) {
        try {
            String name = (String) requestData.get("name");
            logger.info("收到安装插件请求: {}", name);

            // 根据名称查找插件信息
            Plugin plugin = marketRepository.findByName(name);
            if (plugin == null) {
                logger.warn("未找到插件: {}", name);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            // 检查文件是否存在
            String fileName = name + ".zip";
            Path filePath = Paths.get(UPLOAD_DIR, fileName);
            if (!Files.exists(filePath)) {
                logger.warn("插件文件不存在: {}", filePath.toAbsolutePath());
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            // 返回文件
            byte[] fileBytes = Files.readAllBytes(filePath);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", fileName);

            logger.info("成功提供插件文件: {}", filePath.toAbsolutePath());
            return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("处理安装插件请求失败", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping("/download-zip")
    public ResponseEntity<byte[]> downloadZip(@RequestParam("name") String name) {
        try {
            logger.info("收到下载插件请求: {}", name);
            String fileName = name + ".zip";
            Path filePath = Paths.get(UPLOAD_DIR, fileName);

            if (!Files.exists(filePath)) {
                logger.warn("下载失败: 文件不存在 - {}", filePath.toAbsolutePath());
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            byte[] fileBytes = Files.readAllBytes(filePath);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", fileName);

            logger.info("成功下载插件: {}", filePath.toAbsolutePath());
            return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            logger.error("下载插件失败", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
