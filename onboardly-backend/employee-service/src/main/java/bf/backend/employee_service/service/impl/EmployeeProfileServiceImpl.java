package bf.backend.employee_service.service.impl;

import bf.backend.employee_service.dto.request.*;
import bf.backend.employee_service.entity.ContactType;
import bf.backend.employee_service.dto.response.EmployeeFullProfileResponse;
import bf.backend.employee_service.dto.response.EmployeeProfileResponse;
import bf.backend.employee_service.entity.*;
import bf.backend.employee_service.exception.ResourceNotFoundException;
import bf.backend.employee_service.exception.UnauthorizedActionException;
import bf.backend.employee_service.mapper.ContactMapper;
import bf.backend.employee_service.mapper.EmployeeMapper;
import bf.backend.employee_service.repository.OnboardingApplicationRepository;
import bf.backend.employee_service.repository.PersonalDocumentRepository;
import bf.backend.employee_service.repository.EmployeeRepository;
import bf.backend.employee_service.repository.VisaStatusRepository;
import bf.backend.employee_service.service.EmployeeProfileService;
import bf.backend.employee_service.service.FileStorageService;
import bf.backend.employee_service.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeProfileServiceImpl implements EmployeeProfileService {

    private final EmployeeRepository employeeRepository;
    private final VisaStatusRepository visaStatusRepository;
    private final PersonalDocumentRepository personalDocumentRepository;
    private final OnboardingApplicationRepository onboardingApplicationRepository;
    private final FileStorageService fileStorageService;

    @Override
    public EmployeeProfileResponse getMyProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee profile not found for current user"));
        return buildProfileResponse(employee);
    }

    @Override
    public EmployeeProfileResponse getProfileById(String employeeId) {
        Employee employee = findById(employeeId);
        checkAccess(employee);
        return buildProfileResponse(employee);
    }

    @Override
    public EmployeeFullProfileResponse getFullProfileById(String employeeId) {
        Employee employee = findById(employeeId);
        return buildFullProfileResponse(employee);
    }

    @Override
    public EmployeeProfileResponse updatePersonal(String employeeId, UpdatePersonalRequest req) {
        Employee employee = findById(employeeId);
        checkAccess(employee);
        employee.setEmail(req.email());
        employee.setPersonalEmail(req.personalEmail());
        employee.setCellPhone(req.cellPhone());
        employee.setAlternatePhone(req.alternatePhone());
        employee.setWorkPhone(req.workPhone());
        employee.setGender(req.gender());
        employee.setDateOfBirth(req.dateOfBirth());
        return buildProfileResponse(employeeRepository.save(employee));
    }

    @Override
    public String uploadAvatar(String employeeId, MultipartFile file) {
        Employee employee = findById(employeeId);
        checkAccess(employee);

        String oldAvatar = employee.getAvatarUrl();
        if (oldAvatar != null && isS3Key(oldAvatar)) {
            fileStorageService.delete(oldAvatar);
        }

        String s3Key = fileStorageService.upload(file, "avatars/" + employeeId);
        employee.setAvatarUrl(s3Key);
        employeeRepository.save(employee);
        return fileStorageService.generatePresignedDownloadUrl(s3Key, Duration.ofHours(1));
    }

    @Override
    public EmployeeProfileResponse addContact(String employeeId, ContactRequest req) {
        Employee employee = findById(employeeId);
        checkAccess(employee);
        if (req.type() != ContactType.EMERGENCY) {
            throw new bf.backend.employee_service.exception.ValidationException(
                    "Only EMERGENCY contacts can be added via this endpoint.");
        }
        employee.addContact(ContactMapper.toEntity(req));
        return buildProfileResponse(employeeRepository.save(employee));
    }

    @Override
    public EmployeeProfileResponse updateContactById(String employeeId, String contactId, ContactRequest req) {
        Employee employee = findById(employeeId);
        checkAccess(employee);
        Contact contact = employee.getContacts().stream()
                .filter(c -> contactId.equals(c.getId()))
                .findFirst()
                .orElseThrow(() -> new bf.backend.employee_service.exception.ResourceNotFoundException(
                        "Contact not found: " + contactId));
        contact.setType(req.type());
        contact.setFirstName(req.firstName());
        contact.setLastName(req.lastName());
        contact.setMiddleName(req.middleName());
        contact.setCellPhone(req.cellPhone());
        contact.setAlternatePhone(req.alternatePhone());
        contact.setEmail(req.email());
        contact.setRelationship(req.relationship());
        contact.setAddress(req.address());
        return buildProfileResponse(employeeRepository.save(employee));
    }

    @Override
    public EmployeeProfileResponse deleteContactById(String employeeId, String contactId) {
        Employee employee = findById(employeeId);
        checkAccess(employee);
        boolean removed = employee.getContacts()
                .removeIf(c -> contactId.equals(c.getId()));
        if (!removed) {
            throw new bf.backend.employee_service.exception.ResourceNotFoundException(
                    "Contact not found: " + contactId);
        }
        return buildProfileResponse(employeeRepository.save(employee));
    }

    @Override
    public EmployeeProfileResponse updateName(String employeeId, UpdateNameRequest req) {
        Employee employee = findById(employeeId);
        checkAccess(employee);
        employee.setFirstName(req.firstName());
        employee.setLastName(req.lastName());
        employee.setMiddleName(req.middleName());
        employee.setPreferredName(req.preferredName());
        return buildProfileResponse(employeeRepository.save(employee));
    }

    @Override
    public EmployeeProfileResponse updateAddress(String employeeId, UpdateAddressRequest req) {
        Employee employee = findById(employeeId);
        checkAccess(employee);

        Address address = employee.getAddresses().stream()
                .filter(a -> a.getType() == req.type())
                .findFirst()
                .orElseGet(() -> {
                    Address a = new Address();
                    employee.addAddress(a);
                    return a;
                });

        address.setType(req.type());
        address.setAddressLine1(req.addressLine1());
        address.setAddressLine2(req.addressLine2());
        address.setCity(req.city());
        address.setState(req.state());
        address.setZipCode(req.zipCode());

        return buildProfileResponse(employeeRepository.save(employee));
    }

    @Override
    public EmployeeProfileResponse updateContact(String employeeId, UpdateContactRequest req) {
        Employee employee = findById(employeeId);
        checkAccess(employee);

        Contact contact = employee.getContacts().stream()
                .filter(c -> c.getType() == req.type())
                .findFirst()
                .orElseGet(() -> {
                    Contact c = new Contact();
                    employee.addContact(c);
                    return c;
                });

        contact.setType(req.type());
        contact.setFirstName(req.firstName());
        contact.setLastName(req.lastName());
        contact.setMiddleName(req.middleName());
        contact.setCellPhone(req.cellPhone());
        contact.setAlternatePhone(req.alternatePhone());
        contact.setEmail(req.email());
        contact.setRelationship(req.relationship());
        contact.setAddress(req.address());

        return buildProfileResponse(employeeRepository.save(employee));
    }

    @Override
    public EmployeeProfileResponse updateEmployment(String employeeId, UpdateEmploymentRequest req) {
        Employee employee = findById(employeeId);
        checkAccess(employee);
        employee.setEmploymentStartDate(req.employmentStartDate());
        employee.setEmploymentEndDate(req.employmentEndDate());
        employee.setDriverLicense(req.driverLicense());
        employee.setDriverLicenseExpiration(req.driverLicenseExpiration());
        employee.setHouseId(req.houseId());
        return buildProfileResponse(employeeRepository.save(employee));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Employee findById(String employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found: " + employeeId));
    }

    private void checkAccess(Employee employee) {
        if (!SecurityUtils.isHr()
                && !employee.getUserId().equals(SecurityUtils.getCurrentUserId())) {
            throw new UnauthorizedActionException(
                    "Access denied to employee profile " + employee.getId());
        }
    }

    private String resolveAvatarUrl(String avatarUrl) {
        if (avatarUrl != null && isS3Key(avatarUrl)) {
            return fileStorageService.generatePresignedDownloadUrl(avatarUrl, Duration.ofHours(1));
        }
        return avatarUrl;
    }

    private static boolean isS3Key(String value) {
        return !value.startsWith("/") && !value.startsWith("http");
    }

    private EmployeeProfileResponse buildProfileResponse(Employee employee) {
        List<VisaStatus> visaStatuses = visaStatusRepository
                .findByEmployeeIdOrderByLastModificationDateDesc(employee.getId());
        List<PersonalDocument> documents = personalDocumentRepository
                .findByEmployeeIdOrderByCreatedAtDesc(employee.getId());
        List<OnboardingApplication> applications = onboardingApplicationRepository
                .findFirstByEmployeeIdOrderByCreatedAtDesc(employee.getId())
                .map(List::of).orElse(List.of());
        return EmployeeMapper.toProfileResponse(
                employee, resolveAvatarUrl(employee.getAvatarUrl()), visaStatuses, documents, applications);
    }

    private EmployeeFullProfileResponse buildFullProfileResponse(Employee employee) {
        List<VisaStatus> visaStatuses = visaStatusRepository
                .findByEmployeeIdOrderByLastModificationDateDesc(employee.getId());
        List<PersonalDocument> documents = personalDocumentRepository
                .findByEmployeeIdOrderByCreatedAtDesc(employee.getId());
        List<OnboardingApplication> applications = onboardingApplicationRepository
                .findFirstByEmployeeIdOrderByCreatedAtDesc(employee.getId())
                .map(List::of).orElse(List.of());
        return EmployeeMapper.toFullProfileResponse(
                employee, resolveAvatarUrl(employee.getAvatarUrl()), visaStatuses, documents, applications);
    }
}
