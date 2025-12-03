const User = require('../models/user');
const Phone = require('../models/phone');
const mongoose = require('mongoose');
const Cart = require('../models/cart');
const Order = require('../models/order');
const AdminLog = require('../models/adminLog');

// get users list (Admin)
exports.getUsers = async (req, res) => {
  try {
    const { search, page = 1, limit = 10, sortBy = 'lastLogin', sortOrder = 'desc', isDisabled } = req.query;
    const pageNum = parseInt(page, 10);
    const limitNum = parseInt(limit, 10);
    if (isNaN(pageNum) || pageNum < 1) {
      return res.json({ success: false, message: 'Invalid page number' });
    }
    if (isNaN(limitNum) || limitNum < 1) {
      return res.json({ success: false, message: 'Invalid limit number' });
    }
    const query = { isAdmin: false };
    if (typeof isDisabled !== 'undefined') {
      if (isDisabled === 'true') {
        query.isDisabled = true;
      } else if (isDisabled === 'false') {
        query.isDisabled = false;
      }
    }
    if (search) {
      query.$or = [
        { firstName: { $regex: search, $options: 'i' } },
        { lastName: { $regex: search, $options: 'i' } },
        { email: { $regex: search, $options: 'i' } }
      ];
    }
    const skip = (pageNum - 1) * limitNum;
    const total = await User.countDocuments(query);
    const users = await User.find(query)
      .select('-password -verifyToken -wishlist')
      .sort({ [sortBy]: sortOrder === 'asc' ? 1 : -1 })
      .skip(skip)
      .limit(limitNum);

    res.json({ success: true, total, page: pageNum, limit: limitNum, users });
  } catch (err) {
    console.error('adminUser.getUsers error:', err);
    res.json({ success: false, message: 'Server error' });
  }
};

// get single user (Admin)
exports.getUser = async (req, res) => {
  try {
    const { userId } = req.params;
    if (!mongoose.Types.ObjectId.isValid(userId)) {
      return res.json({ success: false, message: 'Invalid user ID' });
    }
    const user = await User.findById(req.params.userId)
      .select('-password -verifyToken -wishlist');
    if (!user) {
      return res.json({ success: false, message: 'User not found' });
    }
    res.json({ success: true, user });
  } catch (err) {
    console.error('adminUser.getUser error:', err);
    res.json({ success: false, message: 'Server error' });
  }
};

// update user info (Admin)
exports.updateUser = async (req, res) => {
  try {
    const { userId } = req.params;
    if (!mongoose.Types.ObjectId.isValid(userId)) {
      return res.json({ success: false,  message:'Invalid user ID'});
    }
    const { firstName, lastName, email, isDisabled } = req.body;
    const user = await User.findById(req.params.userId);
    if (!user) {
      return res.json({ success: false, message: 'User not found' });
    }
    if (user.isAdmin) {
      return res.json({ success: false, message: 'Cannot edit admin user' });
    }
    if (email && email !== user.email) {
      const existing = await User.findOne({ email });
      if (existing) {
        return res.json({ success: false, message: 'Email already in use' });
      }
      user.email = email;
    }
    if (firstName) user.firstName = firstName;
    if (lastName) user.lastName = lastName;
    if (typeof isDisabled === 'boolean') user.isDisabled = isDisabled;
    user.updatedAt = new Date();
    await user.save();

    // Write admin log
    await AdminLog.create({
      adminUserId: req.decoded.id,
      action: 'UPDATE_USER',
      targetType: 'User',
      targetId: user._id
    });

    res.json({ success: true, message: 'User updated successfully' });
  } catch (err) {
    console.error('adminUser.updateUser error:', err);
    res.json({ success: false, message: 'Server error' });
  }
};

// Add controller to update disabled status of a user (Admin)
exports.updateUserStatus = async (req, res) => {
  try {
    const { userId } = req.params;
    const { isDisabled } = req.body;
    if (!mongoose.Types.ObjectId.isValid(userId)) {
      return res.json({ success: false, message: 'Invalid user ID' });
    }
    const user = await User.findById(userId);
    if (!user) {
      return res.json({ success: false, message: 'User not found' });
    }
    if (user.isAdmin) {
      return res.json({ success: false, message: 'Cannot change status of admin user' });
    }
    if (typeof isDisabled !== 'boolean') {
      return res.json({ success: false, message: 'Invalid status value' });
    }
    user.isDisabled = isDisabled;
    user.updatedAt = new Date();
    await user.save();

    // Write admin log
    await AdminLog.create({
      adminUserId: req.decoded.id,
      action: isDisabled ? 'DISABLE_USER' : 'ENABLE_USER',
      targetType: 'User',
      targetId: user._id
    });

    res.json({ success: true, message: `User ${isDisabled ? 'disabled' : 'activated'} successfully` });
  } catch (err) {
    console.error('adminUser.updateUserStatus error:', err);
    res.json({ success: false, message: 'Server error' });
  }
};

