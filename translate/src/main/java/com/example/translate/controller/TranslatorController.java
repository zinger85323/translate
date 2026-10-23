package com.example.translate.controller;

import com.example.translate.service.PhotoTranslatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // 允許所有前端來源呼叫（正式上線可指定網域）
public class TranslatorController {
    @Autowired
    private PhotoTranslatorService translatorService;

    @PostMapping("/translate-image")
    public ResponseEntity<?> translateImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "lang", defaultValue = "en") String targetLang) {
        try {
            // 1. 將手機上傳的圖片暫存到本地或直接轉成 Stream
            File tempFile = File.createTempFile("upload_", file.getOriginalFilename());
            file.transferTo(tempFile);

            // 2. 呼叫你寫好的 Vision 服務
            List<PhotoTranslatorService.RecognizedTextRegion> results =
                    translatorService.processImageTranslation(tempFile.getAbsolutePath(), targetLang);

            // 3. 刪除暫存檔
            tempFile.delete();

            return ResponseEntity.ok(results);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("圖片處理失敗: " + e.getMessage());
        }
    }
}
