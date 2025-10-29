const express = require('express');
const router = express.Router();
const wishlistRoutes = require('../controllers/wishlist.controller');
const checkJWT = require('../middlewares/checkJWT');

// get wishlist
router.get('/profile/wishlist', checkJWT, wishlistRoutes.getWishlist);

// add product to wishlist
router.post('/profile/wishlist', checkJWT, wishlistRoutes.addToWishlist);

// remove product from wishlist
router.delete('/profile/wishlist/:phoneId', checkJWT, wishlistRoutes.removeFromWishlist);

module.exports = router;
