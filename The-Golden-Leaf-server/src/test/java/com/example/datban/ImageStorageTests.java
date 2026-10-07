package com.example.datban;

import com.example.datban.service.ImageStorage;
import com.example.datban.exception.BusinessRuleException;
import org.springframework.mock.web.MockMultipartFile;
import org.junit.jupiter.api.Test;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import static org.assertj.core.api.Assertions.*;

class ImageStorageTests {
    private final ImageStorage storage=new ImageStorage();
    byte[] png(int width) throws Exception {
        var out=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(width,1,BufferedImage.TYPE_INT_RGB),"png",out);return out.toByteArray();
    }
    @Test void originalFilenameCannotEscapeUploadDirectory() throws Exception {
        String url=storage.store(new MockMultipartFile("image","../../escape.png","image/png",png(1)));
        Path root=Paths.get("uploads").toAbsolutePath().normalize();Path output=root.resolve(url.substring("/uploads/".length())).normalize();
        assertThat(output.getParent()).isEqualTo(root);
        try {assertThat(url).matches("/uploads/[0-9a-f-]+\\.png");assertThat(ImageIO.read(output.toFile()).getWidth()).isEqualTo(1);}
        finally {if(output.getParent().equals(root))Files.deleteIfExists(output);}
    }
    @Test void svgAndFakeMimeTypeAreRejected() {
        assertThatThrownBy(()->storage.store(new MockMultipartFile("image","x.png","image/png","<svg><script>alert(1)</script></svg>".getBytes())))
                .isInstanceOf(BusinessRuleException.class);
    }
    @Test void oversizedDimensionsAreRejectedBeforeDecode() throws Exception {
        byte[] bytes=png(4097);
        assertThatThrownBy(()->storage.store(new MockMultipartFile("image","x.png","image/png",bytes))).isInstanceOf(BusinessRuleException.class);
    }
    @Test void emptyFileIsRejected() {
        assertThatThrownBy(()->storage.store(new MockMultipartFile("image",new byte[0]))).isInstanceOf(BusinessRuleException.class);
    }
    @Test void oversizedFileIsRejected() {
        var file=new MockMultipartFile("image",new byte[1]) {public long getSize(){return 6*1024*1024;}};
        assertThatThrownBy(()->storage.store(file)).isInstanceOf(BusinessRuleException.class);
    }
}
