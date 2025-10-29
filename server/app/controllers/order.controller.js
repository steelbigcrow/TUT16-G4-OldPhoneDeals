const Order = require('../models/order');
const Cart = require('../models/cart');
const Phone = require('../models/phone');
const mongoose = require('mongoose');
const ObjectId = mongoose.Types.ObjectId;

/**
 * Checkout - create a new order
 * POST /api/orders
 */
exports.checkout = async (req, res) => {
  try {
    const userId = req.decoded?.id;
    if (!userId) {
      return res.json({success: false, message: 'Unauthorized' });
    }

    const address = req.body.address;
    if (!address || !address.street || !address.city || !address.state || !address.zip || !address.country) {
      return res.json({success: false, message: 'Address is required' });
    }

    // Retrieve cart
    const cart = await Cart.findOne({ userId });
    if (!cart || !cart.items.length) {
      return res.json({success: false, message: 'Cart is empty' });
    }

    // Validate each item stock
    for (const item of cart.items) {
      if (!ObjectId.isValid(item.phoneId)) {
        return res.json({success: false, message: `Invalid phone ID ${item.phoneId}` });
      }
      const phone = await Phone.findById(item.phoneId);
      if (!phone) {
        return res.json({success: false, message: `Phone not found: ${item.phoneId}` });
      }
      if (item.quantity > phone.stock) {
        return res.json({success: false, message: `Insufficient stock for phone ${phone._id}` });
      }
    }

    // Calculate total amount
    const totalAmount = cart.items.reduce((sum, item) => sum + item.price * item.quantity, 0);

    // Create and save order
    const order = new Order({
      userId,
      items: cart.items.map(item => ({
        phoneId: item.phoneId,
        title: item.title,
        quantity: item.quantity,
        price: item.price
      })),
      totalAmount,
      address: {
        street: address.street,
        city: address.city,
        state: address.state,
        zip: address.zip,
        country: address.country
      }
    });
    await order.save();

    // Update phone stock and sales count
    for (const item of cart.items) {
      await Phone.findByIdAndUpdate(item.phoneId, {
        $inc: { stock: -item.quantity, salesCount: item.quantity }
      });
    }

    // Clear cart
    cart.items = [];
    await cart.save();

    return res.status(200).json(order);
  } catch (error) {
    console.error('Error during checkout:', error);
    return res.json({success: false, message: 'Error during checkout', error: error.message });
  }
};

/**
 * Get order history (paginated)
 * GET /api/orders
 */
exports.getOrders = async (req, res) => {
  try {
    const userId = req.decoded?.id;
    if (!userId) {
      return res.json({ message: 'Unauthorized' });
    }

    let { page = 1, limit = 10, sortBy = 'createdAt', sortOrder = 'desc' } = req.query;
    const pageNum = parseInt(page, 10);
    const limitNum = parseInt(limit, 10);
    if (isNaN(pageNum) || pageNum < 1) {
      return res.json({ message: 'Invalid page number' });
    }
    if (isNaN(limitNum) || limitNum < 1) {
      return res.json({ message: 'Invalid limit number' });
    }
    const skip = (pageNum - 1) * limitNum;
    const sortOptions = { [sortBy]: sortOrder === 'asc' ? 1 : -1 };

    const query = { userId };
    const total = await Order.countDocuments(query);
    const orders = await Order.find(query)
      .sort(sortOptions)
      .skip(skip)
      .limit(limitNum);

    return res.json({
      orders,
      total,
      totalPages: Math.ceil(total / limitNum),
      currentPage: pageNum
    });
  } catch (error) {
    console.error('Error getting orders:', error);
    return res.json({ message: 'Error getting orders', error: error.message });
  }
};

/**
 * Get specific order details
 * GET /api/orders/:orderId
 */
exports.getOrder = async (req, res) => {
  try {
    const userId = req.decoded?.id;
    if (!userId) {
      return res.json({ message: 'Unauthorized' });
    }

    const { orderId } = req.params;
    if (!ObjectId.isValid(orderId)) {
      return res.json({ message: 'Invalid order ID' });
    }

    const order = await Order.findById(orderId);
    if (!order) {
      return res.json({ message: 'Order not found' });
    }
    if (order.userId.toString() !== userId) {
      return res.json({ message: 'Forbidden' });
    }

    return res.json(order);
  } catch (error) {
    console.error('Error getting order:', error);
    return res.json({ message: 'Error getting order', error: error.message });
  }
};
