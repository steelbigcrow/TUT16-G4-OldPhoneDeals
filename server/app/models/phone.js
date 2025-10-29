const mongoose = require('mongoose');
const Schema = mongoose.Schema;

// Review sub-document schema
// each review will get a default ObjectId
const ReviewSchema = new Schema({
  reviewerId: {
    type: Schema.Types.ObjectId,
    ref: 'User',
    required: true
  },
  rating: {
    type: Number,
    required: true,
    min: 1,
    max: 5
  },
  comment: {
    type: String,
    required: true
  },
  isHidden: {
    type: Boolean,
    default: false
  },
  createdAt: {
    type: Date,
    default: Date.now
  }
});

// Phone main schema
const PhoneSchema = new Schema({
  title: {
    type: String,
    required: true
  },
  brand: {
    type: String,
    required: true,
    enum: ['Samsung', 'Apple', 'HTC', 'Huawei', 'Nokia', 'LG', 'Motorola', 'Sony', 'BlackBerry']
  },
  image: {
    type: String,
    required: true
  },
  stock: {
    type: Number,
    required: true,
    min: 0
  },
  seller: {
    type: Schema.Types.ObjectId,
    ref: 'User',
    required: true
  },
  price: {
    type: Number,
    required: true,
    min: 0
  },
  reviews: [ReviewSchema],
  isDisabled: {
    type: Boolean,
    default: false
  },
  salesCount: {
    type: Number,
    default: 0
  },
  createdAt: {
    type: Date,
    default: Date.now
  },
  updatedAt: {
    type: Date,
    default: Date.now
  }
});

// virtual field: calculate average rating
PhoneSchema.virtual('averageRating').get(function() {
  if (!this.reviews || this.reviews.length === 0) return 0;
  const sum = this.reviews.reduce((acc, review) => acc + review.rating, 0);
  return sum / this.reviews.length;
});

// ensure virtual fields are visible in JSON
PhoneSchema.set('toJSON', { virtuals: true });
PhoneSchema.set('toObject', { virtuals: true });

module.exports = mongoose.model('Phone', PhoneSchema); 