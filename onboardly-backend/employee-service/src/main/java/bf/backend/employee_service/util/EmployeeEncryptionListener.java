package bf.backend.employee_service.util;

import bf.backend.employee_service.entity.Employee;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeSaveEvent;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmployeeEncryptionListener extends AbstractMongoEventListener<Employee> {

    private final SsnEncryptionConverter ssnConverter;

    @Override
    public void onBeforeSave(BeforeSaveEvent<Employee> event) {
        String plainSsn = event.getSource().getSsn();
        if (plainSsn != null) {
            event.getDocument().put("ssn", ssnConverter.encrypt(plainSsn));
        }
    }

    @Override
    public void onAfterConvert(AfterConvertEvent<Employee> event) {
        String encryptedSsn = event.getDocument().getString("ssn");
        if (encryptedSsn != null) {
            event.getSource().setSsn(ssnConverter.decrypt(encryptedSsn));
        }
    }
}
