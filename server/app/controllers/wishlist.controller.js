const mongoose = require('mongoose');
const ObjectId = mongoose.Types.ObjectId;
const User = require('../models/user');
const Phone = require('../models/phone');

/**
 * Get wishlist for authenticated user
 * GET /api/profile/wishlist
 */
exports.getWishlist = async (req, res) => {
  try {
    const userId = req.decoded?.id;
    if (!userId) {
      return res.json({ message: 'Unauthorized' });
    }

    const user = await User.findById(userId).populate({ path: 'wishlist', populate: { path: 'seller', select: 'firstName lastName' } });
    if (!user) {
      return res.json({ message: 'User not found' });
    }

    return res.json(user.wishlist);
  } catch (error) {
    console.error('Error getting wishlist:', error);
    return res.json({ message: 'Error getting wishlist', error: error.message });
  }
};

/**
 * Add a phone to wishlist
 * POST /api/profile/wishlist
 */
exports.addToWishlist = async (req, res) => {
  try {
    const userId = req.decoded?.id;
    if (!userId) {
      return res.json({ message: 'Unauthorized' });
    }

    const { phoneId } = req.body;
    if (!phoneId) {
      return res.json({ message: 'Phone ID is required' });
    }
    if (!ObjectId.isValid(phoneId)) {
      return res.json({ message: 'Invalid phone ID' });
    }

    const phone = await Phone.findById(phoneId);
    if (!phone) {
      return res.json({ message: 'Phone not found' });
    }

    const user = await User.findById(userId);
    if (!user) {
      return res.json({ message: 'User not found' });
    }

    if (user.wishlist.some(id => id.toString() === phoneId)) {
      return res.json({ message: 'Item already in wishlist' });
    }

    user.wishlist.push(phoneId);
    await user.save();

    return res.json(user.wishlist);
  } catch (error) {
    console.error('Error adding to wishlist:', error);
    return res.json({ message: 'Error adding to wishlist', error: error.message });
  }
};

/**
 * Remove a phone from wishlist
 * DELETE /api/profile/wishlist/:phoneId
 */
exports.removeFromWishlist = async (req, res) => {
  try {
    const userId = req.decoded?.id;
    if (!userId) {
      return res.json({ message: 'Unauthorized' });
    }

    const { phoneId } = req.params;
    if (!ObjectId.isValid(phoneId)) {
      return res.json({ message: 'Invalid phone ID' });
    }

    const user = await User.findById(userId);
    if (!user) {
      return res.json({ message: 'User not found' });
    }

    const index = user.wishlist.findIndex(id => id.toString() === phoneId);
    if (index === -1) {
      return res.json({ message: 'Item not found in wishlist' });
    }

    user.wishlist.splice(index, 1);
    await user.save();

    return res.json(user.wishlist);
  } catch (error) {
    console.error('Error removing from wishlist:', error);
    return res.json({ message: 'Error removing from wishlist', error: error.message });
  }
};
