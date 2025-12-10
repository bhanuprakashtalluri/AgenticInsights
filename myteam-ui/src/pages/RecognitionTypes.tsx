import { useAuth } from '../services/auth';
import { canAccessPage, canPerformAction, Role } from '../services/permissions';

const RecognitionTypesPage = () => {
  const { user } = useAuth();
  if (!user || !canAccessPage(user.role as Role, 'recognitionTypes')) {
    return <div style={{padding: 32, color: '#c00'}}>Access Denied</div>;
  }

  // Example CRUD controls for manager and admin
  const canCreate = canPerformAction(user.role as Role, 'createRecognitionType');
  const canEdit = canPerformAction(user.role as Role, 'editRecognitionType');
  const canDelete = canPerformAction(user.role as Role, 'deleteRecognitionType');

  return (
    <div>
      {/* ...existing code for the recognition types page... */}
      {canCreate && <button>Create Recognition Type</button>}
      {/* For each recognition type row: */}
      {/* {canEdit && <button>Edit</button>} */}
      {/* {canDelete && <button>Delete</button>} */}
    </div>
  );
};

export default RecognitionTypesPage;
