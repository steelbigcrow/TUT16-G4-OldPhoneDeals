const Phone = require('../models/phone');
const mongoose = require('mongoose');
const AdminLog = require('../models/adminLog');

// Get all reviews (Admin) with optional filters and pagination
exports.getAllReviews = async (req, res) => {
  try {
    let { search, visibility = 'all', reviewerId, phoneId, page = 1, limit = 10, sortBy = 'createdAt', sortOrder = 'desc' } = req.query;
    page = parseInt(page, 10);
    limit = parseInt(limit, 10);
    // Validate reviewerId and phoneId if provided
    if (reviewerId && !mongoose.Types.ObjectId.isValid(reviewerId)) {
      return res.json({ success: false, message: 'Invalid reviewerId' });
    }
    if (phoneId && !mongoose.Types.ObjectId.isValid(phoneId)) {
      return res.json({ success: false, message: 'Invalid phoneId' });
    }
    // Validate page and limit as positive integers
    if (isNaN(page) || page < 1) {
      return res.json({ success: false, message: 'Invalid page number' });
    }
    if (isNaN(limit) || limit < 1) {
      return res.json({ success: false, message: 'Invalid limit number' });
    }
    const matchStage = {};
    // Filter by reviewerId
    if (reviewerId && mongoose.Types.ObjectId.isValid(reviewerId)) {
      matchStage['reviews.reviewerId'] = mongoose.Types.ObjectId(reviewerId);
    }
    // Filter by phoneId
    if (phoneId && mongoose.Types.ObjectId.isValid(phoneId)) {
      matchStage['_id'] = mongoose.Types.ObjectId(phoneId);
    }
    // Unwind reviews to filter content and hidden
    const pipeline = [];
    if (Object.keys(matchStage).length) {
      pipeline.push({ $match: matchStage });
    }
    pipeline.push({ $unwind: '$reviews' });
    // Filter by visibility
    if (visibility === 'visible') {
      pipeline.push({ $match: { 'reviews.isHidden': false } });
    } else if (visibility === 'hidden') {
      pipeline.push({ $match: { 'reviews.isHidden': true } });
    }
    // Lookup reviewer data for full name
    pipeline.push({
      $lookup: {
        from: 'users',
        localField: 'reviews.reviewerId',
        foreignField: '_id',
        as: 'reviewer'
      }
    });
    pipeline.push({ $unwind: { path: '$reviewer', preserveNullAndEmptyArrays: true } });
    // Combined search on comment, phone title, or reviewer full name
    if (search) {
      const regex = { $regex: search, $options: 'i' };
      pipeline.push({
        $match: {
          $or: [
            { 'reviews.comment': regex },
            { 'title': regex },
            { $expr: { $regexMatch: { input: { $concat: ['$reviewer.firstName', ' ', '$reviewer.lastName'] }, regex: search, options: 'i' } } }
          ]
        }
      });
    }
    // Sort
    const sortStage = {};
    sortStage[`reviews.${sortBy}`] = sortOrder === 'asc' ? 1 : -1;
    pipeline.push({ $sort: sortStage });
    // Pagination
    const skip = (page - 1) * limit;
    pipeline.push({ $skip: skip });
    pipeline.push({ $limit: limit });
    // Project desired fields matching front-end interface
    pipeline.push({ $project: {
      _id: 0,
      phoneId: '$_id',
      id:        '$reviews._id',
      content:   '$reviews.comment',
      rating:    '$reviews.rating',
      isVisible: { $cond: [ { $eq: ['$reviews.isHidden', true] }, false, true ] },
      createdAt: '$reviews.createdAt',
      user:      { $concat: ['$reviewer.firstName', ' ', '$reviewer.lastName'] },
      listing:   '$title'
    }});
    const reviews = await Phone.aggregate(pipeline);
    // Count total
    const countPipeline = pipeline.slice(0, pipeline.findIndex(stage => stage.$skip !== undefined));
    countPipeline.push({ $count: 'total' });
    const countResult = await Phone.aggregate(countPipeline);
    const total = countResult[0] ? countResult[0].total : 0;
    res.json({ success: true, total, page, limit, reviews });
  } catch (err) {
    console.error('adminReview.getAllReviews error:', err);
    res.json({ success: false, message: 'Server error' });
  }
};

