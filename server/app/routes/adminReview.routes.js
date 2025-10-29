const express = require('express');
const router = express.Router();
const adminReviewController = require('../controllers/adminReview.controller');
const checkJWT = require('../middlewares/checkJWT');
const checkAdmin = require('../middlewares/checkAdmin');

// Get all reviews (Admin)
router.get('/reviews', checkJWT, checkAdmin, adminReviewController.getAllReviews);

// Get reviews of a specific phone (Admin)
router.get('/phones/:phoneId/reviews', checkJWT, checkAdmin, adminReviewController.getReviewsByPhone);

// Toggle review visibility (Admin)
router.patch(
  '/phones/:phoneId/reviews/:reviewId/visibility',
  checkJWT,
  checkAdmin,
  adminReviewController.toggleReviewVisibility
);

// Delete a review (Admin)
router.delete(
  '/phones/:phoneId/reviews/:reviewId',
  checkJWT,
  checkAdmin,
  adminReviewController.deleteReview
);

module.exports = router;
