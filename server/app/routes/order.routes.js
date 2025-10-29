const express = require('express');
const router = express.Router();
const orderController = require('../controllers/order.controller');
const checkJWT = require('../middlewares/checkJWT');

// Checkout - create a new order
router.post('/orders', checkJWT, orderController.checkout);

// Get order history (paginated)
router.get('/orders', checkJWT, orderController.getOrders);

// Get specific order details
router.get('/orders/:orderId', checkJWT, orderController.getOrder);

module.exports = router;
