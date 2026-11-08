package model;

/**
 * Central place for environment-specific settings and secrets.
 *
 * Each value is looked up as a JVM system property first (-DNAME=value, e.g. set
 * with "asadmin create-jvm-options"), then as an environment variable. Nothing
 * secret lives in source control.
 *
 * Keys:
 *   MAIL_USER                  Gmail/SMTP account used to send verification codes
 *   MAIL_PASSWORD              its app password
 *   PAYHERE_MERCHANT_ID        PayHere merchant id
 *   PAYHERE_MERCHANT_SECRET    PayHere merchant secret
 *   PAYHERE_SANDBOX            "true" (default) or "false"
 *   APP_BASE_URL               public URL of the app incl. context path,
 *                              default http://localhost:8080/SkillVerse
 *   PAYHERE_NOTIFY_URL         optional override for the PayHere webhook URL,
 *                              default APP_BASE_URL + /VerifyPayments
 */
public final class AppConfig {

    private AppConfig() {
    }

    public static String get(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            value = System.getenv(key);
        }
        return (value == null || value.trim().isEmpty()) ? defaultValue : value.trim();
    }

    /** Returns the value or throws, naming the missing key (never its value). */
    public static String require(String key) {
        String value = get(key, null);
        if (value == null) {
            throw new IllegalStateException("Missing configuration: set " + key
                    + " as a JVM system property or environment variable.");
        }
        return value;
    }

    public static String baseUrl() {
        String url = get("APP_BASE_URL", "http://localhost:8080/SkillVerse");
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public static String payhereNotifyUrl() {
        return get("PAYHERE_NOTIFY_URL", baseUrl() + "/VerifyPayments");
    }

    public static boolean payhereSandbox() {
        return !"false".equalsIgnoreCase(get("PAYHERE_SANDBOX", "true"));
    }
}
