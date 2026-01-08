package com.medvault.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    /**
     * Upload a file to Cloudinary
     * 
     * @param file   The file to upload
     * @param folder The folder in Cloudinary to store the file
     * @return Map containing upload result with url, public_id, etc.
     */
    public Map<String, Object> uploadFile(MultipartFile file, String folder) throws IOException {
        try {
            log.info("📤 Uploading file to Cloudinary: {}", file.getOriginalFilename());

            // Generate unique filename
            String publicId = folder + "/" + UUID.randomUUID().toString();

            Map<String, Object> uploadParams = ObjectUtils.asMap(
                    "folder", folder,
                    "public_id", publicId,
                    "resource_type", "auto", // auto-detect file type
                    "use_filename", true,
                    "unique_filename", true,
                    "overwrite", false);

            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadParams);

            log.info("✅ File uploaded to Cloudinary successfully: {}", uploadResult.get("url"));
            return uploadResult;

        } catch (IOException e) {
            log.error("❌ Error uploading file to Cloudinary: {}", e.getMessage());
            throw new IOException("Failed to upload file to Cloudinary: " + e.getMessage(), e);
        }
    }

    /**
     * Delete a file from Cloudinary
     * 
     * @param publicId The public ID of the file to delete
     */
    public void deleteFile(String publicId) {
        try {
            log.info("🗑️ Deleting file from Cloudinary: {}", publicId);

            Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());

            log.info("✅ File deleted from Cloudinary: {}", result.get("result"));
        } catch (Exception e) {
            log.error("❌ Error deleting file from Cloudinary: {}", e.getMessage());
            // Don't throw exception, just log - deletion failure shouldn't break the flow
        }
    }

    /**
     * Get file URL from Cloudinary
     * 
     * @param publicId The public ID of the file
     * @return The secure URL of the file
     */
    public String getFileUrl(String publicId) {
        return cloudinary.url()
                .secure(true)
                .generate(publicId);
    }
}
