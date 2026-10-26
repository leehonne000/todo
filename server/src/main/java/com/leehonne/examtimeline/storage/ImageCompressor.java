package com.leehonne.examtimeline.storage;

import com.leehonne.examtimeline.common.BizException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * 服务端图片压缩（契约 §10）：长边压到 1600px 以内再入库。
 * v1 用 JDK ImageIO 原生编解码——无透明存 JPEG（质量 0.82），有透明存 PNG；
 * 之后可无痛换 webp 编码器（只动这里，objectKey 后缀随扩展名走）。
 */
@Component
public class ImageCompressor {

    private static final int MAX_EDGE = 1600;
    private static final float JPEG_QUALITY = 0.82f;

    /** 压缩结果：字节 + 真实 MIME + 文件扩展名（含点） */
    public record CompressedImage(byte[] bytes, String contentType, String extension) {
    }

    public CompressedImage compress(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BizException.badRequest("图片文件为空");
        }
        BufferedImage src;
        try (InputStream in = file.getInputStream()) {
            src = ImageIO.read(in);
        } catch (IOException e) {
            throw BizException.badRequest("图片读取失败");
        }
        if (src == null) {
            throw BizException.badRequest("无法解析该图片（仅支持 jpg/png 等常见格式）");
        }

        BufferedImage scaled = scale(src);
        boolean hasAlpha = scaled.getColorModel().hasAlpha();
        try {
            // 优先 webp（契约 §10）；编码器不可用/失败时回退 jpg/png
            CompressedImage webp = writeWebp(scaled, hasAlpha);
            if (webp != null) {
                return webp;
            }
            if (hasAlpha) {
                return new CompressedImage(writePng(scaled), "image/png", ".png");
            }
            return new CompressedImage(writeJpeg(scaled), "image/jpeg", ".jpg");
        } catch (IOException e) {
            throw new BizException(5000, "图片压缩失败");
        }
    }

    /** webp（gotson webp-imageio）：有透明走无损，无透明走有损 0.82；任何失败返回 null 走回退 */
    private CompressedImage writeWebp(BufferedImage image, boolean hasAlpha) {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType("image/webp");
        if (!writers.hasNext()) {
            return null;
        }
        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             MemoryCacheImageOutputStream mos = new MemoryCacheImageOutputStream(out)) {
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                try {
                    if (hasAlpha) {
                        param.setCompressionType("Lossless");
                    } else {
                        param.setCompressionType("Lossy");
                        param.setCompressionQuality(JPEG_QUALITY);
                    }
                } catch (IllegalArgumentException ignored) {
                    // 编码器不认压缩类型名就用默认参数
                }
            }
            writer.setOutput(mos);
            writer.write(null, new IIOImage(image, null, null), param);
            return new CompressedImage(out.toByteArray(), "image/webp", ".webp");
        } catch (Exception e) {
            return null;
        } finally {
            writer.dispose();
        }
    }

    /** 长边 ≤ 1600：超过就等比缩小，否则原样返回 */
    private BufferedImage scale(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        int longEdge = Math.max(w, h);
        if (longEdge <= MAX_EDGE) {
            return src;
        }
        double ratio = (double) MAX_EDGE / longEdge;
        int nw = (int) Math.round(w * ratio);
        int nh = (int) Math.round(h * ratio);
        int type = src.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage out = new BufferedImage(nw, nh, type);
        Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(src, 0, 0, nw, nh, null);
        } finally {
            g.dispose();
        }
        return out;
    }

    private byte[] writeJpeg(BufferedImage image) throws IOException {
        // JPEG 不带透明：压平成 RGB 再写
        BufferedImage rgb = image;
        if (image.getType() != BufferedImage.TYPE_INT_RGB && image.getType() != BufferedImage.TYPE_BYTE_GRAY) {
            rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgb.createGraphics();
            try {
                g.drawImage(image, 0, 0, java.awt.Color.WHITE, null);
            } finally {
                g.dispose();
            }
        }
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("no jpeg writer");
        }
        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             MemoryCacheImageOutputStream mos = new MemoryCacheImageOutputStream(out)) {
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(JPEG_QUALITY);
            }
            writer.setOutput(mos);
            writer.write(null, new IIOImage(rgb, null, null), param);
            writer.dispose();
            return out.toByteArray();
        }
    }

    private byte[] writePng(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
