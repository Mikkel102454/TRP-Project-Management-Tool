package solutions.trp.pmt.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;

@Service
public class SidekickVideoService {
    private static final Set<String> VIDEOS = Set.of("shush.mp4", "neutral.mp4", "happy.mp4", "mad.mp4");

    private final Map<String, String> checksums = new HashMap<>();

    // Bundled videos are immutable for the lifetime of the application.
    public synchronized String checksum(String filename) throws IOException {
        String cached = checksums.get(filename);
        if (cached != null) return cached;
        Resource resource = get(filename);
        if (resource == null) throw new FileNotFoundException("Sidekick video not found: " + filename);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var stream = new DigestInputStream(resource.getInputStream(), digest)) {
                stream.transferTo(OutputStream.nullOutputStream());
            }
            String checksum = HexFormat.of().formatHex(digest.digest());
            checksums.put(filename, checksum);
            return checksum;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    public Resource get(String filename) {
        if (!VIDEOS.contains(filename)) return null;
        Resource resource = new ClassPathResource("gadget/sidekick/videos/" + filename);
        return resource.isReadable() ? resource : null;
    }
}
