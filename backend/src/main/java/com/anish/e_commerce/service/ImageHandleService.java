package com.anish.e_commerce.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class ImageHandleService {

    @Autowired
    private Cloudinary cloudinary;

    public String uploadFile(MultipartFile file) throws IOException {
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap("folder", "Springboot-Ecommerce/products"));
        return uploadResult.get("secure_url").toString(); // This is the image URL
    }

    public boolean deleteImageByUrl(String imageUrl) {
        try {
            // Extract public_id from URL
            String[] parts = imageUrl.split("/");
            String publicIdWithExtension = parts[parts.length - 1]; // e.g., "image123.jpg"
            String publicId = publicIdWithExtension.substring(0, publicIdWithExtension.lastIndexOf('.'));

            String folder = "Springboot-Ecommerce/products/";
            String fullPublicId = folder + publicId;

            Map result = cloudinary.uploader().destroy(fullPublicId, ObjectUtils.emptyMap());
            return "ok".equals(result.get("result"));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
