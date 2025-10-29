const Phone = require('../models/phone');
const User = require('../models/user');
const mongoose = require('mongoose');
const ObjectId = mongoose.Types.ObjectId;
const jwt = require('jsonwebtoken');

/**
 * Get all phones with optional filtering, sorting, and pagination
 * GET /api/phones
 */
exports.getPhones = async (req, res) => {
  try {
    const {
      search = '',
      brand = '',
      maxPrice = '',
      sortBy = 'createdAt',
      sortOrder = 'desc',
      page = 1,
      limit = 10,
      special = '' // soldOutSoon, bestSellers
    } = req.query;

    const pageNum = parseInt(page, 10);
    const limitNum = parseInt(limit, 10);
    // Validate page and limit as positive integers
    if (isNaN(pageNum) || pageNum < 1) {
      return res.json({ 
        message: 'Invalid page number' });
    }
    if (isNaN(limitNum) || limitNum < 1) {
      return res.json({ 
        message: 'Invalid limit number' });
    }
    const skip = (pageNum - 1) * limitNum;
    
    // Base query - exclude disabled phones
    let query = { isDisabled: false };
    
    // Search by title
    if (search) {
      query.title = { $regex: search, $options: 'i' };
    }
    
    // Filter by brand
    if (brand) {
      query.brand = brand;
    }
    
    // Filter by max price
    if (maxPrice) {
      query.price = { $lte: parseFloat(maxPrice) };
    }
    
    // Handle special queries
    if (special === 'soldOutSoon') {
      // Get phones with low stock (but more than 0)
      query.stock = { $gt: 0 };
      return res.json(
        await Phone.find(query)
          .sort({ stock: 1 })
          .populate('seller', 'firstName lastName')
          .limit(5)
        );
    } 
    else if (special === 'bestSellers') {
      // Get phones with highest average rating and at least 2 reviews

      // Stage 1: Aggregate to find the top 5 best-selling phone IDs and their average ratings.
      const bestPhonesAggregatedData = await Phone.aggregate([
        { $match: { isDisabled: false, "reviews.1": { $exists: true } } }, // Ensure at least 2 reviews
        { $addFields: { calculatedAvgRating: { $avg: "$reviews.rating" } } }, // Calculate average rating
        { $sort: { calculatedAvgRating: -1 } }, // Sort by average rating
        { $limit: 5 },
        { $project: { _id: 1, calculatedAvgRating: 1 } } // Project only IDs and their calculated average rating for sorting
      ]);

      const phoneIds = bestPhonesAggregatedData.map(p => p._id);

      // Stage 2: Fetch the full phone documents for these IDs and populate seller information.
      // The Mongoose virtual 'averageRating' (defined in phone.js model) should be included due to schema settings (toJSON: { virtuals: true }).
      let phones = await Phone.find({ _id: { $in: phoneIds }, isDisabled: false })
                              .populate('seller', 'firstName lastName');
      
      // Stage 3: Sort the fetched phones according to the aggregation order.
      // The `averageRating` virtual field from the schema will be used by the frontend.
      // We sort them here to maintain the "bestSellers" order.
      phones.sort((a, b) => {
        const aData = bestPhonesAggregatedData.find(p => p._id.equals(a._id));
        const bData = bestPhonesAggregatedData.find(p => p._id.equals(b._id));
        // Handle cases where a phone might not be in aggregated data (though unlikely here)
        const ratingA = aData ? aData.calculatedAvgRating : 0;
        const ratingB = bData ? bData.calculatedAvgRating : 0;
        return ratingB - ratingA; // Descending order for best sellers
      });
      
      return res.json(phones);
    }
    
    // Sort options
    const sortOptions = {};
    sortOptions[sortBy] = sortOrder === 'asc' ? 1 : -1;
    
    // Get total count for pagination
    const total = await Phone.countDocuments(query);
    
    // Get phones with pagination
    const phones = await Phone.find(query)
      .sort(sortOptions)
      .skip(skip)
      .limit(limitNum)
      .populate('seller', 'firstName lastName');
    
    return res.json({
      phones,
      totalPages: Math.ceil(total / limitNum),
      currentPage: pageNum,
      total
    });
  } catch (error) {
    console.error('Error getting phones:', error);
    return res.json({ message: 'Error getting phones', error: error.message });
  }
};

