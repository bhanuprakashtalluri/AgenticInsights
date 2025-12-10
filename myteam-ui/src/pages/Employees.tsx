import { useAuth } from '../services/auth';
import { canAccessPage, Role } from '../services/permissions';

const EmployeesPage = () => {
  const { user } = useAuth();
  if (!user || !canAccessPage(user.role as Role, 'employees')) {
    return <div style={{padding: 32, color: '#c00'}}>Access Denied</div>;
  }

  return (
    <div>
      {/* ...existing code for the employees page... */}
    </div>
  );
};

export default EmployeesPage;
