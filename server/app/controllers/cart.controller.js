const Cart = require('../models/cart');
const Phone = require('../models/phone');
const User = require('../models/user');
const mongoose = require('mongoose');
const ObjectId = mongoose.Types.ObjectId;

/**
 * Get cart contents for authenticated user
 * GET /api/cart
 */
exports.getCart = async (req, res) => {
  try {
    // authentication (token checkJWT middleware should set req.decoded)
    const userId = req.decoded?.id;
    if (!userId) {
      return res.json({success: false, message: 'Unauthorized' });
    }

    let cart = await Cart.findOne({ userId });
    if (!cart) {
      cart = new Cart({ userId, items: [] });
      await cart.save();
    }

    // add seller info and rating data
    const enhancedItems = await Promise.all(cart.items.map(async (item) => {
      const phone = await Phone.findById(item.phoneId);
      if (!phone) return item;

      // calculate average rating
      let averageRating = 0;
      if (phone.reviews && phone.reviews.length > 0) {
        const sum = phone.reviews.reduce((acc, review) => acc + review.rating, 0);
        averageRating = sum / phone.reviews.length;
      }

      // get seller info
      const seller = await User.findById(phone.seller, { firstName: 1, lastName: 1 });

      return {
        ...item.toObject(),
        averageRating,
        reviewCount: phone.reviews ? phone.reviews.length : 0,
        seller: seller ? {
          _id: seller._id,
          firstName: seller.firstName,
          lastName: seller.lastName
        } : null
      };
    }));

    // return cart data
    const enhancedCart = {
      ...cart.toObject(),
      items: enhancedItems
    };

    return res.json(enhancedCart);
  } catch (error) {
    console.error('Error getting cart:', error);
    return res.json({ success: false, message: 'Error getting cart', error: error.message });
  }
};

/**
 * Add or update an item in the cart
 * POST /api/cart/items
 */
exports.addOrUpdateItem = async (req, res) => {
  try {
    const userId = req.decoded?.id;
    if (!userId) {
      return res.json({success: false, message: 'Unauthorized' });
    }

    const { phoneId, quantity } = req.body;
    if (!phoneId || quantity == null) {
      return res.json({ success: false, message: 'Phone ID and quantity are required' });
    }
    if (!ObjectId.isValid(phoneId)) {
      return res.json({success: false, message: 'Invalid phone ID' });
    }
    const qty = parseInt(quantity, 10);
    if (isNaN(qty) || qty < 1) {
      return res.json({ success: false, message: 'Quantity must be at least 1' });
    }

    const phone = await Phone.findById(phoneId);
    if (!phone) {
      return res.json({success: false, message: 'Phone not found' });
    }
    if (qty > phone.stock) {
      return res.json({success: false, message: 'Insufficient stock' });
    }

    let cart = await Cart.findOne({ userId });
    if (!cart) {
      cart = new Cart({ userId, items: [] });
    }

    const itemIndex = cart.items.findIndex(item => item.phoneId.toString() === phoneId);
    if (itemIndex > -1) {
      cart.items[itemIndex].quantity = qty;
      cart.items[itemIndex].price = phone.price;
    } else {
      cart.items.push({
        phoneId,
        title: phone.title,
        quantity: qty,
        price: phone.price
      });
    }

    await cart.save();
    return res.json(cart);
  } catch (error) {
    console.error('Error adding/updating cart item:', error);
    return res.json({success: false, message: 'Error adding/updating cart item', error: error.message });
  }
};

/**
 * Update the quantity of an existing cart item
 * PATCH /api/cart/items/:phoneId
 */
exports.updateItemQuantity = async (req, res) => {
  try {
    const userId = req.decoded?.id;
    if (!userId) {
      return res.json({success: false, message: 'Unauthorized' });
    }

    const { phoneId } = req.params;
    const { quantity } = req.body;
    if (!ObjectId.isValid(phoneId)) {
      return res.json({ message: 'Invalid phone ID' });
    }
    const qty = parseInt(quantity, 10);
    if (isNaN(qty) || qty < 1) {
      return res.json({ message: 'Quantity must be at least 1' });
    }

    const phone = await Phone.findById(phoneId);
    if (!phone) {
      return res.json({ message: 'Phone not found' });
    }
    if (qty > phone.stock) {
      return res.json({ message: 'Insufficient stock' });
    }

    let cart = await Cart.findOne({ userId });
    if (!cart) {
      return res.json({ message: 'Cart not found' });
    }

    const itemIndex = cart.items.findIndex(item => item.phoneId.toString() === phoneId);
    if (itemIndex === -1) {
      return res.json({ message: 'Item not found in cart' });
    }

    cart.items[itemIndex].quantity = qty;
    await cart.save();
    return res.json(cart);
  } catch (error) {
    console.error('Error updating cart item:', error);
    return res.json({ message: 'Error updating cart item', error: error.message });
  }
};

/**
 * Remove an item from the cart
 * DELETE /api/cart/items/:phoneId
 */
exports.removeItemFromCart = async (req, res) => {
  try {
    const userId = req.decoded?.id;
    if (!userId) {
      return res.json({ message: 'Unauthorized' });
    }

    const { phoneId } = req.params;
    if (!ObjectId.isValid(phoneId)) {
      return res.json({ message: 'Invalid phone ID' });
    }

    let cart = await Cart.findOne({ userId });
    if (!cart) {
      return res.json({ message: 'Cart not found' });
    }

    const itemIndex = cart.items.findIndex(item => item.phoneId.toString() === phoneId);
    if (itemIndex === -1) {
      return res.json({ message: 'Item not found in cart' });
    }

    cart.items.splice(itemIndex, 1);
    await cart.save();
    return res.json(cart);
  } catch (error) {
    console.error('Error removing cart item:', error);
    return res.json({ message: 'Error removing cart item', error: error.message });
  }
}; 