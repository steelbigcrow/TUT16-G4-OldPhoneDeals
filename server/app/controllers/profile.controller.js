// routes/profile.js
const express = require('express');
const bcrypt = require('bcryptjs');
const User = require('../models/user');


// update user profile

exports.updateProfile = async (req, res) => {
    try {

        const { id, firstName, lastName, email, password } = req.body;

        // ensure all fields are provided
        if (!email || !firstName || !lastName|| !password) {
            return res.json({ success: false, message: 'All fields are required' });
        }

        // 1. find user
        const userOne = await User.findById(id).select('+password');

        if (!userOne) {
            return res.status(404).json({
                success: false,
                message: 'User not found' });
        }
        // 2. verify current password
        const isMatch = await userOne.comparePassword(password);
        console.error('backend user.resetPassword verify current password', isMatch)
        if (!isMatch) {
            return res.json({ success: false, message: 'Current password is incorrect' });
        }
        // update fields
        userOne.firstName = firstName || userOne.firstName;
        userOne.lastName = lastName || userOne.lastName;
        userOne.email = email || userOne.email;

        await userOne.save();

        return res.json({
            success: true,
            message: 'Profile updated successfully',
            user: {
                id: userOne._id,
                firstName: userOne.firstName,
                lastName: userOne.lastName,
                email: userOne.email
            }
        });
    } catch (err) {
        console.error('Update profile error:', err);
        return res.json({ success: false, message: 'Server error' });
    }
};