// delete user (Admin)
exports.deleteUser = async (req, res) => {
  try {
    const { userId } = req.params;
    if (!mongoose.Types.ObjectId.isValid(userId)) {
      return res.json({ success: false, message: 'Invalid user ID' });
    }
    const user = await User.findById(req.params.userId);
    if (!user) {
      return res.json({ success: false, message: 'User not found' });
    }
    if (user.isAdmin) {
      return res.json({ success: false, message: 'Cannot delete admin user' });
    }
    // Cascade delete related data
    const userIdObj = user._id;
    // 1. Remove user's reviews from all phones
    await Phone.updateMany({}, { $pull: { reviews: { reviewerId: userIdObj } } });
    // 2. Delete phones sold by this user
    const phones = await Phone.find({ seller: userIdObj }).select('_id');
    const phoneIds = phones.map(p => p._id);
    await Phone.deleteMany({ seller: userIdObj });
    // 3. Remove deleted phones from other users' wishlists
    await User.updateMany(
      { wishlist: { $in: phoneIds } },
      { $pull: { wishlist: { $in: phoneIds } } }
    );
    // 4. Delete this user's cart and orders
    await Cart.deleteOne({ userId: userIdObj });
    await Order.deleteMany({ userId: userIdObj });
    // 5. Remove deleted phones from other carts and orders
    await Cart.updateMany(
      { 'items.phoneId': { $in: phoneIds } },
      { $pull: { items: { phoneId: { $in: phoneIds } } } }
    );
    await Order.updateMany(
      { 'items.phoneId': { $in: phoneIds } },
      { $pull: { items: { phoneId: { $in: phoneIds } } } }
    );
    // 6. Delete orders with no items remaining
    await Order.deleteMany({ items: { $size: 0 } });
    // 7. Finally delete the user document
    await user.deleteOne();

    // Write admin log
    await AdminLog.create({
      adminUserId: req.decoded.id,
      action: 'DELETE_USER',
      targetType: 'User',
      targetId: userIdObj
    });

    res.json({ success: true, message: 'User deleted successfully' });
  } catch (err) {
    console.error('adminUser.deleteUser error:', err);
    res.json({ success: false, message: 'Server error' });
  }
};

// get user's phone list (Admin)
exports.getUserPhones = async (req, res) => {
  try {
    const { userId } = req.params;
    if (!mongoose.Types.ObjectId.isValid(userId)) {
      return res.json({ success: false, message: 'Invalid user ID' });
    }

    const user = await User.findById(userId);
    if (!user) {
      return res.json({ success: false, message: 'User not found' });
    }

    const pageNum = parseInt(req.query.page, 10) || 1;
    const limitNum = parseInt(req.query.limit, 10) || 10;
    if (isNaN(pageNum) || pageNum < 1) {
      return res.json({ success: false, message: 'Invalid page number' });
    }
    if (isNaN(limitNum) || limitNum < 1) {
      return res.json({ success: false, message: 'Invalid limit number' });
    }

    const allowedSortFields = ['createdAt', 'price', 'stock'];
    const sortBy = allowedSortFields.includes(req.query.sortBy) ? req.query.sortBy : 'createdAt';
    if (req.query.sortBy && !allowedSortFields.includes(req.query.sortBy)) {
      return res.json({ success: false, message: 'Invalid sortBy. Allowed: createdAt, price, stock' });
    }
    const sortOrder = req.query.sortOrder === 'asc'
      ? 1
      : req.query.sortOrder === 'desc' || !req.query.sortOrder
        ? -1
        : null;
    if (sortOrder === null) {
      return res.json({ success: false, message: 'Invalid sortOrder. Allowed: asc, desc' });
    }

    const brandFilter = req.query.brand && req.query.brand !== 'all' ? req.query.brand : null;
    const skip = (pageNum - 1) * limitNum;
    const filter = { seller: new mongoose.Types.ObjectId(userId) };
    if (brandFilter) {
      filter.brand = brandFilter;
    }

    const pipeline = [
      { $match: filter },
      {
        $addFields: {
          reviewsCount: { $size: { $ifNull: ['$reviews', []] } },
          averageRating: {
            $cond: [
              { $gt: [{ $size: { $ifNull: ['$reviews', []] } }, 0] },
              { $avg: '$reviews.rating' },
              0
            ]
          }
        }
      },
      { $sort: { [sortBy]: sortOrder } },
      { $skip: skip },
      { $limit: limitNum },
      {
        $project: {
          _id: 1,
          title: 1,
          brand: 1,
          price: 1,
          stock: 1,
          isDisabled: 1,
          createdAt: 1,
          updatedAt: 1,
          averageRating: 1,
          reviewsCount: 1
        }
      }
    ];

    const [phones, total] = await Promise.all([
      Phone.aggregate(pipeline),
      Phone.countDocuments(filter)
    ]);

    res.json({ success: true, total, page: pageNum, limit: limitNum, phones });
  } catch (err) {
    console.error('adminUser.getUserPhones error:', err);
    res.json({ success: false, message: 'Server error' });
  }
};

