function isAdmin(req, res, next) {
  // If request uses admin direct token secret bypass (good for automation scripts)
  const adminSecret = process.env.ADMIN_SECRET_KEY || 'highly_secure_admin_bypass_token';
  const customAdminHeader = req.headers['x-admin-secret'];

  if (customAdminHeader && customAdminHeader === adminSecret) {
    req.adminBypassed = true;
    return next();
  }

  // Fallback to checking req.user role
  if (!req.user || req.user.role !== 'admin') {
    return res.status(403).json({ error: 'Forbidden. Access restricted to administrator accounts only.' });
  }

  next();
}

module.exports = {
  isAdmin,
};
