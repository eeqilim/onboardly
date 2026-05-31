import { Layout, Menu, Dropdown, Avatar, Button, Space, Typography } from "antd";
import {
	HomeOutlined,
	UserOutlined,
	TeamOutlined,
	IdcardOutlined,
	BankOutlined,
	LogoutOutlined,
    GlobalOutlined,
    SolutionOutlined,
} from "@ant-design/icons";
import { useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import "./Navbar.css";
import logo from "../assets/logo.png";

const { Header } = Layout;

function shouldShowVisaStatus(user) {
    // TODO: Confirm backend profile response field names for citizenship and work authorization.
    const citizenOrGreenCard = user?.citizenOrGreenCard;
    const isCitizenOrResident = user?.isCitizenOrResident;
    const workAuthorization = user?.workAuthorization;

    if (isCitizenOrResident === "YES") return false;
    if (citizenOrGreenCard === "CITIZEN" || citizenOrGreenCard === "GREEN_CARD") {
        return false;
    }
    if (workAuthorization === "US_CITIZEN" || workAuthorization === "GREEN_CARD") {
        return false;
    }

    return true;
}

export default function Navbar() {
	const { isHR, isEmployee, user, logout } = useAuth();
	const location = useLocation();
	const navigate = useNavigate();

	const selectedKey = (() => {
		const path = location.pathname;
		if (path.startsWith("/employee")) return path;
		if (path.startsWith("/hr")) return path;
		return "/";
	})();

	const homePath = isHR ? "/hr/home" : "/employee/home";

	// const employeeItems = [
	// 	{ key: "/employee/home", icon: <HomeOutlined />, label: <Link to="/employee/home">Home</Link> },
	// 	{ key: "/employee/personal-info", icon: <IdcardOutlined />, label: <Link to="/employee/personal-info">Personal Info</Link> },
	// 	{ key: "/employee/onboarding", icon: <FileTextOutlined />, label: <Link to="/employee/onboarding">Onboarding</Link> },
	// 	{ key: "/employee/visa-status", icon: <ProfileOutlined />, label: <Link to="/employee/visa-status">Visa Status</Link> },
	// 	{ key: "/employee/housing", icon: <BankOutlined />, label: <Link to="/employee/housing">Housing</Link> },
	// 	{ key: "/employee/facility-reports", icon: <UserOutlined />, label: <Link to="/employee/facility-reports">Facility Reports</Link> },
	// ];

	// const hrItems = [
	// 	{ key: "/hr/home", icon: <HomeOutlined />, label: <Link to="/hr/home">HR Home</Link> },
	// 	{ key: "/hr/employee-profiles", icon: <TeamOutlined />, label: <Link to="/hr/employee-profiles">Employee Profiles</Link> },
	// 	{ key: "/hr/hiring-management", icon: <ProfileOutlined />, label: <Link to="/hr/hiring-management">Hiring Management</Link> },
	// 	{ key: "/hr/housing-management", icon: <BankOutlined />, label: <Link to="/hr/housing-management">Housing Management</Link> },
	// 	{ key: "/hr/visa-status-management", icon: <IdcardOutlined />, label: <Link to="/hr/visa-status-management">Visa Status</Link> },
	// ];

    const buildNavigationItems = () => {
        if (!user) return [];

        if (isHR) {
        return [
            { key: "/hr/home", icon: <HomeOutlined />, label: 'Home' },
            { key: "/hr/employee-profiles", icon: <TeamOutlined />, label: 'Employee Profiles' },
            { key: "/hr/visa-status-management", icon: <GlobalOutlined />, label: 'Visa Status Management' },
            { key: "/hr/hiring-management", icon: <SolutionOutlined />, label: 'Hiring Management' },
            { key: "/hr/housing-management", icon: <BankOutlined />, label: 'Housing Management' },
        ];
        }

        if (isEmployee) {
        const employeeItems = [
            { key: "/employee/home", icon: <HomeOutlined />, label: 'Home' },
            { key: "/employee/personal-info", icon: <IdcardOutlined />, label: 'Personal Information' },
        ];

        if (shouldShowVisaStatus(user)) {
            employeeItems.push({ 
            key: "/employee/visa-status", 
            icon: <GlobalOutlined />, 
            label: 'Visa Status Management' 
            });
        }

        employeeItems.push({ key: "/employee/housing", icon: <BankOutlined />, label: 'Housing' });
        
        return employeeItems;
        }

        return [];
    };

    const menuItems = buildNavigationItems();

	const handleLogout = () => {
		logout();
		navigate("/login");
	};

	const displayName = user?.username || "Account";

	const userMenu = {
		items: [
			{
				key: "user-name",
				label: (
					<Space>
						<Avatar size="small" icon={<UserOutlined />} />
						<span>{displayName}</span>
					</Space>
				),
				disabled: true,
			},
			{ type: "divider" },
			{
				key: "logout",
				icon: <LogoutOutlined />,
				label: "Logout",
				onClick: handleLogout,
			},
		],
	};

	return (
		<Header className="app-navbar">
			<button
				className="app-navbar-brand"
				type="button"
				onClick={() => navigate(homePath)}
			>
				<img className="app-navbar-logo" src={logo} alt="Onboardly" />
				<Typography.Title className="app-navbar-title" level={5}>Onboardly</Typography.Title>
			</button>

			<Menu
				className="app-navbar-menu"
				mode="horizontal"
				selectedKeys={[selectedKey]}
				items={menuItems}
				onClick={({ key }) => navigate(key)}
			/>

			<div className="app-navbar-account">
				<Dropdown menu={userMenu} placement="bottomRight" arrow>
					<Button type="text">
						<Space>
							<Avatar size="small" icon={<UserOutlined />} />
							<span>{displayName}</span>
						</Space>
					</Button>
				</Dropdown>
			</div>
		</Header>
	);
}
