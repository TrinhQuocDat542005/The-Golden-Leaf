package com.example.datban.service;

import com.example.datban.exception.BusinessRuleException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;

@Service
public class ImageStorage {
    public String store(MultipartFile file) throws IOException {
        if (file.isEmpty() || file.getSize()>5*1024*1024) reject();
        try (var input=file.getInputStream();var stream=ImageIO.createImageInputStream(input)) {
            var readers=ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {reject();return null;}
            var reader=readers.next();
            try {
                reader.setInput(stream,true,true);
                String format=reader.getFormatName().toLowerCase(Locale.ROOT);
                // Inspect dimensions before decoding, so a compressed image cannot allocate an enormous bitmap.
                if (!Set.of("png","jpeg","jpg").contains(format) || reader.getWidth(0)>4096 || reader.getHeight(0)>4096) reject();
                var image=reader.read(0);
                Path root=Paths.get("uploads").toAbsolutePath().normalize();Files.createDirectories(root);
                String name=UUID.randomUUID()+".png";Path destination=root.resolve(name);
                ImageIO.write(image,"png",destination.toFile()); // Re-encode: discard metadata and trailing executable/polyglot content.
                return "/uploads/"+name;
            } finally {reader.dispose();}
        } catch (javax.imageio.IIOException ex) {reject();return null;}
    }
    private void reject() {throw new BusinessRuleException("INVALID_IMAGE","Chỉ nhận PNG/JPEG hợp lệ, tối đa 5 MB và 4096 px");}
}
