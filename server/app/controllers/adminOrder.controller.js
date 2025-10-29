const Order = require('../models/order');
const mongoose = require('mongoose');
const ObjectId = mongoose.Types.ObjectId;

// Get all orders (Admin)
exports.getOrders = async (req, res) => {
  try {
    let { userId, startDate, endDate, page = 1, limit = 10, sortBy = 'createdAt', sortOrder = 'desc', searchTerm, brandFilter } = req.query;
    page = parseInt(page, 10);
    limit = parseInt(limit, 10);
    // Validate userId if provided
    if (userId && !ObjectId.isValid(userId)) {
      return res.json({ success: false, message: 'Invalid userId' });
    }
    // Validate page and limit as positive integers
    if (isNaN(page) || page < 1) {
      return res.json({ success: false, message: 'Invalid page number' });
    }
    if (isNaN(limit) || limit < 1) {
      return res.json({ success: false, message: 'Invalid limit number' });
    }

    // Build match conditions for filtering
    const matchConditions = [];
    if (userId && ObjectId.isValid(userId)) {
      matchConditions.push({ userId: ObjectId(userId) });
    }
    if (startDate) {
      const sd = new Date(startDate);
      if (isNaN(sd)) return res.json({ success: false, message: 'Invalid startDate' });
      matchConditions.push({ createdAt: { $gte: sd } });
    }
    if (endDate) {
      const ed = new Date(endDate);
      if (isNaN(ed)) return res.json({ success: false, message: 'Invalid endDate' });
      matchConditions.push({ createdAt: { $lte: ed } });
    }
    if (brandFilter && brandFilter !== 'All Brands') {
      matchConditions.push({ 'items.title': { $regex: new RegExp(brandFilter, 'i') } });
    }
    if (searchTerm) {
      const regex = new RegExp(searchTerm, 'i');
      matchConditions.push({
        $or: [
          { 'items.title': regex },
          { 'user.firstName': regex },
          { 'user.lastName': regex },
          { 'user.email': regex }
        ]
      });
    }

    // Initial aggregation pipeline with user lookup
    const pipeline = [
      { $lookup: { from: 'users', localField: 'userId', foreignField: '_id', as: 'user' } },
      { $unwind: '$user' }
    ];
    if (matchConditions.length) {
      pipeline.push({ $match: { $and: matchConditions } });
    }

    // Count total after filtering
    const countRes = await Order.aggregate([...pipeline, { $count: 'total' }]);
    const total = countRes[0]?.total || 0;

    // Apply sorting, skipping, and limiting
    pipeline.push(
      { $sort: { [sortBy]: sortOrder === 'asc' ? 1 : -1 } },
      { $skip: (page - 1) * limit },
      { $limit: limit }
    );

    // Execute aggregation for data
    const ordersAgg = await Order.aggregate(pipeline);
    const orders = ordersAgg.map(o => ({
      _id: o._id,
      items: o.items,
      totalAmount: o.totalAmount,
      createdAt: o.createdAt,
      userId: {
        firstName: o.user.firstName,
        lastName: o.user.lastName,
        email: o.user.email
      }
    }));

    res.json({ success: true, total, page, limit, orders });
  } catch (err) {
    console.error('adminOrder.getOrders error:', err);
    res.json({ success: false, message: 'Server error' });
  }
};

// Get a specific order (Admin)
exports.getOrder = async (req, res) => {
  try {
    const { orderId } = req.params;
    if (!ObjectId.isValid(orderId)) {
      return res.json({ success: false, message: 'Invalid order ID' });
    }
    const order = await Order.findById(orderId)
      .populate('userId', 'firstName lastName email')
      .lean();
    if (!order) {
      return res.json({ success: false, message: 'Order not found' });
    }
    res.json({ success: true, order });
  } catch (err) {
    console.error('adminOrder.getOrder error:', err);
    res.json({ success: false, message: 'Server error' });
  }
};

