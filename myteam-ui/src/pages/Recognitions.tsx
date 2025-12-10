import { useAuth } from '../services/auth';
import { canPerformAction, Role } from '../services/permissions';

const RecognitionsPage = () => {
  const { user } = useAuth();
  if (!user || !canPerformAction(user.role as Role, 'viewRecognition')) {
    return <div style={{padding: 32, color: '#c00'}}>Access Denied</div>;
  }

  return (
    <div>
      {/* Example usage for send/edit/delete buttons: */}
      {/* {canPerformAction(user.role as Role, 'sendRecognition') && <button>Send Recognition</button>} */}
      {/* {canPerformAction(user.role as Role, 'editRecognition') && <button>Edit</button>} */}
      {/* {canPerformAction(user.role as Role, 'deleteRecognition') && <button>Delete</button>} */}
    </div>
  );
};

export default RecognitionsPage;