/**
 * Get a specific phone by ID
 * GET /api/phones/:phoneId
 */
exports.getPhone = async (req, res) => {
  try {
    // Optional authentication: decode token if present to allow review visibility for reviewer and seller
    const secret = process.env.JWT_SECRET || process.env.SECRET;
    const authHeader = req.headers.authorization;
    if (authHeader && authHeader.startsWith('Bearer ')) {
      const token = authHeader.split(' ')[1];
      try {
        req.decoded = jwt.verify(token, secret);
      } catch (err) {
        // Invalid token; ignore and treat as unauthenticated

      }
    }
    const { phoneId } = req.params;
    
    if (!ObjectId.isValid(phoneId)) {
      return res.json({success:false, message: 'Invalid phone ID' });
    }
    
    const phone = await Phone.findOne({
      _id: phoneId,
      isDisabled: false
    }).populate('seller', 'firstName lastName');
    
    if (!phone) {
      return res.json({
        success:false,
        message: 'Phone not found' });
    }
    
    // filter current request visible reviews
    const visibleReviews = phone.reviews.filter(review => {
      // case 1: review is not hidden (public)
      if (!review.isHidden) return true;

      // case 2: need to ensure logged in
      if (!req.decoded) return false;

      // case 3: seller can always see all reviews of their own phone
      if (req.decoded.id === phone.seller._id.toString()) return true;

      // case 4: reviewer can always see their own review (even if hidden)
      if (req.decoded.id === review.reviewerId.toString()) return true;

      // other users → filter out
      return false;
    });
    
    // 2) batch query reviewer details of these reviews
    const reviewerIds = visibleReviews.map(r => r.reviewerId);
    const reviewers = await User.find(
      { _id: { $in: reviewerIds } },
      'firstName lastName'
    );
    const reviewersMap = reviewers.reduce((map, reviewer) => {
      map[reviewer._id.toString()] = {
        firstName: reviewer.firstName,
        lastName: reviewer.lastName
      };
      return map;
    }, {});
    
    // 3) build final return object, only contain the first 3 reviews
    const formattedPhone = {
      ...phone.toObject(),
      // Seller is already populated with firstName and lastName via .populate()
      reviews: visibleReviews
        .map(review => ({
          ...review.toObject(),
          reviewer: reviewersMap[review.reviewerId.toString()]
            ? `${reviewersMap[review.reviewerId.toString()].firstName} ${reviewersMap[review.reviewerId.toString()].lastName}`
            : 'Unknown User'
        }))
        .slice(0, 3)
    };
    
    return res.json(formattedPhone);
  } catch (error) {
    console.error('Error getting phone:', error);
    return res.json({success:false, message: 'Error getting phone', error: error.message });
  }
};

/**
 * Get more reviews for a specific phone
 * GET /api/phones/:phoneId/reviews
 */
