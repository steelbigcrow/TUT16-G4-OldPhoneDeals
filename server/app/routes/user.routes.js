const express = require('express');
const router = express.Router();
const userController = require('../controllers/user.controller');

// login
router.post('/login', userController.login);

// register
router.post('/register', userController.register);

router.get('/verify-email', userController.verifyEmail);

// check verification status
router.get('/check-verified', userController.checkVerified);

router.post('/send-reset-password-email', userController.sendRestPasswordEmail);

// get user info
router.get('/user-info', userController.getUserInfo);


// reset password
router.post('/reset-password', userController.resetPassword);


module.exports = router;
