package met.ivan.devblog.util;

import java.text.Normalizer;
import java.util.function.Predicate;

public final class Slugs {

    private Slugs() {
    }

    /**
     * Converts an arbitrary string into a URL-safe slug:
     * lowercase, accent-stripped, non-alphanumeric chars replaced by hyphens,
     * consecutive/leading/trailing hyphens collapsed.
     */
    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "post";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return normalized
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "")
                .replaceAll("-{2,}", "-");
    }

    /**
     * Generates a slug from the title that is guaranteed to be unique according
     * to the provided existsBySlug predicate. Appends -2, -3, … until unique.
     */
    public static String uniqueSlug(String title, Predicate<String> existsBySlug) {
        String base = toSlug(title);
        if (base.isBlank()) {
            base = "post";
        }
        String candidate = base;
        int suffix = 2;
        while (existsBySlug.test(candidate)) {
            candidate = base + "-" + suffix;
            suffix++;
        }
        return candidate;
    }
}
