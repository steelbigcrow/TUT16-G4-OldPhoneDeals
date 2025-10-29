const Phone = require('../models/phone');
const mongoose = require('mongoose');
const AdminLog = require('../models/adminLog');

// Get all phones (Admin)
exports.getPhones = async (req, res) => {
  try {
    let {
      searchTerm,
      brand,
      sellerId,
      page = '1',
      limit = '10',
      sortBy,
      sortOrder
    } = req.query;

    const pageNum = Math.max(Number(page), 1);
    const limitNum = Math.max(Number(limit), 1);

    if (sellerId && !mongoose.Types.ObjectId.isValid(sellerId)) {
      return res.json({ success: false, message: 'Invalid sellerId' });
    }

    const query = {};

    if (searchTerm) {
      query.title = { $regex: searchTerm, $options: 'i' };
    }

    if (brand) {
      query.brand = brand;
    }

    if (sellerId) {
      query.seller = mongoose.Types.ObjectId(sellerId);
    }

    const skip = (pageNum - 1) * limitNum;

    const total = await Phone.countDocuments(query);
    const phones = await Phone.find(query)
        .sort({ [sortBy]: sortOrder === 'asc' ? 1 : -1 })
        .skip(skip)
        .limit(limitNum)
        .populate('seller', 'firstName lastName email')
        .populate({
          path: 'reviews',
          select: 'comment createdAt isHidden rating reviewerId',
          populate: {
            path: 'reviewerId',
            select: 'firstName lastName'
          }
        })
        .lean();

    return res.json({ success: true, total, page: pageNum, limit: limitNum, phones });

  } catch (err) {
    console.error('adminPhone.getPhones error:', err);
    return res.json({ success: false, message: 'Server error' });
  }
};


// Get a specific phone (Admin)
exports.getPhone = async (req, res) => {
  try {
    const { phoneId } = req.params;
    if (!mongoose.Types.ObjectId.isValid(phoneId)) {
      return res.json(
          { success: false, message: 'Invalid phone ID' });
    }
    const phone = await Phone.findById(req.params.phoneId)
      .populate('seller', 'firstName lastName email')
      .lean();
    if (!phone) {
      return res.json({ success: false, message: 'Phone not found' });
    }
    res.json({ success: true, phone });
  } catch (err) {
    console.error('adminPhone.getPhone error:', err);
    res.json({ success: false, message: 'Server error' });
  }
};

// Update a phone (Admin) with admin log
exports.updatePhone = async (req, res) => {
  try {


    const adminId = req.body.adminId; //
    if (!adminId) {
      return res.json({ success: false, message: 'admin id not found' });
    }

    const phone = await Phone.findById( req.params.phoneId);

    if (!phone) {
      return res.json({ success: false, message: 'Phone not found' });
    }
    const { title, brand, image, stock, price, isDisabled } = req.body;

    // record initial state
    const originalIsDisabled = phone.isDisabled;
    const fieldsUpdated = [];

    // field change logic
    if (typeof title !== 'undefined' && title !== phone.title) {
      phone.title = title;
      fieldsUpdated.push('title');
    }

    if (typeof brand !== 'undefined' && brand !== phone.brand) {
      phone.brand = brand;
      fieldsUpdated.push('brand');
    }

    if (typeof image !== 'undefined' && image !== phone.image) {
      phone.image = image;
      fieldsUpdated.push('image');
    }

    if (typeof stock !== 'undefined' && stock !== phone.stock) {
      phone.stock = stock;
      fieldsUpdated.push('stock');
    }

    if (typeof price !== 'undefined' && price !== phone.price) {
      phone.price = price;
      fieldsUpdated.push('price');
    }

    let logAction = null;
    if (typeof isDisabled !== 'undefined' && isDisabled !== originalIsDisabled) {
      phone.isDisabled = isDisabled;
      // if only isDisabled is updated
      if (fieldsUpdated.length === 0) {
        logAction = isDisabled ? 'DISABLE_PHONE' : 'ENABLE_PHONE';
      } else {
        fieldsUpdated.push('isDisabled'); // also join field update
      }
    }

    phone.updatedAt = new Date();

    await phone.save();

    // determine log action
    if (!logAction) {
      if (fieldsUpdated.length > 0) {
        logAction = 'UPDATE_PHONE';
      }
    }
    if (logAction) {
      await AdminLog.create({
        adminUserId: adminId,
        action: logAction,
        targetType: 'Phone',
        targetId: phone._id
      });
    }


    res.json({ success: true, message: 'Phone updated successfully' });
  } catch (err) {
    console.error('adminPhone.updatePhone error:', err);
    res.json({ success: false, message: 'Server error' });
  }
};




// Delete a phone (Admin) with admin log
exports.deletePhone = async (req, res) => {
  try {
    const { phoneId } = req.params;
    const { adminId } = req.query;


    if (!mongoose.Types.ObjectId.isValid(phoneId)) {
      return res.json({ success: false, message: 'Invalid phone ID' });
    }

    const phone = await Phone.findById(phoneId);
    if (!phone) {
      return res.json({ success: false, message: 'Phone not found' });
    }

    // admin log
    await AdminLog.create({
      adminUserId: adminId,          // admin identity
      action: 'DELETE_PHONE',             // operation type
      targetType: 'Phone',                // operation object type
      targetId: phone._id                 // deleted phone id
    });

    await phone.deleteOne();
    return res.json({ success: true, message: 'Phone deleted successfully' });

  } catch (err) {
    console.error('adminPhone.deletePhone error:', err);
    return res.json({ success: false, message: 'Server error' });
  }
};

