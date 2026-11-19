package com.campusdual.cd2026mnd1g1;

import com.campusdual.cd2026mnd1g1.ws.controller.TutorDogPhotoStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TutorDogPhotoStorageTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void storesNormalizedImageUnderGeneratedPrivateNameAndDeletesIt() throws Exception {
        TutorDogPhotoStorage storage = new TutorDogPhotoStorage(this.temporaryDirectory.toString());
        MockMultipartFile upload = new MockMultipartFile(
                "photo",
                "../unexpected-name.png",
                "image/png",
                createPng()
        );

        String storedName = storage.store(upload);

        assertTrue(storedName.matches("tutor-[0-9a-f-]{36}\\.png"));
        Path storedFile = this.temporaryDirectory.resolve(storedName);
        assertTrue(Files.isRegularFile(storedFile));
        assertTrue(ImageIO.read(storedFile.toFile()) != null);

        storage.delete(storedName);
        assertFalse(Files.exists(storedFile));
    }

    @Test
    void rejectsSpoofedImageAndFileAboveMaximumSize() {
        TutorDogPhotoStorage storage = new TutorDogPhotoStorage(this.temporaryDirectory.toString());
        MockMultipartFile spoofed = new MockMultipartFile(
                "photo",
                "dog.png",
                "image/png",
                new byte[] {1, 2, 3, 4}
        );
        MockMultipartFile oversized = new MockMultipartFile(
                "photo",
                "dog.png",
                "image/png",
                new byte[5 * 1024 * 1024 + 1]
        );

        assertThrows(IllegalArgumentException.class, () -> storage.store(spoofed));
        assertThrows(IllegalArgumentException.class, () -> storage.store(oversized));
    }

    private byte[] createPng() throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
