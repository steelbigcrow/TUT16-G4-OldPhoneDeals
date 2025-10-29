const express = require('express');
const router = express.Router();
const adminPhoneController = require('../controllers/adminPhone.controller');
const checkJWT = require('../middlewares/checkJWT');
const checkAdmin = require('../middlewares/checkAdmin');

// Get all phones (Admin)
router.get('/phones', checkJWT, checkAdmin, adminPhoneController.getPhones);

// Get a specific phone (Admin)
router.get('/phones/:phoneId', checkJWT, checkAdmin, adminPhoneController.getPhone);

// Update a phone (Admin)
router.patch('/phones/:phoneId', checkJWT, checkAdmin, adminPhoneController.updatePhone);

// Delete a phone (Admin)
router.delete('/phones/:phoneId', checkJWT, checkAdmin, adminPhoneController.deletePhone);

module.exports = router;
