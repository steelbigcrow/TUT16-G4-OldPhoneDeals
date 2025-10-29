const express = require('express');
const router = express.Router();
const cartController = require('../controllers/cart.controller');
const checkJWT = require('../middlewares/checkJWT');

// get cart
router.get('/cart', checkJWT, cartController.getCart);

// add or update item
router.post('/cart/items', checkJWT, cartController.addOrUpdateItem);

// update item quantity
router.patch('/cart/items/:phoneId', checkJWT, cartController.updateItemQuantity);

// remove item
router.delete('/cart/items/:phoneId', checkJWT, cartController.removeItemFromCart);

module.exports = router; 