const jwt = require('jsonwebtoken');
const User = require('../models/User'); // 根据你项目的路径修改
require('dotenv').config();

module.exports = async function(req, res, next) {
  const secret = process.env.JWT_SECRET || process.env.SECRET;
  const authHeader = req.headers.authorization;
  const token = authHeader && authHeader.startsWith('Bearer ') ? authHeader.split(' ')[1] : null;

  if (!token) {
    return res.status(401).json({ success: false, message: 'No token provided' });
  }

  try {
    const decoded = jwt.verify(token, secret);

    // 检查用户是否存在
    const user = await User.findById(decoded.id);
    if (!user) {
      console.log("JWT valid but user not found in DB (possibly deleted)");
      return res.json({ success: false, message: 'User not found' });
    }

    // attach user info to req
    req.decoded = decoded;
    next();
  } catch (err) {
    console.log("JWT verification failed:", err.message);
    return res.json({ success: false, message: 'Failed to authenticate token' });
  }
};