exports.getMoreReviews = async (req, res) => {
  try {
    // Optional authentication: decode token if present to allow review visibility for reviewer and seller
    const secret = process.env.JWT_SECRET || process.env.SECRET;
    const authHeader = req.headers.authorization;
    if (authHeader && authHeader.startsWith('Bearer ')) {
      const token = authHeader.split(' ')[1];
      try {
        req.decoded = jwt.verify(token, secret);
      } catch (err) {
        // Invalid token; ignore
      }
    }
    const { phoneId } = req.params;
    const { page = 1, limit = 3 } = req.query;
    
    if (!ObjectId.isValid(phoneId)) {
      return res.json({ message: 'Invalid phone ID' });
    }
    
    const pageNum = parseInt(page, 10);
    const limitNum = parseInt(limit, 10);
    // Validate page and limit as positive integers
    if (isNaN(pageNum) || pageNum < 1) {
      return res.json({ message: 'Invalid page number' });
    }
    if (isNaN(limitNum) || limitNum < 1) {
      return res.json({ message: 'Invalid limit number' });
    }
    const skip = (pageNum - 1) * limitNum;
    
    const phone = await Phone.findOne({ 
      _id: phoneId,
      isDisabled: false
    });
    
    if (!phone) {
      return res.json({ message: 'Phone not found' });
    }
    
    // Filter visible reviews or those visible to the current user
    const filteredReviews = phone.reviews.filter(review => {
      // case 1: review is not hidden (public)
      if (!review.isHidden) return true;

      // case 2: need to ensure logged in
      if (!req.decoded) return false;

      // case 3: seller can always see all reviews of their own phone
      if (req.decoded.id === phone.seller.toString()) return true;

      // case 4: reviewer can always see their own review (even if hidden)
      if (req.decoded.id === review.reviewerId.toString()) return true;

      // other users → filter out
      return false;
    });
    
    // Apply pagination
    const paginatedReviews = filteredReviews.slice(skip, skip + limitNum);
    
    // Get reviewer information
    const reviewerIds = paginatedReviews.map(review => review.reviewerId);
    const reviewers = await User.find(
      { _id: { $in: reviewerIds } },
      'firstName lastName'
    );
    
    const reviewersMap = reviewers.reduce((map, reviewer) => {
      map[reviewer._id.toString()] = {
        firstName: reviewer.firstName,
        lastName: reviewer.lastName
      };
      return map;
    }, {});
    
    // Format reviews with reviewer info
    const formattedReviews = paginatedReviews.map(review => ({
      ...review.toObject(),
      reviewer: reviewersMap[review.reviewerId.toString()] 
        ? `${reviewersMap[review.reviewerId.toString()].firstName} ${reviewersMap[review.reviewerId.toString()].lastName}`
        : 'Unknown User'
    }));
    
    return res.json({
      reviews: formattedReviews,
      totalReviews: filteredReviews.length,
      currentPage: pageNum,
      totalPages: Math.ceil(filteredReviews.length / limitNum)
    });
  } catch (error) {
    console.error('Error getting reviews:', error);
    return res.json({ message: 'Error getting reviews', error: error.message });
  }
};

/**
 * Add a review to a phone
 * POST /api/phones/:phoneId/reviews
 */
exports.addReview = async (req, res) => {
  try {
    // User must be authenticated
    if (!req.decoded) {
      return res.json({ message: 'Unauthorized' });
    }
    
    const { phoneId } = req.params;
    const { rating, comment } = req.body;
    const userId = req.decoded.id;
    
    if (!ObjectId.isValid(phoneId)) {
      return res.json({ message: 'Invalid phone ID' });
    }
    
    // Validate input
    if (!rating || !comment) {
      return res.json({ message: 'Rating and comment are required' });
    }
    
    const ratingNum = parseInt(rating, 10);
    if (isNaN(ratingNum) || ratingNum < 1 || ratingNum > 5) {
      return res.json({ message: 'Rating must be between 1 and 5' });
    }
    
    // Find the phone
    const phone = await Phone.findOne({ 
      _id: phoneId,
      isDisabled: false
    });
    
    if (!phone) {
      return res.json({ message: 'Phone not found' });
    }
    
    // Check if user has already reviewed this phone
    const existingReview = phone.reviews.find(
      review => review.reviewerId.toString() === userId
    );
    
    if (existingReview) {
      return res.json({ message: 'You have already reviewed this phone' });
    }
    
    // Check if user is trying to review their own phone
    if (phone.seller.toString() === userId) {
      return res.json({ message: 'You cannot review your own phone' });
    }
    
    // Add the review
    const newReview = {
      reviewerId: userId,
      rating: ratingNum,
      comment,
      createdAt: new Date()
    };
    
    // Push and save the phone, then retrieve the persisted subdocument
    phone.reviews.push(newReview);
    await phone.save();
    // Retrieve the newly saved review subdocument (last element)
    const savedReview = phone.reviews[phone.reviews.length - 1];
    
    // Get reviewer information
    const reviewer = await User.findById(userId, 'firstName lastName');
    // Format and return the new review, including its generated _id and default isHidden
    const formattedReview = {
      _id: savedReview._id,
      reviewerId: savedReview.reviewerId,
      rating: savedReview.rating,
      comment: savedReview.comment,
      createdAt: savedReview.createdAt,
      isHidden: savedReview.isHidden,
      reviewer: reviewer ? `${reviewer.firstName} ${reviewer.lastName}` : 'Unknown User'
    };
    return res.status(201).json(formattedReview);
  } catch (error) {
    console.error('Error adding review:', error);
    return res.json({ message: 'Error adding review', error: error.message });
  }
};

