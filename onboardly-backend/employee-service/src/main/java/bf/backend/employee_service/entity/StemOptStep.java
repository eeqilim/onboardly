package bf.backend.employee_service.entity;

public enum StemOptStep {
    I_983, I_20, OPT_RECEIPT, OPT_EAD;

    public StemOptStep next() {
        StemOptStep[] vals = values();
        int i = ordinal() + 1;
        return i < vals.length ? vals[i] : null;
    }
}
