package com.example.elephantfinancelab_be.global.apiPayload.util;

public final class AiDetailSanitizer {

  private AiDetailSanitizer() {}

  public static String sanitize(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    String sanitized = raw.strip();
    sanitized = sanitized.replaceAll("(?i)(bearer)\\s+[A-Za-z0-9._~+/=-]+", "$1 <redacted>");
    sanitized =
        sanitized.replaceAll(
            "(?i)(\"(?:app[_-]?key|app[_-]?secret|api[_-]?key|authorization|token|password|secret|account(?:[_-]?(?:number|no))?|confirm[_-]?phrase)\"\\s*:\\s*)\"[^\"]*\"",
            "$1\"<redacted>\"");
    sanitized =
        sanitized.replaceAll(
            "(?i)(app[_-]?key|app[_-]?secret|api[_-]?key|authorization|token|password|secret|account(?:[_-]?(?:number|no))?|confirm[_-]?phrase)\\s*[:=]\\s*(\"[^\"]*\"|'[^']*'|[^,\\s}]+)",
            "$1=<redacted>");
    sanitized = sanitized.replaceAll("/(?:Users|home)/[^\\s,}\\]]+", "<local-path-redacted>");
    sanitized = sanitized.replaceAll("[A-Za-z]:\\\\[^\\s,}\\]]+", "<local-path-redacted>");
    sanitized =
        sanitized.replaceAll(
            "\\\\{2}[^\\\\\\s,}\\]]+\\\\[^\\\\\\s,}\\]]+(?:\\\\[^\\\\\\s,}\\]]+)+",
            "<local-path-redacted>");
    if (sanitized.length() > 500) {
      return sanitized.substring(0, 500) + "...";
    }
    return sanitized;
  }
}
