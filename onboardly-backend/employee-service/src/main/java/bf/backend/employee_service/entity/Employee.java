package bf.backend.employee_service.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Employee {

    @Id
    private String id;

    @Indexed(unique = true)
    private Long userId;

    private String firstName;
    private String lastName;
    private String middleName;
    private String preferredName;

    @Indexed(unique = true)
    private String email;

    private String cellPhone;
    private String alternatePhone;
    private String workPhone;
    private String personalEmail;
    private Gender gender;

    private String ssn;

    private LocalDate dateOfBirth;
    private String avatarUrl = "/avatars/default.png";
    private CitizenshipStatus citizenshipStatus;
    private String driverLicense;
    private LocalDate driverLicenseExpiration;
    private Long houseId;
    private LocalDate employmentStartDate;
    private LocalDate employmentEndDate;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private List<Address> addresses = new ArrayList<>();
    private List<Contact> contacts = new ArrayList<>();

    public void addAddress(Address address) {
        addresses.add(address);
    }

    public void removeAddress(Address address) {
        addresses.remove(address);
    }

    public void addContact(Contact contact) {
        contacts.add(contact);
    }

    public void removeContact(Contact contact) {
        contacts.remove(contact);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Employee other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }

    @Override
    public String toString() {
        return "Employee{id=" + id
                + ", userId=" + userId
                + ", firstName='" + firstName + '\''
                + ", lastName='" + lastName + '\''
                + ", email='" + email + '\''
                + ", citizenshipStatus=" + citizenshipStatus
                + '}';
    }
}
