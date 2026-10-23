package com.example.translate.service;

import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.Block;
import com.google.cloud.vision.v1.BoundingPoly;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.Page;
import com.google.cloud.vision.v1.Paragraph;
import com.google.cloud.vision.v1.Symbol;
import com.google.cloud.vision.v1.TextAnnotation;
import com.google.cloud.vision.v1.Word;
import com.google.protobuf.ByteString;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class PhotoTranslatorService {

    public void processImage(String imagePath) throws IOException {
        ByteString imgBytes = ByteString.readFrom(new FileInputStream(imagePath));
        Image img = Image.newBuilder().setContent(imgBytes).build();
    } // <--- 【修正 1】補上這裡漏掉的右大括號，結束 processImage 方法

    // 儲存辨識區塊與翻譯資訊的 DTO
    public static class RecognizedTextRegion {
        public String originalText;
        public String translatedText;
        public BoundingPoly boundingBox; // 包含 4 個角的 (x, y) 座標

        public RecognizedTextRegion(String originalText, String translatedText, BoundingPoly boundingBox) {
            this.originalText = originalText;
            this.translatedText = translatedText;
            this.boundingBox = boundingBox;
        }
    }

    /**
     * 辨識圖片中的文字並進行翻譯
     */
    public List<RecognizedTextRegion> processImageTranslation(String imagePath, String targetLangCode) throws IOException {
        List<RecognizedTextRegion> results = new ArrayList<>();

        // 1. 讀取圖片檔案
        ByteString imgBytes = ByteString.readFrom(new FileInputStream(imagePath));
        Image img = Image.newBuilder().setContent(imgBytes).build();

        // 2. 設定 Vision API 請求 (使用 DOCUMENT_TEXT_DETECTION 適合密集的藥品/收據)
        Feature feat = Feature.newBuilder().setType(Feature.Type.DOCUMENT_TEXT_DETECTION).build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                .addFeatures(feat)
                .setImage(img)
                .build();

        try (ImageAnnotatorClient visionClient = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = visionClient.batchAnnotateImages(List.of(request));
            AnnotateImageResponse res = response.getResponsesList().get(0);

            if (res.hasError()) {
                System.err.println("Vision API 錯誤: " + res.getError().getMessage());
                return results;
            }

            // 3. 取得文字排版層級 (Blocks / Paragraphs)
            TextAnnotation fullTextAnnotation = res.getFullTextAnnotation();
            for (Page page : fullTextAnnotation.getPagesList()) {
                for (Block block : page.getBlocksList()) {
                    StringBuilder blockTextBuilder = new StringBuilder();

                    // 組合 Block 內的文字
                    for (Paragraph paragraph : block.getParagraphsList()) {
                        for (Word word : paragraph.getWordsList()) {
                            for (Symbol symbol : word.getSymbolsList()) {
                                blockTextBuilder.append(symbol.getText());
                            }
                            blockTextBuilder.append(" ");
                        }
                    }

                    String originalText = blockTextBuilder.toString().trim();
                    if (!originalText.isEmpty()) {
                        // 4. 呼叫翻譯模組
                        String translatedText = translateText(originalText, targetLangCode);

                        // 5. 保存文字區塊座標 (BoundingPoly) 與翻譯結果
                        results.add(new RecognizedTextRegion(
                                originalText,
                                translatedText,
                                block.getBoundingBox()
                        ));
                    }
                }
            }
        }
        return results;
    }

    /**
     * Google Cloud Translation 語系翻譯
     */
    private String translateText(String text, String targetLangCode) {
        // 實務上在此處呼叫 Google Translate Client 或第三方 API
        // 【修正 2】清理掉殘留的 `-` 與 `+` 符號，改回正常的 Java 字串串接
        return "[譯 " + targetLangCode + "]: " + text;
    }
}