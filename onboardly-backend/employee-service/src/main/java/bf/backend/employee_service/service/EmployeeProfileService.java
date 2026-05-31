package bf.backend.employee_service.service;

import bf.backend.employee_service.dto.request.*;
import bf.backend.employee_service.dto.response.EmployeeFullProfileResponse;
import bf.backend.employee_service.dto.response.EmployeeProfileResponse;
import org.springframework.web.multipart.MultipartFile;

public interface EmployeeProfileService {

    EmployeeProfileResponse getMyProfile();

    EmployeeProfileResponse getProfileById(String employeeId);

    EmployeeFullProfileResponse getFullProfileById(String employeeId);

    EmployeeProfileResponse updateName(String employeeId, UpdateNameRequest req);

    EmployeeProfileResponse updatePersonal(String employeeId, UpdatePersonalRequest req);

    EmployeeProfileResponse updateAddress(String employeeId, UpdateAddressRequest req);

    EmployeeProfileResponse updateContact(String employeeId, UpdateContactRequest req);

    EmployeeProfileResponse addContact(String employeeId, ContactRequest req);

    EmployeeProfileResponse updateContactById(String employeeId, String contactId, ContactRequest req);

    EmployeeProfileResponse deleteContactById(String employeeId, String contactId);

    EmployeeProfileResponse updateEmployment(String employeeId, UpdateEmploymentRequest req);

    String uploadAvatar(String employeeId, MultipartFile file);
}