// Export orders as CSV or JSON (Admin)
exports.exportOrders = async (req, res) => {
  try {
    let { format = 'csv', userId, startDate, endDate, searchTerm, brandFilter, sortBy = 'createdAt', sortOrder = 'desc' } = req.query;
    // Validate userId if provided
    if (userId && !ObjectId.isValid(userId)) {
      return res.json({ success: false, message: 'Invalid userId' });
    }
    // Build match conditions for filtering
    const matchConditions = [];
    if (userId) {
      matchConditions.push({ userId: ObjectId(userId) });
    }
    if (startDate) {
      const sd = new Date(startDate);
      if (isNaN(sd)) return res.json({ success: false, message: 'Invalid startDate' });
      matchConditions.push({ createdAt: { $gte: sd } });
    }
    if (endDate) {
      const ed = new Date(endDate);
      if (isNaN(ed)) return res.json({ success: false, message: 'Invalid endDate' });
      matchConditions.push({ createdAt: { $lte: ed } });
    }
    if (brandFilter && brandFilter !== 'All Brands') {
      matchConditions.push({ 'items.title': { $regex: new RegExp(brandFilter, 'i') } });
    }
    if (searchTerm) {
      const regex = new RegExp(searchTerm, 'i');
      matchConditions.push({
        $or: [
          { 'items.title': regex },
          { 'user.firstName': regex },
          { 'user.lastName': regex },
          { 'user.email': regex }
        ]
      });
    }

    // Build aggregation pipeline with user lookup and filters
    const pipeline = [
      { $lookup: { from: 'users', localField: 'userId', foreignField: '_id', as: 'user' } },
      { $unwind: '$user' }
    ];
    if (matchConditions.length) {
      pipeline.push({ $match: { $and: matchConditions } });
    }
    // Apply sorting
    pipeline.push({ $sort: { [sortBy]: sortOrder === 'asc' ? 1 : -1 } });

    // Execute aggregation for both CSV and JSON export
    const ordersData = await Order.aggregate(pipeline);

    if (format === 'json') {
      const jsonExport = ordersData.map(order => ({
        timestamp: order.createdAt.toISOString(),
        buyer: `${order.user.firstName} ${order.user.lastName}`,
        items: order.items.map(item => ({ name: item.title, quantity: item.quantity })),
        totalAmount: order.totalAmount
      }));
      res.setHeader('Content-Type', 'application/json');
      res.setHeader('Content-Disposition', 'attachment; filename="orders.json"');
      return res.status(200).send(JSON.stringify(jsonExport, null, 2));
    }

    // CSV export
    const headers = ['Timestamp', 'Buyer', 'Items', 'Total Amount'];
    const escapeCSV = (value) => {
      if (value == null) return '';
      const str = typeof value === 'string' ? value : JSON.stringify(value);
      if (str.includes('"') || str.includes(',') || str.includes('\n')) {
        return '"' + str.replace(/\"/g, '""') + '"';
      }
      return str;
    };
    const header = headers.join(',');
    const rows = ordersData.map(order => {
      const itemsStr = order.items.map(item => `${item.title} x ${item.quantity}`).join('; ');
      const data = [
        order.createdAt.toISOString(),
        `${order.user.firstName} ${order.user.lastName}`,
        itemsStr,
        order.totalAmount
      ];
      return data.map(escapeCSV).join(',');
    });
    const csv = [header, ...rows].join('\n');
    res.setHeader('Content-Type', 'text/csv');
    res.setHeader('Content-Disposition', 'attachment; filename="orders.csv"');
    return res.status(200).send(csv);
  } catch (err) {
    console.error('adminOrder.exportOrders error:', err);
    res.json({ success: false, message: 'Server error' });
  }
};

// Get sales stats (Admin)
exports.getSalesStats = async (req, res) => {
  try {
    const { startDate, endDate } = req.query;
    const query = {};
    if (startDate) {
      const sd = new Date(startDate);
      if (isNaN(sd)) return res.json({ success: false, message: 'Invalid startDate' });
      query.createdAt = { ...(query.createdAt || {}), $gte: sd };
    }
    if (endDate) {
      const ed = new Date(endDate);
      if (isNaN(ed)) return res.json({ success: false, message: 'Invalid endDate' });
      query.createdAt = { ...(query.createdAt || {}), $lte: ed };
    }
    const stats = await Order.aggregate([
      { $match: query },
      { $group: { _id: null, totalSales: { $sum: '$totalAmount' }, totalTransactions: { $sum: 1 } } }
    ]);
    const totalSales = stats[0]?.totalSales || 0;
    const totalTransactions = stats[0]?.totalTransactions || 0;
    res.json({ success: true, totalSales, totalTransactions });
  } catch (err) {
    console.error('adminOrder.getSalesStats error:', err);
    res.json({ success: false, message: 'Server error' });
  }
};
