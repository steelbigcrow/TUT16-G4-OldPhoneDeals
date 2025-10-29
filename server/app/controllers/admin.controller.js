const jwt = require('jsonwebtoken');
const User = require('../models/user');
const Phone = require('../models/phone');
const Order = require('../models/order');

// admin login controller
exports.login = async (req, res) => {
    console.log('backend admin.login: received login request, body:', req.body);
    try {
        const { email, password } = req.body;
        // find user and select password field
        const admin = await User.findOne({ email }).select('+password');
        if (!admin) {
            return res.json({ success: false, message: 'invalid email or password' });
        }
        // verify password
        const isMatch = await admin.comparePassword(password);
        if (!isMatch) {
            return res.json({ success: false, message: 'invalid email or password' });
        }
        // check admin identity
        if (!admin.isAdmin) {
            return res.json({ success: false, message: 'admin identity verification failed' });
        }
        // check email verification and account status
        if (!admin.isVerified) {
            return res.json({ success: false, message: 'this email is not verified' });
        }
        if (admin.isDisabled) {
            return res.json({ success: false, message: 'this account is disabled' });
        }
        // generate jwt token: 7 days
        const secret = process.env.JWT_SECRET || process.env.SECRET;
        const token = jwt.sign({ id: admin._id, email: admin.email }, secret, { expiresIn: '7d' });
        // update last login time
        admin.lastLogin = new Date();
        await admin.save();
        // return token and admin info
        res.json({
            success: true,
            token,
            admin: {
                id: admin._id,
                firstName: admin.firstName,
                lastName: admin.lastName,
                email: admin.email
            }
        });
    } catch (err) {
        console.error('backend admin.login: login exception', err);
        res.json({ success: false, message: err.message });
    }
};

// get current admin info
exports.getAdminInfo = async (req, res) => {
    try {
        const adminId = req.decoded.id;
        const admin = await User.findById(adminId).select('-password');
        if (!admin) {
            return res.json({ success: false, message: 'admin not found' });
        }
        res.json({
            success: true,
            admin: {
                id: admin._id,
                firstName: admin.firstName,
                lastName: admin.lastName,
                email: admin.email
            }
        });
    } catch (err) {
        console.error('backend admin.getAdminInfo: get info exception', err);
        res.json({ success: false, message: err.message });
    }
};

// get dashboard stats (Admin)
exports.getDashboardStats = async (req, res) => {
  try {
    const totalUsers = await User.countDocuments({ isAdmin: { $ne: true } });
    const totalListings = await Phone.countDocuments();
    const reviewCountResult = await Phone.aggregate([
      { $project: { reviewCount: { $size: { $ifNull: ['$reviews', []] } } } },
      { $group: { _id: null, total: { $sum: '$reviewCount' } } }
    ]);
    const totalReviews = reviewCountResult.length > 0 ? reviewCountResult[0].total : 0;
    const totalSales = await Order.countDocuments();
    res.json({ success: true, data: { totalUsers, totalListings, totalReviews, totalSales } });
  } catch (err) {
    console.error('backend admin.getDashboardStats: load stats exception', err);
    res.status(500).json({ success: false, message: 'server error' });
  }
};
