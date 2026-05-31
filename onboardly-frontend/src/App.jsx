import { AuthProvider } from "./auth/AuthContext";
import {
  ApprovedEmployeeRoute,
  EmployeeRoute,
  HrRoute,
} from "./auth/AuthGuards";
import { Navigate, Route, Routes } from "react-router-dom";
import Login from "./pages/Login";
import Registration from "./pages/employee/Registration";
import Onboarding from "./pages/employee/Onboarding";
import OnboardingDocs from "./pages/employee/OnboardingDocs";
import WaitingReview from "./pages/employee/WaitingReview";
import EmployeeHome from "./pages/employee/EmployeeHome";
import PersonalInfo from "./pages/employee/PersonalInfo";
import VisaStatus from "./pages/employee/VisaStatus";
import Housing from "./pages/employee/Housing";
import FacilityReport from "./pages/employee/FacilityReport";
import HrHome from "./pages/hr/HrHome";
import EmployeeProfiles from "./pages/hr/EmployeeProfiles";
import EmployeeProfile from "./pages/hr/EmployeeProfile";
import HiringManagement from "./pages/hr/HiringManagement";
import HousingManagement from "./pages/hr/HousingManagement";
import HousingDetails from "./pages/hr/HousingDetails";
import VisaStatusManagement from "./pages/hr/VisaStatusManagement";
import FormApplication from "./pages/hr/FormApplication";
import ReceivedDocuments from "./pages/hr/ReceivedDocuments";
import FacilityForm from "./pages/hr/FacilityForm";
import HousingForm from "./pages/hr/HousingForm";

function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/" element={<Navigate to="/login" replace />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Registration />} />
        <Route
          path="/employee/onboarding"
          element={
            <EmployeeRoute>
              <Onboarding />
            </EmployeeRoute>
          }
        />
        <Route
          path="/employee/onboarding/documents"
          element={
            <EmployeeRoute>
              <OnboardingDocs />
            </EmployeeRoute>
          }
        />
        <Route
          path="/employee/waiting-review"
          element={
            <EmployeeRoute>
              <WaitingReview />
            </EmployeeRoute>
          }
        />
        <Route
          path="/employee/home"
          element={
            <ApprovedEmployeeRoute>
              <EmployeeHome />
            </ApprovedEmployeeRoute>
          }
        />
        <Route
          path="/employee/personal-info"
          element={
            <ApprovedEmployeeRoute>
              <PersonalInfo />
            </ApprovedEmployeeRoute>
          }
        />
        <Route
          path="/employee/visa-status"
          element={
            <ApprovedEmployeeRoute>
              <VisaStatus />
            </ApprovedEmployeeRoute>
          }
        />
        <Route
          path="/employee/housing"
          element={
            <ApprovedEmployeeRoute>
              <Housing />
            </ApprovedEmployeeRoute>
          }
        />
        <Route
          path="/employee/facility-reports"
          element={
            <ApprovedEmployeeRoute>
              <FacilityReport />
            </ApprovedEmployeeRoute>
          }
        />
        <Route
          path="/hr/home"
          element={
            <HrRoute>
              <HrHome />
            </HrRoute>
          }
        />
        <Route
          path="/hr/employee-profiles"
          element={
            <HrRoute>
              <EmployeeProfiles />
            </HrRoute>
          }
        />
        <Route
          path="/hr/employee-profile/:employeeId"
          element={
            <HrRoute>
              <EmployeeProfile />
            </HrRoute>
          }
        />
        <Route
          path="/hr/hiring-management"
          element={
            <HrRoute>
              <HiringManagement />
            </HrRoute>
          }
        />
        <Route
          path="/hr/form-application/:id"
          element={
            <HrRoute>
              <FormApplication />
            </HrRoute>
          }
        />
        <Route
          path="/hr/received-documents/:id"
          element={
            <HrRoute>
              <ReceivedDocuments />
            </HrRoute>
          }
        />
        <Route
          path="/hr/housing-management"
          element={
            <HrRoute>
              <HousingManagement />
            </HrRoute>
          }
        />
        <Route
          path="/hr/housing-details/:houseId"
          element={
            <HrRoute>
              <HousingDetails />
            </HrRoute>
          }
        />
        <Route
          path="/hr/housing-form/"
          element={
            <HrRoute>
              <HousingForm />
            </HrRoute>
          }
        />
        <Route
          path="/hr/housing/facility-form/:houseId"
          element={
            <HrRoute>
              <FacilityForm />
            </HrRoute>
          }
        />
        <Route
          path="/hr/visa-status-management"
          element={
            <HrRoute>
              <VisaStatusManagement />
            </HrRoute>
          }
        />
      </Routes>
    </AuthProvider>
  );
}

export default App;
