package bf.backend.employee_service.util;

public final class SsnUtils {

    private SsnUtils() {}

    public static String maskSsn(String ssn) {
        if (ssn == null || ssn.isBlank()) return "***-**-****";
        String digits = ssn.replaceAll("[^0-9]", "");
        if (digits.length() < 4) return "***-**-****";
        return "***-**-" + digits.substring(digits.length() - 4);
    }
}
