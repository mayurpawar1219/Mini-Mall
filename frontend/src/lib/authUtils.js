/**
 * Returns the default dashboard or landing route based on the user's role.
 * 
 * @param {string} role - The user's role (e.g., 'CUSTOMER', 'STAFF', 'ADMIN')
 * @returns {string} The route to navigate to
 */
export const getDefaultRouteForRole = (role) => {
  switch (role) {
    case 'ADMIN':
      return '/admin';
    case 'STAFF':
      return '/staff';
    case 'CUSTOMER':
      return '/dashboard'; // Customer dashboard
    default:
      return '/'; // Fallback to home
  }
};

/**
 * Returns a user-friendly display name for a given role.
 * 
 * @param {string} role - The user's role
 * @returns {string} The display name
 */
export const getRoleDisplayName = (role) => {
  switch (role) {
    case 'ADMIN':
      return 'Administrator';
    case 'STAFF':
      return 'Staff';
    case 'CUSTOMER':
      return 'Customer';
    default:
      return 'User';
  }
};
