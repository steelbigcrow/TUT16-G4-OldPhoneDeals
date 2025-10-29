const express = require('express');
const router = express.Router();
const adminOrderController = require('../controllers/adminOrder.controller');
const checkJWT = require('../middlewares/checkJWT');
const checkAdmin = require('../middlewares/checkAdmin');

// Export orders as CSV (Admin)
router.get('/orders/export', checkJWT, checkAdmin, adminOrderController.exportOrders);

// Sales stats for orders (Admin)
router.get('/orders/stats', checkJWT, checkAdmin, adminOrderController.getSalesStats);

// Get all orders (Admin)
router.get('/orders', checkJWT, checkAdmin, adminOrderController.getOrders);

// Get a specific order (Admin)
router.get('/orders/:orderId', checkJWT, checkAdmin, adminOrderController.getOrder);

module.exports = router;
