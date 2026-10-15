package com.telemedicine.clinicaldocsservice.adapter;

/** Minimal PDF bytes for clinical document storage. */
public final class SimplePdfGenerator {

    private SimplePdfGenerator() {}

    public static byte[] generate(String title, String body) {
        String safeTitle = escape(title == null ? "Clinical Document" : title);
        String safeBody = escape(body == null ? "" : body);
        String stream = "BT /F1 12 Tf 50 750 Td (" + safeTitle + ") Tj 0 -20 Td (" + safeBody + ") Tj ET";
        String objects =
                """
                1 0 obj<< /Type /Catalog /Pages 2 0 R >>endobj
                2 0 obj<< /Type /Pages /Kids [3 0 R] /Count 1 >>endobj
                3 0 obj<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources<< /Font<< /F1 5 0 R >> >> >>endobj
                4 0 obj<< /Length %d >>stream
                %s
                endstream
                endobj
                5 0 obj<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>endobj
                """
                        .formatted(stream.length(), stream);
        String header = "%PDF-1.4\n";
        String xref =
                """
                xref
                0 6
                0000000000 65535 f
                0000000009 00000 n
                0000000058 00000 n
                0000000115 00000 n
                0000000240 00000 n
                0000000350 00000 n
                """;
        String trailer = "trailer<< /Size 6 /Root 1 0 R >>startxref\n400\n%%EOF";
        return (header + objects + xref + trailer).getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }
}
