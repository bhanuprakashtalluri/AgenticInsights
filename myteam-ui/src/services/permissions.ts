// Centralized permissions utility
export type Role = 'employee' | 'teamlead' | 'manager' | 'admin';

export const PAGE_PERMISSIONS: Record<string, Role[]> = {
  dashboard: ['employee', 'teamlead', 'manager', 'admin'],
  leaderboard: ['employee', 'teamlead', 'manager', 'admin'],
  employees: ['manager', 'admin'],
  recognitions: ['teamlead', 'manager', 'admin'],
  recognitionTypes: ['manager', 'admin'], // allow manager
};

export const ACTION_PERMISSIONS = {
  sendRecognition: ['teamlead', 'manager', 'admin'],
  editRecognition: ['manager', 'admin'],
  deleteRecognition: ['admin'],
  viewRecognition: ['employee', 'teamlead', 'manager', 'admin'],
  viewEmployees: ['manager', 'admin'],
  viewRecognitionTypes: ['manager', 'admin'], // allow manager
  createRecognitionType: ['manager', 'admin'],
  editRecognitionType: ['manager', 'admin'],
  deleteRecognitionType: ['manager', 'admin'],
};

export function canAccessPage(role: Role, page: string) {
  return PAGE_PERMISSIONS[page]?.includes(role);
}

export function canPerformAction(role: Role, action: keyof typeof ACTION_PERMISSIONS) {
  return ACTION_PERMISSIONS[action]?.includes(role);
}