// get user's reviews (Admin)
exports.getUserReviews = async (req, res) => {
  try {
    const { userId } = req.params;
    if (!mongoose.Types.ObjectId.isValid(userId)) {
      return res.json({ success: false, message: 'Invalid user ID' });
    }
    const user = await User.findById(userId);
    if (!user) {
      return res.json({ success: false, message: 'User not found' });
    }

    const pageNum = parseInt(req.query.page, 10) || 1;
    const limitNum = parseInt(req.query.limit, 10) || 10;
    if (isNaN(pageNum) || pageNum < 1) {
      return res.json({ success: false, message: 'Invalid page number' });
    }
    if (isNaN(limitNum) || limitNum < 1) {
      return res.json({ success: false, message: 'Invalid limit number' });
    }

    const allowedSortFields = ['rating', 'createdAt'];
    const sortBy = allowedSortFields.includes(req.query.sortBy) ? req.query.sortBy : 'createdAt';
    if (req.query.sortBy && !allowedSortFields.includes(req.query.sortBy)) {
      return res.json({ success: false, message: 'Invalid sortBy. Allowed: rating, createdAt' });
    }
    const sortOrder = req.query.sortOrder === 'asc'
      ? 1
      : req.query.sortOrder === 'desc' || !req.query.sortOrder
        ? -1
        : null;
    if (sortOrder === null) {
      return res.json({ success: false, message: 'Invalid sortOrder. Allowed: asc, desc' });
    }

    const brandFilter = req.query.brand && req.query.brand !== 'all' ? req.query.brand : null;
    const skip = (pageNum - 1) * limitNum;
    const objectUserId = new mongoose.Types.ObjectId(userId);
    const matchConditions = { 'reviews.reviewerId': objectUserId };
    if (brandFilter) {
      matchConditions.brand = brandFilter;
    }

    const pipeline = [
      { $match: matchConditions },
      {
        $addFields: {
          reviewsCount: { $size: { $ifNull: ['$reviews', []] } },
          averageRating: {
            $cond: [
              { $gt: [{ $size: { $ifNull: ['$reviews', []] } }, 0] },
              { $avg: '$reviews.rating' },
              0
            ]
          }
        }
      },
      { $unwind: '$reviews' },
      { $match: { 'reviews.reviewerId': objectUserId } },
      { $sort: sortBy === 'rating' ? { 'reviews.rating': sortOrder } : { 'reviews.createdAt': sortOrder } },
      { $skip: skip },
      { $limit: limitNum },
      {
        $project: {
          _id: 0,
          reviewId: '$reviews._id',
          phoneId: '$_id',
          phoneTitle: '$title',
          phoneBrand: '$brand',
          phonePrice: '$price',
          phoneStock: '$stock',
          averageRating: '$averageRating',
          reviewsCount: '$reviewsCount',
          reviewRating: '$reviews.rating',
          reviewComment: '$reviews.comment',
          reviewCreatedAt: '$reviews.createdAt',
          isHidden: '$reviews.isHidden'
        }
      }
    ];

    const reviews = await Phone.aggregate(pipeline);

    const countMatch = { 'reviews.reviewerId': objectUserId };
    if (brandFilter) {
      countMatch.brand = brandFilter;
    }
    const countPipeline = [
      { $match: countMatch },
      { $unwind: '$reviews' },
      { $match: { 'reviews.reviewerId': objectUserId } },
      { $count: 'total' }
    ];
    const countResult = await Phone.aggregate(countPipeline);
    const total = countResult.length > 0 ? countResult[0].total : 0;

    res.json({ success: true, total, page: pageNum, limit: limitNum, reviews });
  } catch (err) {
    console.error('adminUser.getUserReviews error:', err);
    res.json({ success: false, message: 'Server error' });
  }
};