const express = require('express');
const router = express.Router();
const adminUserController = require('../controllers/adminUser.controller');
const checkJWT = require('../middlewares/checkJWT');
const checkAdmin = require('../middlewares/checkAdmin');

// get all users
router.get('/users', checkJWT, checkAdmin, adminUserController.getUsers);

// get a user
router.get('/users/:userId', checkJWT, checkAdmin, adminUserController.getUser);

// update a user
router.patch('/users/:userId', checkJWT, checkAdmin, adminUserController.updateUser);

// Add endpoint to toggle user disabled status
router.patch('/users/:userId/status', checkJWT, checkAdmin, adminUserController.updateUserStatus);

// delete a user
router.delete('/users/:userId', checkJWT, checkAdmin, adminUserController.deleteUser);

// get all phones of a user
router.get('/users/:userId/phones', checkJWT, checkAdmin, adminUserController.getUserPhones);

// get all reviews of a user
router.get('/users/:userId/reviews', checkJWT, checkAdmin, adminUserController.getUserReviews);

module.exports = router; 