// Get reviews of a specific phone (Admin)
exports.getReviewsByPhone = async (req, res) => {
  try {
    const { phoneId } = req.params;
    if (!mongoose.Types.ObjectId.isValid(phoneId)) {
      return res.json({ success: false, message: 'Invalid phone ID' });
    }
    const phone = await Phone.findById(phoneId).populate('reviews.reviewerId', 'firstName lastName');
    if (!phone) {
      return res.json({ success: false, message: 'Phone not found' });
    }
    const formatted = phone.reviews.map(r => ({
      reviewId: r._id,
      reviewer: r.reviewerId ? `${r.reviewerId.firstName} ${r.reviewerId.lastName}` : 'Unknown',
      rating: r.rating,
      comment: r.comment,
      isHidden: r.isHidden,
      createdAt: r.createdAt
    }));
    res.json({ success: true, reviews: formatted });
  } catch (err) {
    console.error('adminReview.getReviewsByPhone error:', err);
    res.json({ success: false, message: 'Server error' });
  }
};

// Toggle review visibility (Admin)
exports.toggleReviewVisibility = async (req, res) => {
  try {
    const { phoneId, reviewId } = req.params;
    let { isHidden } = req.body;
    if (typeof isHidden === 'string') {
      if (isHidden === 'true') isHidden = true;
      else if (isHidden === 'false') isHidden = false;
    }
    if (!mongoose.Types.ObjectId.isValid(phoneId) || !mongoose.Types.ObjectId.isValid(reviewId)) {
      return res.json({ success: false, message: 'Invalid ID' });
    }
    if (typeof isHidden !== 'boolean') {
      return res.json({ success: false, message: 'isHidden must be boolean' });
    }
    const phone = await Phone.findById(phoneId);
    if (!phone) {
      return res.json({ success: false, message: 'Phone not found' });
    }
    const review = phone.reviews.id(reviewId);
    if (!review) {
      return res.json({ success: false, message: 'Review not found' });
    }
    review.isHidden = isHidden;
    await phone.save();
    // Record admin action for audit
    await AdminLog.create({
      adminUserId: req.decoded.id,
      action: isHidden ? 'HIDE_REVIEW' : 'SHOW_REVIEW',
      targetType: 'Review',
      targetId: reviewId
    });
    res.json({ success: true, message: 'Review visibility updated successfully' });
  } catch (err) {
    console.error('adminReview.toggleReviewVisibility error:', err);
    res.json({ success: false, message: 'Server error' });
  }
};

// Delete a review (Admin)
exports.deleteReview = async (req, res) => {
  try {
    const { phoneId, reviewId } = req.params;
    if (!mongoose.Types.ObjectId.isValid(phoneId) || !mongoose.Types.ObjectId.isValid(reviewId)) {
      return res.json({ success: false, message: 'Invalid ID' });
    }
    const phone = await Phone.findById(phoneId);
    if (!phone) {
      return res.json({ success: false, message: 'Phone not found' });
    }
    const review = phone.reviews.id(reviewId);
    if (!review) {
      return res.json({ success: false, message: 'Review not found' });
    }
    review.remove();
    await phone.save();
    // Record admin action for audit
    await AdminLog.create({
      adminUserId: req.decoded.id,
      action: 'DELETE_REVIEW',
      targetType: 'Review',
      targetId: reviewId
    });
    res.status(204).send();
  } catch (err) {
    console.error('adminReview.deleteReview error:', err);
    res.json({ success: false, message: 'Server error' });
  }
};
