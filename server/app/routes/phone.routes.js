const express = require('express');
const router = express.Router();
const phoneController = require('../controllers/phone.controller');
const checkJWT = require('../middlewares/checkJWT');
const saveImage = require('../middlewares/saveImage')
// get phones
router.get('/phones', phoneController.getPhones);

// get phones by seller
router.get('/phones/seller/:sellerId',
    checkJWT,
    phoneController.getPhonesBySeller);

// get a phone
router.get('/phones/:phoneId', phoneController.getPhone);

// get more reviews
router.get('/phones/:phoneId/reviews', phoneController.getMoreReviews);

// add a review
router.post(
  '/phones/:phoneId/reviews',
  checkJWT,
  phoneController.addReview
);

// toggle review visibility
router.patch(
  '/phones/:phoneId/reviews/:reviewId/visibility',
  checkJWT,
  phoneController.toggleReviewVisibility
);

// delete a phone
router.delete('/phones/delete-phone/:phoneId',
    checkJWT,
    phoneController.deletePhone);

// toggle phone disabled status
router.put('/phones/disable-phone/:phoneId',
    checkJWT,
    phoneController.togglePhoneDisabled);

// add a phone
router.post('/phones/add-phone',
    checkJWT,
    phoneController.addPhone);

// save phone image
router.post('/phones/upload-image',
    saveImage,
    phoneController.uploadImage);

// get all reviews of the current user as a seller (including hidden ones)
router.get('/phones/get-reviews-by-id/:sellerId',
    checkJWT,
    phoneController.getPhonesReviewsByUserID);

module.exports = router;
