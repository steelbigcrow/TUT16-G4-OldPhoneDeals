const mongoose = require('mongoose');
const User = require('../models/user');

module.exports = async (req, res, next) => {
  const userId = req.decoded.id;
  // Validate that the decoded id is a valid ObjectId
  if (!mongoose.Types.ObjectId.isValid(userId)) {
    return res.json({ success: false, message: 'Invalid user ID' });
  }

  try {
    const user = await User.findById(userId);
    if (!user || !user.isAdmin) {
      return res.json({ success: false, message: 'Forbidden: Admins only' });
    }
    next();
  } catch (err) {
    console.error('Error in checkAdmin middleware:', err);
    res.json({ success: false, message: 'Server error' });
  }
}; 