/**
 * Toggle visibility of a review
 * PATCH /api/phones/:phoneId/reviews/:reviewId/visibility
 */
exports.toggleReviewVisibility = async (req, res) => {
  try {
    // User must be authenticated
    if (!req.decoded) {
      return res.json({ message: 'Unauthorized' });
    }
    
    const { phoneId, reviewId } = req.params;
    let { isHidden } = req.body;
    if (typeof isHidden === 'string') {
      if (isHidden === 'true') isHidden = true;
      else if (isHidden === 'false') isHidden = false;
    }
    const userId = req.decoded.id;
    
    if (!ObjectId.isValid(phoneId) || !ObjectId.isValid(reviewId)) {
      return res.json({
        success: false,
        message: 'Invalid phone or review ID' });
    }
    
    if (typeof isHidden !== 'boolean') {
      return res.json({
        success: false,
        message: 'isHidden must be a boolean value' });
    }
    
    // Find the phone
    const phone = await Phone.findById(phoneId);
    
    if (!phone) {
      return res.json({
        success: false,
        message: 'Phone not found' });
    }
    
    // Find the review
    const reviewIndex = phone.reviews.findIndex(
      review => review._id.toString() === reviewId
    );
    
    if (reviewIndex === -1) {
      return res.json({
        success: false,
        message: 'Review not found' });
    }
    
    const review = phone.reviews[reviewIndex];
    
    // Check if user is authorized to toggle visibility (must be the reviewer or the seller)
    if (
      review.reviewerId.toString() !== userId && 
      phone.seller.toString() !== userId
    ) {
      return res.json({
        success: false,
        message: 'You are not authorized to change this review visibility' 
      });
    }
    
    // Update visibility
    phone.reviews[reviewIndex].isHidden = isHidden;
    
    await phone.save();
    
    return res.json({
      success: true,
      message: 'Review visibility updated successfully' });
  } catch (error) {
    console.error('Error toggling review visibility:', error);
    return res.json({
      success: false,
      message: 'Error toggling review visibility', 
      error: error.message 
    });
  }
};

/**
 * Get all phones by seller
 * GET /api/phones/by-seller/:sellerId
 */
exports.getPhonesBySeller = async (req, res) => {

  try {
    // User must be authenticated
    if (!req.decoded) {
      return res.json({ message: 'Unauthorized' });
    }

    const { sellerId } = req.params;

    if (!ObjectId.isValid(sellerId)) {
      return res.json({ message: 'Invalid seller ID' });
    }

    // query all phones of this seller
    const phones = await Phone.find({ seller: sellerId })
        .populate('seller', 'firstName lastName') // Populate seller's first and last name
        .exec();

    if (!phones || phones.length === 0) {
      return res.json({ message: 'No phones found for this seller' });
    }

    return res.json(phones);
  } catch (error) {
    console.error('Error getting phones by seller:', error);
    return res.json({ message: 'Error getting phones by seller', error: error.message });
  }
};


exports.deletePhone = async (req, res) => {
  try {
    // User must be authenticated
    if (!req.decoded) {
      return res.json({ message: 'Unauthorized' });
    }
    const { phoneId } = req.params;

    if (!ObjectId.isValid(phoneId)) {
      return res.json({ message: 'Invalid phone ID' });
    }

    const deletedPhone = await Phone.findByIdAndDelete(phoneId);

    if (!deletedPhone) {
      return res.json({ message: 'Phone not found' });
    }

    return res.json({ message: 'Phone deleted successfully' });
  } catch (error) {
    console.error('Error deleting phone:', error);
    return res.json({ message: 'Error deleting phone', error: error.message });
  }
};

/**
 * Toggle phone's isDisabled status
 * PUT /api/phones/disable-phone/:phoneId
 */
