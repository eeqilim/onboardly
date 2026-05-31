package bf.backend.employee_service.mapper;

import bf.backend.employee_service.dto.request.OnboardingApplicationRequest;
import bf.backend.employee_service.dto.response.*;
import bf.backend.employee_service.entity.*;
import bf.backend.employee_service.util.SsnUtils;

import java.util.List;

public final class EmployeeMapper {

    private EmployeeMapper() {}

    public static EmployeeProfileResponse toProfileResponse(
            Employee e,
            String resolvedAvatarUrl,
            List<VisaStatus> visaStatuses,
            List<PersonalDocument> documents,
            List<OnboardingApplication> applications) {
        return new EmployeeProfileResponse(
                e.getId(),
                e.getUserId(),
                e.getFirstName(),
                e.getLastName(),
                e.getMiddleName(),
                e.getPreferredName(),
                e.getEmail(),
                e.getPersonalEmail(),
                e.getCellPhone(),
                e.getAlternatePhone(),
                e.getWorkPhone(),
                e.getGender(),
                SsnUtils.maskSsn(e.getSsn()),
                e.getDateOfBirth(),
                resolvedAvatarUrl,
                e.getCitizenshipStatus(),
                e.getDriverLicense(),
                e.getDriverLicenseExpiration(),
                e.getHouseId(),
                e.getEmploymentStartDate(),
                e.getEmploymentEndDate(),
                e.getCreatedAt(),
                e.getAddresses().stream().map(AddressMapper::toResponse).toList(),
                e.getContacts().stream().map(ContactMapper::toResponse).toList(),
                visaStatuses.stream().map(VisaStatusMapper::toResponse).toList(),
                documents.stream().map(DocumentMapper::toResponse).toList(),
                applications.stream().map(OnboardingMapper::toResponse).toList()
        );
    }

    public static EmployeeFullProfileResponse toFullProfileResponse(
            Employee e,
            String resolvedAvatarUrl,
            List<VisaStatus> visaStatuses,
            List<PersonalDocument> documents,
            List<OnboardingApplication> applications) {
        return new EmployeeFullProfileResponse(
                e.getId(),
                e.getUserId(),
                e.getFirstName(),
                e.getLastName(),
                e.getMiddleName(),
                e.getPreferredName(),
                e.getEmail(),
                e.getPersonalEmail(),
                e.getCellPhone(),
                e.getAlternatePhone(),
                e.getWorkPhone(),
                e.getGender(),
                e.getSsn(),
                e.getDateOfBirth(),
                resolvedAvatarUrl,
                e.getCitizenshipStatus(),
                e.getDriverLicense(),
                e.getDriverLicenseExpiration(),
                e.getHouseId(),
                e.getEmploymentStartDate(),
                e.getEmploymentEndDate(),
                e.getCreatedAt(),
                e.getAddresses().stream().map(AddressMapper::toResponse).toList(),
                e.getContacts().stream().map(ContactMapper::toResponse).toList(),
                visaStatuses.stream().map(VisaStatusMapper::toResponse).toList(),
                documents.stream().map(DocumentMapper::toResponse).toList(),
                applications.stream().map(OnboardingMapper::toResponse).toList()
        );
    }

    public static EmployeeSummaryResponse toSummary(Employee e, VisaStatus activeVisa) {
        VisaStatusResponse activeVisaResponse = activeVisa != null
                ? VisaStatusMapper.toResponse(activeVisa)
                : null;
        return new EmployeeSummaryResponse(
                e.getId(),
                e.getUserId(),
                e.getFirstName(),
                e.getLastName(),
                e.getPreferredName(),
                e.getEmail(),
                SsnUtils.maskSsn(e.getSsn()),
                e.getEmploymentStartDate(),
                activeVisaResponse
        );
    }

    public static HrOnboardingProfileResponse toHrOnboardingProfile(
            Employee e,
            String resolvedAvatarUrl,
            VisaStatus activeVisa,
            List<PersonalDocument> documents,
            OnboardingApplication application) {

        ContactResponse reference = e.getContacts().stream()
                .filter(c -> c.getType() == ContactType.REFERENCE)
                .findFirst()
                .map(ContactMapper::toResponse)
                .orElse(null);

        List<ContactResponse> emergencyContacts = e.getContacts().stream()
                .filter(c -> c.getType() == ContactType.EMERGENCY)
                .map(ContactMapper::toResponse)
                .toList();

        return new HrOnboardingProfileResponse(
                e.getId(),
                e.getUserId(),
                e.getFirstName(),
                e.getLastName(),
                e.getMiddleName(),
                e.getPreferredName(),
                e.getEmail(),
                e.getPersonalEmail(),
                e.getCellPhone(),
                e.getAlternatePhone(),
                e.getWorkPhone(),
                e.getGender(),
                e.getSsn(),
                e.getDateOfBirth(),
                resolvedAvatarUrl,
                e.getCitizenshipStatus(),
                e.getDriverLicense(),
                e.getDriverLicenseExpiration(),
                e.getHouseId(),
                e.getEmploymentStartDate(),
                e.getEmploymentEndDate(),
                e.getCreatedAt(),
                e.getAddresses().stream().map(AddressMapper::toResponse).toList(),
                reference,
                emergencyContacts,
                activeVisa != null ? VisaStatusMapper.toResponse(activeVisa) : null,
                documents.stream().map(DocumentMapper::toResponse).toList(),
                application != null ? OnboardingMapper.toResponse(application) : null
        );
    }

    public static void applyOnboardingRequest(Employee e, OnboardingApplicationRequest r) {
        e.setFirstName(r.firstName());
        e.setLastName(r.lastName());
        e.setMiddleName(r.middleName());
        e.setPreferredName(r.preferredName());
        e.setEmail(r.email());
        e.setCellPhone(r.cellPhone());
        e.setAlternatePhone(r.alternatePhone());
        e.setGender(r.gender());
        e.setSsn(r.ssn());
        e.setDateOfBirth(r.dateOfBirth());
        e.setCitizenshipStatus(r.citizenshipStatus());
        e.setDriverLicense(r.driverLicense());
        e.setDriverLicenseExpiration(r.driverLicenseExpiration());
    }
}
