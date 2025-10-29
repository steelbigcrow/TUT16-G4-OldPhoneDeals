const express = require('express');
const router = express.Router();
const adminController = require('../controllers/admin.controller');
const checkJWT = require('../middlewares/checkJWT');
const checkAdmin = require('../middlewares/checkAdmin');

// admin login
router.post('/auth/login', adminController.login);

// get current admin info
router.get('/auth/info', checkJWT, checkAdmin, adminController.getAdminInfo);

// dashboard stats
router.get('/stats', checkJWT, checkAdmin, adminController.getDashboardStats);

module.exports = router;