exports.togglePhoneDisabled = async (req, res) => {
  try {

    // User must be authenticated
    if (!req.decoded) {
      return res.json({ message: 'Unauthorized' });
    }

    const { phoneId } = req.params;
    const { isDisabled } = req.body;

    console.log("Backend isDisabled", isDisabled)

    if (!ObjectId.isValid(phoneId)) {
      return res.json({ message: 'Invalid phone ID' });
    }

    if (typeof isDisabled !== 'boolean') {
      return res.json({ message: 'isDisabled must be a boolean value' });
    }

    const PhoneOne = await Phone.findById(phoneId);
    if (!PhoneOne) {
      return res.json({ message: 'Phone not found' });
    }

    PhoneOne.isDisabled = isDisabled;
    await PhoneOne.save();


    return res.json({
      success: true,
      message: 'Phone updated successfully',
      phone: PhoneOne });
  } catch (error) {
    console.error('Error toggling phone status:', error);
    return res.json({ message: 'Error updating phone status', error: error.message });
  }
};

exports.addPhone = async (req, res) => {
  try {
    // User must be authenticated
    if (!req.decoded) {
      return res.json({
        success: false,
        message: 'Unauthorized' });
    }

    const { title, brand, image, stock, price, seller } = req.body;

    console.log("Backend addPhone ",req.body)
    // parameter validation
    if (!title || !brand || !image ||  !stock  ||  !price  || !seller) {

      return res.json({
        success: false,
        message: 'Missing or invalid required fields' });
    }

    // todo: more detailed parameter validation logic


    if (!ObjectId.isValid(seller)) {
      return res.json({
        success: false,
        message: 'Invalid seller ID' });
    }

    // create new phone object
    const newPhone = new Phone({
      title,
      brand,
      image,
      stock,
      price,
      seller,
      isDisabled: false
    });

    await newPhone.save();

    return res.json({
      success: true,
      message: 'Phone created successfully',
      phone: newPhone });
  } catch (error) {
    console.error('Error adding phone:', error);
    return res.json({
      success: false,
      message: 'Error adding phone', error: error.message });
  }
};

// upload phone image
exports.uploadImage = async (req, res) => {
  try {
    if (!req.file) {
      return res.json({ success: false, message: 'No file uploaded' });
    }
    console.log("Backend uploadImage check if there is a file ",req.file)

    const fileName = req.file.filename; // filename decided by multer
    const fileUrl = `/static/images/${fileName}`;

    return res.json({
      success: true,
      message: 'Image uploaded successfully',
      url: fileUrl
    });
  } catch (err) {
    console.log('Upload error:', err);
    return res.json({ success: false, message: 'Upload failed' });
  }
};

/**
 * get all reviews of current user's phones (including hidden)
 * GET /api/phones/reviews/get-reviews-by-id
 */
exports.getPhonesReviewsByUserID = async (req, res) => {

  try {
    if (!req.decoded) {
      return res.json({ message: 'Unauthorized: No user data' });
    }

    // precondition: this interface needs authentication middleware to extract user ID
    const sellerId = req.decoded.id; 

    // find all phones of this seller
    const phones = await Phone.find({ seller: sellerId });


    const allReviews = [];
    for (const phone of phones) {
      const reviews = phone.reviews.map(review => ({
        ...review.toObject(),
        phoneId: phone._id,
        phoneTitle: phone.title,
        // seller can always see all reviews
        isVisible: true
      }));
      allReviews.push(...reviews);
    }

    // query user info by reviewerId
    const reviewerIds = [...new Set(allReviews.map(r => r.reviewerId.toString()))];
    const reviewers = await User.find({ _id: { $in: reviewerIds } }, 'firstName lastName');
    const reviewersMap = Object.fromEntries(
        reviewers.map(user => [user._id.toString(), `${user.firstName} ${user.lastName}`])
    );

    const formatted = allReviews.map(r => ({
      ...r,
      reviewer: reviewersMap[r.reviewerId.toString()] || 'Unknown',
    }));
    return res.json({ success: true, message: 'got review list successfully' ,reviews: formatted});

  } catch (err) {
    console.error('Error fetching reviews:', err);
    return res.json({ message: 'Error fetching reviews', error: err.message });
  }
};
