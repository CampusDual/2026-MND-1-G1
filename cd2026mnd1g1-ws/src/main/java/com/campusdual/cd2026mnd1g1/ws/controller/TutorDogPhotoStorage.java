package com.campusdual.cd2026mnd1g1.ws.controller;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

/**
 * Valida y guarda fotos de perros en el directorio privado configurado.
 */
@Component
public class TutorDogPhotoStorage {

    private static final long MAX_FILE_SIZE = 5L * 1024L * 1024L;
    private static final long MAX_PIXEL_COUNT = 20_000_000L;

    private final String configuredDirectory;

    public TutorDogPhotoStorage(
            @org.springframework.beans.factory.annotation.Value("${app.dogs.photos.directory:}")
            String configuredDirectory
    ) {
        this.configuredDirectory = configuredDirectory;
    }

    public String store(MultipartFile upload) {
        if (upload == null || upload.isEmpty()) {
            return null;
        }
        if (upload.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("La foto no puede superar 5 MB");
        }
        if (this.configuredDirectory == null || this.configuredDirectory.isBlank()) {
            throw new TutorDogPhotoStorageException("El almacenamiento privado de fotos no está configurado");
        }
        Path destination = null;
        try {
            byte[] originalBytes = upload.getBytes();
            ValidatedImage image = decodeImage(originalBytes);
            ByteArrayOutputStream normalizedImage = new ByteArrayOutputStream();
            if (!ImageIO.write(image.image(), image.format(), normalizedImage)) {
                throw new IllegalArgumentException("No se pudo procesar la imagen enviada");
            }
            byte[] safeBytes = normalizedImage.toByteArray();
            if (safeBytes.length > MAX_FILE_SIZE) {
                throw new IllegalArgumentException("La foto procesada supera el límite de 5 MB");
            }
            Path root = createPrivateDirectory();
            String filename = "tutor-" + UUID.randomUUID() + "." + image.format();
            destination = root.resolve(filename);
            Files.write(destination, safeBytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            return filename;
        } catch (IOException | SecurityException exception) {
            if (destination != null) {
                try {
                    Files.deleteIfExists(destination);
                } catch (IOException | SecurityException cleanupException) {
                    exception.addSuppressed(cleanupException);
                }
            }
            throw new TutorDogPhotoStorageException("No se pudo guardar la foto del perro", exception);
        }
    }

    public void delete(String filename) {
        if (filename == null || filename.isBlank() || this.configuredDirectory == null
                || this.configuredDirectory.isBlank()) {
            return;
        }
        try {
            Path root = Path.of(this.configuredDirectory).toRealPath();
            Path photo = root.resolve(filename).normalize();
            if (photo.startsWith(root) && Files.isRegularFile(photo)) {
                Files.delete(photo);
            }
        } catch (IOException | SecurityException exception) {
            throw new TutorDogPhotoStorageException("No se pudo limpiar la foto temporal", exception);
        }
    }

    private Path createPrivateDirectory() throws IOException {
        Path configuredPath = Path.of(this.configuredDirectory).toAbsolutePath().normalize();
        Files.createDirectories(configuredPath);
        return configuredPath.toRealPath();
    }

    private ValidatedImage decodeImage(byte[] bytes) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) {
                throw new IllegalArgumentException("El archivo enviado no es una imagen JPEG o PNG válida");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("Solo se admiten imágenes JPEG o PNG válidas");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!format.equals("jpeg") && !format.equals("jpg") && !format.equals("png")) {
                    throw new IllegalArgumentException("Solo se admiten imágenes JPEG o PNG");
                }
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_PIXEL_COUNT) {
                    throw new IllegalArgumentException("Las dimensiones de la imagen no son válidas");
                }
                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw new IllegalArgumentException("No se pudo decodificar la imagen enviada");
                }
                return new ValidatedImage(image, format.equals("jpg") ? "jpeg" : format);
            } finally {
                reader.dispose();
            }
        }
    }

    private record ValidatedImage(BufferedImage image, String format) {
    }
}
