package com.example.applicationservice.service;

import com.example.applicationservice.dto.response.DocumentTemplateResponse;

import java.util.List;

public final class DocumentTemplateCatalog {

    public static final String ONBOARDING = "ONBOARDING";
    public static final String OPT_STEM = "OPT_STEM";

    private static final List<TemplateSpec> ONBOARDING_TEMPLATES = List.of(
            new TemplateSpec(
                    ONBOARDING,
                    "W4",
                    "W-4 Form",
                    1,
                    "templates/onboarding/fw4.pdf",
                    "Required onboarding tax form"
            ),
            new TemplateSpec(
                    ONBOARDING,
                    "I9",
                    "I-9 Form",
                    1,
                    "templates/onboarding/i9.pdf",
                    "Required employment eligibility verification form"
            ),
            new TemplateSpec(
                    ONBOARDING,
                    "COMPANY_POLICY",
                    "Company Policy Agreement",
                    1,
                    "templates/onboarding/company-policy.pdf",
                    "Required company policy agreement"
            ),
            new TemplateSpec(
                    ONBOARDING,
                    "DIRECT_DEPOSIT",
                    "Direct Deposit Form",
                    0,
                    "templates/onboarding/direct-deposit.pdf",
                    "Direct deposit form"
            )
    );

    private static final List<TemplateSpec> OPT_STEM_TEMPLATES = List.of(
            new TemplateSpec(
                    OPT_STEM,
                    "I_983",
                    "I-983 Form",
                    1,
                    "templates/opt-stem/i983.pdf",
                    "Required OPT STEM training plan template"
            )
    );

    private DocumentTemplateCatalog() {}

    public static List<TemplateSpec> forApplicationType(String applicationType) {
        String normalizedApplicationType = applicationType == null ? null : applicationType.trim();
        if (ONBOARDING.equalsIgnoreCase(normalizedApplicationType)) {
            return ONBOARDING_TEMPLATES;
        }
        if (OPT_STEM.equalsIgnoreCase(normalizedApplicationType)) {
            return OPT_STEM_TEMPLATES;
        }

        return List.of();
    }

    public record TemplateSpec(
            String applicationType,
            String type,
            String title,
            Integer isRequired,
            String path,
            String description
    ) {
        public DocumentTemplateResponse toResponse() {
            return DocumentTemplateResponse.builder()
                    .applicationType(applicationType)
                    .type(type)
                    .isRequired(isRequired)
                    .path(path)
                    .description(description)
                    .title(title)
                    .build();
        }
    }
}
