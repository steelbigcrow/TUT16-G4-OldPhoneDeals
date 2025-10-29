const express = require('express');
const router = express.Router();
const profileController = require('../controllers/profile.controller');
const checkJWT = require('../middlewares/checkJWT');


// update user's profile
router.post('/update-profile' , checkJWT , profileController.updateProfile);



module.exports = router;